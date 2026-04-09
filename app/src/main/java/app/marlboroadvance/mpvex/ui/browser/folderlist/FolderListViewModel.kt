package app.marlboroadvance.mpvex.ui.browser.folderlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.FoldersPreferences
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.media.MetadataRetrieval
import app.marlboroadvance.mpvex.utils.storage.FolderViewScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

data class FolderWithNewCount(
  val folder: VideoFolder,
  val newVideoCount: Int = 0,
)

class FolderListViewModel(
  application: Application,
) : BaseBrowserViewModel(application),
  KoinComponent {
  private val foldersPreferences: FoldersPreferences by inject()
  private val appearancePreferences: AppearancePreferences by inject()
  private val browserPreferences: app.marlboroadvance.mpvex.preferences.BrowserPreferences by inject()
  private val playbackStateRepository: PlaybackStateRepository by inject()

  private val _allVideoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  private val _videoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  val videoFolders: StateFlow<List<VideoFolder>> = _videoFolders.asStateFlow()

  private val _foldersWithNewCount = MutableStateFlow<List<FolderWithNewCount>>(emptyList())
  val foldersWithNewCount: StateFlow<List<FolderWithNewCount>> = _foldersWithNewCount.asStateFlow()

  // Only show loading on fresh install (when there's no cached data)
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Track if initial load has completed to prevent empty state flicker
  // Default to true to prevent "Scanning" UI on tab switch
  private val _hasCompletedInitialLoad = MutableStateFlow(true)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  // Track if folders were deleted leaving list empty
  private val _foldersWereDeleted = MutableStateFlow(false)
  val foldersWereDeleted: StateFlow<Boolean> = _foldersWereDeleted.asStateFlow()

  // Track previous folder count to detect if all folders were deleted
  private var previousFolderCount = 0

  /*
   * TRACKING LOADING STATE
   */
  private val _scanStatus = MutableStateFlow<String?>(null)
  val scanStatus: StateFlow<String?> = _scanStatus.asStateFlow()

  private val _isEnriching = MutableStateFlow(false)
  val isEnriching: StateFlow<Boolean> = _isEnriching.asStateFlow()

  // Track the current scan job to prevent concurrent scans
  private var currentScanJob: Job? = null

  companion object {
    private const val TAG = "FolderListViewModel"

    fun factory(application: Application) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FolderListViewModel(application) as T
      }
  }

  init {
    // Load cached folders instantly for immediate display
    val hasCachedData = loadCachedFolders()

    // If no cached data (first launch), scan immediately and show loading. 
    if (!hasCachedData) {
      _hasCompletedInitialLoad.value = false
      loadVideoFolders()
    } else {
      // If we have data, we scan silently in background after a short delay
      viewModelScope.launch(Dispatchers.IO) {
        kotlinx.coroutines.delay(2000) 
        loadVideoFolders()
      }
    }

    // Refresh folders on global media library changes
    MediaLibraryEvents.changes
      .onEach {
        MediaFileRepository.clearCache()
        loadVideoFolders()
      }
      .launchIn(viewModelScope)

    // Filter folders based on blacklist
    viewModelScope.launch {
      combine(_allVideoFolders, foldersPreferences.blacklistedFolders.changes()) { folders, blacklist ->
        folders.filter { folder -> folder.path !in blacklist && folder.videoCount > 0 }
      }.collectLatest { filteredFolders ->
        if (previousFolderCount > 0 && filteredFolders.isEmpty()) {
          _foldersWereDeleted.value = true
        } else if (filteredFolders.isNotEmpty()) {
          _foldersWereDeleted.value = false
        }
        previousFolderCount = filteredFolders.size
        _videoFolders.value = filteredFolders
        calculateNewVideoCounts(filteredFolders)
        saveFoldersToCache(filteredFolders)
      }
    }
  }

  private fun loadCachedFolders(): Boolean {
    var hasCachedData = false
    val prefs = getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
    val cachedJson = prefs.getString("folders", null)

    if (cachedJson != null) {
      try {
        val folders = parseFoldersFromJson(cachedJson)
        if (folders.isNotEmpty()) {
          Log.d(TAG, "Loaded ${folders.size} folders from cache instantly")
          hasCachedData = true
          _allVideoFolders.value = folders
          _hasCompletedInitialLoad.value = true
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error loading cached folders", e)
      }
    }
    return hasCachedData
  }

  private fun saveFoldersToCache(folders: List<VideoFolder>) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val prefs = getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
        val json = serializeFoldersToJson(folders)
        prefs.edit().putString("folders", json).apply()
      } catch (e: Exception) {
        Log.e(TAG, "Error saving folders to cache")
      }
    }
  }

  private fun serializeFoldersToJson(folders: List<VideoFolder>): String {
    return folders.joinToString(separator = "|") { folder ->
      "${folder.bucketId}::${folder.name}::${folder.path}::${folder.videoCount}::${folder.totalSize}::${folder.totalDuration}::${folder.lastModified}"
    }
  }

  private fun parseFoldersFromJson(json: String): List<VideoFolder> {
    return try {
      json.split("|").mapNotNull { item ->
        val parts = item.split("::")
        if (parts.size == 7) {
          VideoFolder(
            bucketId = parts[0],
            name = parts[1],
            path = parts[2],
            videoCount = parts[3].toIntOrNull() ?: 0,
            totalSize = parts[4].toLongOrNull() ?: 0L,
            totalDuration = parts[5].toLongOrNull() ?: 0L,
            lastModified = parts[6].toLongOrNull() ?: 0L,
          )
        } else null
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  private fun calculateNewVideoCounts(folders: List<VideoFolder>) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val showLabel = appearancePreferences.showUnplayedOldVideoLabel.get()
        if (!showLabel) {
          _foldersWithNewCount.value = folders.map { FolderWithNewCount(it, 0) }
          return@launch
        }

        val thresholdDays = appearancePreferences.unplayedOldVideoDays.get()
        val thresholdMillis = thresholdDays * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val foldersWithCounts = folders.map { folder ->
          try {
            val videos = app.marlboroadvance.mpvex.repository.MediaFileRepository
              .getVideosInFolder(getApplication(), folder.bucketId)

            val newCount = videos.count { video ->
              val videoAge = currentTime - (video.dateModified * 1000)
              val isRecent = videoAge <= thresholdMillis
              val playbackState = playbackStateRepository.getVideoDataByTitle(video.displayName)
              val isUnplayed = playbackState == null
              isRecent && isUnplayed
            }
            FolderWithNewCount(folder, newCount)
          } catch (e: Exception) {
            FolderWithNewCount(folder, 0)
          }
        }
        _foldersWithNewCount.value = foldersWithCounts
      } catch (e: Exception) {
        _foldersWithNewCount.value = folders.map { FolderWithNewCount(it, 0) }
      }
    }
  }

  override fun refresh() {
    Log.d(TAG, "Hard refreshing folder list (Pull to Refresh)")
    
    // Condition 2: Reset completion flag to force scanning UI for pull-to-refresh
    _isLoading.value = true
    _hasCompletedInitialLoad.value = false
    _scanStatus.value = "Preparing scan..."
    
    MediaFileRepository.clearCache()
    FolderViewScanner.clearCache()
    triggerMediaScan()
    
    viewModelScope.launch(Dispatchers.IO) {
      kotlinx.coroutines.delay(1000)
      loadVideoFolders()
    }
  }

  fun ensureDataLoaded() {
    if (_allVideoFolders.value.isEmpty()) {
      loadVideoFolders()
    }
  }

  /**
   * Called when storage permission is granted.
   */
  fun onPermissionGranted() {
    val isListEmpty = _allVideoFolders.value.isEmpty()
    Log.d(TAG, "Permission granted (isListEmpty=$isListEmpty), triggering scan")
    
    // Condition 1: Only force scanning UI if list is empty (first time grant)
    if (isListEmpty) {
      _isLoading.value = true
      _hasCompletedInitialLoad.value = false
      _scanStatus.value = "Preparing scan..."
    }
    
    MediaFileRepository.clearCache()
    FolderViewScanner.clearCache()
    triggerMediaScan()
    
    viewModelScope.launch(Dispatchers.IO) {
      kotlinx.coroutines.delay(800)
      loadVideoFolders()
    }
  }
  
  private fun triggerMediaScan() {
    try {
      val externalStorage = android.os.Environment.getExternalStorageDirectory()
      android.media.MediaScannerConnection.scanFile(
        getApplication(),
        arrayOf(externalStorage.absolutePath),
        null,
      ) { path, uri ->
        Log.d(TAG, "Media scan completed for: $path -> $uri")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to trigger media scan", e)
    }
  }

  fun recalculateNewVideoCounts() {
    calculateNewVideoCounts(_videoFolders.value)
  }

  suspend fun renameFolder(folder: VideoFolder, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val oldFile = File(folder.path)
      val newFile = File(oldFile.parent, newName)
      if (newFile.exists()) return@withContext Result.failure(Exception("Exists"))

      if (oldFile.renameTo(newFile)) {
        android.media.MediaScannerConnection.scanFile(getApplication(), arrayOf(oldFile.path, newFile.path), null) { _, _ -> }
        Result.success(Unit)
      } else Result.failure(Exception("Failed"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun loadVideoFolders() {
    currentScanJob?.cancel()
    currentScanJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        val hasExistingData = _allVideoFolders.value.isNotEmpty()
        
        // Only show full-screen loading if explicitly requested (via _hasCompletedInitialLoad = false)
        // or if we have absolutely no data to show yet.
        if (!hasExistingData || !_hasCompletedInitialLoad.value) {
          _isLoading.value = true
          _scanStatus.value = "Scanning storage..."
        }

        val currentFoldersMap = _allVideoFolders.value.associateBy { it.bucketId }

        val fastFolders = app.marlboroadvance.mpvex.repository.MediaFileRepository
          .getAllVideoFoldersFast(
            context = getApplication(),
            onProgress = { count -> _scanStatus.value = "Found $count folders..." }
          )

        if (fastFolders.isEmpty() && hasExistingData && _hasCompletedInitialLoad.value) {
             _isLoading.value = false
             _scanStatus.value = null
             return@launch
        }

        var needsEnrichment = false
        val mergedFolders = fastFolders.map { fastFolder ->
             val cached = currentFoldersMap[fastFolder.bucketId]
             val cachedIsEnriched = cached != null && (cached.videoCount == 0 || cached.totalDuration > 0)
             
             if (cached != null && cached.videoCount == fastFolder.videoCount && 
                 cached.lastModified == fastFolder.lastModified && cachedIsEnriched) {
                 cached
             } else {
                 needsEnrichment = true
                 fastFolder
             }
        }

        _allVideoFolders.value = mergedFolders
        _isLoading.value = false 
        _hasCompletedInitialLoad.value = true
        
        if (mergedFolders.isEmpty()) {
             _scanStatus.value = null
             return@launch
        }

        val needsDurationEnrichment = needsEnrichment && MetadataRetrieval.isFolderMetadataNeeded(browserPreferences)
        if (!needsDurationEnrichment) {
             _scanStatus.value = null
             return@launch
        }

        _isEnriching.value = true
        _scanStatus.value = "Processing metadata..."
        
        val enrichedFolders = MetadataRetrieval.enrichFoldersIfNeeded(
            context = getApplication(),
            folders = mergedFolders,
            browserPreferences = browserPreferences,
            metadataCache = metadataCache,
            onProgress = { processed, total ->
               _scanStatus.value = "Processing metadata $processed/$total"
            }
          )
        _allVideoFolders.value = enrichedFolders

      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error loading", e)
        _hasCompletedInitialLoad.value = true
      } finally {
        _isLoading.value = false
        _isEnriching.value = false
        _scanStatus.value = null
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    currentScanJob?.cancel()
  }
}
