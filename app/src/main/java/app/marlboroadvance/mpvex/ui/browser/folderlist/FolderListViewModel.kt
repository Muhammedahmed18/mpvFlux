package app.marlboroadvance.mpvex.ui.browser.folderlist

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.FoldersPreferences
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.storage.FolderViewScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.resume

@Immutable
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

  private val _allVideoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  private val _videoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())

  // A4: Added distinctUntilChanged to prevent UI recomposition if the list content hasn't changed
  val videoFolders: StateFlow<List<VideoFolder>> = _videoFolders.asStateFlow()

  private val _foldersWithNewCount = MutableStateFlow<List<FolderWithNewCount>>(emptyList())
  val foldersWithNewCount: StateFlow<List<FolderWithNewCount>> = _foldersWithNewCount.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _hasCompletedInitialLoad = MutableStateFlow(false)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  private val _scanStatus = MutableStateFlow<String?>(null)
  val scanStatus: StateFlow<String?> = _scanStatus.asStateFlow()

  private val _isEnriching = MutableStateFlow(false)
  val isEnriching: StateFlow<Boolean> = _isEnriching.asStateFlow()

  private var currentScanJob: Job? = null
  private var newVideoCountJob: Job? = null
  private var cacheLoadJob: Job? = null

  companion object {
    private const val TAG = "FolderListViewModel"

    fun factory(application: Application) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FolderListViewModel(application) as T
      }
  }

  init {
    loadCachedFolders()

    observeLibraryChanges()

    combine(_allVideoFolders, foldersPreferences.blacklistedFolders.changes()) { folders, blacklist ->
      Pair(folders, blacklist)
    }
      .distinctUntilChanged()
      .onEach { (folders, blacklist) ->
        applyFiltersAndNotify(folders, blacklist)
      }
      .launchIn(viewModelScope)
  }

  // debounce(Long) is a @FlowPreview API; opt in at the narrowest scope.
  @OptIn(FlowPreview::class)
  private fun observeLibraryChanges() {
    MediaLibraryEvents.changes
      .debounce(500L)
      .onEach { loadVideoFolders() }
      .launchIn(viewModelScope)
  }

  private suspend fun applyFiltersAndNotify(allFolders: List<VideoFolder>, blacklist: Set<String>) {
    val filtered = allFolders.filter { it.path !in blacklist && it.videoCount > 0 }
    // A4: Explicit equality check before updating the StateFlow
    if (_videoFolders.value != filtered) {
      _videoFolders.value = filtered
      calculateNewVideoCounts(filtered)
      saveFoldersToCache(filtered)
    }
  }

  private fun loadCachedFolders() {
    cacheLoadJob = viewModelScope.launch(Dispatchers.IO) {
      val prefs = getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
      val cachedJson = prefs.getString("folders", null)

      if (cachedJson != null) {
        try {
          val folders = parseFoldersFromJson(cachedJson)
          if (folders.isNotEmpty()) {
            withContext(Dispatchers.Main) {
              _allVideoFolders.value = folders
              _hasCompletedInitialLoad.value = true
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error loading cached folders", e)
        }
      }
    }
  }

  private fun saveFoldersToCache(folders: List<VideoFolder>) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val prefs = getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
        val json = serializeFoldersToJson(folders)
        prefs.edit().putString("folders", json).apply()
      } catch (e: Exception) {
        Log.e(TAG, "Error saving folders to cache", e)
      }
    }
  }

  private fun serializeFoldersToJson(folders: List<VideoFolder>): String {
    val array = JSONArray()
    for (folder in folders) {
      val obj = JSONObject().apply {
        put("bucketId", folder.bucketId)
        put("name", folder.name)
        put("path", folder.path)
        put("videoCount", folder.videoCount)
        put("totalSize", folder.totalSize)
        put("totalDuration", folder.totalDuration)
        put("lastModified", folder.lastModified)
      }
      array.put(obj)
    }
    return array.toString()
  }

  private fun parseFoldersFromJson(json: String): List<VideoFolder> {
    return try {
      val array = JSONArray(json)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        VideoFolder(
          bucketId    = obj.getString("bucketId"),
          name        = obj.getString("name"),
          path        = obj.getString("path"),
          videoCount  = obj.optInt("videoCount", 0),
          totalSize   = obj.optLong("totalSize", 0L),
          totalDuration = obj.optLong("totalDuration", 0L),
          lastModified  = obj.optLong("lastModified", 0L),
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing folders from JSON cache", e)
      emptyList()
    }
  }

  private fun calculateNewVideoCounts(folders: List<VideoFolder>) {
    newVideoCountJob?.cancel()
    newVideoCountJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        val showLabel = appearancePreferences.showUnplayedOldVideoLabel.get()
        if (!showLabel) {
          val emptyCounts = folders.map { FolderWithNewCount(it, 0) }
          withContext(Dispatchers.Main) {
            if (_foldersWithNewCount.value != emptyCounts) {
              _foldersWithNewCount.value = emptyCounts
            }
          }
          return@launch
        }

        val thresholdDays = appearancePreferences.unplayedOldVideoDays.get()
        val thresholdMillis = thresholdDays * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val foldersWithCounts = folders.map { folder ->
          try {
            val videos = MediaFileRepository.getVideosInFolder(getApplication(), folder.bucketId)
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

        withContext(Dispatchers.Main) {
          if (_foldersWithNewCount.value != foldersWithCounts) {
            _foldersWithNewCount.value = foldersWithCounts
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val fallback = folders.map { FolderWithNewCount(it, 0) }
        withContext(Dispatchers.Main) {
          if (_foldersWithNewCount.value != fallback) {
            _foldersWithNewCount.value = fallback
          }
        }
      }
    }
  }

  override fun refresh() {
    performFullSystemScan()
  }

  fun ensureDataLoaded() {
    if (_allVideoFolders.value.isEmpty() && !_isLoading.value && !_isScanning.value) {
      loadVideoFolders()
    }
  }

  fun onPermissionGranted() {
    performFullSystemScan()
  }

  private fun performFullSystemScan() {
    cacheLoadJob?.cancel()
    currentScanJob?.cancel()
    currentScanJob = viewModelScope.launch {
      _isScanning.value = true
      try {
        withContext(Dispatchers.IO) {
          MediaFileRepository.clearCache()
          FolderViewScanner.clearCache()
          triggerMediaScanAwait()
        }

        val folders = withContext(Dispatchers.IO) {
          MediaFileRepository.getAllVideoFolders(getApplication())
        }
        _allVideoFolders.value = folders
        _hasCompletedInitialLoad.value = true
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "Full system scan failed", e)
      } finally {
        _isScanning.value = false
        _isLoading.value = false
      }
    }
  }

  private suspend fun triggerMediaScanAwait() = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine<Unit> { continuation ->
      try {
        val knownPaths = _allVideoFolders.value.map { it.path }.distinct()
        val pathsToScan: Array<String> = if (knownPaths.isNotEmpty()) {
          knownPaths.toTypedArray()
        } else {
          arrayOf(android.os.Environment.getExternalStorageDirectory().absolutePath)
        }

        var completedCount = 0
        android.media.MediaScannerConnection.scanFile(
          getApplication(),
          pathsToScan,
          null,
        ) { path, uri ->
          completedCount++
          if (completedCount >= pathsToScan.size && continuation.isActive) {
            continuation.resume(Unit)
          }
        }
      } catch (e: Exception) {
        if (continuation.isActive) continuation.resume(Unit)
      }
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
        val updatedFolders = _allVideoFolders.value.map { f ->
          if (f.path == folder.path) {
            f.copy(name = newName, path = newFile.absolutePath, bucketId = newFile.absolutePath)
          } else {
            f
          }
        }
        withContext(Dispatchers.Main) {
          _allVideoFolders.value = updatedFolders
        }
        android.media.MediaScannerConnection.scanFile(getApplication(), arrayOf(oldFile.path, newFile.path), null) { _, _ -> }
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteFolders(folders: List<VideoFolder>): Pair<Int, Int> = withContext(Dispatchers.IO) {
    var successCount = 0
    var failureCount = 0

    folders.forEach { folder ->
      try {
        // Enumerate the folder's videos up-front: once the files are gone MediaStore
        // can no longer list them, so we'd lose the keys needed to purge their traces.
        val videos = runCatching {
          MediaFileRepository.getVideosInFolder(getApplication(), folder.bucketId)
        }.getOrDefault(emptyList())

        val dir = File(folder.path)
        val deleted = if (dir.exists()) dir.deleteRecursively() else true

        if (deleted) {
          purgeVideoTraces(videos)
          successCount++
        } else {
          failureCount++
        }
      } catch (e: Exception) {
        failureCount++
      }
    }

    if (successCount > 0) {
      MediaLibraryEvents.notifyChanged()
    }
    Pair(successCount, failureCount)
  }

  fun loadVideoFolders(forceShowScanning: Boolean = false) {
    val hasExistingData = _allVideoFolders.value.isNotEmpty()

    cacheLoadJob?.cancel()
    currentScanJob?.cancel()
    currentScanJob = viewModelScope.launch {
      if (!hasExistingData) _isLoading.value = true
      
      if (!hasExistingData || forceShowScanning) {
        _isScanning.value = true
      }

      try {
        val folders = withContext(Dispatchers.IO) {
          MediaFileRepository.getAllVideoFolders(getApplication())
        }
        // A4: Only update the flow if the new list is different
        if (_allVideoFolders.value != folders) {
          _allVideoFolders.value = folders
        }
        _hasCompletedInitialLoad.value = true
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "Error loading folders", e)
      } finally {
        _isLoading.value = false
        _isScanning.value = false
      }
    }
  }
}
