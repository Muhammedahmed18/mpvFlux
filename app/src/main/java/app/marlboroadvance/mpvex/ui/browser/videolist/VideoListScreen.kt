package app.marlboroadvance.mpvex.ui.browser.videolist

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import android.content.Intent
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import app.marlboroadvance.mpvex.ui.browser.cards.videoCardGroupPosition
import app.marlboroadvance.mpvex.ui.browser.sheets.rememberSortOrderLabels
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
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
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.VideoSortType
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.SelectionAction
import app.marlboroadvance.mpvex.ui.browser.components.SelectionToolbar
import app.marlboroadvance.mpvex.ui.browser.components.SelectionToolbarDefaults
import app.marlboroadvance.mpvex.ui.browser.dialogs.CopyOrMoveSheet
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionHaptics
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Search
import app.marlboroadvance.mpvex.ui.browser.components.selectionToolbarScroll
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.AddToPlaylistDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FileOperationProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderPickerDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.LoadingDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.sheets.SortBottomSheet
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.fab.FabScrollHelper
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionSummary
import app.marlboroadvance.mpvex.ui.browser.components.ActionFeedbackHost
import app.marlboroadvance.mpvex.ui.browser.components.rememberActionFeedbackState
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.MarkAsHost
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.LocalSafeBack
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
    val safeBack = LocalSafeBack.current
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    val decoderPreferences = koinInject<DecoderPreferences>()
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
    val showDecoderChip by browserPreferences.showDecoderChip.collectAsState()
    val tryHwDecoding by decoderPreferences.tryHWDecoding.collectAsState()
    val showUnplayedOldVideoLabel by appearancePreferences.showUnplayedOldVideoLabel.collectAsState()
    val unplayedOldVideoDays by appearancePreferences.unplayedOldVideoDays.collectAsState()

    val videoCardSettings = remember(
      unlimitedNameLines, showThumbnails, showVideoExtension, showSizeChip,
      showResolutionChip, showFramerateInResolution, showProgressBar,
      showDateChip, showDecoderChip, tryHwDecoding, showUnplayedOldVideoLabel, unplayedOldVideoDays
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
        showDecoderChip = showDecoderChip,
        tryHwDecoding = tryHwDecoding,
        showUnplayedOldVideoLabel = showUnplayedOldVideoLabel,
        unplayedOldVideoDays = unplayedOldVideoDays
      )
    }

    // Sorting
    val videoSortType by browserPreferences.videoSortType.collectAsState()
    val videoSortOrder by browserPreferences.videoSortOrder.collectAsState()
    val allSortedVideosWithInfo =
      remember(videosWithPlaybackInfo, videoSortType, videoSortOrder) {
        val infoById = videosWithPlaybackInfo.associateBy { it.video.id }
        val sortedVideos = SortUtils.sortVideos(videosWithPlaybackInfo.map { it.video }, videoSortType, videoSortOrder)
        sortedVideos.map { video ->
          infoById[video.id] ?: VideoWithPlaybackInfo(video)
        }
      }

    // Search filters the folder's already-loaded videos in memory by filename only.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val sortedVideosWithInfo = remember(allSortedVideosWithInfo, searchQuery) {
      val query = searchQuery.trim()
      if (query.isEmpty()) {
        allSortedVideosWithInfo
      } else {
        allSortedVideosWithInfo.filter { it.video.displayName.contains(query, ignoreCase = true) }
      }
    }
    val noSearchResults =
      searchQuery.isNotBlank() && sortedVideosWithInfo.isEmpty() && allSortedVideosWithInfo.isNotEmpty()

    // Top bar subtitle, e.g. "12 videos · 8 watched" (M3 Expressive top app bar subtitle).
    val videoCountLabel = pluralStringResource(
      R.plurals.card_video_count,
      sortedVideosWithInfo.size,
      sortedVideosWithInfo.size,
    )
    val watchedCount = remember(sortedVideosWithInfo) { sortedVideosWithInfo.count { it.isWatched } }
    val watchedLabel = stringResource(R.string.topbar_subtitle_watched, videoCountLabel, watchedCount)
    val topBarSubtitle = when {
      sortedVideosWithInfo.isEmpty() -> null
      watchedCount > 0 -> watchedLabel
      else -> videoCountLabel
    }

    val feedback = rememberActionFeedbackState()

    // Selection manager
    val sortedVideos = remember(sortedVideosWithInfo) { sortedVideosWithInfo.map { it.video } }
    val selectionManager =
      rememberSelectionManager(
        items = sortedVideos,
        getId = { it.id },
        onDeleteItems = { items, _ -> viewModel.deleteVideosWithProgress(items) },
        onRenameItem = { video, newName -> viewModel.renameVideo(video, newName) },
        feedback = feedback,
        deletedMessage = R.plurals.delete_result_videos,
      )
    val deleteProgress by viewModel.deleteProgress.collectAsStateWithLifecycle()

    // UI State
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val markAsOpen = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
    val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }
    val copyOrMoveOpen = rememberSaveable { mutableStateOf(false) }

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

    // Selection toolbar state. It collapses to just the Delete FAB while scrolling down.
    val showFloatingBottomBar = selectionManager.isInSelectionMode
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(selectionManager.isInSelectionMode) {
      if (selectionManager.isInSelectionMode) toolbarExpanded = true
    }
    SelectionHaptics(selectionManager.selectedCount)

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
    // Back closes search first (selection, when active, takes the back press before this).
    BackHandler(enabled = searchActive && !selectionManager.isInSelectionMode) {
      searchActive = false
      searchQuery = ""
    }

    val extractAudio = app.marlboroadvance.mpvex.ui.browser.sheets.rememberExtractAudioController()

    Scaffold(
      topBar = {
        val selectionSummary =
          if (selectionManager.isInSelectionMode) {
            remember(selectionManager, selectionManager.state) {
              SelectionSummary.ofVideos(selectionManager.getSelectedItems())
            }
          } else {
            null
          }
        BrowserTopBar(
          title = displayFolderName,
          subtitle = topBarSubtitle,
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectionSummary = selectionSummary,
          selectedCount = selectionManager.selectedCount,
          totalCount = sortedVideosWithInfo.size,
          onBackClick = {
            if (selectionManager.isInSelectionMode) {
              selectionManager.clear()
            } else {
              safeBack()
            }
          },
          onCancelSelection = { selectionManager.clear() },
          onSearchClick = { searchActive = true },
          searchActive = searchActive,
          searchQuery = searchQuery,
          onSearchQueryChange = { searchQuery = it },
          onSearchClose = {
            searchActive = false
            searchQuery = ""
          },
          searchPlaceholder = stringResource(R.string.search_videos),
          onSortClick = { sortDialogOpen.value = true },
          onSettingsClick = {
            backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
          },
          isSingleSelection = selectionManager.isSingleSelection,
          // Single-video actions sit in the top bar (shown only when one video is selected);
          // everything else lives in the floating SelectionToolbar below.
          onInfoClick = {
            val video = selectionManager.getSelectedItems().firstOrNull()
            if (video != null) {
              context.startActivity(
                app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity.createIntent(context, video.uri, video.path),
              )
              selectionManager.clear()
            }
          },
          onExtractAudioClick = {
            if (extractAudio.open(selectionManager.getSelectedItems().singleOrNull())) {
              selectionManager.clear()
            }
          },
          extractAudioEnabled = !extractAudio.isRunning,
          showSelectAllWithActions = true,
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
        )
      },
    ) { padding ->
      app.marlboroadvance.mpvex.ui.browser.sheets.ExtractAudioHost(extractAudio)
      val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()

      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .selectionToolbarScroll(
              onExpand = { toolbarExpanded = true },
              onCollapse = { toolbarExpanded = false },
            ),
      ) {
        VideoListContent(
          folderId = bucketId,
          videosWithInfo = sortedVideosWithInfo,
          searchActive = searchActive,
          searchQuery = searchQuery,
          noSearchResults = noSearchResults,
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

        // Strings are read here (not inside the action lambdas) so they follow the locale.
        val playlistLabel = stringResource(R.string.add_to_playlist)
        val renameLabel = stringResource(R.string.rename)
        val sendLabel = stringResource(R.string.selection_send_to_folder)
        val deleteLabel = stringResource(R.string.delete)
        val markAsLabel = stringResource(R.string.mark_as_title)

        val isSingle = selectionManager.isSingleSelection
        val markAs = SelectionAction(ImageVector.vectorResource(R.drawable.ic_sym_done_all), markAsLabel, { markAsOpen.value = true })

        // One video:  Copy/Move · Add to playlist · Rename · Mark as   [Delete]
        // Several:    Copy/Move · Add to playlist · Mark as            [Delete]
        // Info and Extract audio are in the top bar for a single video.
        SelectionToolbar(
          visible = showFloatingBottomBar,
          expanded = toolbarExpanded,
          modifier = Modifier.align(Alignment.BottomCenter),
          primaryAction = SelectionAction(Icons.Rounded.Delete, deleteLabel, { deleteDialogOpen.value = true }, destructive = true),
          actions = buildList {
            add(SelectionAction(Icons.AutoMirrored.Rounded.DriveFileMove, sendLabel, { copyOrMoveOpen.value = true }))
            add(SelectionAction(Icons.AutoMirrored.Rounded.PlaylistAdd, playlistLabel, { addToPlaylistDialogOpen.value = true }))
            if (isSingle) {
              add(SelectionAction(Icons.Rounded.DriveFileRenameOutline, renameLabel, { renameDialogOpen.value = true }))
            }
            add(markAs)
          },
        )
      }

      DeleteProgressDialog(
        progress = deleteProgress,
        onCancel = { viewModel.cancelDeletion() },
      )

      CopyOrMoveSheet(
        isOpen = copyOrMoveOpen.value,
        itemCount = selectionManager.selectedCount,
        onDismiss = { copyOrMoveOpen.value = false },
        onCopy = {
          operationType.value = CopyPasteOps.OperationType.Copy
          if (CopyPasteOps.canUseDirectFileOperations()) folderPickerOpen.value = true else treePickerLauncher.launch(null)
        },
        onMove = {
          operationType.value = CopyPasteOps.OperationType.Move
          if (CopyPasteOps.canUseDirectFileOperations()) folderPickerOpen.value = true else treePickerLauncher.launch(null)
        },
      )

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

      ActionFeedbackHost(feedback)

      // "Mark as" sheet (New / Last played / Finished / Unwatched) with Undo.
      MarkAsHost(
        open = markAsOpen,
        selectedVideos = { selectionManager.getSelectedItems() },
        allFinished = if (markAsOpen.value) {
          val selectedIds = selectionManager.getSelectedItems().map { it.id }.toSet()
          val selected = videosWithPlaybackInfo.filter { it.video.id in selectedIds }
          selected.isNotEmpty() && selected.all { it.isWatched }
        } else {
          false
        },
        feedback = feedback,
        onApplied = { selectionManager.clear() },
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
      titleRes = R.plurals.delete_title_video,
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
        titleRes = R.string.rename_title_file,
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
    titlePrefix = stringResource(if (operationType.value is CopyPasteOps.OperationType.Copy) R.string.copy_to else R.string.move_to),
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
    message = stringResource(R.string.moving_to_private_space),
  )

  // Private Space Completion Dialog
  if (showPrivateSpaceCompletionDialog.value) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showPrivateSpaceCompletionDialog.value = false },
      title = { Text(text = stringResource(R.string.dialog_moved_private_title), style = MaterialTheme.typography.headlineSmall) },
      text = {
        Text(
          text = pluralStringResource(
            R.plurals.private_space_moved,
            privateSpaceMovedCount.intValue,
            privateSpaceMovedCount.intValue,
          ),
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        androidx.compose.material3.Button(
          onClick = { showPrivateSpaceCompletionDialog.value = false },
        ) {
          Text(stringResource(R.string.close))
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
  searchActive: Boolean,
  searchQuery: String,
  noSearchResults: Boolean,
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


  // Leaving the folder stops its thumbnail batch (progress is kept for next time).
  DisposableEffect(folderId) {
    onDispose { thumbnailRepository.stopFolderGeneration(folderId) }
  }

  // Not restarted for every filtered subset while searching; it resumes with the full list on close.
  LaunchedEffect(folderId, showVideoThumbnails, videosWithInfo.size, thumbWidthPx, thumbHeightPx, searchActive) {
    if (showVideoThumbnails && videosWithInfo.isNotEmpty() && !searchActive) {
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
    videosWithInfo.isEmpty() && !isLoading && !noSearchResults -> {
      EmptyState(
        icon = Icons.Filled.VideoLibrary,
        title = stringResource(R.string.no_videos_in_folder),
        message = stringResource(R.string.no_videos_in_folder_message),
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
      // Search keeps the browsing position: it is saved when search opens and restored when it
      // closes; typing scrolls the matches to the top.
      var preSearchScroll by remember { mutableStateOf<Pair<Int, Int>?>(null) }
      LaunchedEffect(searchActive) {
        if (searchActive) {
          preSearchScroll = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        } else {
          preSearchScroll?.let { (index, offset) -> listState.scrollToItem(index, offset) }
          preSearchScroll = null
        }
      }
      LaunchedEffect(searchQuery) {
        if (searchActive) listState.scrollToItem(0)
      }
      FabScrollHelper.trackScrollForFabVisibility(listState = listState, gridState = null, isFabVisible = isFabVisible, expanded = false, onExpandedChange = {})
      val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
      val hasEnoughItems = videosWithInfo.size > 20
      val scrollbarAlpha by animateFloatAsState(targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f, animationSpec = tween(durationMillis = 200), label = "scrollbarAlpha")

      if (noSearchResults) {
        EmptyState(
          icon = Icons.Rounded.Search,
          title = stringResource(R.string.no_results_found),
          message = stringResource(R.string.no_results_found_message),
          modifier = modifier.fillMaxSize(),
        )
      } else PullRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, listState = listState, modifier = modifier.fillMaxSize()) {
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
              modifier = Modifier.fillMaxSize().readableContentWidth(),
              contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp, bottom = if (showFloatingBottomBar) SelectionToolbarDefaults.contentBottomPadding() else 16.dp),
              // Video rows carry their own 8dp padding, so only a small gap is needed.
              verticalArrangement = Arrangement.spacedBy(2.dp),
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
                  isMarkedNew = videoWithInfo.isMarkedNew,
                  isNew = videoWithInfo.isNew,
                  isWatched = videoWithInfo.isWatched,
                  onClick = currentOnClick,
                  onLongClick = currentOnLongClick,
                  onThumbClick = currentOnThumbClick,
                  showSubtitleIndicator = showSubtitleIndicator,
                  allowThumbnailGeneration = true,
                  groupPosition = videoCardGroupPosition(
                    hasPrevious = index > 0,
                    hasNext = index < videosWithInfo.lastIndex,
                  ),
                  inSelectionMode = selectionManager.isInSelectionMode,
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
  val showDecoderChip by browserPreferences.showDecoderChip.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()

  val sortLabels = rememberSortOrderLabels()
  SortBottomSheet(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = stringResource(R.string.sort_view_options),
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
      browserPreferences.showDecoderChip.set(false)
    },
    types = listOf(VideoSortType.Title.displayName, VideoSortType.Duration.displayName, VideoSortType.Date.displayName, VideoSortType.Size.displayName),
    icons = listOf(ImageVector.vectorResource(id = R.drawable.sort_by_alpha_24px), Icons.Filled.AccessTime, Icons.Filled.CalendarToday, Icons.Filled.SwapVert),
    getLabelForType = { type, _ ->
      when (type) {
        VideoSortType.Title.displayName -> Pair("A-Z", "Z-A")
        VideoSortType.Duration.displayName -> Pair(sortLabels.shortest, sortLabels.longest)
        VideoSortType.Date.displayName -> Pair(sortLabels.oldest, sortLabels.newest)
        VideoSortType.Size.displayName -> Pair(sortLabels.smallest, sortLabels.largest)
        else -> Pair(sortLabels.asc, sortLabels.desc)
      }
    },
    visibilityToggles = listOf(
      VisibilityToggle(label = stringResource(R.string.pref_storage_thumbnails_title), checked = showThumbnails, onCheckedChange = { browserPreferences.showVideoThumbnails.set(it) }),
      VisibilityToggle(label = stringResource(R.string.view_toggle_show_extension), checked = showVideoExtension, onCheckedChange = { browserPreferences.showVideoExtension.set(it) }),
      VisibilityToggle(label = stringResource(R.string.pref_subtitles), checked = showSubtitleIndicator, onCheckedChange = { browserPreferences.showSubtitleIndicator.set(it) }),
      VisibilityToggle(label = stringResource(R.string.view_toggle_full_name), checked = unlimitedNameLines, onCheckedChange = { appearancePreferences.unlimitedNameLines.set(it) }),
      VisibilityToggle(label = stringResource(R.string.media_info_size), checked = showSizeChip, onCheckedChange = { browserPreferences.showSizeChip.set(it) }),
      VisibilityToggle(label = stringResource(R.string.media_info_resolution), checked = showResolutionChip, onCheckedChange = { browserPreferences.showResolutionChip.set(it) }),
      VisibilityToggle(label = stringResource(R.string.view_toggle_framerate), checked = showFramerateInResolution, onCheckedChange = { browserPreferences.showFramerateInResolution.set(it) }),
      VisibilityToggle(label = stringResource(R.string.view_toggle_date), checked = showDateChip, onCheckedChange = { browserPreferences.showDateChip.set(it) }),
      VisibilityToggle(label = stringResource(R.string.view_toggle_decoder), checked = showDecoderChip, onCheckedChange = { browserPreferences.showDecoderChip.set(it) }),
    ),
  )
}
