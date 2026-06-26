package app.marlboroadvance.mpvex.ui.browser.folderlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
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
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.NavigationBarState
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
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
import kotlinx.coroutines.delay
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

    val viewModel: FolderListViewModel = viewModel(
      factory = FolderListViewModel.factory(context.applicationContext as android.app.Application)
    )
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val gesturePreferences = koinInject<GesturePreferences>()

    val videoFolders by viewModel.videoFolders.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsStateWithLifecycle()
    val deleteProgress by viewModel.deleteProgress.collectAsStateWithLifecycle()

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
        viewModel.deleteFolders(folders)
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

    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }

    val onRefresh: suspend () -> Unit = remember(viewModel) { { viewModel.refresh() } }

    val currentTapThumbnailToSelect by androidx.compose.runtime.rememberUpdatedState(tapThumbnailToSelect)

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
              isLoading = isLoading,
              isScanning = isScanning,
              hasCompletedInitialLoad = hasCompletedInitialLoad,
              tapThumbnailToSelect = { currentTapThumbnailToSelect },
              navigationBarHeight = navigationBarHeight,
              listState = listState,
              isRefreshing = isRefreshing,
              selectionManager = selectionManager,
              folderCardSettings = folderCardSettings,
              onRefresh = onRefresh,
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
        unlimitedNameLines = unlimitedNameLines,
        showFolderPath = showFolderPath,
        showTotalVideosChip = showTotalVideosChip,
        showTotalDurationChip = showTotalDurationChip,
        showTotalSizeChip = showTotalSizeChip,
        showDateChip = showDateChip,
        onSortTypeChange = { browserPreferences.folderSortType.set(it) },
        onSortOrderChange = { browserPreferences.folderSortOrder.set(it) },
        onReset = {
          browserPreferences.folderSortType.set(FolderSortType.Title)
          browserPreferences.folderSortOrder.set(SortOrder.Ascending)
          appearancePreferences.unlimitedNameLines.set(false)
          browserPreferences.showFolderPath.set(false)
          browserPreferences.showTotalVideosChip.set(true)
          browserPreferences.showTotalDurationChip.set(false)
          browserPreferences.showTotalSizeChip.set(false)
          browserPreferences.showDateChip.set(false)
        },
        onUnlimitedNameLinesChange = { appearancePreferences.unlimitedNameLines.set(it) },
        onShowFolderPathChange = { browserPreferences.showFolderPath.set(it) },
        onShowTotalVideosChipChange = { browserPreferences.showTotalVideosChip.set(it) },
        onShowTotalDurationChipChange = { browserPreferences.showTotalDurationChip.set(it) },
        onShowTotalSizeChipChange = { browserPreferences.showTotalSizeChip.set(it) },
        onShowDateChipChange = { browserPreferences.showDateChip.set(it) },
      )

      // Gated so getSelectedItems()/map only runs while the sheet is open.
      if (deleteDialogOpen.value) {
        val selectedForDelete = selectionManager.getSelectedItems()
        DeleteConfirmationDialog(
          isOpen = true,
          onDismiss = { deleteDialogOpen.value = false },
          onConfirm = { selectionManager.deleteSelected() },
          itemType = "folder",
          itemCount = selectedForDelete.size,
          itemNames = selectedForDelete.map { it.name },
        )
      }

      DeleteProgressDialog(
        progress = deleteProgress,
        onCancel = { viewModel.cancelDeletion() },
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
  isLoading: Boolean,
  isScanning: Boolean,
  hasCompletedInitialLoad: Boolean,
  tapThumbnailToSelect: () -> Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  selectionManager: SelectionManager<VideoFolder, String>,
  folderCardSettings: FolderCardSettings,
  onRefresh: suspend () -> Unit,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
) {
  val showFullScreenLoading = (isLoading && folders.isEmpty()) || (!hasCompletedInitialLoad && folders.isEmpty())
  val showEmpty = folders.isEmpty() && !isLoading && !isScanning && hasCompletedInitialLoad

  // A3: Anti-flicker for scanning indicator. 
  // We wait for 400ms before showing the bar to prevent flashes on fast background scans.
  var showScanningIndicator by remember { mutableStateOf(false) }
  LaunchedEffect(isScanning) {
    if (isScanning) {
      delay(400)
      showScanningIndicator = true
    } else {
      showScanningIndicator = false
    }
  }

  val scrollbarAlpha by remember(folders.size) {
    val hasEnoughItems = folders.size > 20
    derivedStateOf {
      if (!hasEnoughItems || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)) 0f else 1f
    }
  }

  PullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    listState = listState,
    modifier = Modifier.fillMaxSize(),
  ) {
    if (showFullScreenLoading || showEmpty) {
      if (showFullScreenLoading) {
        LoadingState(
          title = "Scanning for videos...",
          message = "Please wait",
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
            top = 12.dp,
            bottom = navigationBarHeight + 12.dp
          ),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(folders, key = { it.bucketId }) { folder ->
            val isSelected by remember(selectionManager, folder.bucketId) {
              derivedStateOf { selectionManager.isSelected(folder) }
            }
            FolderCard(
              folder = folder,
              settings = folderCardSettings,
              isSelected = isSelected,
              onClick = { onFolderClick(folder) },
              onLongClick = { onFolderLongClick(folder) },
              onThumbClick = {
                if (tapThumbnailToSelect()) onFolderLongClick(folder)
                else onFolderClick(folder)
              },
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

      // A3: Replaced direct isScanning check with showScanningIndicator
      if (showScanningIndicator) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(horizontal = 16.dp)
            .align(Alignment.TopCenter)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
  unlimitedNameLines: Boolean,
  showFolderPath: Boolean,
  showTotalVideosChip: Boolean,
  showTotalDurationChip: Boolean,
  showTotalSizeChip: Boolean,
  showDateChip: Boolean,
  onSortTypeChange: (FolderSortType) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
  onReset: () -> Unit,
  onUnlimitedNameLinesChange: (Boolean) -> Unit,
  onShowFolderPathChange: (Boolean) -> Unit,
  onShowTotalVideosChipChange: (Boolean) -> Unit,
  onShowTotalDurationChipChange: (Boolean) -> Unit,
  onShowTotalSizeChipChange: (Boolean) -> Unit,
  onShowDateChipChange: (Boolean) -> Unit,
) {
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
    onReset = onReset,
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
      VisibilityToggle(label = "Full Name", checked = unlimitedNameLines, onCheckedChange = onUnlimitedNameLinesChange),
      VisibilityToggle(label = "Path", checked = showFolderPath, onCheckedChange = onShowFolderPathChange),
      VisibilityToggle(label = "Total Videos", checked = showTotalVideosChip, onCheckedChange = onShowTotalVideosChipChange),
      VisibilityToggle(label = "Total Duration", checked = showTotalDurationChip, onCheckedChange = onShowTotalDurationChipChange),
      VisibilityToggle(label = "Folder Size", checked = showTotalSizeChip, onCheckedChange = onShowTotalSizeChipChange),
      VisibilityToggle(label = "Date", checked = showDateChip, onCheckedChange = onShowDateChipChange),
    ),
  )
}
