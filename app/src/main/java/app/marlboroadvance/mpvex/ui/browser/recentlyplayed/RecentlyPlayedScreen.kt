package app.marlboroadvance.mpvex.ui.browser.recentlyplayed

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.fab.FabScrollHelper
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistDetailScreen
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
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

    val recentItems by viewModel.recentItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteFilesCheckbox = rememberSaveable { mutableStateOf(false) }
    val advancedPreferences = koinInject<AdvancedPreferences>()
    val appearancePreferences = koinInject<app.marlboroadvance.mpvex.preferences.AppearancePreferences>()
    val enableRecentlyPlayed by advancedPreferences.enableRecentlyPlayed.collectAsState()

    val showLinkDialog = remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Selection manager for all items (videos and playlists)
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

          // Delete videos from history
          if (videos.isNotEmpty()) {
            val (videoSuccess, videoFail) = viewModel.deleteVideosFromHistory(videos, deleteFiles)
            successCount += videoSuccess
            failCount += videoFail
          }

          // Delete playlist items from history
          if (playlistIds.isNotEmpty()) {
            val (playlistSuccess, playlistFail) = viewModel.deletePlaylistsFromHistory(playlistIds)
            successCount += playlistSuccess
            failCount += playlistFail
          }

          Pair(successCount, failCount)
        },
        onRenameItem = null, // Cannot rename from history screen
        onOperationComplete = { },
      )

    // Handle back button during selection mode
    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }
    
    // File picker for opening external files
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

    // Track scroll for FAB visibility - create states here to pass to content
    val listState = remember { LazyListState() }
    val browserPreferences = koinInject<BrowserPreferences>()
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

  Scaffold(
        topBar = {
          BrowserTopBar(
            title = "Recently Played",
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = recentItems.size,
            onBackClick = null, // No back button for recently played screen
            onCancelSelection = { selectionManager.clear() },
            onSortClick = null, // No sorting in recently played
            onSettingsClick = {
              backStack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
            },
            isSingleSelection = selectionManager.isSingleSelection,
            onInfoClick = null, // No info in recently played
            onShareClick = null,
            onPlayClick = null,
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll = { selectionManager.clear() },
            onDeleteClick = { deleteDialogOpen.value = true },
          )
        },
    ) { padding ->
      when {
        !enableRecentlyPlayed -> {
          EmptyState(
            icon = Icons.Filled.History,
            title = "Recently Played is disabled",
            message = "Enable it in Advanced Settings to track your playback history",
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
          )
        }

        isLoading && recentItems.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        recentItems.isEmpty() -> {
          EmptyState(
            icon = Icons.Filled.History,
            title = "No recently played videos",
            message = "Videos you play will appear here",
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
          )
        }

        else -> {
          RecentItemsContent(
            recentItems = recentItems,
            selectionManager = selectionManager,
            onVideoClick = { video ->
              // Always play individual videos without creating a playlist
              // regardless of playlist mode setting
              MediaUtils.playFile(video, context, "recently_played")
            },
            onPlaylistClick = { playlistItem ->
              // Navigate to playlist detail screen
              backStack.add(PlaylistDetailScreen(playlistItem.playlist.id))
            },
            videoCardSettings = videoCardSettings,
            folderCardSettings = folderCardSettings,
            modifier = Modifier.padding(padding),
            isInSelectionMode = selectionManager.isInSelectionMode,
            listState = listState,
          )
        }
      }

      // Delete confirmation dialog
      if (deleteDialogOpen.value && selectionManager.isInSelectionMode) {
        // Remove selected items from history
        val itemCount = selectionManager.selectedCount
        val itemText = if (itemCount == 1) "item" else "items"
        val deleteFiles = deleteFilesCheckbox.value

        val title = if (deleteFiles) {
          "Delete $itemCount $itemText?"
        } else {
          "Remove $itemCount $itemText from history?"
        }

        val subtitle = buildString {
          if (deleteFiles) {
            append("This will permanently delete the original video file(s) from your device storage.\n\n")
            append("This action cannot be undone.")
          } else {
            append("This will remove the selected $itemText from your recently played list. ")
            append("The original video files will not be deleted.")
          }
        }

        ConfirmDialog(
          title = title,
          subtitle = subtitle,
          customContent = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = deleteFilesCheckbox.value,
                onCheckedChange = {
                  deleteFilesCheckbox.value = it
                },
              )
              Text(
                text = "Also delete original file(s)",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          },
          onConfirm = {
            selectionManager.deleteSelected(deleteFilesCheckbox.value)
            deleteDialogOpen.value = false
            deleteFilesCheckbox.value = false
          },
          onCancel = {
            deleteDialogOpen.value = false
            deleteFilesCheckbox.value = false
          },
        )
      }
      
      // Link dialog
      PlayLinkSheet(
        isOpen = showLinkDialog.value,
        onDismiss = { showLinkDialog.value = false },
        onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
      )
    }
  }
}

@Composable
private fun RecentItemsContent(
  recentItems: List<RecentlyPlayedItem>,
  selectionManager: SelectionManager<RecentlyPlayedItem, String>,
  onVideoClick: (Video) -> Unit,
  onPlaylistClick: suspend (RecentlyPlayedItem.PlaylistItem) -> Unit,
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

  val coroutineScope = rememberCoroutineScope()
  val isRefreshing = remember { mutableStateOf(false) }

  val thumbWidthDp = 160.dp
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

  val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (!hasEnoughItems) 0f else 1f,
    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
    label = "scrollbarAlpha",
  )

  PullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { },
    listState = listState,
    modifier = modifier.fillMaxSize(),
  ) {
    val navigationBarHeight = LocalNavigationBarHeight.current
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
            bottom = if (isInSelectionMode) 88.dp else 16.dp
          ),
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
                VideoCard(
                  video = item.video,
                  settings = videoCardSettings,
                  isRecentlyPlayed = true,
                  progressPercentage = item.progressPercentage,
                  isWatched = item.isWatched,
                  isSelected = selectionManager.isSelected(item),
                  onClick = {
                    if (selectionManager.isInSelectionMode) {
                      selectionManager.toggle(item)
                    } else {
                      onVideoClick(item.video)
                    }
                  },
                  onLongClick = { selectionManager.toggle(item) },
                  onThumbClick = if (tapThumbnailToSelect) {
                    { selectionManager.toggle(item) }
                  } else {
                      {
                        if (selectionManager.isInSelectionMode) {
                          selectionManager.toggle(item)
                        } else {
                          onVideoClick(item.video)
                        }
                      }
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
                FolderCard(
                   folder = folderModel,
                   settings = folderCardSettings,
                   isSelected = selectionManager.isSelected(item),
                  onClick = {
                    if (selectionManager.isInSelectionMode) {
                      selectionManager.toggle(item)
                    } else {
                      coroutineScope.launch {
                        onPlaylistClick(item)
                      }
                    }
                  },
                  onLongClick = { selectionManager.toggle(item) },
                  onThumbClick = {
                    if (tapThumbnailToSelect) {
                      selectionManager.toggle(item)
                    } else {
                      if (selectionManager.isInSelectionMode) {
                        selectionManager.toggle(item)
                      } else {
                        coroutineScope.launch {
                          onPlaylistClick(item)
                        }
                      }
                    }
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
