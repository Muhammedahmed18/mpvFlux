package app.marlboroadvance.mpvex.ui.browser.filesystem

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.NavigationBarState
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.AddToPlaylistDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FileOperationProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderPickerDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.sheets.SortBottomSheet
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.browser.states.PermissionDeniedState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.R

/**
 * File System Directory screen - shows contents of a specific directory
 */
@Serializable
data class FileSystemDirectoryScreen(
  val path: String,
) : app.marlboroadvance.mpvex.presentation.Screen {
  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  override fun Content() {
    FileSystemBrowserScreen(path = path)
  }
}

/**
 * File System Browser screen - browses directories and shows both folders and videos
 * @param path The directory path to browse, or null for storage roots
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileSystemBrowserScreen(path: String? = null) {
  val context = LocalContext.current
  val backstack = LocalBackStack.current
  val coroutineScope = rememberCoroutineScope()
  val browserPreferences = koinInject<BrowserPreferences>()
  val playerPreferences = koinInject<app.marlboroadvance.mpvex.preferences.PlayerPreferences>()
  val appearancePreferences = koinInject<app.marlboroadvance.mpvex.preferences.AppearancePreferences>()
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  // ViewModel - use path parameter if provided, otherwise show roots
  val viewModel: FileSystemBrowserViewModel = viewModel(
    key = "FileSystemBrowser_${path ?: "root"}",
    factory = FileSystemBrowserViewModel.factory(
      context.applicationContext as android.app.Application,
      path,
    ),
  )

  // State collection
  val currentPath by viewModel.currentPath.collectAsState()
  val items by viewModel.items.collectAsState()
  val videoFilesWithPlayback by viewModel.videoFilesWithPlayback.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val error by viewModel.error.collectAsState()
  val isAtRoot by viewModel.isAtRoot.collectAsState()
  val breadcrumbs by viewModel.breadcrumbs.collectAsState()
  val playlistMode by playerPreferences.playlistMode.collectAsState()
  val itemsWereDeletedOrMoved by viewModel.itemsWereDeletedOrMoved.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()

  // VideoCard settings
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val showVideoExtension by browserPreferences.showVideoExtension.collectAsState()
  val showSizeChip by browserPreferences.showSizeChip.collectAsState()
  val showResolutionChip by browserPreferences.showResolutionChip.collectAsState()
  val showFramerateInResolution by browserPreferences.showFramerateInResolution.collectAsState()
  val showProgressBar by browserPreferences.showProgressBar.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showUnplayedOldVideoLabel by appearancePreferences.showUnplayedOldVideoLabel.collectAsState()
  val unplayedOldVideoDays by appearancePreferences.unplayedOldVideoDays.collectAsState()

  // FolderCard specific settings
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()

  val videoCardSettings = remember(
    unlimitedNameLines, showThumbnails, showVideoExtension, showSizeChip,
    showResolutionChip, showFramerateInResolution, showProgressBar,
    showDateChip, showUnplayedOldVideoLabel, unplayedOldVideoDays
  ) {
    app.marlboroadvance.mpvex.ui.browser.cards.VideoCardSettings(
      unlimitedNameLines = unlimitedNameLines,
      showThumbnails = showThumbnails,
      showVideoExtension = showVideoExtension,
      showSizeChip = showSizeChip,
      showResolutionChip = showResolutionChip,
      showFramerateInResolution = showFramerateInResolution,
      showProgressBar = showProgressBar,
      showDateChip = showDateChip,
      showUnplayedOldVideoLabel = showUnplayedOldVideoLabel,
      unplayedOldVideoDays = unplayedOldVideoDays
    )
  }

  val folderCardSettings = remember(
    unlimitedNameLines, showTotalVideosChip, showTotalDurationChip,
    showTotalSizeChip, showDateChip, showFolderPath
  ) {
    app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings(
      unlimitedNameLines = unlimitedNameLines,
      showTotalVideosChip = showTotalVideosChip,
      showTotalDurationChip = showTotalDurationChip,
      showTotalSizeChip = showTotalSizeChip,
      showDateChip = showDateChip,
      showFolderPath = showFolderPath
    )
  }

  // Use standalone local states instead of CompositionLocal to avoid scroll issues with predictive back gesture
  val listState = remember { LazyListState() }
  
  // UI state
  val isRefreshing = remember { mutableStateOf(false) }
  val showLinkDialog = remember { mutableStateOf(false) }
  val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
  val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
  val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
  val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }

  // Get navigation bar height from MainScreen
  val navigationBarHeight = app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight.current

  // Copy/Move state
  val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
  val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
  val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
  val operationProgress by CopyPasteOps.operationProgress.collectAsState()

  // Bottom bar visibility state
  var showFloatingBottomBar by remember { mutableStateOf(false) }
  var showBottomNavigation by remember { mutableStateOf(true) }

  // Animation duration for responsive slide animations
  val animationDuration = 200

  // Selection managers - separate for folders and videos
  val folders = items.filterIsInstance<FileSystemItem.Folder>()
  val videos = items.filterIsInstance<FileSystemItem.VideoFile>().map { it.video }

  val folderSelectionManager = rememberSelectionManager(
    items = folders,
    getId = { it.path },
    onDeleteItems = { foldersToDelete, _ ->
      viewModel.deleteFolders(foldersToDelete)
    },
    onRenameItem = { folder, newName ->
      viewModel.renameFolder(folder, newName)
    },
    onOperationComplete = { viewModel.refresh() },
  )

  val videoSelectionManager = rememberSelectionManager(
    items = videos,
    getId = { it.id },
    onDeleteItems = { videosToDelete, _ ->
      viewModel.deleteVideos(videosToDelete)
    },
    onRenameItem = { video, newName ->
      viewModel.renameVideo(video, newName)
    },
    onOperationComplete = { viewModel.refresh() },
  )

  // Determine which selection manager is active
  val isInSelectionMode = folderSelectionManager.isInSelectionMode || videoSelectionManager.isInSelectionMode
  val selectedCount = folderSelectionManager.selectedCount + videoSelectionManager.selectedCount
  val totalCount = folders.size + videos.size
  val isMixedSelection = folderSelectionManager.isInSelectionMode && videoSelectionManager.isInSelectionMode

  // Update bottom bar visibility with optimized animation sequencing
  LaunchedEffect(isInSelectionMode, videoSelectionManager.isInSelectionMode, isMixedSelection) {
    // Show floating bar and hide bottom navigation when appropriate.
    // Play Store gating is intentionally bypassed here.
    val shouldShowFloatingBar = isInSelectionMode && videoSelectionManager.isInSelectionMode && !isMixedSelection
    
    if (shouldShowFloatingBar) {
      // Entering selection mode: Hide bottom navigation immediately, then show floating bar
      showBottomNavigation = false
      showFloatingBottomBar = true
    } else {
      // Exiting selection mode: Hide floating bar and show bottom navigation immediately for better responsiveness
      showFloatingBottomBar = false
      showBottomNavigation = true
    }
  }

  // Permissions
  val permissionState = PermissionUtils.handleStoragePermission(
    onPermissionGranted = { viewModel.refresh() },
  )

  // Combined NavigationBarState updates for better performance and responsiveness
  LaunchedEffect(
    showBottomNavigation, 
    isInSelectionMode, 
    isMixedSelection, 
    videoSelectionManager.isInSelectionMode,
    permissionState.status
  ) {
    if (isAtRoot) {
      try {
        val onlyVideosSelected = videoSelectionManager.isInSelectionMode && !folderSelectionManager.isInSelectionMode

        // Update NavigationBarState for reactive navigation bar visibility
        NavigationBarState.updateBottomBarVisibility(showBottomNavigation)
        NavigationBarState.updateSelectionState(
          inSelectionMode = isInSelectionMode,
          onlyVideos = onlyVideosSelected
        )
        NavigationBarState.updatePermissionState(
          denied = permissionState.status is PermissionStatus.Denied
        )
      } catch (e: Exception) {
        Log.e("FileSystemBrowserScreen", "Failed to update NavigationBarState", e)
      }
    }
  }

  // Cleanup: Restore bottom navigation bar when leaving the screen
  DisposableEffect(Unit) {
    onDispose {
      if (isAtRoot) {
        try {
          // Restore bottom navigation when leaving the screen
          NavigationBarState.updateBottomBarVisibility(true)
        } catch (e: Exception) {
          Log.e("FileSystemBrowserScreen", "Failed to restore MainScreen bottom bar visibility", e)
        }
      }
    }
  }

  // File picker
  val filePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    uri?.let {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          it,
          Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      MediaUtils.playFile(it.toString(), context, "open_file")
    }
  }

  // Tree picker for Play Store-safe copy/move destinations
  val treePickerLauncher = rememberLauncherForActivityResult(
    contract = OpenDocumentTreeContract(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    val selectedVideos = videoSelectionManager.getSelectedItems()
    if (selectedVideos.isEmpty() || operationType.value == null) return@rememberLauncherForActivityResult

    runCatching {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }

    progressDialogOpen.value = true
    coroutineScope.launch {
      when (operationType.value) {
        is CopyPasteOps.OperationType.Copy -> {
          CopyPasteOps.copyFilesToTreeUri(context, selectedVideos, uri)
        }

        is CopyPasteOps.OperationType.Move -> {
          CopyPasteOps.moveFilesToTreeUri(context, selectedVideos, uri)
        }

        else -> {}
      }
    }
  }

  // Listen for lifecycle resume events
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Optimized predictive back handler for immediate response
  val shouldHandleBack = isInSelectionMode
  BackHandler(enabled = shouldHandleBack) {
    folderSelectionManager.clear()
    videoSelectionManager.clear()
  }

  // Main content
  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      topBar = {
        BrowserTopBar(
          title = if (isAtRoot) {
            stringResource(R.string.app_name)
          } else {
            breadcrumbs.lastOrNull()?.name ?: "Tree View"
          },
          isInSelectionMode = isInSelectionMode,
          selectedCount = selectedCount,
          totalCount = totalCount,
          onBackClick = if (isAtRoot) {
            null
          } else {
            { backstack.removeLastOrNull() }
          },
          onCancelSelection = {
            folderSelectionManager.clear()
            videoSelectionManager.clear()
          },
          onSortClick = { sortDialogOpen.value = true },
          onSettingsClick = {
            backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
          },
          onDeleteClick = if (videoSelectionManager.isInSelectionMode && !isMixedSelection) {
            null
          } else {
            { deleteDialogOpen.value = true }
          },
          onRenameClick = { renameDialogOpen.value = true },
          isSingleSelection = (videoSelectionManager.isSingleSelection || folderSelectionManager.isSingleSelection) && !isMixedSelection,
          onInfoClick = if (videoSelectionManager.isInSelectionMode && !folderSelectionManager.isInSelectionMode) {
            {
              val video = videoSelectionManager.getSelectedItems().firstOrNull()
              if (video != null) {
                val intent = Intent(context, app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.data = video.uri
                context.startActivity(intent)
                videoSelectionManager.clear()
              }
            }
          } else {
            null
          },
          onShareClick = null,
          onPlayClick = null,
          onSelectAll = {
            folderSelectionManager.selectAll()
            videoSelectionManager.selectAll()
          },
          onInvertSelection = {
            folderSelectionManager.invertSelection()
            videoSelectionManager.invertSelection()
          },
          onDeselectAll = {
            folderSelectionManager.clear()
            videoSelectionManager.clear()
          },
          onAddToPlaylistClick = null,
        )
      },
    ) { padding ->
      Box(modifier = Modifier.padding(padding)) {
        when (permissionState.status) {
          PermissionStatus.Granted -> {
            FileSystemBrowserContent(
                listState = listState,
                items = items,
                videoFilesWithPlayback = videoFilesWithPlayback,
                isLoading = isLoading && items.isEmpty(),
                isRefreshing = isRefreshing,
                error = error,
                isAtRoot = isAtRoot,
                breadcrumbs = breadcrumbs,
                itemsWereDeletedOrMoved = itemsWereDeletedOrMoved,
                showSubtitleIndicator = showSubtitleIndicator,
                navigationBarHeight = navigationBarHeight,
                videoCardSettings = videoCardSettings,
                folderCardSettings = folderCardSettings,
                onRefresh = { viewModel.refresh() },
              onFolderClick = { folder ->
                if (isInSelectionMode) {
                  folderSelectionManager.toggle(folder)
                } else {
                  backstack.add(FileSystemDirectoryScreen(folder.path))
                }
              },
              onFolderLongClick = { folder ->
                folderSelectionManager.toggle(folder)
              },
              onVideoClick = { video ->
                if (isInSelectionMode) {
                  videoSelectionManager.toggle(video)
                } else {
                  // If playlist mode is enabled, play all videos in current folder starting from clicked one
                  if (playlistMode) {
                    val startIndex = videos.indexOfFirst { it.id == video.id }
                    if (startIndex >= 0) {
                      if (videos.size == 1) {
                        // Single video - play normally
                        MediaUtils.playFile(video, context)
                      } else {
                        // Multiple videos - play as playlist starting from clicked video
                        val intent = Intent(Intent.ACTION_VIEW, videos[startIndex].uri)
                        intent.setClass(context, app.marlboroadvance.mpvex.ui.player.PlayerActivity::class.java)
                        intent.putExtra("internal_launch", true)
                        intent.putParcelableArrayListExtra("playlist", ArrayList(videos.map { it.uri }))
                        intent.putExtra("playlist_index", startIndex)
                        intent.putExtra("launch_source", "playlist")
                        context.startActivity(intent)
                      }
                    } else {
                      MediaUtils.playFile(video, context)
                    }
                  } else {
                    MediaUtils.playFile(video, context)
                  }
                }
              },
              onVideoLongClick = { video ->
                videoSelectionManager.toggle(video)
              },
              onBreadcrumbClick = { component ->
                // Navigate to the breadcrumb by popping until we reach it
                // or pushing if it's a new path
                backstack.add(FileSystemDirectoryScreen(component.fullPath))
              },
              folderSelectionManager = folderSelectionManager,
              videoSelectionManager = videoSelectionManager,
              modifier = Modifier,
            )
          }

          is PermissionStatus.Denied -> {
            PermissionDeniedState(
              onRequestPermission = { permissionState.launchPermissionRequest() },
              modifier = Modifier,
            )
          }
        }
      }
    }

    // Independent Floating Bottom Bar - positioned at absolute bottom
    // Play Store gating is intentionally bypassed here.
    AnimatedVisibility(
      visible = showFloatingBottomBar,
      enter = slideInVertically(
        animationSpec = tween(durationMillis = animationDuration),
        initialOffsetY = { fullHeight -> fullHeight }
      ),
      exit = slideOutVertically(
        animationSpec = tween(durationMillis = animationDuration),
        targetOffsetY = { fullHeight -> fullHeight }
      ),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      BrowserBottomBar(
        isSelectionMode = true,
        onCopyClick = {
          operationType.value = CopyPasteOps.OperationType.Copy
          if (CopyPasteOps.canUseDirectFileOperations()) {
            folderPickerOpen.value = true
          } else {
            treePickerLauncher.launch(null)
          }
        },
        onMoveClick = {
          operationType.value = CopyPasteOps.OperationType.Move
          if (CopyPasteOps.canUseDirectFileOperations()) {
            folderPickerOpen.value = true
          } else {
            treePickerLauncher.launch(null)
          }
        },
        onRenameClick = { renameDialogOpen.value = true },
        onDeleteClick = { deleteDialogOpen.value = true },
        onAddToPlaylistClick = { addToPlaylistDialogOpen.value = true },
        showRename = videoSelectionManager.isSingleSelection,
        modifier = Modifier.padding(bottom = 0.dp) // Zero bottom padding - absolute bottom
      )
    }

    // Dialogs
    PlayLinkSheet(
      isOpen = showLinkDialog.value,
      onDismiss = { showLinkDialog.value = false },
      onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
    )

    FileSystemSortBottomSheet(
      isOpen = sortDialogOpen.value,
      onDismiss = { sortDialogOpen.value = false },
    )

    DeleteConfirmationDialog(
      isOpen = deleteDialogOpen.value,
      onDismiss = { deleteDialogOpen.value = false },
      onConfirm = {
        if (folderSelectionManager.isInSelectionMode) {
          folderSelectionManager.deleteSelected()
        }
        if (videoSelectionManager.isInSelectionMode) {
          videoSelectionManager.deleteSelected()
        }
      },
      itemType = when {
        folderSelectionManager.isInSelectionMode && videoSelectionManager.isInSelectionMode -> "item"
        folderSelectionManager.isInSelectionMode -> "folder"
        else -> "video"
      },
      itemCount = selectedCount,
      itemNames = (folderSelectionManager.getSelectedItems().map { it.name } +
        videoSelectionManager.getSelectedItems().map { it.displayName }),
    )

    // Rename Dialog
    if (renameDialogOpen.value) {
      if (videoSelectionManager.isSingleSelection) {
        val video = videoSelectionManager.getSelectedItems().firstOrNull()
        if (video != null) {
          val baseName = video.displayName.substringBeforeLast('.')
          val extension = "." + video.displayName.substringAfterLast('.', "")
          RenameDialog(
            isOpen = true,
            onDismiss = { renameDialogOpen.value = false },
            onConfirm = { newName -> videoSelectionManager.renameSelected(newName) },
            currentName = baseName,
            itemType = "file",
            extension = if (extension != ".") extension else null,
          )
        }
      } else if (folderSelectionManager.isSingleSelection) {
        val folder = folderSelectionManager.getSelectedItems().firstOrNull()
        if (folder != null) {
          RenameDialog(
            isOpen = true,
            onDismiss = { renameDialogOpen.value = false },
            onConfirm = { newName ->
              folderSelectionManager.renameSelected(newName)
            },
            currentName = folder.name,
            itemType = "folder",
          )
        }
      }
    }

    // Folder Picker Dialog
    FolderPickerDialog(
      isOpen = folderPickerOpen.value,
      currentPath = currentPath,
      onDismiss = { folderPickerOpen.value = false },
      onFolderSelected = { destinationPath ->
        folderPickerOpen.value = false
        val selectedVideos = videoSelectionManager.getSelectedItems()
        if (selectedVideos.isNotEmpty() && operationType.value != null) {
          progressDialogOpen.value = true
          coroutineScope.launch {
            when (operationType.value) {
              is CopyPasteOps.OperationType.Copy -> {
                CopyPasteOps.copyFiles(context, selectedVideos, destinationPath)
              }

              is CopyPasteOps.OperationType.Move -> {
                CopyPasteOps.moveFiles(context, selectedVideos, destinationPath)
              }

              else -> {}
            }
          }
        }
      },
    )

    // File Operation Progress Dialog
    if (operationType.value != null) {
      FileOperationProgressDialog(
        isOpen = progressDialogOpen.value,
        operationType = operationType.value!!,
        progress = operationProgress,
        onCancel = {
          CopyPasteOps.cancelOperation()
        },
        onDismiss = {
          progressDialogOpen.value = false
          // Set flag if move operation was successful
          if (operationType.value is CopyPasteOps.OperationType.Move &&
            operationProgress.isComplete &&
            operationProgress.error == null) {
            viewModel.setItemsWereDeletedOrMoved()
          }
          operationType.value = null
          videoSelectionManager.clear()
          viewModel.refresh()
        },
      )
    }

    // Add to Playlist Dialog
    AddToPlaylistDialog(
      isOpen = addToPlaylistDialogOpen.value,
      videos = videoSelectionManager.getSelectedItems(),
      onDismiss = { addToPlaylistDialogOpen.value = false },
      onSuccess = {
        videoSelectionManager.clear()
        viewModel.refresh()
      },
    )
  }
}


/**
 * Recursively collects all videos from a folder and its subfolders
 */
private suspend fun collectVideosRecursively(
  context: Context,
  folderPath: String,
): List<app.marlboroadvance.mpvex.domain.media.model.Video> {
  val videos = mutableListOf<app.marlboroadvance.mpvex.domain.media.model.Video>()

  try {
    // Scan the current directory using MediaFileRepository
    val items = app.marlboroadvance.mpvex.repository.MediaFileRepository
      .scanDirectory(context, folderPath, showAllFileTypes = false)
      .getOrNull() ?: emptyList()

    // Add videos from current folder
    items.filterIsInstance<FileSystemItem.VideoFile>().forEach { videoFile ->
      videos.add(videoFile.video)
    }

    // Recursively scan subfolders
    items.filterIsInstance<FileSystemItem.Folder>().forEach { folder ->
      val subVideos = collectVideosRecursively(context, folder.path)
      videos.addAll(subVideos)
    }
  } catch (e: Exception) {
    Log.e("FileSystemBrowserScreen", "Error collecting videos from $folderPath", e)
  }

  return videos
}

/**
 * Plays a list of videos as a playlist
 */
private fun playVideosAsPlaylist(
  context: Context,
  videos: List<app.marlboroadvance.mpvex.domain.media.model.Video>,
) {
  if (videos.isEmpty()) return

  if (videos.size == 1) {
    // Single video - play normally
    MediaUtils.playFile(videos.first(), context)
  } else {
    // Multiple videos - play as playlist
    val intent = Intent(Intent.ACTION_VIEW, videos.first().uri)
    intent.setClass(context, app.marlboroadvance.mpvex.ui.player.PlayerActivity::class.java)
    intent.putExtra("internal_launch", true)
    intent.putParcelableArrayListExtra("playlist", ArrayList(videos.map { it.uri }))
    intent.putExtra("playlist_index", 0)
    intent.putExtra("launch_source", "playlist")
    context.startActivity(intent)
  }
}

@Composable
private fun FileSystemBrowserContent(
  listState: LazyListState,
  items: List<FileSystemItem>,
  videoFilesWithPlayback: Map<Long, Float>,
  isLoading: Boolean,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  error: String?,
  isAtRoot: Boolean,
  breadcrumbs: List<app.marlboroadvance.mpvex.domain.browser.PathComponent>,
  itemsWereDeletedOrMoved: Boolean,
  showSubtitleIndicator: Boolean,
  navigationBarHeight: Dp,
  videoCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.VideoCardSettings,
  folderCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings,
  onRefresh: suspend () -> Unit,
  onFolderClick: (FileSystemItem.Folder) -> Unit,
  onFolderLongClick: (FileSystemItem.Folder) -> Unit,
  onVideoClick: (app.marlboroadvance.mpvex.domain.media.model.Video) -> Unit,
  onVideoLongClick: (app.marlboroadvance.mpvex.domain.media.model.Video) -> Unit,
  onBreadcrumbClick: (app.marlboroadvance.mpvex.domain.browser.PathComponent) -> Unit,
  folderSelectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<FileSystemItem.Folder, String>,
  videoSelectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<app.marlboroadvance.mpvex.domain.media.model.Video, Long>,
  modifier: Modifier = Modifier,
) {
  val gesturePreferences = koinInject<GesturePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val thumbnailRepository = koinInject<app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository>()
  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
  val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()

  // Calculate thumbnail dimensions for list mode
  val thumbWidthDp = 160.dp
  val density = androidx.compose.ui.platform.LocalDensity.current
  val aspect = 16f / 9f
  val thumbWidthPx = with(density) { thumbWidthDp.roundToPx() }
  val thumbHeightPx = ((thumbWidthPx.toFloat() / aspect).toInt())

  val folders = items.filterIsInstance<FileSystemItem.Folder>()
  val videos = items.filterIsInstance<FileSystemItem.VideoFile>().map { it.video }

  // Create a unique folderId based on the current directories
  val folderId = remember(folders, isAtRoot, breadcrumbs) {
    if (isAtRoot && breadcrumbs.isEmpty()) {
      "filesystem_root"
    } else {
      breadcrumbs.lastOrNull()?.fullPath ?: "filesystem_${breadcrumbs.size}"
    }
  }

  // Generate thumbnails sequentially
  LaunchedEffect(folderId, showVideoThumbnails, videos.size, thumbWidthPx, thumbHeightPx) {
    if (showVideoThumbnails && videos.isNotEmpty()) {
      thumbnailRepository.startFolderThumbnailGeneration(
        folderId = folderId,
        videos = videos,
        widthPx = thumbWidthPx,
        heightPx = thumbHeightPx,
      )
    }
  }

  when {
    isLoading -> {
      Box(
        modifier = modifier
          .fillMaxSize()
          .padding(bottom = 80.dp), // Account for bottom navigation bar
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(48.dp),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    error != null -> {
      EmptyState(
        icon = Icons.Filled.Folder,
        title = "Error loading directory",
        message = error,
        modifier = modifier.fillMaxSize(),
      )
    }

    items.isEmpty() && itemsWereDeletedOrMoved && !isAtRoot -> {
      EmptyState(
        icon = Icons.Filled.FolderOpen,
        title = "Empty folder",
        message = "This folder contains no videos or subfolders",
        modifier = modifier.fillMaxSize(),
      )
    }

    else -> {
      // Check if at top of list to hide scrollbar during pull-to-refresh
      val isAtTop by remember {
        derivedStateOf {
          listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
      }

      // Only show scrollbar if list has more than 20 items
      val hasEnoughItems = items.size > 20

      // Animate scrollbar alpha
      val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbarAlpha",
      )

      PullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        listState = listState,
        modifier = modifier.fillMaxSize(),
      ) {
        Box(
          modifier = Modifier.fillMaxSize()
        ) {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              start = 8.dp,
              end = 8.dp,
              bottom = navigationBarHeight
            ),
          ) {
            // Breadcrumb navigation (if not at root)
            if (!isAtRoot && breadcrumbs.isNotEmpty()) {
              item {
                BreadcrumbNavigation(
                  breadcrumbs = breadcrumbs,
                  onBreadcrumbClick = onBreadcrumbClick,
                )
              }
            }

            // Folders first
            items(
              items = items.filterIsInstance<FileSystemItem.Folder>(),
              key = { it.path },
            ) { folder ->
              val folderModel = app.marlboroadvance.mpvex.domain.media.model.VideoFolder(
                bucketId = folder.path,
                name = folder.name,
                path = folder.path,
                videoCount = folder.videoCount,
                totalSize = folder.totalSize,
                totalDuration = folder.totalDuration,
                lastModified = folder.lastModified / 1000,
              )

              FolderCard(
                folder = folderModel,
                settings = folderCardSettings,
                isSelected = folderSelectionManager.isSelected(folder),
                onClick = { onFolderClick(folder) },
                onLongClick = { onFolderLongClick(folder) },
                onThumbClick = if (tapThumbnailToSelect) {
                  { onFolderLongClick(folder) }
                } else {
                  { onFolderClick(folder) }
                },
              )
            }

            // Videos second
            items(
              items = items.filterIsInstance<FileSystemItem.VideoFile>(),
              key = { "${it.video.id}_${it.video.path}" },
            ) { videoFile ->
              VideoCard(
                video = videoFile.video,
                settings = videoCardSettings,
                progressPercentage = videoFilesWithPlayback[videoFile.video.id],
                isRecentlyPlayed = false,
                isSelected = videoSelectionManager.isSelected(videoFile.video),
                onClick = { onVideoClick(videoFile.video) },
                onLongClick = { onVideoLongClick(videoFile.video) },
                onThumbClick = if (tapThumbnailToSelect) {
                  { onVideoLongClick(videoFile.video) }
                } else {
                  { onVideoClick(videoFile.video) }
                },
                showSubtitleIndicator = showSubtitleIndicator,
                overrideShowSizeChip = null,
                overrideShowResolutionChip = null,
                useFolderNameStyle = false,
              )
            }
          }
          
          // Scrollbar with bottom padding to avoid overlap with navigation
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(bottom = navigationBarHeight)
          ) {
            LazyColumnScrollbar(
              state = listState,
              settings = ScrollbarSettings(
                thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
                thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
              ),
            ) {
              // Empty content - scrollbar only
            }
          }
        }
      }
    }
  }
}

@Composable
fun FileSystemSortBottomSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val appearancePreferences = koinInject<app.marlboroadvance.mpvex.preferences.AppearancePreferences>()
  val folderSortType by browserPreferences.folderSortType.collectAsState()
  val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()
  val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val showVideoExtension by browserPreferences.showVideoExtension.collectAsState()
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  val showSizeChip by browserPreferences.showSizeChip.collectAsState()
  val showResolutionChip by browserPreferences.showResolutionChip.collectAsState()
  val showFramerateInResolution by browserPreferences.showFramerateInResolution.collectAsState()
  val showProgressBar by browserPreferences.showProgressBar.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()

  SortBottomSheet(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = "Sort & View Options",
    sortType = folderSortType.displayName,
    onSortTypeChange = { typeName ->
      app.marlboroadvance.mpvex.preferences.FolderSortType.entries.find { it.displayName == typeName }?.let {
        browserPreferences.folderSortType.set(it)
      }
    },
    sortOrderAsc = folderSortOrder.isAscending,
    onSortOrderChange = { isAsc ->
      browserPreferences.folderSortOrder.set(
        if (isAsc) app.marlboroadvance.mpvex.preferences.SortOrder.Ascending
        else app.marlboroadvance.mpvex.preferences.SortOrder.Descending,
      )
    },
    onReset = {
      browserPreferences.folderSortType.set(app.marlboroadvance.mpvex.preferences.FolderSortType.Title)
      browserPreferences.folderSortOrder.set(app.marlboroadvance.mpvex.preferences.SortOrder.Ascending)
      browserPreferences.showVideoThumbnails.set(true)
      browserPreferences.showVideoExtension.set(false)
      appearancePreferences.unlimitedNameLines.set(false)
      browserPreferences.showFolderPath.set(false)
      browserPreferences.showTotalVideosChip.set(true)
      browserPreferences.showTotalSizeChip.set(false)
      browserPreferences.showSizeChip.set(true)
      browserPreferences.showResolutionChip.set(true)
      browserPreferences.showFramerateInResolution.set(false)
      browserPreferences.showSubtitleIndicator.set(true)
      browserPreferences.showProgressBar.set(true)
    },
    types = listOf(
      app.marlboroadvance.mpvex.preferences.FolderSortType.Title.displayName,
      app.marlboroadvance.mpvex.preferences.FolderSortType.Date.displayName,
      app.marlboroadvance.mpvex.preferences.FolderSortType.Size.displayName,
      app.marlboroadvance.mpvex.preferences.FolderSortType.VideoCount.displayName,
    ),
    icons = listOf(
      ImageVector.vectorResource(id = R.drawable.sort_by_alpha_24px),
      Icons.Filled.CalendarToday,
      Icons.Filled.SwapVert,
      Icons.Filled.VideoLibrary,
    ),
    getLabelForType = { type, _ ->
      when (type) {
        app.marlboroadvance.mpvex.preferences.FolderSortType.Title.displayName -> Pair("A-Z", "Z-A")
        app.marlboroadvance.mpvex.preferences.FolderSortType.Date.displayName -> Pair("Oldest", "Newest")
        app.marlboroadvance.mpvex.preferences.FolderSortType.Size.displayName -> Pair("Smallest", "Largest")
        app.marlboroadvance.mpvex.preferences.FolderSortType.VideoCount.displayName -> Pair("Fewest", "Most")
        else -> Pair("Asc", "Desc")
      }
    },
    visibilityToggles = listOf(
      VisibilityToggle(
        label = "Video Thumbnails",
        checked = showVideoThumbnails,
        onCheckedChange = { browserPreferences.showVideoThumbnails.set(it) },
      ),
      VisibilityToggle(
        label = "Extension",
        checked = showVideoExtension,
        onCheckedChange = { browserPreferences.showVideoExtension.set(it) },
      ),
      VisibilityToggle(
        label = "Full Name",
        checked = unlimitedNameLines,
        onCheckedChange = { appearancePreferences.unlimitedNameLines.set(it) },
      ),
      VisibilityToggle(
        label = "Path",
        checked = showFolderPath,
        onCheckedChange = { browserPreferences.showFolderPath.set(it) },
      ),
      VisibilityToggle(
        label = "Total Videos",
        checked = showTotalVideosChip,
        onCheckedChange = { browserPreferences.showTotalVideosChip.set(it) },
      ),
      VisibilityToggle(
        label = "Folder Size",
        checked = showTotalSizeChip,
        onCheckedChange = { browserPreferences.showTotalSizeChip.set(it) },
      ),
      VisibilityToggle(
        label = "Size",
        checked = showSizeChip,
        onCheckedChange = { browserPreferences.showSizeChip.set(it) },
      ),
      VisibilityToggle(
        label = "Resolution",
        checked = showResolutionChip,
        onCheckedChange = { browserPreferences.showResolutionChip.set(it) },
      ),
      VisibilityToggle(
        label = "Framerate",
        checked = showFramerateInResolution,
        onCheckedChange = { browserPreferences.showFramerateInResolution.set(it) },
      ),
      VisibilityToggle(
        label = "Subtitles",
        checked = showSubtitleIndicator,
        onCheckedChange = { browserPreferences.showSubtitleIndicator.set(it) },
      ),
      VisibilityToggle(
        label = "Progress Bar",
        checked = showProgressBar,
        onCheckedChange = { browserPreferences.showProgressBar.set(it) },
      ),
    )
  )
}

/**
 * Breadcrumb navigation item
 */
@Composable
private fun BreadcrumbNavigation(
  breadcrumbs: List<app.marlboroadvance.mpvex.domain.browser.PathComponent>,
  onBreadcrumbClick: (app.marlboroadvance.mpvex.domain.browser.PathComponent) -> Unit,
) {
  androidx.compose.foundation.lazy.LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items(breadcrumbs) { component ->
      androidx.compose.material3.TextButton(
        onClick = { onBreadcrumbClick(component) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
          contentColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Text(
          text = component.name,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
      }
      
      if (component != breadcrumbs.last()) {
        Icon(
          imageVector = Icons.Default.PlayArrow, // Using PlayArrow as a simple chevron
          contentDescription = null,
          modifier = Modifier.size(12.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
      }
    }
  }
}
