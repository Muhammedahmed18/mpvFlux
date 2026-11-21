package app.marlboroadvance.mpvex.utils.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import app.marlboroadvance.mpvex.database.dao.FolderIndexDao
import app.marlboroadvance.mpvex.database.entities.FolderIndexEntity
import app.marlboroadvance.mpvex.database.entities.FolderVideoEntity
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Persistent per-folder index (Room).
 *
 * - Each folder is stored independently: aggregates of its DIRECT videos plus one row per
 *   direct video, so opening a folder can render from disk with no MediaStore/filesystem scan.
 * - The index is seeded once by a library-wide pass ([seedIfEmpty]) that stores folder rows only;
 *   a folder's video rows are stored the first time that folder is opened ([updateFolderVideos]).
 * - After that only affected folders are re-read ([refreshFolder]); [reconcileAll] is a cheap
 *   diff-and-write-changes pass used for unscoped "library changed" events.
 * - Recursive totals for the tree view are rolled up in memory from the stored rows.
 *
 * All work runs on [Dispatchers.IO].
 */
class FolderIndexRepository(
  private val context: Context,
  private val dao: FolderIndexDao,
) {
  /** Folder with RECURSIVE totals (tree view). */
  data class RolledFolder(
    val path: String,
    val name: String,
    val videoCount: Int,
    val totalSize: Long,
    val totalDuration: Long,
    val lastModified: Long,
    val hasSubfolders: Boolean,
  )

  private val writeMutex = Mutex()
  private val seedMutex = Mutex()

  // Bumped on every write; guards the in-memory rollup against being stored stale.
  private val version = AtomicLong(0)

  @Volatile
  private var lastSeedAttempt = 0L

  @Volatile
  private var rollupCache: Map<String, RolledFolder>? = null

  companion object {
    private const val TAG = "FolderIndexRepository"
    private const val SEED_RETRY_INTERVAL_MS = 30_000L

    fun get(): FolderIndexRepository = KoinJavaComponent.get(FolderIndexRepository::class.java)
  }

  // ---------------------------------------------------------------------------------------
  // Album view (folders that directly contain videos)
  // ---------------------------------------------------------------------------------------

  /** Emits the album-view folder list whenever any folder row changes. */
  fun observeVideoFolders(): Flow<List<VideoFolder>> =
    dao.observeAllFolders().map { rows -> rows.toVideoFolders() }

  /** Cached folders; seeds the index first if it has never been built. */
  suspend fun getVideoFolders(): List<VideoFolder> =
    withContext(Dispatchers.IO) {
      seedIfEmpty()
      dao.getAllFolders().toVideoFolders()
    }

  suspend fun isEmpty(): Boolean = withContext(Dispatchers.IO) { dao.folderCount() == 0 }

  // ---------------------------------------------------------------------------------------
  // Seeding / reconciling (library-wide, background only)
  // ---------------------------------------------------------------------------------------

  /** Builds the folder rows with one library pass if the index is empty. */
  suspend fun seedIfEmpty() =
    withContext(Dispatchers.IO) {
      if (dao.folderCount() > 0) return@withContext
      seedMutex.withLock {
        if (dao.folderCount() > 0) return@withLock
        // A library with no videos at all stays "empty": don't rescan on every call.
        val now = System.currentTimeMillis()
        if (now - lastSeedAttempt < SEED_RETRY_INTERVAL_MS) return@withLock
        lastSeedAttempt = now
        reconcileAll()
      }
    }

  /**
   * One library-wide pass that writes ONLY the folders whose aggregates changed and removes
   * folders that no longer exist. Per-video rows of unchanged folders are left alone.
   */
  suspend fun reconcileAll() =
    withContext(Dispatchers.IO) {
      val scanned = FolderViewScanner.scanAllFolders(context)
      if (scanned == null) {
        Log.w(TAG, "Library scan failed, keeping existing index")
        return@withContext
      }

      writeMutex.withLock {
        val existing = dao.getAllFolders().associateBy { it.path }
        val now = System.currentTimeMillis()

        val upserts = ArrayList<FolderIndexEntity>()
        for ((path, data) in scanned) {
          val old = existing[path]
          if (old != null &&
            old.name == data.name &&
            old.videoCount == data.videoCount &&
            old.totalSize == data.totalSize &&
            old.totalDuration == data.totalDuration &&
            old.lastModified == data.lastModified &&
            old.hasSubfolders == data.hasSubfolders
          ) {
            continue
          }
          upserts +=
            FolderIndexEntity(
              path = path,
              name = data.name,
              videoCount = data.videoCount,
              totalSize = data.totalSize,
              totalDuration = data.totalDuration,
              lastModified = data.lastModified,
              hasSubfolders = data.hasSubfolders,
              // Keep the stored video rows/signature: they are revalidated the next time the
              // folder is opened, and the signature mismatch triggers a rewrite then.
              signature = old?.signature ?: "",
              videosIndexedAt = old?.videosIndexedAt ?: 0L,
              lastIndexed = now,
            )
        }

        // An empty scan can also mean "permission missing / MediaStore not ready": in that case
        // only drop folders whose directory is really gone.
        val removed =
          existing.keys.filter { path ->
            path !in scanned && (scanned.isNotEmpty() || !File(path).exists())
          }

        if (upserts.isNotEmpty() || removed.isNotEmpty()) {
          dao.applyChanges(upserts, removed)
          invalidateRollup()
        }
        Log.d(TAG, "Reconciled index: ${upserts.size} upserted, ${removed.size} removed, ${scanned.size} scanned")
      }
    }

  // ---------------------------------------------------------------------------------------
  // Per-folder access
  // ---------------------------------------------------------------------------------------

  /**
   * Cached direct videos of [folderPath], or null when the folder's videos haven't been
   * indexed yet. Never touches MediaStore or the filesystem.
   */
  suspend fun getCachedVideos(folderPath: String): List<Video>? =
    withContext(Dispatchers.IO) {
      val folder = dao.getFolder(folderPath) ?: return@withContext null
      if (folder.videosIndexedAt == 0L) return@withContext null
      dao
        .getVideos(folderPath)
        .map { it.toVideo(folder.path) }
        .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

  /**
   * Stores freshly scanned [videos] for [folderPath]. A no-op when nothing changed
   * (same signature), so revalidating on every open costs one read and no write.
   */
  suspend fun updateFolderVideos(
    folderPath: String,
    videos: List<Video>,
  ) = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      val existing = dao.getFolder(folderPath)
      if (existing == null && videos.isEmpty()) return@withLock

      val signature = signatureOf(videos)
      if (existing != null && existing.videosIndexedAt > 0L && existing.signature == signature) {
        return@withLock
      }

      val now = System.currentTimeMillis()
      val folder =
        FolderIndexEntity(
          path = folderPath,
          name = File(folderPath).name,
          videoCount = videos.size,
          totalSize = videos.sumOf { it.size },
          totalDuration = videos.sumOf { it.duration },
          lastModified = videos.maxOfOrNull { it.dateModified } ?: 0L,
          hasSubfolders = existing?.hasSubfolders ?: false,
          signature = signature,
          videosIndexedAt = now,
          lastIndexed = now,
        )
      dao.replaceFolderWithVideos(folder, videos.map { it.toEntity(folderPath) })
      invalidateRollup()
    }
  }

  /** Re-reads ONE folder (live) and writes it to the index if it changed. */
  suspend fun refreshFolder(folderPath: String) =
    withContext(Dispatchers.IO) {
      if (!File(folderPath).exists()) {
        removeSubtree(folderPath)
        return@withContext
      }
      val videos = VideoScanUtils.getVideosInFolder(context, folderPath)
      updateFolderVideos(folderPath, videos)
    }

  /**
   * Refreshes only the folders affected by [paths] (folder paths or video file paths).
   */
  suspend fun refreshAffected(paths: Collection<String>) =
    withContext(Dispatchers.IO) {
      val folders =
        paths
          .mapNotNull { p ->
            val file = File(p)
            val isVideoFile = FileTypeUtils.VIDEO_EXTENSIONS.contains(file.extension.lowercase(Locale.getDefault()))
            if (isVideoFile) file.parent else p
          }.toSet()
      for (folder in folders) {
        try {
          refreshFolder(folder)
        } catch (e: Exception) {
          if (e is kotlinx.coroutines.CancellationException) throw e
          Log.w(TAG, "Failed to refresh $folder", e)
        }
      }
    }

  /** Removes a folder and everything under it (deleted/renamed/moved away). */
  suspend fun removeSubtree(folderPath: String) =
    withContext(Dispatchers.IO) {
      writeMutex.withLock { removeSubtreeLocked(folderPath) }
    }

  /**
   * Moves a folder's row to its new path right after a rename so lists don't flicker. The old
   * subtree is dropped; descendants and the folder's videos are re-indexed by the next
   * reconcile / the next time the folder is opened.
   */
  suspend fun renameFolder(
    oldPath: String,
    newPath: String,
  ) = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      val old = dao.getFolder(oldPath)
      removeSubtreeLocked(oldPath)
      if (old != null) {
        dao.upsertFolder(
          old.copy(
            path = newPath,
            name = File(newPath).name,
            signature = "",
            videosIndexedAt = 0L,
            lastIndexed = System.currentTimeMillis(),
          ),
        )
        invalidateRollup()
      }
    }
  }

  private suspend fun removeSubtreeLocked(folderPath: String) {
    val prefix = folderPath.trimEnd('/') + "/"
    val paths = dao.getAllFolders().map { it.path }.filter { it == folderPath || it.startsWith(prefix) }
    if (paths.isNotEmpty()) {
      dao.applyChanges(emptyList(), paths)
      invalidateRollup()
    }
  }

  /** Drops the whole index (it is rebuilt on the next use). */
  suspend fun clearAll() =
    withContext(Dispatchers.IO) {
      writeMutex.withLock {
        dao.clearAll()
        invalidateRollup()
      }
    }

  // ---------------------------------------------------------------------------------------
  // Tree view (recursive totals)
  // ---------------------------------------------------------------------------------------

  /**
   * Folders (including ancestors with no direct videos) with recursive totals, keyed by path.
   * Computed from the stored rows and kept in memory until the next write.
   */
  suspend fun rolledUp(): Map<String, RolledFolder> =
    withContext(Dispatchers.IO) {
      rollupCache?.let { return@withContext it }
      seedIfEmpty()
      val startVersion = version.get()
      val computed = computeRollup(dao.getAllFolders())
      if (version.get() == startVersion) rollupCache = computed
      computed
    }

  private fun invalidateRollup() {
    version.incrementAndGet()
    rollupCache = null
  }

  private fun computeRollup(rows: List<FolderIndexEntity>): Map<String, RolledFolder> {
    if (rows.isEmpty()) return emptyMap()

    val direct = rows.associateBy { it.path }
    val allPaths = HashSet<String>(direct.keys)
    val sep = File.separatorChar

    for (path in direct.keys) {
      var current = File(path).parent
      while (current != null) {
        if (current == "/" || current.length <= 1) break
        if (!allPaths.add(current)) break // ancestors above were already added
        current = File(current).parent
      }
    }

    val childrenByParent = HashMap<String, MutableList<String>>()
    for (path in allPaths) {
      val parentSep = path.lastIndexOf(sep)
      if (parentSep > 0) {
        val parent = path.substring(0, parentSep)
        if (allPaths.contains(parent)) childrenByParent.getOrPut(parent) { mutableListOf() }.add(path)
      }
    }

    val totalCount = HashMap<String, Int>(allPaths.size)
    val totalSize = HashMap<String, Long>(allPaths.size)
    val totalDuration = HashMap<String, Long>(allPaths.size)
    val lastModified = HashMap<String, Long>(allPaths.size)
    for (path in allPaths) {
      val row = direct[path]
      totalCount[path] = row?.videoCount ?: 0
      totalSize[path] = row?.totalSize ?: 0L
      totalDuration[path] = row?.totalDuration ?: 0L
      lastModified[path] = row?.lastModified ?: 0L
    }

    // Deepest first so every child is complete before it is added to its parent.
    val sorted = allPaths.sortedByDescending { p -> p.count { it == sep } }
    for (path in sorted) {
      val parentSep = path.lastIndexOf(sep)
      if (parentSep <= 0) continue
      val parent = path.substring(0, parentSep)
      if (!allPaths.contains(parent)) continue

      totalCount[parent] = (totalCount[parent] ?: 0) + (totalCount[path] ?: 0)
      totalSize[parent] = (totalSize[parent] ?: 0L) + (totalSize[path] ?: 0L)
      totalDuration[parent] = (totalDuration[parent] ?: 0L) + (totalDuration[path] ?: 0L)
      val childLast = lastModified[path] ?: 0L
      if (childLast > (lastModified[parent] ?: 0L)) lastModified[parent] = childLast
    }

    val result = HashMap<String, RolledFolder>(allPaths.size)
    for (path in allPaths) {
      val count = totalCount[path] ?: 0
      if (count <= 0) continue
      result[path] =
        RolledFolder(
          path = path,
          name = File(path).name,
          videoCount = count,
          totalSize = totalSize[path] ?: 0L,
          totalDuration = totalDuration[path] ?: 0L,
          lastModified = lastModified[path] ?: 0L,
          hasSubfolders = childrenByParent[path]?.any { (totalCount[it] ?: 0) > 0 } == true,
        )
    }
    return result
  }

  // ---------------------------------------------------------------------------------------
  // Mapping helpers
  // ---------------------------------------------------------------------------------------

  private fun List<FolderIndexEntity>.toVideoFolders(): List<VideoFolder> =
    filter { it.videoCount > 0 }
      .map {
        VideoFolder(
          bucketId = it.path,
          name = it.name,
          path = it.path,
          videoCount = it.videoCount,
          totalSize = it.totalSize,
          totalDuration = it.totalDuration,
          lastModified = it.lastModified,
        )
      }.sortedBy { it.name.lowercase(Locale.getDefault()) }

  private fun signatureOf(videos: List<Video>): String {
    var pathHash = 0L
    var size = 0L
    var duration = 0L
    var newest = 0L
    for (v in videos) {
      pathHash += v.path.hashCode().toLong() * 31 + v.dateModified
      size += v.size
      duration += v.duration
      if (v.dateModified > newest) newest = v.dateModified
    }
    return "${videos.size}:$size:$duration:$newest:$pathHash"
  }

  private fun Video.toEntity(folderPath: String) =
    FolderVideoEntity(
      path = path,
      folderPath = folderPath,
      mediaId = id,
      uri = uri.toString(),
      displayName = displayName,
      size = size,
      duration = duration,
      dateModified = dateModified,
      dateAdded = dateAdded,
      mimeType = mimeType,
      width = width,
      height = height,
      fps = fps,
      hasEmbeddedSubtitles = hasEmbeddedSubtitles,
      subtitleCodec = subtitleCodec,
    )

  private fun FolderVideoEntity.toVideo(folderPath: String) =
    Video(
      id = mediaId,
      title = displayName.substringBeforeLast('.'),
      displayName = displayName,
      path = path,
      uri = Uri.parse(uri),
      duration = duration,
      durationFormatted = VideoScanUtils.formatDuration(duration),
      size = size,
      sizeFormatted = VideoScanUtils.formatFileSize(size),
      dateModified = dateModified,
      dateAdded = dateAdded,
      mimeType = mimeType,
      bucketId = folderPath,
      bucketDisplayName = File(folderPath).name,
      width = width,
      height = height,
      fps = fps,
      resolution = VideoScanUtils.formatResolution(width, height),
      hasEmbeddedSubtitles = hasEmbeddedSubtitles,
      subtitleCodec = subtitleCodec,
    )
}
