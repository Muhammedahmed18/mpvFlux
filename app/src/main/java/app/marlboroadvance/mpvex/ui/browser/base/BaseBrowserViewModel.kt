package app.marlboroadvance.mpvex.ui.browser.base

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteProgress
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils.StorageOps
import app.marlboroadvance.mpvex.utils.storage.FileTypeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Base ViewModel for browser screens with shared functionality
 */
abstract class BaseBrowserViewModel(
  application: Application,
) : AndroidViewModel(application),
  KoinComponent {
  protected val metadataCache: VideoMetadataCacheRepository by inject()
  protected val thumbnailRepository: ThumbnailRepository by inject()
  protected val playbackStateRepository: PlaybackStateRepository by inject()
  protected val recentlyPlayedRepository: RecentlyPlayedRepository by inject()

  private val _deleteProgress = MutableStateFlow<DeleteProgress?>(null)

  /** Live progress of an in-flight folder deletion, or null when none is running. Drives the
   *  determinate "X / N deleted" dialog. */
  val deleteProgress: StateFlow<DeleteProgress?> = _deleteProgress.asStateFlow()

  private var deleteJob: Deferred<Pair<Int, Int>>? = null
  /**
   * Observable recently played file path for highlighting
   * Automatically filters out non-existent files
   */
  val recentlyPlayedFilePath: StateFlow<String?> =
    RecentlyPlayedOps
      .observeLastPlayedPath()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  /**
   * Refresh the data (to be implemented by subclasses)
   */
  abstract fun refresh()

  /**
   * Delete videos from storage
   * Automatically removes from recently played history and invalidates cache
   *
   * @return Pair of (deletedCount, failedCount)
   */
  open suspend fun deleteVideos(videos: List<Video>): Pair<Int, Int> {
    val result = StorageOps.deleteVideos(getApplication(), videos)
    purgeVideoTraces(videos)
    return result
  }

  /**
   * Remove every trace of the given videos so deletion leaves nothing behind:
   * cached metadata, thumbnail caches (memory + disk), saved resume position, and
   * recently-played history. Each removal is independent so one failure can't abort the rest.
   * Call after the underlying files have been deleted.
   */
  protected suspend fun purgeVideoTraces(videos: List<Video>) {
    if (videos.isEmpty()) return

    metadataCache.invalidateVideos(videos.map { it.path })
    thumbnailRepository.removeThumbnails(videos)

    videos.forEach { video ->
      runCatching { playbackStateRepository.deleteByTitle(video.displayName) }
      runCatching { recentlyPlayedRepository.deleteByFilePath(video.path) }
    }
  }

  /** Cancel an in-flight folder deletion. Files already removed stay removed and their traces are
   *  still purged; the remaining files are left in place. */
  fun cancelDeletion() {
    deleteJob?.cancel()
  }

  /**
   * Delete the folders at [folderPaths] file-by-file, publishing live progress to [deleteProgress]
   * so the UI can show a determinate "X / N deleted" dialog. The work runs on [viewModelScope]
   * (not the caller's scope) so a user-triggered [cancelDeletion] stops it cleanly and the deletion
   * survives if the caller's composition leaves mid-operation. Returns (deletedFolders, failedFolders).
   */
  protected suspend fun deleteFoldersWithProgress(folderPaths: List<String>): Pair<Int, Int> {
    val deferred = viewModelScope.async(Dispatchers.IO) { performFolderDeletion(folderPaths) }
    deleteJob = deferred
    return try {
      deferred.await()
    } catch (e: CancellationException) {
      // The user hit Cancel (the child job was cancelled), not the caller's scope dying — swallow
      // it and report neutral counts so no spurious success toast fires.
      Pair(0, 0)
    } finally {
      deleteJob = null
      _deleteProgress.value = null
    }
  }

  private suspend fun performFolderDeletion(folderPaths: List<String>): Pair<Int, Int> {
    // Enumerate every file in each tree up-front. walkTopDown is recursive, so this also reaches
    // videos in sub-folders that a flat/bucket scan would miss — those are exactly the thumbnails
    // that used to be orphaned after a delete.
    val perFolderFiles = folderPaths.associateWith { path ->
      val dir = File(path)
      if (dir.exists()) dir.walkTopDown().filter { it.isFile }.toList() else emptyList()
    }
    val total = perFolderFiles.values.sumOf { it.size }
    _deleteProgress.value = DeleteProgress(total = total, completed = 0)

    // Stop in-flight batch thumbnail generation for these folders so it can't write a thumbnail
    // back to disk after we purge it. (writeToDisk's file-existence guard covers the rest.)
    thumbnailRepository.cancelFolderGeneration(folderPaths)

    var successCount = 0
    var failureCount = 0
    var completed = 0
    val deletedVideos = ArrayList<Video>()

    try {
      folderPaths.forEach { path ->
        var folderFailed = false
        perFolderFiles.getValue(path).forEach { file ->
          currentCoroutineContext().ensureActive()
          val purgeCandidate = if (FileTypeUtils.isVideoFile(file)) videoFromFile(file) else null
          val deleted = runCatching { file.delete() }.getOrDefault(false)
          if (deleted) {
            purgeCandidate?.let { deletedVideos += it }
          } else {
            folderFailed = true
          }
          completed++
          _deleteProgress.value = DeleteProgress(total, completed, file.name)
        }

        // Remove the now-empty directory tree; report failure if anything survived.
        val dir = File(path)
        runCatching { if (dir.exists()) dir.deleteRecursively() }
        if (dir.exists()) folderFailed = true
        if (folderFailed) failureCount++ else successCount++
      }
    } finally {
      // Always purge traces for whatever we actually deleted — even on cancel — so a partial delete
      // never leaves orphaned thumbnails/metadata behind. NonCancellable lets this finish while the
      // job is being cancelled.
      withContext(NonCancellable) { purgeVideoTraces(deletedVideos) }
    }

    if (successCount > 0) {
      MediaLibraryEvents.notifyChanged()
    }
    return Pair(successCount, failureCount)
  }

  /** Build a minimal [Video] from a file on disk. Only the fields the trace-purge relies on (path,
   *  size, dateModified, displayName) are meaningful; the rest are inert defaults. The size +
   *  dateModified pair reproduces the same thumbnail-cache key the generator used. */
  private fun videoFromFile(file: File): Video {
    val dateModified = file.lastModified() / 1000
    return Video(
      id = file.absolutePath.hashCode().toLong(),
      title = file.nameWithoutExtension,
      displayName = file.name,
      path = file.absolutePath,
      uri = Uri.fromFile(file),
      duration = 0L,
      durationFormatted = "",
      size = file.length(),
      sizeFormatted = "",
      dateModified = dateModified,
      dateAdded = dateModified,
      mimeType = "video/*",
      bucketId = file.parent ?: "",
      bucketDisplayName = file.parentFile?.name ?: "",
      width = 0,
      height = 0,
      fps = 0f,
      resolution = "",
    )
  }

  /**
   * Rename a video
   * Automatically updates recently played history and invalidates old cache entry
   *
   * @param video Video to rename
   * @param newDisplayName New display name (including extension)
   * @return Result indicating success or failure
   */
  open suspend fun renameVideo(
    video: Video,
    newDisplayName: String,
  ): Result<Unit> {
    val oldPath = video.path
    val result = StorageOps.renameVideo(getApplication(), video, newDisplayName)

    // Invalidate cache for old path (new path will be re-cached on next access)
    result.onSuccess {
      metadataCache.invalidateVideo(oldPath)
    }

    return result
  }
}
