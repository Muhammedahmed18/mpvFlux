package app.marlboroadvance.mpvex.ui.browser.videolist

import android.app.Application
import android.net.Uri
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
import app.marlboroadvance.mpvex.utils.history.PlaybackHistoryCache
import app.marlboroadvance.mpvex.utils.storage.FolderViewScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.combine

@Immutable
data class VideoWithPlaybackInfo(
  val video: Video,
  val timeRemaining: Long? = null, // in seconds
  val progressPercentage: Float? = null, // 0.0 to 1.0
  val isOldAndUnplayed: Boolean = false, // true if video is older than threshold and never played
  val isWatched: Boolean = false, // true if video has any playback history
  val isMarkedNew: Boolean = false, // marked as New from "Mark as" and not played since
  val isNew: Boolean = false, // precomputed: shows the NEW chip (marked New, or old-and-unplayed within the day limit)
)

/** Settings the badge snapshot depends on; a change in any of them rebuilds the snapshot. */
private data class BadgeSettings(
  val watchedFraction: Float,
  val showNewLabel: Boolean,
  val newLabelDays: Int,
)

class VideoListViewModel(
  application: Application,
  private val bucketId: String,
) : BaseBrowserViewModel(application),
  KoinComponent {
  // playbackStateRepository and recentlyPlayedRepository are provided by BaseBrowserViewModel
  private val appearancePreferences: app.marlboroadvance.mpvex.preferences.AppearancePreferences by inject()
  private val browserPreferences: app.marlboroadvance.mpvex.preferences.BrowserPreferences by inject()
  // Using MediaFileRepository singleton directly

  // Internal source of truth (pre-enrichment). The UI consumes [videosWithPlaybackInfo] only.
  private val _videos = MutableStateFlow<List<Video>>(emptyList())

  private val _videosWithPlaybackInfo = MutableStateFlow<List<VideoWithPlaybackInfo>>(emptyList())
  val videosWithPlaybackInfo: StateFlow<List<VideoWithPlaybackInfo>> = _videosWithPlaybackInfo.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Recomputes when either the video list OR the recently-played history changes. Reading _videos
  // as a flow (not _videos.value snapshot inside the map) keeps the highlight correct after the
  // list reloads but history hasn't re-emitted.
  val lastPlayedInFolderPath: StateFlow<String?> =
    combine(
      _videos,
      recentlyPlayedRepository.observeRecentlyPlayed(limit = 100),
    ) { videos, recentlyPlayedList ->
      val folderPath = videos.firstOrNull()?.path?.let { File(it).parent }
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

  private val tag = "VideoListViewModel"

  init {
    loadVideos()

    // Observe metadata-impacting preferences and re-enrich when any of them is toggled.
    // We carry the full combination (not an OR of them) so distinctUntilChanged reacts to *every*
    // toggle — e.g. turning one chip off while another stays on. drop(1) skips the initial replay
    // since the init loadVideos() above already performs first-load enrichment.
    viewModelScope.launch {
      combine(
        browserPreferences.showResolutionChip.changes(),
        browserPreferences.showFramerateInResolution.changes(),
        browserPreferences.showSubtitleIndicator.changes(),
        browserPreferences.showVideoThumbnails.changes()
      ) { showResolution, showFps, showSub, showThumb ->
        listOf(showResolution, showFps, showSub, showThumb)
      }
      .distinctUntilChanged()
      .drop(1)
      .collectLatest {
        if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences) && _videos.value.isNotEmpty()) {
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

    // Progress bars, watched ticks and NEW chips follow the video list, the shared playback
    // history and the related settings; Room re-emits when saved progress or marks change.
    observePlaybackInfo()
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

  /**
   * Ensures that data is loaded. If videos are already present, it does nothing.
   * This is safe to call during recompositions or lifecycle events.
   */
  fun ensureDataLoaded() {
    if (_videos.value.isEmpty()) {
      Log.d(tag, "ensureDataLoaded: No data found, triggering load")
      loadVideos()
    } else {
      // Playback info is flow-driven, so progress bars already follow the database.
      Log.d(tag, "ensureDataLoaded: Data already present, skipping redundant scan")
    }
  }

  override suspend fun renameVideo(video: Video, newDisplayName: String): Result<Unit> {
    val result = super.renameVideo(video, newDisplayName)
    if (result.isSuccess) {
      // Soft update local state immediately
      val oldPath = video.path
      val parent = File(oldPath).parentFile
      val newPath = parent?.let { File(it, newDisplayName).absolutePath } ?: oldPath
      
      val updatedVideos = _videos.value.map { v ->
        if (v.path == oldPath) {
          v.copy(
            path = newPath,
            displayName = newDisplayName,
            title = newDisplayName.substringBeforeLast('.'),
            uri = Uri.fromFile(File(newPath))
          )
        } else {
          v
        }
      }
      _videos.value = updatedVideos
    }
    return result
  }

  private fun loadVideos() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Step 1: Immediate load from MediaStore (fast)
        val freshVideoList = MediaFileRepository.getVideosInFolder(getApplication(), bucketId)
        
        // Merge with existing metadata to avoid UI flicker and redundant enrichment
        val currentVideos = _videos.value.associateBy { it.path }
        val mergedList = freshVideoList.map { freshVideo ->
          val existing = currentVideos[freshVideo.path]
          if (existing != null && (existing.fps > 0f || existing.subtitleCodec.isNotEmpty())) {
            // Preserve enriched metadata
            freshVideo.copy(
              fps = existing.fps,
              resolution = existing.resolution,
              hasEmbeddedSubtitles = existing.hasEmbeddedSubtitles,
              subtitleCodec = existing.subtitleCodec,
            )
          } else {
            freshVideo
          }
        }

        // Show initial list immediately so thumbnails can start generating
        _videos.value = mergedList
        awaitPlaybackInfo(mergedList.size)
        _isLoading.value = false // Stop loading spinner as soon as we have the basic list

        // Step 2: Enrich with detailed metadata (framerate, resolution, codec) if needed
        if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences) && mergedList.isNotEmpty()) {
          Log.d(tag, "Metadata chips enabled, enriching ${mergedList.size} videos in background")
          val enrichedList = MetadataRetrieval.enrichVideosIfNeeded(
            context = getApplication(),
            videos = mergedList,
            browserPreferences = browserPreferences,
            metadataCache = metadataCache
          )
          
          // Update list with enriched metadata
          _videos.value = enrichedList
        }

        if (freshVideoList.isEmpty()) {
          Log.d(tag, "No videos found for bucket $bucketId - attempting media rescan")
          triggerMediaScan()
          delay(1000)
          val retryVideoList = MediaFileRepository.getVideosInFolder(getApplication(), bucketId)
          _videos.value = retryVideoList

          if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences) && retryVideoList.isNotEmpty()) {
            val enrichedRetryList = MetadataRetrieval.enrichVideosIfNeeded(
              context = getApplication(),
              videos = retryVideoList,
              browserPreferences = browserPreferences,
              metadataCache = metadataCache
            )
            _videos.value = enrichedRetryList
          }
        }
      } catch (e: Exception) {
        Log.e(tag, "Error loading videos for bucket $bucketId", e)
        _videos.value = emptyList()
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Keeps [videosWithPlaybackInfo] in step with the video list, the shared playback history
   * (one map, no per-video queries), the "marked New" titles and the badge settings. The
   * snapshot is built in memory off the main thread, and unchanged rows keep their previous
   * instance so only rows whose badge state changed recompose.
   */
  private fun observePlaybackInfo() {
    val settings =
      combine(
        browserPreferences.watchedThreshold.changes(),
        appearancePreferences.showUnplayedOldVideoLabel.changes(),
        appearancePreferences.unplayedOldVideoDays.changes(),
      ) { threshold, showNewLabel, days ->
        BadgeSettings(
          watchedFraction = threshold / 100f,
          showNewLabel = showNewLabel,
          newLabelDays = days,
        )
      }

    combine(
      _videos,
      PlaybackHistoryCache.playback.filterNotNull(),
      PlaybackHistoryCache.markedNew.filterNotNull(),
      settings,
    ) { videos, history, markedNew, badgeSettings ->
      buildPlaybackInfo(videos, history, markedNew, badgeSettings, _videosWithPlaybackInfo.value)
    }
      .flowOn(Dispatchers.Default)
      .onEach { _videosWithPlaybackInfo.value = it }
      .launchIn(viewModelScope)
  }

  /** Waits (briefly) until the snapshot for a list of [size] videos exists, so the list never flashes empty. */
  private suspend fun awaitPlaybackInfo(size: Int) {
    withTimeoutOrNull(3_000) { _videosWithPlaybackInfo.first { it.size == size } }
  }

  private fun buildPlaybackInfo(
    videos: List<Video>,
    history: Map<String, app.marlboroadvance.mpvex.database.dao.PlaybackBadgeRow>,
    markedNew: Set<String>,
    settings: BadgeSettings,
    previous: List<VideoWithPlaybackInfo>,
  ): List<VideoWithPlaybackInfo> {
    val previousById = previous.associateBy { it.video.id }
    val now = System.currentTimeMillis()
    val newLabelMillis = settings.newLabelDays * 24 * 60 * 60 * 1000L

    return videos.map { video ->
      val state = history[video.displayName]

      var progress: Float? = null
      var isWatched = false
      if (state != null && video.duration > 0) {
        // Duration is in milliseconds, convert to seconds
        val durationSeconds = video.duration / 1000
        val watchedSeconds = durationSeconds - state.timeRemaining.toLong()
        val progressValue = (watchedSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)

        // Only show progress for videos that are 1-99% complete
        if (progressValue in 0.01f..0.99f) progress = progressValue
        isWatched = state.hasBeenWatched || progressValue >= settings.watchedFraction
      }

      // Unplayed means there is no playback state record.
      val isOldAndUnplayed = state == null
      val isMarkedNew = state == null && video.displayName in markedNew
      val isNew = isMarkedNew ||
        (settings.showNewLabel && isOldAndUnplayed && now - video.dateModified * 1000 <= newLabelMillis)

      val info = VideoWithPlaybackInfo(
        video = video,
        timeRemaining = state?.timeRemaining?.toLong(),
        progressPercentage = progress,
        isOldAndUnplayed = isOldAndUnplayed,
        isWatched = isWatched,
        isMarkedNew = isMarkedNew,
        isNew = isNew,
      )
      // Reuse the previous instance when nothing changed for this video.
      previousById[video.id]?.takeIf { it == info } ?: info
    }
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
