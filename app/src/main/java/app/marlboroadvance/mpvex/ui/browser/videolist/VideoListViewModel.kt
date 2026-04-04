package app.marlboroadvance.mpvex.ui.browser.videolist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.media.MetadataRetrieval
import app.marlboroadvance.mpvex.utils.storage.FolderViewScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import androidx.compose.runtime.Immutable

@Immutable
data class VideoWithPlaybackInfo(
  val video: Video,
  val timeRemaining: Long? = null, // in seconds
  val progressPercentage: Float? = null, // 0.0 to 1.0
  val isOldAndUnplayed: Boolean = false, // true if video is older than threshold and never played
  val isWatched: Boolean = false, // true if video has any playback history
)

class VideoListViewModel(
  application: Application,
  private val bucketId: String,
) : BaseBrowserViewModel(application),
  KoinComponent {
  private val playbackStateRepository: PlaybackStateRepository by inject()
  private val appearancePreferences: app.marlboroadvance.mpvex.preferences.AppearancePreferences by inject()
  private val browserPreferences: app.marlboroadvance.mpvex.preferences.BrowserPreferences by inject()
  private val recentlyPlayedRepository: app.marlboroadvance.mpvex.domain.recentlyplayed.repository.RecentlyPlayedRepository by inject()
  // Using MediaFileRepository singleton directly

  private val _videos = MutableStateFlow<List<Video>>(emptyList())
  val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  private val _videosWithPlaybackInfo = MutableStateFlow<List<VideoWithPlaybackInfo>>(emptyList())
  val videosWithPlaybackInfo: StateFlow<List<VideoWithPlaybackInfo>> = _videosWithPlaybackInfo.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Track if items were deleted/moved leaving folder empty
  private val _videosWereDeletedOrMoved = MutableStateFlow(false)
  val videosWereDeletedOrMoved: StateFlow<Boolean> = _videosWereDeletedOrMoved.asStateFlow()

  val lastPlayedInFolderPath: StateFlow<String?> =
    recentlyPlayedRepository
      .observeRecentlyPlayed(limit = 100)
      .map { recentlyPlayedList ->
        val folderPath = _videos.value.firstOrNull()?.path?.let { File(it).parent }
        if (folderPath != null) {
          recentlyPlayedList.firstOrNull { entity ->
            try {
              File(entity.filePath).parent == folderPath
            } catch (_: Exception) {
              false
            }
          }?.filePath
        } else {
          null
        }
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // Track previous video count to detect if folder became empty
  private var previousVideoCount = 0

  private val tag = "VideoListViewModel"

  init {
    loadVideos()

    // Observe metadata-impacting preferences to trigger re-enrichment when toggled
    viewModelScope.launch {
      kotlinx.coroutines.flow.combine(
        browserPreferences.showResolutionChip.changes(),
        browserPreferences.showFramerateInResolution.changes(),
        browserPreferences.showSubtitleIndicator.changes(),
        browserPreferences.showVideoThumbnails.changes()
      ) { showResolution, showFps, showSub, showThumb ->
        // Return a combined signal - only trigger if something that needs enrichment becomes true
        showResolution || showFps || showSub || showThumb
      }
      .distinctUntilChanged()
      .collectLatest { needsEnrichment ->
        if (needsEnrichment && _videos.value.isNotEmpty()) {
          val videosNeedEnrichment = _videos.value.any { video ->
            video.fps == 0f || video.subtitleCodec.isEmpty()
          }
          if (videosNeedEnrichment) {
            Log.d(tag, "Metadata preferences changed and videos need enrichment, reloading")
            loadVideos()
          }
        }
      }
    }

    // Listen for global media library changes and refresh this list when they occur
    viewModelScope.launch(Dispatchers.IO) {
      MediaLibraryEvents.changes.collectLatest {
        // Clear cache when media library changes
        MediaFileRepository.clearCache()
        loadVideos()
      }
    }
  }

  override fun refresh() {
    Log.d(tag, "Refreshing video list for bucket: $bucketId")
    
    // Only show full loading state if we have no videos yet
    if (_videos.value.isEmpty()) {
      _isLoading.value = true
    }
    
    // Clear cache to force fresh data from filesystem
    MediaFileRepository.clearCache()
    FolderViewScanner.clearCache()
    
    // Trigger media scan in background
    triggerMediaScan()
    
    // Reload videos immediately without artificial delay
    loadVideos()
  }

  private fun loadVideos() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Step 1: Immediate load from MediaStore (fast)
        var videoList = MediaFileRepository.getVideosInFolder(getApplication(), bucketId)
        
        // Show initial list immediately so thumbnails can start generating
        _videos.value = videoList
        loadPlaybackInfo(videoList)
        _isLoading.value = false // Stop loading spinner as soon as we have the basic list

        // Step 2: Enrich with detailed metadata (framerate, resolution, codec) if needed
        if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences) && videoList.isNotEmpty()) {
          Log.d(tag, "Metadata chips enabled, enriching ${videoList.size} videos in background")
          val enrichedList = MetadataRetrieval.enrichVideosIfNeeded(
            context = getApplication(),
            videos = videoList,
            browserPreferences = browserPreferences,
            metadataCache = metadataCache
          )
          
          // Update list with enriched metadata
          _videos.value = enrichedList
          loadPlaybackInfo(enrichedList)
        }

        // Check if folder became empty after having videos
        if (previousVideoCount > 0 && videoList.isEmpty()) {
          _videosWereDeletedOrMoved.value = true
        } else if (videoList.isNotEmpty()) {
          _videosWereDeletedOrMoved.value = false
        }
        previousVideoCount = videoList.size

        if (videoList.isEmpty()) {
          Log.d(tag, "No videos found for bucket $bucketId - attempting media rescan")
          triggerMediaScan()
          delay(1000)
          val retryVideoList = MediaFileRepository.getVideosInFolder(getApplication(), bucketId)
          _videos.value = retryVideoList
          loadPlaybackInfo(retryVideoList)

          if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences) && retryVideoList.isNotEmpty()) {
            val enrichedRetryList = MetadataRetrieval.enrichVideosIfNeeded(
              context = getApplication(),
              videos = retryVideoList,
              browserPreferences = browserPreferences,
              metadataCache = metadataCache
            )
            _videos.value = enrichedRetryList
            loadPlaybackInfo(enrichedRetryList)
          }
        }
      } catch (e: Exception) {
        Log.e(tag, "Error loading videos for bucket $bucketId", e)
        _videos.value = emptyList()
        _videosWithPlaybackInfo.value = emptyList()
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Set flag indicating videos were deleted or moved
   */
  fun setVideosWereDeletedOrMoved() {
    _videosWereDeletedOrMoved.value = true
  }

  private suspend fun loadPlaybackInfo(videos: List<Video>) {
    val videosWithInfo =
      videos.map { video ->
        val playbackState = playbackStateRepository.getVideoDataByTitle(video.displayName)
        val watchedThreshold = browserPreferences.watchedThreshold.get()

        // Calculate watch progress (0.0 to 1.0)
        val progress = if (playbackState != null && video.duration > 0) {
          // Duration is in milliseconds, convert to seconds
          val durationSeconds = video.duration / 1000
          val timeRemaining = playbackState.timeRemaining.toLong()
          val watched = durationSeconds - timeRemaining
          val progressValue = (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)

          // Only show progress for videos that are 1-99% complete
          if (progressValue in 0.01f..0.99f) progressValue else null
        } else {
          null
        }

        // Check if video is old and unplayed
        // Video is old if it's been more than threshold days since it was added/modified
        // Video is unplayed if there's no playback state record
        val isOldAndUnplayed = playbackState == null

        val isWatched = if (playbackState != null && video.duration > 0) {
           val durationSeconds = video.duration / 1000
           val timeRemaining = playbackState.timeRemaining.toLong()
           val watched = durationSeconds - timeRemaining
           val progressValue = (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
           val calculatedWatched = progressValue >= (watchedThreshold / 100f)
           playbackState.hasBeenWatched || calculatedWatched
        } else {
           false
        }

        VideoWithPlaybackInfo(
          video = video,
          timeRemaining = playbackState?.timeRemaining?.toLong(),
          progressPercentage = progress,
          isOldAndUnplayed = isOldAndUnplayed,
          isWatched = isWatched,
        )
      }
    _videosWithPlaybackInfo.value = videosWithInfo
  }

  private fun triggerMediaScan() {
    try {
      // Trigger a targeted media scan for the specific folder
      val folder = File(bucketId)
      
      if (folder.exists() && folder.isDirectory) {
        // Scan all video files in the folder
        val videoFiles = folder.listFiles { file ->
          file.isFile && file.extension.lowercase() in listOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "mpg", "mpeg", "ts", "m2ts"
          )
        }
        
        if (!videoFiles.isNullOrEmpty()) {
          val filePaths = videoFiles.map { it.absolutePath }.toTypedArray()
          
          android.media.MediaScannerConnection.scanFile(
            getApplication(),
            filePaths,
            null, // Let MediaScanner detect MIME types
          ) { path, uri ->
            Log.d(tag, "Media scan completed for: $path -> $uri")
          }
          
          Log.d(tag, "Triggered media scan for ${filePaths.size} files in: $bucketId")
        } else {
          Log.d(tag, "No video files found in folder: $bucketId")
        }
      } else {
        // Fallback to scanning external storage root
        val externalStorage = android.os.Environment.getExternalStorageDirectory()
        android.media.MediaScannerConnection.scanFile(
          getApplication(),
          arrayOf(externalStorage.absolutePath),
          arrayOf("video/*"),
        ) { path, uri ->
          Log.d(tag, "Media scan completed for: $path -> $uri")
        }
        Log.d(tag, "Triggered media scan for: ${externalStorage.absolutePath}")
      }
    } catch (e: Exception) {
      Log.e(tag, "Failed to trigger media scan", e)
    }
  }

  companion object {
    fun factory(
      application: Application,
      bucketId: String,
    ) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = VideoListViewModel(application, bucketId) as T
    }
  }
}
