package app.marlboroadvance.mpvex.ui.browser.videolist

import android.content.Intent
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.VideoSortType
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.AddToPlaylistDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FileOperationProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderPickerDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.LoadingDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.sheets.SortBottomSheet
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.fab.FabScrollHelper
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt
import app.marlboroadvance.mpvex.R

@Serializable
data class VideoListScreen(
  private val bucketId: String,
  private val folderName: String,
) : Screen {
  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backstack = LocalBackStack.current
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ViewModel
    val viewModel: VideoListViewModel =
      viewModel(
        key = "VideoListViewModel_$bucketId",
        factory = VideoListViewModel.factory(context.applicationContext as android.app.Application, bucketId),
      )
    val videosWithPlaybackInfo by viewModel.videosWithPlaybackInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsStateWithLifecycle()
    val lastPlayedInFolderPath by viewModel.lastPlayedInFolderPath.collectAsStateWithLifecycle()
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

    // Sorting
    val videoSortType by browserPreferences.videoSortType.collectAsState()
    val videoSortOrder by browserPreferences.videoSortOrder.collectAsState()
    val sortedVideosWithInfo =
      remember(videosWithPlaybackInfo, videoSortType, videoSortOrder) {
        val infoById = videosWithPlaybackInfo.associateBy { it.video.id }
        val sortedVideos = SortUtils.sortVideos(videosWithPlaybackInfo.map { it.video }, videoSortType, videoSortOrder)
        sortedVideos.map { video ->
          infoById[video.id] ?: VideoWithPlaybackInfo(video)
        }
      }

    // Selection manager
    val sortedVideos = remember(sortedVideosWithInfo) { sortedVideosWithInfo.map { it.video } }
    val selectionManager =
      rememberSelectionManager(
        items = sortedVideos,
        getId = { it.id },
        onDeleteItems = { items, _ -> viewModel.deleteVideos(items) },
        onRenameItem = { video, newName -> viewModel.renameVideo(video, newName) }
      )

    // UI State
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
    val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }

    // Copy/Move state
    val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
    val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
    val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
    val treePickerLauncher =
      rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val selectedVideos = selectionManager.getSelectedItems()
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

    // Private space state
    val movingToPrivateSpace = rememberSaveable { mutableStateOf(false) }
    val showPrivateSpaceCompletionDialog = rememberSaveable { mutableStateOf(false) }
    val privateSpaceMovedCount = remember { mutableIntStateOf(0) }

    val displayFolderName = videosWithPlaybackInfo.firstOrNull()?.video?.bucketDisplayName ?: folderName

    // FAB visibility state
    val isFabVisible = remember { mutableStateOf(true) }

    // Bottom bar animation state
    var showFloatingBottomBar by remember { mutableStateOf(false) }
    val animationDuration = 300

    LaunchedEffect(selectionManager.isInSelectionMode) {
      showFloatingBottomBar = selectionManager.isInSelectionMode
    }

    DisposableEffect(lifecycleOwner) {
      val observer =
        LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.ensureDataLoaded()
          }
        }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
      }
    }

    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }

    Scaffold(
      topBar = {
        BrowserTopBar(
          title = displayFolderName,
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          totalCount = sortedVideosWithInfo.size,
          onBackClick = {
            if (selectionManager.isInSelectionMode) {
              selectionManager.clear()
            } else {
              backstack.removeLastOrNull()
            }
          },
          onCancelSelection = { selectionManager.clear() },
          onSortClick = { sortDialogOpen.value = true },
          onSettingsClick = {
            backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
          },
          isSingleSelection = selectionManager.isSingleSelection,
          onInfoClick = {
            if (selectionManager.isSingleSelection) {
              val video = selectionManager.getSelectedItems().firstOrNull()
              if (video != null) {
                val intent = Intent(context, app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.data = video.uri
                context.startActivity(intent)
                selectionManager.clear()
              }
            }
          },
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
        )
      },
    ) { padding ->
      val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()

      Box(modifier = Modifier.fillMaxSize()) {
        VideoListContent(
          folderId = bucketId,
          videosWithInfo = sortedVideosWithInfo,
          videoCardSettings = videoCardSettings,
          isLoading = isLoading && videosWithPlaybackInfo.isEmpty(),
          isRefreshing = isRefreshing,
          recentlyPlayedFilePath = lastPlayedInFolderPath ?: recentlyPlayedFilePath,
          autoScrollToLastPlayed = autoScrollToLastPlayed,
          onRefresh = { viewModel.refresh() },
          selectionManager = selectionManager,
          onVideoClick = remember(selectionManager) {
            { video ->
              if (selectionManager.isInSelectionMode) {
                selectionManager.toggle(video)
              } else {
                MediaUtils.playFile(video, context, "video_list")
              }
            }
          },
          onVideoLongClick = remember(selectionManager) {
            { video -> selectionManager.toggle(video) }
          },
          isFabVisible = isFabVisible,
          modifier = Modifier.padding(padding),
          showFloatingBottomBar = showFloatingBottomBar,
        )

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
            showRename = selectionManager.isSingleSelection
          )
        }
      }

      // All overlays (sort/delete/rename/copy-move/private-space/playlist) live in their
      // own composable so their driving state stays out of the main Content scope.
      VideoListDialogs(
        selectionManager = selectionManager,
        viewModel = viewModel,
        browserPreferences = browserPreferences,
        videoSortType = videoSortType,
        videoSortOrder = videoSortOrder,
        firstVideoPath = videosWithPlaybackInfo.firstOrNull()?.video?.path,
        sortDialogOpen = sortDialogOpen,
        deleteDialogOpen = deleteDialogOpen,
        renameDialogOpen = renameDialogOpen,
        addToPlaylistDialogOpen = addToPlaylistDialogOpen,
        folderPickerOpen = folderPickerOpen,
        progressDialogOpen = progressDialogOpen,
        operationType = operationType,
        movingToPrivateSpace = movingToPrivateSpace,
        showPrivateSpaceCompletionDialog = showPrivateSpaceCompletionDialog,
        privateSpaceMovedCount = privateSpaceMovedCount,
      )
    }
  }
}

@Composable
private fun VideoListDialogs(
  selectionManager: SelectionManager<Video, Long>,
  viewModel: VideoListViewModel,
  browserPreferences: BrowserPreferences,
  videoSortType: VideoSortType,
  videoSortOrder: SortOrder,
  firstVideoPath: String?,
  sortDialogOpen: androidx.compose.runtime.MutableState<Boolean>,
  deleteDialogOpen: androidx.compose.runtime.MutableState<Boolean>,
  renameDialogOpen: androidx.compose.runtime.MutableState<Boolean>,
  addToPlaylistDialogOpen: androidx.compose.runtime.MutableState<Boolean>,
  folderPickerOpen: androidx.compose.runtime.MutableState<Boolean>,
  progressDialogOpen: androidx.compose.runtime.MutableState<Boolean>,
  operationType: androidx.compose.runtime.MutableState<CopyPasteOps.OperationType?>,
  movingToPrivateSpace: androidx.compose.runtime.MutableState<Boolean>,
  showPrivateSpaceCompletionDialog: androidx.compose.runtime.MutableState<Boolean>,
  privateSpaceMovedCount: androidx.compose.runtime.MutableIntState,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Sort Sheet
  VideoSortBottomSheet(
    isOpen = sortDialogOpen.value,
    onDismiss = { sortDialogOpen.value = false },
    sortType = videoSortType,
    sortOrder = videoSortOrder,
    onSortTypeChange = { type -> browserPreferences.videoSortType.set(type) },
    onSortOrderChange = { order -> browserPreferences.videoSortOrder.set(order) },
  )

  // Delete Dialog (Sheet) — gated so getSelectedItems()/map only runs while the
  // sheet is actually open, not on every Content recomposition.
  if (deleteDialogOpen.value) {
    val selectedForDelete = selectionManager.getSelectedItems()
    DeleteConfirmationDialog(
      isOpen = true,
      onDismiss = { deleteDialogOpen.value = false },
      onConfirm = { selectionManager.deleteSelected() },
      itemType = "video",
      itemCount = selectedForDelete.size,
      itemNames = selectedForDelete.map { it.displayName },
    )
  }

  // Rename Dialog
  if (renameDialogOpen.value && selectionManager.isSingleSelection) {
    val video = selectionManager.getSelectedItems().firstOrNull()
    if (video != null) {
      val baseName = video.displayName.substringBeforeLast('.')
      val extension = "." + video.displayName.substringAfterLast('.', "")
      RenameDialog(
        isOpen = true,
        onDismiss = { renameDialogOpen.value = false },
        onConfirm = { newName -> selectionManager.renameSelected(newName) },
        currentName = baseName,
        itemType = "file",
        extension = if (extension != ".") extension else null,
      )
    }
  }

  // Folder Picker (Sheet)
  FolderPickerDialog(
    isOpen = folderPickerOpen.value,
    currentPath =
      firstVideoPath?.let { File(it).parent }
        ?: Environment.getExternalStorageDirectory().absolutePath,
    titlePrefix = if (operationType.value is CopyPasteOps.OperationType.Copy) "Copy to" else "Move to",
    onDismiss = { folderPickerOpen.value = false },
    onFolderSelected = { destinationPath ->
      folderPickerOpen.value = false
      val selectedVideos = selectionManager.getSelectedItems()
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

  // File Operation Progress (Sheet)
  if (operationType.value != null) {
    // Collected here (not at Content scope) so frequent progress emissions during a
    // copy/move only recompose this dialog, not the whole screen body.
    val operationProgress by CopyPasteOps.operationProgress.collectAsStateWithLifecycle()
    FileOperationProgressDialog(
      isOpen = progressDialogOpen.value,
      operationType = operationType.value!!,
      progress = operationProgress,
      onCancel = { CopyPasteOps.cancelOperation() },
      onDismiss = {
        progressDialogOpen.value = false
        operationType.value = null
        selectionManager.clear()
        viewModel.refresh()
      },
    )
  }

  // Private Space Loading Sheet
  LoadingDialog(
    isOpen = movingToPrivateSpace.value,
    message = "Moving to private space...",
  )

  // Private Space Completion Dialog
  if (showPrivateSpaceCompletionDialog.value) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showPrivateSpaceCompletionDialog.value = false },
      title = { Text(text = "Moved to Private Space", style = MaterialTheme.typography.headlineSmall) },
      text = {
        Text(
          text = "Successfully moved ${privateSpaceMovedCount.intValue} video(s) to private space.\n\n" +
              "To access private space, long press on the app name at the top of the main screen.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        androidx.compose.material3.Button(
          onClick = { showPrivateSpaceCompletionDialog.value = false },
        ) {
          Text("Close")
        }
      },
    )
  }

  // Add to Playlist Dialog
  AddToPlaylistDialog(
    isOpen = addToPlaylistDialogOpen.value,
    videos = selectionManager.getSelectedItems(),
    onDismiss = { addToPlaylistDialogOpen.value = false },
    onSuccess = {
      selectionManager.clear()
      viewModel.refresh()
    },
  )
}

@Composable
private fun VideoListContent(
  folderId: String,
  videosWithInfo: List<VideoWithPlaybackInfo>,
  videoCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.VideoCardSettings,
  isLoading: Boolean,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  recentlyPlayedFilePath: String?,
  autoScrollToLastPlayed: Boolean,
  onRefresh: suspend () -> Unit,
  selectionManager: SelectionManager<Video, Long>,
  onVideoClick: (Video) -> Unit,
  onVideoLongClick: (Video) -> Unit,
  isFabVisible: androidx.compose.runtime.MutableState<Boolean>,
  modifier: Modifier = Modifier,
  showFloatingBottomBar: Boolean = false,
) {
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val gesturePreferences = koinInject<GesturePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val density = LocalDensity.current
  val navigationBarHeight = app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight.current
  val thumbWidthDp = 160.dp
  val aspect = 16f / 9f
  val thumbWidthPx = with(density) { thumbWidthDp.roundToPx() }
  val thumbHeightPx = (thumbWidthPx / aspect).roundToInt()

  LaunchedEffect(folderId, showVideoThumbnails, videosWithInfo.size, thumbWidthPx, thumbHeightPx) {
    if (showVideoThumbnails && videosWithInfo.isNotEmpty()) {
      thumbnailRepository.startFolderThumbnailGeneration(
        folderId = folderId,
        videos = videosWithInfo.map { it.video },
        widthPx = thumbWidthPx,
        heightPx = thumbHeightPx,
      )
    }
  }

  when {
    isLoading && videosWithInfo.isEmpty() -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary)
      }
    }
    videosWithInfo.isEmpty() && !isLoading -> {
      EmptyState(
        icon = Icons.Filled.VideoLibrary,
        title = "No videos in this folder",
        message = "Videos you add to this folder will appear here",
        modifier = modifier.fillMaxSize(),
      )
    }
    else -> {
      val rememberedListIndex = rememberSaveable { mutableIntStateOf(0) }
      val rememberedListOffset = rememberSaveable { mutableIntStateOf(0) }
      val initialListIndex = if (rememberedListIndex.intValue > 0) {
          rememberedListIndex.intValue
      } else if (autoScrollToLastPlayed && recentlyPlayedFilePath != null && videosWithInfo.isNotEmpty()) {
          videosWithInfo.indexOfFirst { it.video.path == recentlyPlayedFilePath }.coerceAtLeast(0)
      } else 0
      val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex, initialFirstVisibleItemScrollOffset = rememberedListOffset.intValue)
      LaunchedEffect(listState) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
          .collectLatest { (index, offset) ->
            rememberedListIndex.intValue = index
            rememberedListOffset.intValue = offset
          }
      }
      FabScrollHelper.trackScrollForFabVisibility(listState = listState, gridState = null, isFabVisible = isFabVisible, expanded = false, onExpandedChange = {})
      val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
      val hasEnoughItems = videosWithInfo.size > 20
      val scrollbarAlpha by animateFloatAsState(targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f, animationSpec = tween(durationMillis = 200), label = "scrollbarAlpha")

      PullRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, listState = listState, modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = navigationBarHeight)) {
          LazyColumnScrollbar(
            state = listState,
            settings = ScrollbarSettings(
              thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
              thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
            ),
          ) {
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp, bottom = if (showFloatingBottomBar) 88.dp else 16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(
                count = videosWithInfo.size,
                key = { index -> videosWithInfo[index].video.id },
              ) { index ->
                val videoWithInfo = videosWithInfo[index]
                val video = videoWithInfo.video

                val currentOnClick = remember(video.id, onVideoClick) {
                  { onVideoClick(video) }
                }
                val currentOnLongClick = remember(video.id, onVideoLongClick) {
                  { onVideoLongClick(video) }
                }
                val currentOnThumbClick = remember(video.id, tapThumbnailToSelect, onVideoClick, onVideoLongClick) {
                  if (tapThumbnailToSelect) {
                    { onVideoLongClick(video) }
                  } else {
                    { onVideoClick(video) }
                  }
                }

                // Read selection through a keyed derivedStateOf so toggling one card
                // only recomposes that card, not every visible item that reads the
                // shared SelectionManager state.
                val isSelected by remember(selectionManager, video.id) {
                  derivedStateOf { selectionManager.isSelected(video) }
                }

                VideoCard(
                  video = video,
                  settings = videoCardSettings,
                  progressPercentage = videoWithInfo.progressPercentage,
                  isRecentlyPlayed = recentlyPlayedFilePath?.let { video.path == it } ?: false,
                  isSelected = isSelected,
                  isOldAndUnplayed = videoWithInfo.isOldAndUnplayed,
                  isWatched = videoWithInfo.isWatched,
                  onClick = currentOnClick,
                  onLongClick = currentOnLongClick,
                  onThumbClick = currentOnThumbClick,
                  showSubtitleIndicator = showSubtitleIndicator,
                  allowThumbnailGeneration = true,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VideoSortBottomSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  sortType: VideoSortType,
  sortOrder: SortOrder,
  onSortTypeChange: (VideoSortType) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val appearancePreferences = koinInject<AppearancePreferences>()
  val showThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val showVideoExtension by browserPreferences.showVideoExtension.collectAsState()
  val showSizeChip by browserPreferences.showSizeChip.collectAsState()
  val showResolutionChip by browserPreferences.showResolutionChip.collectAsState()
  val showFramerateInResolution by browserPreferences.showFramerateInResolution.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()

  SortBottomSheet(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = "Sort & View Options",
    sortType = sortType.displayName,
    onSortTypeChange = { typeName -> VideoSortType.entries.find { it.displayName == typeName }?.let(onSortTypeChange) },
    sortOrderAsc = sortOrder.isAscending,
    onSortOrderChange = { isAsc -> onSortOrderChange(if (isAsc) SortOrder.Ascending else SortOrder.Descending) },
    onReset = {
      onSortTypeChange(VideoSortType.Title)
      onSortOrderChange(SortOrder.Ascending)
      browserPreferences.showVideoThumbnails.set(true)
      browserPreferences.showVideoExtension.set(false)
      browserPreferences.showSubtitleIndicator.set(true)
      appearancePreferences.unlimitedNameLines.set(false)
      browserPreferences.showSizeChip.set(true)
      browserPreferences.showResolutionChip.set(true)
      browserPreferences.showFramerateInResolution.set(false)
      browserPreferences.showDateChip.set(true)
    },
    types = listOf(VideoSortType.Title.displayName, VideoSortType.Duration.displayName, VideoSortType.Date.displayName, VideoSortType.Size.displayName),
    icons = listOf(ImageVector.vectorResource(id = R.drawable.sort_by_alpha_24px), Icons.Filled.AccessTime, Icons.Filled.CalendarToday, Icons.Filled.SwapVert),
    getLabelForType = { type, _ ->
      when (type) {
        VideoSortType.Title.displayName -> Pair("A-Z", "Z-A")
        VideoSortType.Duration.displayName -> Pair("Shortest", "Longest")
        VideoSortType.Date.displayName -> Pair("Oldest", "Newest")
        VideoSortType.Size.displayName -> Pair("Smallest", "Biggest")
        else -> Pair("Asc", "Desc")
      }
    },
    visibilityToggles = listOf(
      VisibilityToggle(label = "Thumbnails", checked = showThumbnails, onCheckedChange = { browserPreferences.showVideoThumbnails.set(it) }),
      VisibilityToggle(label = "Extension", checked = showVideoExtension, onCheckedChange = { browserPreferences.showVideoExtension.set(it) }),
      VisibilityToggle(label = "Subtitles", checked = showSubtitleIndicator, onCheckedChange = { browserPreferences.showSubtitleIndicator.set(it) }),
      VisibilityToggle(label = "Full Name", checked = unlimitedNameLines, onCheckedChange = { appearancePreferences.unlimitedNameLines.set(it) }),
      VisibilityToggle(label = "Size", checked = showSizeChip, onCheckedChange = { browserPreferences.showSizeChip.set(it) }),
      VisibilityToggle(label = "Resolution", checked = showResolutionChip, onCheckedChange = { browserPreferences.showResolutionChip.set(it) }),
      VisibilityToggle(label = "Framerate", checked = showFramerateInResolution, onCheckedChange = { browserPreferences.showFramerateInResolution.set(it) }),
      VisibilityToggle(label = "Date", checked = showDateChip, onCheckedChange = { browserPreferences.showDateChip.set(it) }),
    ),
  )
}
