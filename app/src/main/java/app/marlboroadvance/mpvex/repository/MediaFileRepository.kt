package app.marlboroadvance.mpvex.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.browser.PathComponent
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.utils.storage.FolderIndexRepository
import app.marlboroadvance.mpvex.utils.storage.TreeViewScanner
import app.marlboroadvance.mpvex.utils.storage.VideoScanUtils
import app.marlboroadvance.mpvex.utils.storage.StorageVolumeUtils
import app.marlboroadvance.mpvex.utils.storage.FileTypeUtils
import app.marlboroadvance.mpvex.utils.media.MediaInfoOps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * Unified repository for ALL media file operations
 * Consolidates FileSystemRepository, VideoRepository functionality
 *
 * This repository handles:
 * - Video folder discovery (album view)
 * - File system browsing (tree view)
 * - Video file listing
 * - Metadata extraction
 * - Path operations
 * - Storage volume detection
 */
object MediaFileRepository {
  private const val TAG = "MediaFileRepository"

  /**
   * Kept for source compatibility. Folder data now lives in the persistent per-folder index
   * ([FolderIndexRepository]); there is no global in-memory cache to drop, and refreshing is
   * done per folder instead of by clearing everything.
   */
  @Suppress("unused")
  fun clearCache() {
    Log.d(TAG, "clearCache() is a no-op; folders are refreshed individually")
  }

  // =============================================================================
  // FOLDER OPERATIONS (Album View)
  // =============================================================================

  /**
   * Scans all storage volumes to find all folders containing videos
   */
  suspend fun getAllVideoFolders(
    context: Context
  ): List<VideoFolder> =
    withContext(Dispatchers.IO) {
      try {
        FolderIndexRepository.get().getVideoFolders()
      } catch (e: Exception) {
        Log.e(TAG, "Error scanning for video folders", e)
        emptyList()
      }
    }

  /**
   * Fast scan using MediaStore - same as getAllVideoFolders
   * Kept for backward compatibility
   */
  suspend fun getAllVideoFoldersFast(
    context: Context,
    onProgress: ((Int) -> Unit)? = null,
  ): List<VideoFolder> = getAllVideoFolders(context)

  /**
   * No-op enrichment - MediaStore already provides all metadata
   * Kept for backward compatibility
   */
  suspend fun enrichVideoFolders(
    context: Context,
    folders: List<VideoFolder>,
    onProgress: ((Int, Int) -> Unit)? = null,
  ): List<VideoFolder> = folders

  // =============================================================================
  // VIDEO FILE OPERATIONS
  // =============================================================================

  /**
   * Gets all videos in a specific folder
   * @param bucketId Folder path
   */
  suspend fun getVideosInFolder(
    context: Context,
    bucketId: String
  ): List<Video> =
    withContext(Dispatchers.IO) {
      try {
        val videos = VideoScanUtils.getVideosInFolder(context, bucketId)
        // Keep the persistent index in step with what was just read.
        try {
          FolderIndexRepository.get().updateFolderVideos(bucketId, videos)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Index update failed for $bucketId", e)
        }
        videos
      } catch (e: Exception) {
        Log.e(TAG, "Error getting videos for bucket $bucketId", e)
        emptyList()
      }
    }

  /**
   * Gets videos from multiple folders
   * Shows all videos including hidden ones.
   */
  suspend fun getVideosForBuckets(
    context: Context,
    bucketIds: Set<String>
  ): List<Video> =
    withContext(Dispatchers.IO) {
      val result = mutableListOf<Video>()
      for (id in bucketIds) {
        runCatching { result += getVideosInFolder(context, id) }
      }
      result
    }

  /**
   * Creates Video objects from a list of files
   */
  suspend fun getVideosFromFiles(
    context: Context,
    files: List<File>,
  ): List<Video> =
    withContext(Dispatchers.IO) {
      files.mapNotNull { file ->
        try {
          val folderPath = file.parent ?: ""
          val folderName = file.parentFile?.name ?: ""
          createVideoFromFile(context, file, folderPath, folderName)
        } catch (e: Exception) {
          Log.w(TAG, "Error creating video from file: ${file.absolutePath}", e)
          null
        }
      }
    }

  /**
   * Creates a Video object from a file with full metadata extraction
   */
  private suspend fun createVideoFromFile(
    context: Context,
    file: File,
    bucketId: String,
    bucketDisplayName: String,
  ): Video {
    val path = file.absolutePath
    val displayName = file.name
    val title = file.nameWithoutExtension
    val dateModified = file.lastModified() / 1000

    val extension = file.extension.lowercase()
    val mimeType = FileTypeUtils.getMimeTypeFromExtension(extension)
    val uri = Uri.fromFile(file)

    // Extract metadata directly (no cache)
    var size = file.length()
    var duration = 0L
    var width = 0
    var height = 0
    var fps = 0f
    var hasEmbeddedSubtitles = false
    var subtitleCodec = ""

    // Extract metadata using MediaInfo
    MediaInfoOps.extractBasicMetadata(context, uri, displayName).onSuccess { metadata ->
      if (metadata.sizeBytes > 0) size = metadata.sizeBytes
      duration = metadata.durationMs
      width = metadata.width
      height = metadata.height
      fps = metadata.fps
      hasEmbeddedSubtitles = metadata.hasEmbeddedSubtitles
      subtitleCodec = metadata.subtitleCodec
    }

    return Video(
      id = app.marlboroadvance.mpvex.domain.media.model.VideoIds.fromPath(path),
      title = title,
      displayName = displayName,
      path = path,
      uri = uri,
      duration = duration,
      durationFormatted = formatDuration(duration),
      size = size,
      sizeFormatted = formatFileSize(size),
      dateModified = dateModified,
      dateAdded = dateModified,
      mimeType = mimeType,
      bucketId = bucketId,
      bucketDisplayName = bucketDisplayName,
      width = width,
      height = height,
      fps = fps,
      resolution = formatResolutionWithFps(width, height, fps),
      hasEmbeddedSubtitles = hasEmbeddedSubtitles,
      subtitleCodec = subtitleCodec,
    )
  }

  /**
   * Creates a Video object from a file with pre-fetched metadata
   * Use this when metadata has already been batch-extracted
   */
  private fun createVideoFromFileWithMetadata(
    file: File,
    bucketId: String,
    bucketDisplayName: String,
    metadata: MediaInfoOps.VideoMetadata?,
  ): Video {
    val path = file.absolutePath
    val displayName = file.name
    val title = file.nameWithoutExtension
    val dateModified = file.lastModified() / 1000

    val extension = file.extension.lowercase()
    val mimeType = FileTypeUtils.getMimeTypeFromExtension(extension)
    val uri = Uri.fromFile(file)

    // Use pre-fetched metadata
    var size = file.length()
    var duration = 0L
    var width = 0
    var height = 0
    var fps = 0f

    metadata?.let {
      if (it.sizeBytes > 0) size = it.sizeBytes
      duration = it.durationMs
      width = it.width
      height = it.height
      fps = it.fps
    }
    val hasEmbeddedSubtitles = metadata?.hasEmbeddedSubtitles ?: false
    val subtitleCodec = metadata?.subtitleCodec ?: ""

    return Video(
      id = app.marlboroadvance.mpvex.domain.media.model.VideoIds.fromPath(path),
      title = title,
      displayName = displayName,
      path = path,
      uri = uri,
      duration = duration,
      durationFormatted = formatDuration(duration),
      size = size,
      sizeFormatted = formatFileSize(size),
      dateModified = dateModified,
      dateAdded = dateModified,
      mimeType = mimeType,
      bucketId = bucketId,
      bucketDisplayName = bucketDisplayName,
      width = width,
      height = height,
      fps = fps,
      resolution = formatResolutionWithFps(width, height, fps),
      hasEmbeddedSubtitles = hasEmbeddedSubtitles,
      subtitleCodec = subtitleCodec,
    )
  }

  // =============================================================================
  // FILE SYSTEM BROWSING (Tree View)
  // =============================================================================

  /**
   * Gets the default root path for the filesystem browser
   */
  fun getDefaultRootPath(): String = Environment.getExternalStorageDirectory().absolutePath

  /**
   * Parses a path into breadcrumb components
   */
  fun getPathComponents(path: String): List<PathComponent> {
    if (path.isBlank()) return emptyList()

    val components = mutableListOf<PathComponent>()
    val normalizedPath = path.trimEnd('/')
    val parts = normalizedPath.split("/").filter { it.isNotEmpty() }

    components.add(PathComponent("Root", "/"))

    var currentPath = ""
    for (part in parts) {
      currentPath += "/$part"
      components.add(PathComponent(part, currentPath))
    }

    return components
  }

  /**
   * Instant, index-only view of a directory: child folders (recursive counts) and the folder's
   * direct videos, read from the persistent index. No MediaStore or filesystem scan.
   * Returns null when this folder's videos have not been indexed yet.
   */
  suspend fun scanDirectoryCached(
    context: Context,
    path: String,
  ): List<FileSystemItem>? =
    withContext(Dispatchers.IO) {
      try {
        val videos = FolderIndexRepository.get().getCachedVideos(path) ?: return@withContext null
        val folders = TreeViewScanner.getFoldersInDirectory(context, path)
        buildItems(folders, videos)
      } catch (e: Exception) {
        Log.w(TAG, "Cached directory read failed: $path", e)
        null
      }
    }

  /**
   * Scans a directory and returns its contents (folders and video files).
   * Child folders come from the persistent index; videos are read live from MediaStore (with
   * filesystem fallback) and written back to the index when they changed.
   * @param showAllFileTypes If true, shows all files. If false, shows only videos.
   * @param useFastCount Unused, kept for source compatibility.
   */
  suspend fun scanDirectory(
    context: Context,
    path: String,
    showAllFileTypes: Boolean = false,
    useFastCount: Boolean = false,
  ): Result<List<FileSystemItem>> =
    withContext(Dispatchers.IO) {
      try {
        val directory = File(path)

        // Validation checks
        if (!directory.exists()) {
          FolderIndexRepository.get().removeSubtree(path)
          return@withContext Result.failure(Exception("Directory does not exist: $path"))
        }

        if (!directory.canRead()) {
          return@withContext Result.failure(Exception("Cannot read directory: $path"))
        }

        if (!directory.isDirectory) {
          return@withContext Result.failure(Exception("Path is not a directory: $path"))
        }

        // Live videos of this folder only, then persist them if they changed.
        val videos = VideoScanUtils.getVideosInFolder(context, path)
        try {
          FolderIndexRepository.get().updateFolderVideos(path, videos)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Index update failed for $path", e)
        }

        // Child folders from the index (recursive counts).
        val folders = TreeViewScanner.getFoldersInDirectory(context, path)

        Result.success(buildItems(folders, videos))
      } catch (e: CancellationException) {
        // A newer load replaced this one: don't report it as a scan failure.
        throw e
      } catch (e: SecurityException) {
        Log.e(TAG, "Security exception scanning directory: $path", e)
        Result.failure(Exception("Permission denied: ${e.message}"))
      } catch (e: Exception) {
        Log.e(TAG, "Error scanning directory: $path", e)
        Result.failure(e)
      }
    }

  private fun buildItems(
    folders: List<TreeViewScanner.FolderData>,
    videos: List<Video>,
  ): List<FileSystemItem> {
    val items = ArrayList<FileSystemItem>(folders.size + videos.size)
    folders.forEach { folderData ->
      items.add(
        FileSystemItem.Folder(
          name = folderData.name,
          path = folderData.path,
          lastModified = File(folderData.path).lastModified(),
          videoCount = folderData.videoCount,
          totalSize = folderData.totalSize,
          totalDuration = folderData.totalDuration,
          hasSubfolders = folderData.hasSubfolders,
        ),
      )
    }
    videos.forEach { video ->
      items.add(
        FileSystemItem.VideoFile(
          name = video.displayName,
          path = video.path,
          lastModified = File(video.path).lastModified(),
          video = video,
        ),
      )
    }
    return items
  }

  /**
   * Gets all storage volume roots with recursive video counts
   */
  suspend fun getStorageRoots(context: Context): List<FileSystemItem.Folder> =
    withContext(Dispatchers.IO) {
      val roots = mutableListOf<FileSystemItem.Folder>()

      try {
        // Primary storage (internal)
        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage.exists() && primaryStorage.canRead()) {
          val primaryPath = primaryStorage.absolutePath
          
          // Get recursive count for this storage root
          val folderData = TreeViewScanner.getFolderDataRecursive(context, primaryPath)
          
          roots.add(
            FileSystemItem.Folder(
              name = "Internal Storage",
              path = primaryPath,
              lastModified = primaryStorage.lastModified(),
              videoCount = folderData?.videoCount ?: 0,
              totalSize = folderData?.totalSize ?: 0L,
              totalDuration = folderData?.totalDuration ?: 0L,
              hasSubfolders = true,
            ),
          )
        }

        // External volumes (SD cards, USB OTG)
        val externalVolumes = StorageVolumeUtils.getExternalStorageVolumes(context)
        for (volume in externalVolumes) {
          val volumePath = StorageVolumeUtils.getVolumePath(volume)
          if (volumePath != null) {
            val volumeDir = File(volumePath)
            if (volumeDir.exists() && volumeDir.canRead()) {
              val volumeName = volume.getDescription(context)
              
              // Get recursive count for this storage root
              val folderData = TreeViewScanner.getFolderDataRecursive(context, volumePath)
              
              roots.add(
                FileSystemItem.Folder(
                  name = volumeName,
                  path = volumeDir.absolutePath,
                  lastModified = volumeDir.lastModified(),
                  videoCount = folderData?.videoCount ?: 0,
                  totalSize = folderData?.totalSize ?: 0L,
                  totalDuration = folderData?.totalDuration ?: 0L,
                  hasSubfolders = true,
                ),
              )
            }
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error getting storage roots", e)
      }

      roots
    }

  // =============================================================================
  // FORMATTING UTILITIES
  // =============================================================================

  private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0s"

    val seconds = durationMs / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
      hours > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
      minutes > 0 -> String.format(Locale.getDefault(), "%d:%02d", minutes, secs)
      else -> "${secs}s"
    }
  }

  private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
      Locale.getDefault(),
      "%.1f %s",
      bytes / 1024.0.pow(digitGroups.toDouble()),
      units[digitGroups],
    )
  }

  private fun formatResolution(
    width: Int,
    height: Int,
  ): String {
    if (width <= 0 || height <= 0) return "--"

    val label =
      when {
        width >= 7680 || height >= 4320 -> "4320p"
        width >= 3840 || height >= 2160 -> "2160p"
        width >= 2560 || height >= 1440 -> "1440p"
        width >= 1920 || height >= 1080 -> "1080p"
        width >= 1280 || height >= 720 -> "720p"
        width >= 854 || height >= 480 -> "480p"
        width >= 640 || height >= 360 -> "360p"
        width >= 426 || height >= 240 -> "240p"
        else -> "${height}p"
      }

    return label
  }

  private fun formatResolutionWithFps(
    width: Int,
    height: Int,
    fps: Float,
  ): String {
    val baseResolution = formatResolution(width, height)
    if (baseResolution == "--" || fps <= 0f) return baseResolution

    // Show only the integer part for frame rates, without rounding
    val fpsFormatted = fps.toInt().toString()

    return "$baseResolution@$fpsFormatted"
  }
}
