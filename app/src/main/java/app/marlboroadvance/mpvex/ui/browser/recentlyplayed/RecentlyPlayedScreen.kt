package app.marlboroadvance.mpvex.ui.browser.recentlyplayed

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationSheet
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistDetailScreen
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject

@Serializable
object RecentlyPlayedScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val viewModel: RecentlyPlayedViewModel =
      viewModel(factory = RecentlyPlayedViewModel.factory(context.applicationContext as android.app.Application))

    val recentItems by viewModel.recentItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showDeleteSheet = rememberSaveable { mutableStateOf(false) }
    val advancedPreferences = koinInject<AdvancedPreferences>()
    val appearancePreferences = koinInject<app.marlboroadvance.mpvex.preferences.AppearancePreferences>()
    val enableRecentlyPlayed by advancedPreferences.enableRecentlyPlayed.collectAsState()

    val listState = rememberLazyListState()
    val browserPreferences = koinInject<BrowserPreferences>()
    val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()

    val selectionManager =
      rememberSelectionManager(
        items = recentItems,
        getId = { item ->
          when (item) {
            is RecentlyPlayedItem.VideoItem -> "video_${item.video.id}"
            is RecentlyPlayedItem.PlaylistItem -> "playlist_${item.playlist.id}"
          }
        },
        onDeleteItems = { items, deleteFiles ->
          val videos = items.filterIsInstance<RecentlyPlayedItem.VideoItem>().map { it.video }
          val playlistIds = items.filterIsInstance<RecentlyPlayedItem.PlaylistItem>().map { it.playlist.id }
          var successCount = 0
          var failCount = 0
          if (videos.isNotEmpty()) {
            val (videoSuccess, videoFail) = viewModel.deleteVideosFromHistory(videos, deleteFiles)
            successCount += videoSuccess
            failCount += videoFail
          }
          if (playlistIds.isNotEmpty()) {
            val (playlistSuccess, playlistFail) = viewModel.deletePlaylistsFromHistory(playlistIds)
            successCount += playlistSuccess
            failCount += playlistFail
          }
          Pair(successCount, failCount)
        },
        onRenameItem = null,
        onOperationComplete = { },
      )

    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }

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

  Scaffold(
        topBar = {
          BrowserTopBar(
            title = "Recently Played",
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = recentItems.size,
            onBackClick = null,
            onCancelSelection = { selectionManager.clear() },
            onSortClick = null,
            onSettingsClick = {
              backStack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
            },
            isSingleSelection = selectionManager.isSingleSelection,
            onInfoClick = null,
            onShareClick = null,
            onPlayClick = null,
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll = { selectionManager.clear() },
            onDeleteClick = { showDeleteSheet.value = true },
          )
        },
    ) { padding ->
      when {
        !enableRecentlyPlayed -> {
          EmptyState(
            icon = Icons.Filled.History,
            title = "History is disabled",
            message = "Enable it in Advanced Settings to track your playback history",
            modifier = Modifier.fillMaxSize().padding(padding),
          )
        }

        isLoading && recentItems.isEmpty() -> {
          Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            LoadingIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary)
          }
        }

        recentItems.isEmpty() -> {
          EmptyState(
            icon = Icons.Filled.History,
            title = "No recent items",
            message = "Videos and playlists you play will appear here",
            modifier = Modifier.fillMaxSize().padding(padding),
          )
        }

        else -> {
          RecentItemsContent(
            recentItems = recentItems,
            selectionManager = selectionManager,
            onVideoClick = { video -> MediaUtils.playFile(video, context, "recently_played") },
            onPlaylistClick = { playlistItem -> backStack.add(PlaylistDetailScreen(playlistItem.playlist.id)) },
            videoCardSettings = videoCardSettings,
            folderCardSettings = folderCardSettings,
            modifier = Modifier.padding(padding),
            isInSelectionMode = selectionManager.isInSelectionMode,
            listState = listState,
          )
        }
      }

      DeleteConfirmationSheet(
          isOpen = showDeleteSheet.value && selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          onDismiss = { showDeleteSheet.value = false },
          onConfirm = { deleteFiles ->
              selectionManager.deleteSelected(deleteFiles)
              showDeleteSheet.value = false
          }
      )
      
    }
  }
}

@Composable
private fun RecentItemsContent(
  recentItems: List<RecentlyPlayedItem>,
  selectionManager: SelectionManager<RecentlyPlayedItem, String>,
  onVideoClick: (Video) -> Unit,
  onPlaylistClick: (RecentlyPlayedItem.PlaylistItem) -> Unit,
  videoCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.VideoCardSettings,
  folderCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings,
  modifier: Modifier = Modifier,
  isInSelectionMode: Boolean = false,
  listState: LazyListState,
) {
  val gesturePreferences = koinInject<GesturePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val density = LocalDensity.current
  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()

  val thumbWidthDp = 140.dp
  val aspect = 16f / 9f
  val thumbWidthPx = with(density) { thumbWidthDp.roundToPx() }
  val thumbHeightPx = (thumbWidthPx / aspect).toInt()

  val recentVideos = remember(recentItems) {
    recentItems.filterIsInstance<RecentlyPlayedItem.VideoItem>().map { it.video }
  }

  LaunchedEffect(recentVideos.size, showVideoThumbnails, thumbWidthPx, thumbHeightPx) {
    if (showVideoThumbnails && recentVideos.isNotEmpty()) {
      thumbnailRepository.startFolderThumbnailGeneration(
        folderId = "recently_played",
        videos = recentVideos,
        widthPx = thumbWidthPx,
        heightPx = thumbHeightPx,
      )
    }
  }

  val hasEnoughItems = recentItems.size > 20
  val scrollbarAlpha by animateFloatAsState(
    targetValue = if (!hasEnoughItems) 0f else 1f,
    animationSpec = tween(durationMillis = 200),
    label = "scrollbarAlpha",
  )

  PullRefreshBox(
    isRefreshing = remember { mutableStateOf(false) },
    onRefresh = { },
    listState = listState,
    modifier = modifier.fillMaxSize(),
  ) {
    val navigationBarHeight = LocalNavigationBarHeight.current
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
          contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(
            count = recentItems.size,
            key = { index ->
              when (val item = recentItems[index]) {
                is RecentlyPlayedItem.VideoItem -> "video_${item.video.id}_${item.timestamp}"
                is RecentlyPlayedItem.PlaylistItem -> "playlist_${item.playlist.id}_${item.timestamp}"
              }
            },
          ) { index ->
            when (val item = recentItems[index]) {
              is RecentlyPlayedItem.VideoItem -> {
                // Keyed derivedStateOf isolates this row's recomposition from
                // selection changes on other rows sharing the SelectionManager state.
                val isSelected by remember(selectionManager, item.video.id) {
                  derivedStateOf { selectionManager.isSelected(item) }
                }
                VideoCard(
                  video = item.video,
                  settings = videoCardSettings,
                  isRecentlyPlayed = true,
                  progressPercentage = item.progressPercentage,
                  isWatched = item.isWatched,
                  isSelected = isSelected,
                  onClick = {
                    if (selectionManager.isInSelectionMode) selectionManager.toggle(item)
                    else onVideoClick(item.video)
                  },
                  onLongClick = { selectionManager.toggle(item) },
                  onThumbClick = {
                    if (tapThumbnailToSelect || selectionManager.isInSelectionMode) selectionManager.toggle(item)
                    else onVideoClick(item.video)
                  },
                  showSubtitleIndicator = showSubtitleIndicator,
                )
              }

              is RecentlyPlayedItem.PlaylistItem -> {
                val folderModel = VideoFolder(
                  bucketId = item.playlist.id.toString(),
                  name = item.playlist.name,
                  path = "",
                  videoCount = item.videoCount,
                  totalSize = 0,
                  totalDuration = 0,
                  lastModified = item.playlist.updatedAt / 1000,
                )
                val isSelected by remember(selectionManager, item.playlist.id) {
                  derivedStateOf { selectionManager.isSelected(item) }
                }
                FolderCard(
                   folder = folderModel,
                   settings = folderCardSettings,
                   isSelected = isSelected,
                   onClick = {
                    if (selectionManager.isInSelectionMode) selectionManager.toggle(item)
                    else onPlaylistClick(item)
                  },
                  onLongClick = { selectionManager.toggle(item) },
                  onThumbClick = {
                    if (tapThumbnailToSelect || selectionManager.isInSelectionMode) selectionManager.toggle(item)
                    else onPlaylistClick(item)
                  },
                  customIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                )
              }
            }
          }
        }
      }
    }
  }
}
