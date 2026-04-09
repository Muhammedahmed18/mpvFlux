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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import app.marlboroadvance.mpvex.BuildConfig
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
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
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
    val videos by viewModel.videos.collectAsState()
    val videosWithPlaybackInfo by viewModel.videosWithPlaybackInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val lastPlayedInFolderPath by viewModel.lastPlayedInFolderPath.collectAsState()
    val videosWereDeletedOrMoved by viewModel.videosWereDeletedOrMoved.collectAsState()
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
        // Maintain the playback info mapping — O(1) lookup per item
        sortedVideos.map { video ->
          infoById[video.id] ?: VideoWithPlaybackInfo(video)
        }
      }

    // Selection manager
    val selectionManager =
      rememberSelectionManager(
        items = sortedVideosWithInfo.map { it.video },
        getId = { it.id },
        onDeleteItems = { items, _ -> viewModel.deleteVideos(items) },
        onRenameItem = { video, newName -> viewModel.renameVideo(video, newName) },
        onOperationComplete = { viewModel.refresh() },
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
    val operationProgress by CopyPasteOps.operationProgress.collectAsState()
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

    val displayFolderName = videos.firstOrNull()?.bucketDisplayName ?: folderName

    // FAB visibility state
    val isFabVisible = remember { mutableStateOf(true) }

    // Bottom bar animation state
    var showFloatingBottomBar by remember { mutableStateOf(false) }
    val animationDuration = 300

    // Handle selection mode changes with animation
    LaunchedEffect(selectionManager.isInSelectionMode) {
      showFloatingBottomBar = selectionManager.isInSelectionMode
    }

    // Predictive back: Only intercept when in selection mode
    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }

    // Listen for lifecycle resume events and refresh videos when coming into focus
    DisposableEffect(lifecycleOwner) {
      val observer =
        LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            // Use ensureDataLoaded instead of refresh to prevent redundant hard scans
            viewModel.ensureDataLoaded()
          }
        }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
      }
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
          isLoading = isLoading && videos.isEmpty(),
          isRefreshing = isRefreshing,
          recentlyPlayedFilePath = lastPlayedInFolderPath ?: recentlyPlayedFilePath,
          videosWereDeletedOrMoved = videosWereDeletedOrMoved,
          autoScrollToLastPlayed = autoScrollToLastPlayed,
          onRefresh = { viewModel.refresh() },
          selectionManager = selectionManager,
          onVideoClick = { video ->
            if (selectionManager.isInSelectionMode) {
              selectionManager.toggle(video)
            } else {
              // Always use MediaUtils.playFile which lets PlayerActivity auto-generate playlist
              // This avoids TransactionTooLargeException from passing large playlists
              // PlayerActivity will auto-generate playlist from folder if playlistMode is enabled
              MediaUtils.playFile(video, context, "video_list")
            }
          },
          onVideoLongClick = { video -> selectionManager.toggle(video) },
          isFabVisible = isFabVisible,
          modifier = Modifier.padding(padding),
          showFloatingBottomBar = showFloatingBottomBar,
        )

        // Floating Material 3 Button Group overlay with animation
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
            showRename = selectionManager.isSingleSelection
          )
        }
      }

      // Sort Sheet
      VideoSortBottomSheet(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = videoSortType,
        sortOrder = videoSortOrder,
        onSortTypeChange = { type -> browserPreferences.videoSortType.set(type) },
        onSortOrderChange = { order -> browserPreferences.videoSortOrder.set(order) },
      )

      // Delete Dialog
      DeleteConfirmationDialog(
        isOpen = deleteDialogOpen.value,
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = { selectionManager.deleteSelected() },
        itemType = "video",
        itemCount = selectionManager.selectedCount,
        itemNames = selectionManager.getSelectedItems().map { it.displayName },
      )

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

      // Folder Picker Dialog
      FolderPickerDialog(
        isOpen = folderPickerOpen.value,
        currentPath =
          videos.firstOrNull()?.let { File(it.path).parent }
            ?: Environment.getExternalStorageDirectory().absolutePath,
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
              operationProgress.error == null
            ) {
              viewModel.setVideosWereDeletedOrMoved()
            }
            operationType.value = null
            selectionManager.clear()
            viewModel.refresh()
          },
        )
      }

      // Private Space Loading Dialog
      LoadingDialog(
        isOpen = movingToPrivateSpace.value,
        message = "Moving to private space...",
      )

      // Private Space Completion Dialog
      if (showPrivateSpaceCompletionDialog.value) {
        androidx.compose.material3.AlertDialog(
          onDismissRequest = { showPrivateSpaceCompletionDialog.value = false },
          title = {
            Text(
              text = "Moved to Private Space",
              style = MaterialTheme.typography.headlineSmall,
            )
          },
          text = {
            Text(
              text =
                "Successfully moved ${privateSpaceMovedCount.intValue} video(s) to private space.\n\n" +
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
  }
}

@Composable
private fun VideoListContent(
  folderId: String,
  videosWithInfo: List<VideoWithPlaybackInfo>,
  videoCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.VideoCardSettings,
  isLoading: Boolean,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  recentlyPlayedFilePath: String?,
  videosWereDeletedOrMoved: Boolean,
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
  // Standardized thumbnail size (must match VideoCard.kt thumbWidthDp)
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
      Box(
        modifier = modifier
          .fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(48.dp),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    videosWithInfo.isEmpty() && videosWereDeletedOrMoved -> {
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

      val listState = rememberLazyListState(
          initialFirstVisibleItemIndex = initialListIndex,
          initialFirstVisibleItemScrollOffset = rememberedListOffset.intValue
      )

      LaunchedEffect(listState) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
          .collectLatest { (index, offset) ->
            rememberedListIndex.intValue = index
            rememberedListOffset.intValue = offset
          }
      }

      FabScrollHelper.trackScrollForFabVisibility(
        listState = listState,
        gridState = null,
        isFabVisible = isFabVisible,
        expanded = false,
        onExpandedChange = {},
      )

      val isAtTop by remember {
        derivedStateOf {
          listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
      }

      val hasEnoughItems = videosWithInfo.size > 20

      val scrollbarAlpha by animateFloatAsState(
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
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                bottom = if (showFloatingBottomBar) 88.dp else 16.dp
              ),
            ) {
              items(
                count = videosWithInfo.size,
                key = { index -> "${videosWithInfo[index].video.id}_${videosWithInfo[index].video.path}" },
              ) { index ->
                val videoWithInfo = videosWithInfo[index]
                val isRecentlyPlayed = recentlyPlayedFilePath?.let { videoWithInfo.video.path == it } ?: false

                VideoCard(
                  video = videoWithInfo.video,
                  settings = videoCardSettings,
                  progressPercentage = videoWithInfo.progressPercentage,
                  isRecentlyPlayed = isRecentlyPlayed,
                  isSelected = selectionManager.isSelected(videoWithInfo.video),
                  isOldAndUnplayed = videoWithInfo.isOldAndUnplayed,
                  isWatched = videoWithInfo.isWatched,
                  onClick = { onVideoClick(videoWithInfo.video) },
                  onLongClick = { onVideoLongClick(videoWithInfo.video) },
                  onThumbClick = if (tapThumbnailToSelect) {
                    { onVideoLongClick(videoWithInfo.video) }
                  } else {
                    { onVideoClick(videoWithInfo.video) }
                  },
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
    onSortTypeChange = { typeName ->
      VideoSortType.entries.find { it.displayName == typeName }?.let(onSortTypeChange)
    },
    sortOrderAsc = sortOrder.isAscending,
    onSortOrderChange = { isAsc ->
      onSortOrderChange(if (isAsc) SortOrder.Ascending else SortOrder.Descending)
    },
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
    types =
      listOf(
        VideoSortType.Title.displayName,
        VideoSortType.Duration.displayName,
        VideoSortType.Date.displayName,
        VideoSortType.Size.displayName,
      ),
    icons =
      listOf(
        ImageVector.vectorResource(id = R.drawable.sort_by_alpha_24px),
        Icons.Filled.AccessTime,
        Icons.Filled.CalendarToday,
        Icons.Filled.SwapVert,
      ),
    getLabelForType = { type, _ ->
      when (type) {
        VideoSortType.Title.displayName -> Pair("A-Z", "Z-A")
        VideoSortType.Duration.displayName -> Pair("Shortest", "Longest")
        VideoSortType.Date.displayName -> Pair("Oldest", "Newest")
        VideoSortType.Size.displayName -> Pair("Smallest", "Biggest")
        else -> Pair("Asc", "Desc")
      }
    },
    visibilityToggles =
      listOf(
        VisibilityToggle(
          label = "Thumbnails",
          checked = showThumbnails,
          onCheckedChange = { browserPreferences.showVideoThumbnails.set(it) },
        ),
        VisibilityToggle(
          label = "Extension",
          checked = showVideoExtension,
          onCheckedChange = { browserPreferences.showVideoExtension.set(it) },
        ),
        VisibilityToggle(
          label = "Subtitles",
          checked = showSubtitleIndicator,
          onCheckedChange = { browserPreferences.showSubtitleIndicator.set(it) },
        ),
        VisibilityToggle(
          label = "Full Name",
          checked = unlimitedNameLines,
          onCheckedChange = { appearancePreferences.unlimitedNameLines.set(it) },
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
          label = "Date",
          checked = showDateChip,
          onCheckedChange = { browserPreferences.showDateChip.set(it) },
        ),
      ),
  )
}
