package app.marlboroadvance.mpvex.domain.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.utils.media.MediaInfoOps
import `is`.xyz.mpv.FastThumbnails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class ThumbnailRepository(
  private val context: Context,
) {
  private val appearancePreferences by lazy {
    org.koin.java.KoinJavaComponent.get<app.marlboroadvance.mpvex.preferences.AppearancePreferences>(
      app.marlboroadvance.mpvex.preferences.AppearancePreferences::class.java
    )
  }
  private val diskCacheDimension = 1024
  private val diskJpegQuality = 100
  private val memoryCache: LruCache<String, Bitmap>
  private val diskDir: File = File(context.filesDir, "thumbnails").apply { mkdirs() }
  private val ongoingOperations = ConcurrentHashMap<String, Deferred<Bitmap?>>()

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val maxconcurrentfolders = 3

  // Global limit: only allow 2 thumbnails to be generated at a time to save battery and CPU
  private val generationSemaphore = Semaphore(2)

  private val _thumbnailReadyKeys =
    MutableSharedFlow<String>(
      extraBufferCapacity = 256,
    )
  val thumbnailReadyKeys: SharedFlow<String> = _thumbnailReadyKeys.asSharedFlow()

  private data class FolderState(
    val signature: String,
    @Volatile var nextIndex: Int = 0,
  )

  private val folderStates = ConcurrentHashMap<String, FolderState>()
  private val folderJobs = ConcurrentHashMap<String, Job>()

  private val useMediaStoreForVideo = ConcurrentHashMap<String, Boolean>()

  init {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    val cacheSizeKb = maxMemoryKb / 6
    memoryCache =
      object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(
          key: String,
          value: Bitmap,
        ): Int = value.byteCount / 1024
      }
  }

  suspend fun getThumbnail(
    video: Video,
    widthPx: Int,
    heightPx: Int,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      val key = thumbnailKey(video)

      if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
        return@withContext null
      }

      memoryCache.get(key)?.let { return@withContext it }

      // Remove any stale cancelled Deferred before computing — a cancelled Deferred
      // will never complete, so any awaiter on it would suspend forever.
      ongoingOperations[key]?.let { existing ->
        if (existing.isCancelled) ongoingOperations.remove(key, existing)
      }

      // computeIfAbsent is atomic on ConcurrentHashMap: the lambda only runs if no
      // entry exists for this key. Any second coroutine arriving concurrently finds
      // the already-stored Deferred and awaits it, closing the race window that
      // existed between the old null-check and the separate store below.
      val deferred = ongoingOperations.computeIfAbsent(key) {
        async {
          try {
            loadFromDisk(video)?.let { thumbnail ->
              memoryCache.put(key, thumbnail)
              _thumbnailReadyKeys.tryEmit(key)
              return@async thumbnail
            }

            if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
              return@async null
            }

            // Acquire permit to generate. This throttles the CPU-intensive generation process.
            generationSemaphore.withPermit {
              val aspect = if (widthPx > 0 && heightPx > 0) {
                widthPx.toFloat() / heightPx.toFloat()
              } else {
                16f / 9f
              }

              val targetWidth = diskCacheDimension
              val targetHeight = (targetWidth / aspect).toInt()

              val videoKey = videoBaseKey(video)
              val thumbnail = if (useMediaStoreForVideo.containsKey(videoKey)) {
                generateWithMediaStore(video, targetWidth, targetHeight)
              } else {
                val fastResult = generateWithFastThumbnails(video, targetWidth, targetHeight)
                if (fastResult == null) {
                  useMediaStoreForVideo[videoKey] = true
                  generateWithMediaStore(video, targetWidth, targetHeight)
                } else {
                  fastResult
                }
              }

              if (thumbnail == null) {
                return@withPermit null
              }

              memoryCache.put(key, thumbnail)
              _thumbnailReadyKeys.tryEmit(key)
              writeToDisk(video, thumbnail)

              thumbnail
            }
          } finally {
            ongoingOperations.remove(key)
          }
        }
      }

      return@withContext deferred.await()
    }

  suspend fun getCachedThumbnail(
    video: Video,
    widthPx: Int,
    heightPx: Int,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
        return@withContext null
      }

      val key = thumbnailKey(video)
      synchronized(memoryCache) { memoryCache.get(key) }?.let { return@withContext it }
      loadFromDisk(video)?.let { thumbnail ->
        synchronized(memoryCache) { memoryCache.put(key, thumbnail) }
        return@withContext thumbnail
      }
      null
    }

  fun getThumbnailFromMemory(video: Video): Bitmap? {
    if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
      return null
    }
    val key = thumbnailKey(video)
    return synchronized(memoryCache) { memoryCache.get(key) }
  }

  fun clearThumbnailCache() {
    folderJobs.values.forEach { it.cancel() }
    folderJobs.clear()
    folderStates.clear()
    ongoingOperations.clear()
    useMediaStoreForVideo.clear()

    synchronized(memoryCache) {
      memoryCache.evictAll()
    }

    runCatching {
      if (diskDir.exists()) {
        diskDir.listFiles()?.forEach { it.delete() }
      }
    }
  }

  fun startFolderThumbnailGeneration(
    folderId: String,
    videos: List<Video>,
    widthPx: Int,
    heightPx: Int,
  ) {
    val filteredVideos = if (appearancePreferences.showNetworkThumbnails.get()) {
      videos
    } else {
      videos.filterNot { isNetworkUrl(it.path) }
    }

    if (filteredVideos.isEmpty()) return

    folderJobs.entries.removeAll { !it.value.isActive }

    if (folderJobs.size >= maxconcurrentfolders && !folderJobs.containsKey(folderId)) {
      folderJobs.entries.firstOrNull()?.let { (oldestId, job) ->
        job.cancel()
        folderJobs.remove(oldestId)
        folderStates.remove(oldestId)
      }
    }

    val signature = folderSignature(filteredVideos, widthPx, heightPx)
    val state =
      folderStates.compute(folderId) { _, existing ->
        if (existing == null || existing.signature != signature) {
          FolderState(signature = signature, nextIndex = 0)
        } else {
          existing
        }
      }!!

    folderJobs.remove(folderId)?.cancel()
    folderJobs[folderId] =
      repositoryScope.launch {
        var i = state.nextIndex
        while (i < filteredVideos.size) {
          val video = filteredVideos[i]
          getThumbnail(video, widthPx, heightPx)
          i++
          state.nextIndex = i
        }
      }
  }

  fun thumbnailKey(video: Video): String = videoBaseKey(video)

  private fun videoBaseKey(video: Video): String {
    if (isNetworkUrl(video.path)) {
      val base = video.path.ifBlank { video.uri.toString() }
      return "$base|network"
    }

    return if (video.path.isNotBlank()) {
      "${video.path}|local"
    } else {
      "${video.size}|${video.dateModified}|${video.duration}|${video.id}"
    }
  }

  private fun keyToFileName(key: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(key.toByteArray())
    val hex = digest.joinToString("") { b -> "%02x".format(b) }
    return "$hex.jpg"
  }

  private fun diskKey(video: Video): String {
    val baseKey = videoBaseKey(video)
    return if (isNetworkUrl(video.path)) {
      "$baseKey|disk|d$diskCacheDimension|v2|pos3"
    } else {
      "$baseKey|disk|d$diskCacheDimension|v2"
    }
  }

  private fun loadFromDisk(video: Video): Bitmap? {
    val diskFile = File(diskDir, keyToFileName(diskKey(video)))
    if (!diskFile.exists()) return null
    return runCatching {
      val options =
        BitmapFactory.Options().apply {
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
      BitmapFactory.decodeFile(diskFile.absolutePath, options)
    }.getOrNull()
  }

  private fun writeToDisk(video: Video, bitmap: Bitmap) {
    val diskFile = File(diskDir, keyToFileName(diskKey(video)))
    runCatching {
      FileOutputStream(diskFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, diskJpegQuality, out)
        out.flush()
      }
    }
  }

  private suspend fun rotateIfNeeded(
    video: Video,
    bitmap: Bitmap
  ): Bitmap {
    val rotation = MediaInfoOps.getRotation(context, video.uri, video.displayName)
    if (rotation == 0) return bitmap
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotation.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private suspend fun generateWithFastThumbnails(
    video: Video,
    width: Int,
    height: Int,
  ): Bitmap? {
    return runCatching {
      val positionSec = preferredPositionSeconds(video)
      val dimension = maxOf(width, height)
      val bmp = FastThumbnails.generateAsync(
        video.path.ifBlank { video.uri.toString() },
        positionSec,
        dimension,
        useHwDec = false
      ) ?: return@runCatching null

      rotateIfNeeded(video, bmp)
    }.getOrNull()
  }

  private suspend fun generateWithMediaStore(
    video: Video,
    width: Int,
    height: Int,
  ): Bitmap? {
    if (isNetworkUrl(video.path)) {
      return null
    }

    return withContext(Dispatchers.IO) {
      val mediaStoreThumbnail = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          val contentUri = android.content.ContentUris.withAppendedId(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            video.id
          )
          context.contentResolver.loadThumbnail(
            contentUri,
            android.util.Size(width, height),
            null
          )
        } else {
          @Suppress("DEPRECATION")
          val thumbnail = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            video.id,
            android.provider.MediaStore.Video.Thumbnails.MINI_KIND,
            null
          )
          if (thumbnail != null) {
            val scaled = Bitmap.createScaledBitmap(
              thumbnail,
              width,
              (width * thumbnail.height) / thumbnail.width,
              true
            )
            if (scaled != thumbnail) {
              thumbnail.recycle()
            }
            rotateIfNeeded(video, scaled)
          } else {
            null
          }
        }
      }.getOrNull()

      if (mediaStoreThumbnail != null) {
        return@withContext mediaStoreThumbnail
      }

      runCatching {
        val file = java.io.File(video.path)
        if (!file.exists()) {
          return@runCatching null
        }

        val thumbnail = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          android.media.ThumbnailUtils.createVideoThumbnail(
            file,
            android.util.Size(width, height),
            null
          )
        } else {
          @Suppress("DEPRECATION")
          android.media.ThumbnailUtils.createVideoThumbnail(
            video.path,
            android.provider.MediaStore.Video.Thumbnails.MINI_KIND
          )?.let { thumb ->
            Bitmap.createScaledBitmap(
              thumb,
              width,
              (width * thumb.height) / thumb.width,
              true
            ).also {
              if (it != thumb) thumb.recycle()
            }
          }
        }

        if (thumbnail != null) {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            thumbnail
          } else {
            rotateIfNeeded(video, thumbnail)
          }
        } else {
          null
        }
      }.getOrNull()
    }
  }

  private fun preferredPositionSeconds(video: Video): Double {
    val isNetworkUrl = isNetworkUrl(video.path)

    if (isNetworkUrl) {
      val durationSec = video.duration / 1000.0
      if (durationSec > 0.0) {
        return 2.0.coerceIn(0.0, max(0.0, durationSec - 0.1))
      }
      return 2.0
    }

    val durationSec = video.duration / 1000.0
    if (durationSec <= 0.0 || durationSec < 20.0) return 0.0
    val candidate = 3.0
    return candidate.coerceIn(0.0, max(0.0, durationSec - 0.1))
  }

  private fun isNetworkUrl(path: String): Boolean {
    return path.startsWith("http://", ignoreCase = true) ||
      path.startsWith("https://", ignoreCase = true) ||
      path.startsWith("rtmp://", ignoreCase = true) ||
      path.startsWith("rtsp://", ignoreCase = true) ||
      path.startsWith("ftp://", ignoreCase = true) ||
      path.startsWith("sftp://", ignoreCase = true)
  }

  private fun folderSignature(
    videos: List<Video>,
    widthPx: Int,
    heightPx: Int,
  ): String {
    val md = MessageDigest.getInstance("MD5")
    md.update("$widthPx|$heightPx|".toByteArray())
    for (v in videos) {
      md.update(v.path.toByteArray())
      md.update("|".toByteArray())
      md.update(v.size.toString().toByteArray())
      md.update("|".toByteArray())
      md.update(v.dateModified.toString().toByteArray())
      md.update(";".toByteArray())
    }
    return md.digest().joinToString("") { b -> "%02x".format(b) }
  }
}
