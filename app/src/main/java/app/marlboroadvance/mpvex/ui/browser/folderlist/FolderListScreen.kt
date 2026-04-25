package app.marlboroadvance.mpvex.ui.browser.folderlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.FolderSortType
import app.marlboroadvance.mpvex.preferences.FolderViewMode
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.NavigationBarState
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.SortBottomSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.browser.states.LoadingState
import app.marlboroadvance.mpvex.ui.browser.states.PermissionDeniedState
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoListScreen
import app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
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

  @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
  @Composable
  private fun MediaStoreFolderListContent() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val viewModel: FolderListViewModel = viewModel(
      factory = FolderListViewModel.factory(context.applicationContext as android.app.Application)
    )
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val gesturePreferences = koinInject<GesturePreferences>()

    val videoFolders by viewModel.videoFolders.collectAsState()
    val foldersWithNewCount by viewModel.foldersWithNewCount.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsState()

    val folderSortType by browserPreferences.folderSortType.collectAsState()
    val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()
    val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()

    val listState = rememberLazyListState()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen = rememberSaveable { mutableStateOf(false) }

    val sortedFolders = remember(videoFolders, folderSortType, folderSortOrder) {
      SortUtils.sortFolders(videoFolders, folderSortType, folderSortOrder)
    }

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
      FolderCardSettings(
        unlimitedNameLines = unlimitedNameLines,
        showTotalVideosChip = showTotalVideosChip,
        showTotalDurationChip = showTotalDurationChip,
        showTotalSizeChip = showTotalSizeChip,
        showDateChip = showDateChip,
        showFolderPath = showFolderPath
      )
    }

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

    LaunchedEffect(selectionManager.isInSelectionMode) {
      NavigationBarState.updateBottomBarVisibility(!selectionManager.isInSelectionMode)
      NavigationBarState.updateSelectionState(selectionManager.isInSelectionMode, false)
    }

    val permissionState = PermissionUtils.handleStoragePermission(
      onPermissionGranted = { viewModel.onPermissionGranted() },
    )

    LaunchedEffect(permissionState.status) {
      NavigationBarState.updatePermissionState(
        denied = permissionState.status is PermissionStatus.Denied
      )
    }

    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.recalculateNewVideoCounts()
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }

    Scaffold(
      topBar = {
        if (permissionState.status !is PermissionStatus.Denied) {
          BrowserTopBar(
            title = stringResource(R.string.app_name),
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = videoFolders.size,
            onCancelSelection = { selectionManager.clear() },
            onSortClick = { sortDialogOpen.value = true },
            onSettingsClick = { backstack.add(PreferencesScreen) },
            onDeleteClick = { deleteDialogOpen.value = true },
            onRenameClick = { renameDialogOpen.value = true },
            isSingleSelection = selectionManager.isSingleSelection,
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll = { selectionManager.clear() },
          )
        }
      },
    ) { padding ->
      Box(modifier = Modifier.padding(padding)) {
        when (permissionState.status) {
          PermissionStatus.Granted -> {
            FolderListContent(
              folders = sortedFolders,
              foldersWithNewCount = foldersWithNewCount,
              recentlyPlayedFilePath = recentlyPlayedFilePath,
              isLoading = isLoading,
              scanStatus = scanStatus,
              hasCompletedInitialLoad = hasCompletedInitialLoad,
              tapThumbnailToSelect = tapThumbnailToSelect,
              navigationBarHeight = navigationBarHeight,
              listState = listState,
              isRefreshing = isRefreshing,
              selectionManager = selectionManager,
              folderCardSettings = folderCardSettings,
              onRefresh = { viewModel.refresh() },
              onFolderClick = { folder ->
                if (selectionManager.isInSelectionMode) selectionManager.toggle(folder)
                else backstack.add(VideoListScreen(folder.bucketId, folder.name))
              },
              onFolderLongClick = { selectionManager.toggle(it) },
            )
          }
          is PermissionStatus.Denied -> {
            PermissionDeniedState(onRequestPermission = { permissionState.launchPermissionRequest() })
          }
        }
      }

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

      if (renameDialogOpen.value && selectionManager.isSingleSelection) {
        RenameDialog(
          isOpen = true,
          onDismiss = { renameDialogOpen.value = false },
          currentName = selectionManager.getSelectedItems().firstOrNull()?.name ?: "",
          onConfirm = { selectionManager.renameSelected(it) },
          itemType = "folder",
        )
      }
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
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  folderCardSettings: FolderCardSettings,
  onRefresh: suspend () -> Unit,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
) {
  val showLoading = (isLoading && folders.isEmpty()) || !hasCompletedInitialLoad
  val showEmpty = folders.isEmpty() && !isLoading && hasCompletedInitialLoad

  val isAtTop by remember {
    derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
  }

  val hasEnoughItems = folders.size > 20
  val scrollbarAlpha by animateFloatAsState(
    targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f,
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
          message = scanStatus ?: "Please wait",
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        EmptyState(
          icon = Icons.Rounded.Folder,
          title = "No video folders found",
          message = "Your library is empty or folders are being filtered.",
          modifier = Modifier.fillMaxSize(),
        )
      }
    } else {
      Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = 8.dp, 
            end = 8.dp, 
            top = 12.dp, // Added breathing space from top bar
            bottom = navigationBarHeight + 12.dp // Added bottom breathing space
          ),
          verticalArrangement = Arrangement.spacedBy(8.dp), // Added space between cards
        ) {
          items(folders, key = { it.bucketId }) { folder ->
            FolderCard(
              folder = folder,
              settings = folderCardSettings,
              isSelected = selectionManager.isSelected(folder),
              onClick = { onFolderClick(folder) },
              onLongClick = { onFolderLongClick(folder) },
              onThumbClick = { if (tapThumbnailToSelect) onFolderLongClick(folder) else onFolderClick(folder) },
            )
          }
        }

        Box(modifier = Modifier.fillMaxSize().padding(bottom = navigationBarHeight)) {
          LazyColumnScrollbar(
            state = listState,
            settings = ScrollbarSettings(
              thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
              thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
            ),
          ) {}
        }
      }
      if (scanStatus != null && !showLoading) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().padding(2.dp),
          color = MaterialTheme.colorScheme.secondary,
        )
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

  SortBottomSheet(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = "Sort & View Options",
    sortType = sortType.displayName,
    onSortTypeChange = { typeName ->
      FolderSortType.entries.find { it.displayName == typeName }?.let(onSortTypeChange)
    },
    sortOrderAsc = sortOrder.isAscending,
    onSortOrderChange = { onSortOrderChange(if (it) SortOrder.Ascending else SortOrder.Descending) },
    onReset = {
      onSortTypeChange(FolderSortType.Title)
      onSortOrderChange(SortOrder.Ascending)
      appearancePreferences.unlimitedNameLines.set(false)
      browserPreferences.showFolderPath.set(false)
      browserPreferences.showTotalVideosChip.set(true)
      browserPreferences.showTotalDurationChip.set(false)
      browserPreferences.showTotalSizeChip.set(false)
      browserPreferences.showDateChip.set(false)
    },
    types = listOf(FolderSortType.Title.displayName, FolderSortType.Date.displayName, FolderSortType.Size.displayName, FolderSortType.VideoCount.displayName),
    icons = listOf(ImageVector.vectorResource(id = R.drawable.sort_by_alpha_24px), Icons.Rounded.CalendarToday, Icons.Rounded.SwapVert, Icons.Rounded.VideoLibrary),
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
      VisibilityToggle(label = "Full Name", checked = unlimitedNameLines, onCheckedChange = { appearancePreferences.unlimitedNameLines.set(it) }),
      VisibilityToggle(label = "Path", checked = showFolderPath, onCheckedChange = { browserPreferences.showFolderPath.set(it) }),
      VisibilityToggle(label = "Total Videos", checked = showTotalVideosChip, onCheckedChange = { browserPreferences.showTotalVideosChip.set(it) }),
      VisibilityToggle(label = "Total Duration", checked = showTotalDurationChip, onCheckedChange = { browserPreferences.showTotalDurationChip.set(it) }),
      VisibilityToggle(label = "Folder Size", checked = showTotalSizeChip, onCheckedChange = { browserPreferences.showTotalSizeChip.set(it) }),
      VisibilityToggle(label = "Date", checked = showDateChip, onCheckedChange = { browserPreferences.showDateChip.set(it) }),
    ),
  )
}
