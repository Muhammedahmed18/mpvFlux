package app.marlboroadvance.mpvex.ui.browser.folderlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import java.io.File
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.FolderSortType
import app.marlboroadvance.mpvex.preferences.FolderViewMode
import app.marlboroadvance.mpvex.preferences.FoldersPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.NavigationBarState
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.sheets.SortBottomSheet
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.fab.FabScrollHelper
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.browser.states.LoadingState
import app.marlboroadvance.mpvex.ui.browser.states.PermissionDeniedState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoListScreen
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject

@Serializable
object FolderListScreen : Screen {
  @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val browserPreferences = koinInject<BrowserPreferences>()
    val folderViewMode by browserPreferences.folderViewMode.collectAsState()

    when (folderViewMode) {
      FolderViewMode.AlbumView -> MediaStoreFolderListContent()
    }
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun MediaStoreFolderListContent() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ViewModels and preferences
    val viewModel: FolderListViewModel = viewModel(
      factory = FolderListViewModel.factory(context.applicationContext as android.app.Application)
    )
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val gesturePreferences = koinInject<GesturePreferences>()
    val foldersPreferences = koinInject<FoldersPreferences>()

    // State collection
    val videoFolders by viewModel.videoFolders.collectAsState()
    val foldersWithNewCount by viewModel.foldersWithNewCount.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsState()
    val foldersWereDeleted by viewModel.foldersWereDeleted.collectAsState()

    // Preferences
    val folderSortType by browserPreferences.folderSortType.collectAsState()
    val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()
    val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()

    // UI state - use standalone states to avoid scroll issues with predictive back gesture
    val listState = rememberLazyListState()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
    val showLinkDialog = remember { mutableStateOf(false) }

    // Pre-fetch strings for use in lambdas

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

    // Sorting and filtering
    val sortedFolders = remember(videoFolders, folderSortType, folderSortOrder) {
      SortUtils.sortFolders(videoFolders, folderSortType, folderSortOrder)
    }

    // FolderCard settings
    val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
    val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
    val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
    val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
    val showDateChip by browserPreferences.showDateChip.collectAsState()
    val showFolderPath by browserPreferences.showFolderPath.collectAsState()

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

    // Selection manager
    val selectionManager = rememberSelectionManager(
      items = sortedFolders,
      getId = { it.bucketId },
      onDeleteItems = { folders, _ ->
        val ids = folders.map { it.bucketId }.toSet()
        val videos = MediaFileRepository.getVideosForBuckets(context, ids)
        viewModel.deleteVideos(videos)
        Pair(videos.size, 0)
      },
      onRenameItem = { folder, newName ->
        viewModel.renameFolder(folder, newName)
      },
      onOperationComplete = { viewModel.refresh() },
    )

    // Permissions
    val permissionState = PermissionUtils.handleStoragePermission(
      onPermissionGranted = { viewModel.refresh() },
    )

    // Update NavigationBarState about permission state
    LaunchedEffect(permissionState.status) {
      NavigationBarState.updatePermissionState(
        denied = permissionState.status is com.google.accompanist.permissions.PermissionStatus.Denied
      )
    }

    // Lifecycle observer for refresh
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          viewModel.recalculateNewVideoCounts()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Optimized back handler for immediate response
    val shouldHandleBack = selectionManager.isInSelectionMode
    androidx.activity.compose.BackHandler(enabled = shouldHandleBack) {
      selectionManager.clear()
    }



    Scaffold(
      topBar = {
        // Hide top bar when permission is denied
        if (permissionState.status !is com.google.accompanist.permissions.PermissionStatus.Denied) {
          BrowserTopBar(
            title = stringResource(R.string.app_name),
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = videoFolders.size,
            onBackClick = null,
            onCancelSelection = { selectionManager.clear() },
            onSortClick = { sortDialogOpen.value = true },
            onSettingsClick = {
              backstack.add(PreferencesScreen)
            },
            onDeleteClick = { deleteDialogOpen.value = true },
            onRenameClick = { renameDialogOpen.value = true },
            isSingleSelection = selectionManager.isSingleSelection,
            onInfoClick = null,
            onShareClick = null,
            onPlayClick = null,
            onBlacklistClick = null,
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
        )
      }
    },
  ) { padding ->
      Box(modifier = Modifier.padding(padding)) {
        when (permissionState.status) {
          com.google.accompanist.permissions.PermissionStatus.Granted -> {
            FolderListContent(
              folders = sortedFolders,
              foldersWithNewCount = foldersWithNewCount,
              recentlyPlayedFilePath = recentlyPlayedFilePath,
              isLoading = isLoading,
              scanStatus = scanStatus,
              hasCompletedInitialLoad = hasCompletedInitialLoad,
              foldersWereDeleted = foldersWereDeleted,
              tapThumbnailToSelect = tapThumbnailToSelect,
              navigationBarHeight = navigationBarHeight,
              listState = listState,
              isRefreshing = isRefreshing,
              selectionManager = selectionManager,
              folderCardSettings = folderCardSettings,
              onRefresh = { viewModel.refresh() },
              onFolderClick = { folder ->
                if (selectionManager.isInSelectionMode) {
                  selectionManager.toggle(folder)
                } else {
                  backstack.add(VideoListScreen(folder.bucketId, folder.name))
                }
              },
              onFolderLongClick = { folder ->
                selectionManager.toggle(folder)
              },
            )
          }
          is com.google.accompanist.permissions.PermissionStatus.Denied -> {
            PermissionDeniedState(
              onRequestPermission = { permissionState.launchPermissionRequest() },
              modifier = Modifier,
            )
          }
        }
      }

      // Dialogs
      PlayLinkSheet(
        isOpen = showLinkDialog.value,
        onDismiss = { showLinkDialog.value = false },
        onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
      )

      FolderSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = folderSortType,
        sortOrder = folderSortOrder,
        onSortTypeChange = { browserPreferences.folderSortType.set(it) },
        onSortOrderChange = { browserPreferences.folderSortOrder.set(it) },
      )

      DeleteConfirmationDialog(
        isOpen = deleteDialogOpen.value,
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = { selectionManager.deleteSelected() },
        itemType = "folder",
        itemCount = selectionManager.selectedCount,
        itemNames = selectionManager.getSelectedItems().map { it.name },
      )

      RenameDialog(
        isOpen = renameDialogOpen.value,
        onDismiss = { renameDialogOpen.value = false },
        currentName = selectionManager.getSelectedItems().firstOrNull()?.name ?: "",
        onConfirm = { newName ->
          selectionManager.renameSelected(newName)
        },
        itemType = "folder",
      )
    }
  }
}

@Composable
private fun FolderListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<FolderWithNewCount>,
  recentlyPlayedFilePath: String?,
  isLoading: Boolean,
  scanStatus: String?,
  hasCompletedInitialLoad: Boolean,
  foldersWereDeleted: Boolean,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  folderCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings,
  onRefresh: suspend () -> Unit,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
) {
  val showLoading = isLoading && !hasCompletedInitialLoad
  val showEmpty = folders.isEmpty() && hasCompletedInitialLoad && !foldersWereDeleted

  // Scrollbar alpha animation
  val isAtTop by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
    }
  }

  val hasEnoughItems = folders.size > 20
  val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f,
    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
    label = "scrollbarAlpha",
  )

  PullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    listState = listState,
    modifier = Modifier.fillMaxSize(),
  ) {
    if (showLoading || showEmpty) {
      if (showLoading) {
        LoadingState(
          icon = Icons.Rounded.Folder,
          title = "Scanning for videos...",
          message = scanStatus ?: "Please wait while we search your device",
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        EmptyState(
          icon = Icons.Rounded.Folder,
          title = "No video folders found",
          message = "Add some video files to your device to see them here",
          modifier = Modifier.fillMaxSize(),
        )
      }
    } else {
      ListContent(
        folders = folders,
        foldersWithNewCount = foldersWithNewCount,
        recentlyPlayedFilePath = recentlyPlayedFilePath,
        tapThumbnailToSelect = tapThumbnailToSelect,
        navigationBarHeight = navigationBarHeight,
        listState = listState,
        scrollbarAlpha = scrollbarAlpha,
        selectionManager = selectionManager,
        folderCardSettings = folderCardSettings,
        onFolderClick = onFolderClick,
        onFolderLongClick = onFolderLongClick,
      )

      // Show background enrichment progress
      if (scanStatus != null) {
        androidx.compose.material3.LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
          color = MaterialTheme.colorScheme.secondary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }
    }
  }
}

@Composable
private fun ListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<FolderWithNewCount>,
  recentlyPlayedFilePath: String?,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  scrollbarAlpha: Float,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  folderCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        bottom = navigationBarHeight
      ),
    ) {
      items(folders) { folder ->
        val isRecentlyPlayed = recentlyPlayedFilePath?.let { filePath ->
          val file = File(filePath)
          file.parent == folder.path
        } ?: false

        val newCount = foldersWithNewCount
          .find { it.folder.bucketId == folder.bucketId }
          ?.newVideoCount ?: 0

        FolderCard(
        folder = folder,
        settings = folderCardSettings,
        isSelected = selectionManager.isSelected(folder),
          onClick = { onFolderClick(folder) },
          onLongClick = { onFolderLongClick(folder) },
          onThumbClick = if (tapThumbnailToSelect) {
            { onFolderLongClick(folder) }
          } else {
            { onFolderClick(folder) }
          },
        )
      }
    }

    // Scrollbar with bottom padding
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

@Composable
private fun FolderSortDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  sortType: FolderSortType,
  sortOrder: SortOrder,
  onSortTypeChange: (FolderSortType) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val appearancePreferences = koinInject<AppearancePreferences>()
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val folderViewMode by browserPreferences.folderViewMode.collectAsState()

  val isAlbumView = folderViewMode == FolderViewMode.AlbumView

  SortBottomSheet(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = if (isAlbumView) "Sort & View Options" else "View Options",
    sortType = sortType.displayName,
    onSortTypeChange = { typeName ->
      FolderSortType.entries
        .find { it.displayName == typeName }
        ?.let(onSortTypeChange)
    },
    sortOrderAsc = sortOrder.isAscending,
    onSortOrderChange = { isAsc ->
      onSortOrderChange(if (isAsc) SortOrder.Ascending else SortOrder.Descending)
    },
    types = listOf(
      FolderSortType.Title.displayName,
      FolderSortType.Date.displayName,
      FolderSortType.Size.displayName,
      FolderSortType.VideoCount.displayName,
    ),
    icons = listOf(
      ImageVector.vectorResource(id = R.drawable.sort_by_alpha_24px),
      Icons.Rounded.CalendarToday,
      Icons.Rounded.SwapVert,
      Icons.Rounded.VideoLibrary,
    ),
    getLabelForType = { type, _ ->
      when (type) {
        FolderSortType.Title.displayName -> Pair("A-Z", "Z-A")
        FolderSortType.Date.displayName -> Pair("Oldest", "Newest")
        FolderSortType.Size.displayName -> Pair("Smallest", "Largest")
        FolderSortType.VideoCount.displayName -> Pair("Fewest", "Most")
        else -> Pair("Asc", "Desc")
      }
    },
    visibilityToggles = listOf(
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
        label = "Total Duration",
        checked = showTotalDurationChip,
        onCheckedChange = { browserPreferences.showTotalDurationChip.set(it) },
      ),
      VisibilityToggle(
        label = "Folder Size",
        checked = showTotalSizeChip,
        onCheckedChange = { browserPreferences.showTotalSizeChip.set(it) },
      ),
      VisibilityToggle(
        label = "Date",
        checked = showDateChip,
        onCheckedChange = { browserPreferences.showDateChip.set(it) },
      ),
    ),
  )
}
