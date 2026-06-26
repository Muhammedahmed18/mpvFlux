package app.marlboroadvance.mpvex.ui.browser.folderlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.repository.MediaFileRepository
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.resume

class FolderListViewModel(
  application: Application,
) : BaseBrowserViewModel(application),
  KoinComponent {
  private val foldersPreferences: FoldersPreferences by inject()
  private val browserPreferences: app.marlboroadvance.mpvex.preferences.BrowserPreferences by inject()

  private val _allVideoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  private val _videoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  val videoFolders: StateFlow<List<VideoFolder>> = _videoFolders.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _hasCompletedInitialLoad = MutableStateFlow(false)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  private var currentScanJob: Job? = null
  private var cacheLoadJob: Job? = null

  companion object {
    private const val TAG = "FolderListViewModel"

    // ignoreUnknownKeys keeps an older on-disk cache readable after the model gains/loses a field.
    private val folderCacheJson = Json { ignoreUnknownKeys = true }

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
    // Skip the disk-cache write when nothing changed. (The StateFlow itself already dedups equal
    // values, so this guard exists to avoid the redundant SharedPreferences write, not the emission.)
    if (_videoFolders.value != filtered) {
      _videoFolders.value = filtered
      saveFoldersToCache(filtered)
    }
  }

  private fun loadCachedFolders() {
    cacheLoadJob = viewModelScope.launch(Dispatchers.IO) {
      val prefs = getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
      val cachedJson = prefs.getString("folders", null)

      if (cachedJson != null) {
        try {
          val folders = folderCacheJson.decodeFromString<List<VideoFolder>>(cachedJson)
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
        val json = folderCacheJson.encodeToString(folders)
        prefs.edit().putString("folders", json).apply()
      } catch (e: Exception) {
        Log.e(TAG, "Error saving folders to cache", e)
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

  suspend fun deleteFolders(folders: List<VideoFolder>): Pair<Int, Int> =
    deleteFoldersWithProgress(folders.map { it.path })

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
        _allVideoFolders.value = folders
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
