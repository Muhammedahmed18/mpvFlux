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
import app.marlboroadvance.mpvex.utils.storage.FolderIndexRepository
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class FolderListViewModel(
  application: Application,
) : BaseBrowserViewModel(application),
  KoinComponent {
  private val foldersPreferences: FoldersPreferences by inject()
  private val browserPreferences: app.marlboroadvance.mpvex.preferences.BrowserPreferences by inject()

  // The persistent per-folder index (Room) is the cache: it survives restarts and is updated
  // one folder at a time.
  private val folderIndex: FolderIndexRepository by inject()

  private val _allVideoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  private val _videoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  val videoFolders: StateFlow<List<VideoFolder>> = _videoFolders.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _hasCompletedInitialLoad = MutableStateFlow(false)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  // True once the saved index has been read and reflected in [videoFolders] (or is known to be
  // empty). Until then the list is simply not ready, which is not the same as "scanning".
  private val _hasCheckedIndex = MutableStateFlow(false)
  val hasCheckedIndex: StateFlow<Boolean> = _hasCheckedIndex.asStateFlow()

  private var currentScanJob: Job? = null

  // Folder/file paths reported by MediaLibraryEvents since the last debounced refresh.
  private val pendingChangedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

  companion object {
    private const val TAG = "FolderListViewModel"

    fun factory(application: Application) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FolderListViewModel(application) as T
      }
  }

  init {
    // Cached folders show immediately; later index writes (any screen) flow in here too.
    observeIndex()

    observeLibraryChanges()

    // Reconcile quietly in the background when there is already an index (no spinner).
    // An empty index is seeded once the storage permission callback fires.
    viewModelScope.launch {
      if (!folderIndex.isEmpty()) loadVideoFolders()
    }

    // Drop the old SharedPreferences folder cache; the Room index replaces it.
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        getApplication<Application>()
          .getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
          .edit()
          .remove("folders")
          .apply()
      }
    }

    combine(_allVideoFolders, foldersPreferences.blacklistedFolders.changes()) { folders, blacklist ->
      Pair(folders, blacklist)
    }
      .distinctUntilChanged()
      .onEach { (folders, blacklist) ->
        applyFiltersAndNotify(folders, blacklist)
      }
      .launchIn(viewModelScope)
  }

  private fun observeIndex() {
    folderIndex
      .observeVideoFolders()
      .onEach { folders ->
        if (folders.isNotEmpty()) {
          _allVideoFolders.value = folders
          _hasCompletedInitialLoad.value = true
        } else if (_hasCompletedInitialLoad.value) {
          // Index was emptied after a completed load (all videos removed).
          _allVideoFolders.value = emptyList()
        } else {
          // Nothing indexed yet (first launch): the scan state may show right away.
          _hasCheckedIndex.value = true
        }
      }.launchIn(viewModelScope)
  }

  // debounce(Long) is a @FlowPreview API; opt in at the narrowest scope.
  @OptIn(FlowPreview::class)
  private fun observeLibraryChanges() {
    MediaLibraryEvents.changedPaths
      .onEach { pendingChangedPaths += it }
      .launchIn(viewModelScope)

    MediaLibraryEvents.changes
      .debounce(500L)
      .onEach { onLibraryChanged() }
      .launchIn(viewModelScope)
  }

  private fun onLibraryChanged() {
    val paths = pendingChangedPaths.toList()
    pendingChangedPaths.removeAll(paths.toSet())
    if (paths.isEmpty()) {
      // Unscoped change: cheap diff of the library, only changed folders are written.
      loadVideoFolders()
    } else {
      // Scoped change: re-read just the affected folders.
      viewModelScope.launch {
        try {
          folderIndex.refreshAffected(paths)
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.e(TAG, "Error refreshing affected folders", e)
        }
      }
    }
  }

  private fun applyFiltersAndNotify(allFolders: List<VideoFolder>, blacklist: Set<String>) {
    val filtered = allFolders.filter { it.path !in blacklist && it.videoCount > 0 }
    // The StateFlow dedups equal values itself.
    _videoFolders.value = filtered
    // Only the saved index can make this non-empty, so the cached list is now on its way to
    // the screen (set after the list so the screen never sees the flag with an empty list).
    if (allFolders.isNotEmpty()) _hasCheckedIndex.value = true
  }

  override fun refresh() {
    performFullSystemScan()
  }

  /** Pull-to-refresh: suspends until the scan finishes so the pull spinner tracks it. */
  suspend fun refreshAndAwait() {
    performFullSystemScan().join()
  }

  fun ensureDataLoaded() {
    if (_allVideoFolders.value.isEmpty() && !_isLoading.value && !_isScanning.value) {
      loadVideoFolders()
    }
  }

  fun onPermissionGranted() {
    // Granted on every launch too: only do the heavy scan when there is nothing indexed yet,
    // otherwise show the cache and reconcile quietly in the background.
    viewModelScope.launch {
      if (folderIndex.isEmpty()) performFullSystemScan() else loadVideoFolders()
    }
  }

  private fun performFullSystemScan(): Job {
    currentScanJob?.cancel()
    val job = viewModelScope.launch {
      _isScanning.value = true
      try {
        withContext(Dispatchers.IO) {
          triggerMediaScanAwait()
          folderIndex.reconcileAll()
        }
        _hasCompletedInitialLoad.value = true
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "Full system scan failed", e)
      } finally {
        _isScanning.value = false
        _isLoading.value = false
      }
    }
    currentScanJob = job
    return job
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
        // Move the index row so the list updates in place instead of dropping the folder.
        folderIndex.renameFolder(folder.path, newFile.absolutePath)
        android.media.MediaScannerConnection.scanFile(getApplication(), arrayOf(oldFile.path, newFile.path), null) { _, _ -> }
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteFolders(folders: List<VideoFolder>): Pair<Int, Int> {
    val result = deleteFoldersWithProgress(folders.map { it.path })
    // Drop only the folders that are really gone; a partial failure keeps its row.
    withContext(Dispatchers.IO) {
      folders.filter { !File(it.path).exists() }.forEach { folderIndex.removeSubtree(it.path) }
    }
    return result
  }

  /**
   * All videos in [folders], folder by folder in the given order, each folder's videos in the
   * user's video sort order. Used by the selection toolbar (Play, Mark as, Add to playlist).
   */
  suspend fun videosInFolders(folders: List<VideoFolder>): List<app.marlboroadvance.mpvex.domain.media.model.Video> =
    withContext(Dispatchers.IO) {
      val sortType = browserPreferences.videoSortType.get()
      val sortOrder = browserPreferences.videoSortOrder.get()
      folders.flatMap { folder ->
        try {
          app.marlboroadvance.mpvex.utils.sort.SortUtils.sortVideos(
            MediaFileRepository.getVideosInFolder(getApplication(), folder.bucketId),
            sortType,
            sortOrder,
          )
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.e(TAG, "Error loading videos for ${folder.path}", e)
          emptyList()
        }
      }
    }

  /**
   * Hides [folders] from the library by adding them to the folder blacklist.
   * Returns the previous blacklist so the caller can offer Undo via [restoreBlacklist].
   */
  fun hideFolders(folders: List<VideoFolder>): Set<String> {
    val previous = foldersPreferences.blacklistedFolders.get()
    foldersPreferences.blacklistedFolders.set(previous + folders.map { it.path })
    return previous
  }

  fun restoreBlacklist(previous: Set<String>) {
    foldersPreferences.blacklistedFolders.set(previous)
  }

  /**
   * Brings the index up to date in the background. The spinner is only shown while the index is
   * empty (first launch); otherwise cached folders stay on screen and changes flow in.
   */
  fun loadVideoFolders(forceShowScanning: Boolean = false) {
    currentScanJob?.cancel()
    currentScanJob = viewModelScope.launch {
      val indexEmpty = folderIndex.isEmpty()
      if (indexEmpty) _isLoading.value = true
      if (indexEmpty || forceShowScanning) _isScanning.value = true

      try {
        withContext(Dispatchers.IO) { folderIndex.reconcileAll() }
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
