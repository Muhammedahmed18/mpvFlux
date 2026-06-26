package app.marlboroadvance.mpvex.ui.browser.filesystem

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.browser.PathComponent
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.media.MetadataRetrieval
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import app.marlboroadvance.mpvex.utils.storage.FolderViewScanner
import app.marlboroadvance.mpvex.utils.storage.TreeViewScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * ViewModel for FileSystem Browser - based on Fossify's ItemsFragment logic
 * Handles directory navigation, file loading, sorting, and state management
 */
class FileSystemBrowserViewModel(
  application: Application,
  initialPath: String? = null,
) : BaseBrowserViewModel(application),
  KoinComponent {
  // playbackStateRepository is provided by BaseBrowserViewModel
  private val browserPreferences: BrowserPreferences by inject()
  private val appearancePreferences: app.marlboroadvance.mpvex.preferences.AppearancePreferences by inject()

  // Special marker for "show storage volumes" mode
  // Similar to Fossify's root/home folder detection
  private val STORAGE_ROOTS_MARKER = "__STORAGE_ROOTS__"

  // Home directory - the top-most directory we can navigate to (set once on init)
  // This prevents navigation errors when the app opens to a specific storage volume
  private var homeDirectory: String? = null

  // Current directory path - corresponds to Fossify's currentPath
  // If initialPath is null, we'll determine it after checking storage volumes
  private val _currentPath = MutableStateFlow(initialPath ?: STORAGE_ROOTS_MARKER)
  val currentPath: StateFlow<String> = _currentPath.asStateFlow()

  // Unsorted items from filesystem scan - before sorting is applied
  // Similar to Fossify's items list before sorting
  private val _unsortedItems = MutableStateFlow<List<FileSystemItem>>(emptyList())

  // Sorted and filtered items ready for display
  // Similar to Fossify's final sorted items list
  private val _items = MutableStateFlow<List<FileSystemItem>>(emptyList())
  val items: StateFlow<List<FileSystemItem>> = _items.asStateFlow()

  // Video playback progress map - similar to Fossify's playback tracking
  private val _videoFilesWithPlayback = MutableStateFlow<Map<Long, Float>>(emptyMap())
  val videoFilesWithPlayback: StateFlow<Map<Long, Float>> = _videoFilesWithPlayback.asStateFlow()

  // Loading state - similar to Fossify's showProgressBar/hideProgressBar
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Error state for displaying error messages
  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  // Breadcrumb components for navigation - similar to Fossify's Breadcrumbs
  private val _breadcrumbs = MutableStateFlow<List<PathComponent>>(emptyList())
  val breadcrumbs: StateFlow<List<PathComponent>> = _breadcrumbs.asStateFlow()

  // Whether we're at the home directory (top-most allowed directory)
  // Similar to Fossify's check for home folder or root
  val isAtRoot: StateFlow<Boolean> =
    _currentPath
      .map { it == STORAGE_ROOTS_MARKER || it == homeDirectory }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialPath == null)

  companion object {
    private const val TAG = "FileSystemBrowserVM"

    fun factory(
      application: Application,
      initialPath: String? = null,
    ) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FileSystemBrowserViewModel(application, initialPath) as T
    }
  }

  init {
    // If no initial path was specified, check storage volumes and navigate accordingly
    if (initialPath == null) {
      viewModelScope.launch(Dispatchers.IO) {
        val roots = MediaFileRepository.getStorageRoots(getApplication())
        if (roots.size == 1) {
          // Only one storage volume, navigate directly to it and set as home
          val singleRoot = roots.first()
          homeDirectory = singleRoot.path
          Log.d(TAG, "Single storage volume found, setting as home: ${singleRoot.path}")
          _currentPath.value = singleRoot.path
        } else {
          // Multiple roots - home is the storage roots view
          homeDirectory = null
        }
        // If multiple roots or none, stay at STORAGE_ROOTS_MARKER
        loadCurrentDirectory()
      }
    } else {
      // Specific path provided - set it as home directory
      homeDirectory = initialPath
      Log.d(TAG, "Initial path provided, setting as home: $initialPath")
      // Load initial directory - similar to Fossify's openPath() in onCreate
      loadCurrentDirectory()
    }

    // Refresh on global media library changes
    // Similar to Fossify's media scan completion listener
    viewModelScope.launch(Dispatchers.IO) {
      MediaLibraryEvents.changes.collectLatest {
        // Clear cache when media library changes
        MediaFileRepository.clearCache()
        loadCurrentDirectory()
      }
    }

    // Apply sorting whenever items or sort preferences change
    // Based on Fossify's ChangeSortingDialog callback and sorting logic
    viewModelScope.launch {
      combine(
        _unsortedItems,
        browserPreferences.folderSortType.changes(),
        browserPreferences.folderSortOrder.changes(),
      ) { items, sortType, sortOrder ->
        // Sort using the same logic as Fossify's FileDirItem.sort()
        SortUtils.sortFileSystemItems(items, sortType, sortOrder)
      }.collectLatest { sortedItems ->
        _items.value = sortedItems
        Log.d(TAG, "Items sorted: ${sortedItems.size} items")
      }
    }
  }

  /**
   * Refresh current directory
   * Equivalent to Fossify's refreshFragment() callback
   */
  override fun refresh() {
    Log.d(TAG, "Refreshing current directory: ${_currentPath.value}")
    
    // Only show full loading state if we have no items yet
    if (_items.value.isEmpty()) {
      _isLoading.value = true
    }
    
    // Clear all caches to force fresh data from filesystem
    MediaFileRepository.clearCache()
    FolderViewScanner.clearCache()
    TreeViewScanner.clearCache()
    
    // Trigger media scan in background
    triggerMediaScan()
    
    // Reload directory immediately without artificial delay
    loadCurrentDirectory()
  }
  
  /**
   * Trigger a media scan for the current directory
   */
  private fun triggerMediaScan() {
    try {
      val path = _currentPath.value
      
      // Skip if we're at storage roots marker
      if (path == STORAGE_ROOTS_MARKER) {
        return
      }
      
      val folder = File(path)
      
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
          ) { scanPath, uri ->
            Log.d(TAG, "Media scan completed for: $scanPath -> $uri")
          }
          
          Log.d(TAG, "Triggered media scan for ${filePaths.size} files in: $path")
        } else {
          Log.d(TAG, "No video files found in folder: $path")
        }
      } else {
        // Fallback to scanning external storage root
        val externalStorage = android.os.Environment.getExternalStorageDirectory()
        android.media.MediaScannerConnection.scanFile(
          getApplication(),
          arrayOf(externalStorage.absolutePath),
          null,
        ) { scanPath, uri ->
          Log.d(TAG, "Media scan completed for: $scanPath -> $uri")
        }
        Log.d(TAG, "Triggered media scan for: ${externalStorage.absolutePath}")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to trigger media scan", e)
    }
  }

  /**
   * Delete folders (and their contents) file-by-file with live progress, cancellation, and full
   * trace cleanup. Shared with the album view via [deleteFoldersWithProgress] in the base ViewModel.
   */
  suspend fun deleteFolders(folders: List<FileSystemItem.Folder>): Pair<Int, Int> =
    deleteFoldersWithProgress(folders.map { it.path })

  /**
   * Delete videos - delegates to base class implementation
   * Similar to Fossify's deleteFiles() for individual files
   */
  override suspend fun deleteVideos(videos: List<Video>): Pair<Int, Int> {
    Log.d(TAG, "Deleting ${videos.size} videos")
    return super.deleteVideos(videos)
  }

  /**
   * Rename a folder on the filesystem
   */
  suspend fun renameFolder(folder: FileSystemItem.Folder, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val oldFile = File(folder.path)
      val newFile = File(oldFile.parent, newName)

      if (newFile.exists()) {
        return@withContext Result.failure(Exception("A folder with this name already exists"))
      }

      val success = oldFile.renameTo(newFile)
      if (success) {
        Log.d(TAG, "Successfully renamed folder from ${oldFile.path} to ${newFile.path}")

        // Update local state immediately to avoid flicker
        val updatedItems = _unsortedItems.value.map { item ->
            if (item is FileSystemItem.Folder && item.path == oldFile.path) {
                item.copy(name = newName, path = newFile.absolutePath)
            } else {
                item
            }
        }
        _unsortedItems.value = updatedItems

        // Notify MediaStore about both paths to trigger indexing updates
        android.media.MediaScannerConnection.scanFile(
          getApplication(),
          arrayOf(oldFile.absolutePath, newFile.absolutePath),
          null
        ) { _, _ -> }

        Result.success(Unit)
      } else {
        Result.failure(Exception("Could not rename folder. Check permissions."))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error renaming folder", e)
      Result.failure(e)
    }
  }

  /**
   * Rename a video file - delegates to base class implementation
   * Based on Fossify's RenameDialog and file renaming logic
   */
  override suspend fun renameVideo(
    video: Video,
    newDisplayName: String,
  ): Result<Unit> {
    Log.d(TAG, "Renaming video ${video.displayName} to $newDisplayName")
    val result = super.renameVideo(video, newDisplayName)
    if (result.isSuccess) {
        // Soft update local state immediately
        val oldPath = video.path
        val parent = File(oldPath).parentFile
        val newPath = parent?.let { File(it, newDisplayName).absolutePath } ?: oldPath
        
        val updatedItems = _unsortedItems.value.map { item ->
            if (item is FileSystemItem.VideoFile && item.video.path == oldPath) {
                val updatedVideo = item.video.copy(
                    path = newPath,
                    displayName = newDisplayName,
                    title = newDisplayName.substringBeforeLast('.'),
                    uri = Uri.fromFile(File(newPath))
                )
                item.copy(video = updatedVideo, name = newDisplayName, path = newPath)
            } else {
                item
            }
        }
        _unsortedItems.value = updatedItems
    }
    return result
  }

  /**
   * Load current directory contents
   * Main loading logic based on Fossify's getItems() and getRegularItemsOf()
   */
  private fun loadCurrentDirectory() {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      _error.value = null

      try {
        val path = _currentPath.value

        // Special case: Show storage roots at the special marker
        // Similar to Fossify's StoragePickerDialog logic
        if (path == STORAGE_ROOTS_MARKER) {
          Log.d(TAG, "Loading storage roots")
          _breadcrumbs.value = emptyList()
          val roots = MediaFileRepository.getStorageRoots(getApplication())
          _unsortedItems.value = roots
          Log.d(TAG, "Loaded ${roots.size} storage roots")
        } else {
          // Update breadcrumbs for real paths
          // Similar to Fossify's Breadcrumbs.setBreadcrumb()
          _breadcrumbs.value = MediaFileRepository.getPathComponents(path)
          Log.d(TAG, "Breadcrumbs updated: ${_breadcrumbs.value.size} components")

          // Get hidden files preference
          // Scan directory - equivalent to Fossify's getRegularItemsOf()
          // Always show only videos (showAllFileTypes = false)
          MediaFileRepository
            .scanDirectory(getApplication(), path, showAllFileTypes = false)
            .onSuccess { freshItems ->
              
              // Merge with existing metadata to avoid UI flicker and redundant enrichment
              val currentVideos = _unsortedItems.value.filterIsInstance<FileSystemItem.VideoFile>()
                  .associateBy { it.video.path }
                  
              val mergedItems = freshItems.map { item ->
                  if (item is FileSystemItem.VideoFile) {
                      val existing = currentVideos[item.video.path]
                      if (existing != null && (existing.video.fps > 0f || existing.video.subtitleCodec.isNotEmpty())) {
                          item.copy(video = item.video.copy(
                              fps = existing.video.fps,
                              resolution = existing.video.resolution,
                              hasEmbeddedSubtitles = existing.video.hasEmbeddedSubtitles,
                              subtitleCodec = existing.video.subtitleCodec
                          ))
                      } else {
                          item
                      }
                  } else {
                      item
                  }
              }

              _unsortedItems.value = mergedItems

              val folderCount = mergedItems.filterIsInstance<FileSystemItem.Folder>().size
              val videoCount = mergedItems.filterIsInstance<FileSystemItem.VideoFile>().size
              Log.d(TAG, "Loaded directory: $path with $folderCount folders, $videoCount videos")

              // Enrich videos with metadata if chips are enabled
              val enrichedItems = if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences)) {
                Log.d(TAG, "Metadata chips enabled, enriching $videoCount videos")
                val videoFiles = mergedItems.filterIsInstance<FileSystemItem.VideoFile>()
                val videos = videoFiles.map { it.video }
                val enrichedVideos = MetadataRetrieval.enrichVideosIfNeeded(
                  context = getApplication(),
                  videos = videos,
                  browserPreferences = browserPreferences,
                  metadataCache = metadataCache
                )
                
                // Replace videos in items with enriched versions
                val enrichedVideoMap = enrichedVideos.associateBy { it.id }
                mergedItems.map { item ->
                  when (item) {
                    is FileSystemItem.VideoFile -> {
                      val enrichedVideo = enrichedVideoMap[item.video.id]
                      if (enrichedVideo != null) {
                        item.copy(video = enrichedVideo)
                      } else {
                        item
                      }
                    }
                    else -> item
                  }
                }
              } else {
                mergedItems
              }

              _unsortedItems.value = enrichedItems

              // Load playback info for videos
              // Similar to Fossify's playback state tracking
              loadPlaybackInfo(enrichedItems)
            }.onFailure { error ->
              _error.value = error.message
              _unsortedItems.value = emptyList()
              Log.e(TAG, "Error loading directory: $path", error)
            }
        }
      } catch (e: Exception) {
        _error.value = e.message
        _unsortedItems.value = emptyList()
        Log.e(TAG, "Exception loading directory", e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Load playback progress information for video files
   * Based on playback state tracking (not directly in Fossify, but similar pattern)
   */
  private fun loadPlaybackInfo(items: List<FileSystemItem>) {
    viewModelScope.launch(Dispatchers.IO) {
      val videoFiles = items.filterIsInstance<FileSystemItem.VideoFile>()
      val playbackMap = mutableMapOf<Long, Float>()

      Log.d(TAG, "Loading playback info for ${videoFiles.size} videos")

      videoFiles.forEach { videoFile ->
        val video = videoFile.video
        val playbackState = playbackStateRepository.getVideoDataByTitle(video.displayName)

        if (playbackState != null && video.duration > 0) {
          val durationSeconds = video.duration / 1000
          val timeRemaining = playbackState.timeRemaining.toLong()
          val watched = durationSeconds - timeRemaining
          val progressValue = (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)

          // Only show progress for videos that are 1-99% complete
          // Similar to how media players show partial progress
          if (progressValue in 0.01f..0.99f) {
            playbackMap[video.id] = progressValue
          }
        }
      }

      _videoFilesWithPlayback.value = playbackMap
      Log.d(TAG, "Loaded playback info for ${playbackMap.size} videos with progress")
    }
  }
}
