package app.marlboroadvance.mpvex.ui.browser.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.cards.M3UVideoCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Playlist detail screen showing videos in a playlist.
 *
 * **M3U Playlist Behavior:**
 * M3U playlists (streaming URLs) are handled differently to prevent ANR issues:
 * - Each stream is played individually (no playlist navigation in PlayerActivity)
 * - No next/previous buttons - each stream URL is opened standalone
 * - This prevents loading thousands of URLs into memory at once
 * - Users can manually select and play different streams from the list
 *
 * **Regular Playlist Behavior:**
 * Local file playlists support full playlist navigation:
 * - Next/previous buttons available during playback
 * - Playlist continuation and shuffle modes
 * - Full playlist loaded into PlayerActivity
 */
@Serializable
data class PlaylistDetailScreen(val playlistId: Int) : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel
    val viewModel: PlaylistDetailViewModel =
      viewModel(
        key = "PlaylistDetailViewModel_$playlistId",
        factory = PlaylistDetailViewModel.factory(
          context.applicationContext as android.app.Application,
          playlistId,
        ),
      )

    val playlist by viewModel.playlist.collectAsState()
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<app.marlboroadvance.mpvex.preferences.AppearancePreferences>()
    val videoItems by viewModel.videoItems.collectAsState()
    val videos = videoItems.map { it.video }
    val isLoading by viewModel.isLoading.collectAsState()
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
    val isRefreshing = remember { mutableStateOf(false) }

    // Selection manager
    val selectionManager = rememberSelectionManager(
      items = videoItems,
      getId = { it.playlistItem.id },
      onDeleteItems = { itemsToDelete, _ ->
        viewModel.removePlaylistItems(itemsToDelete.map { it.playlistItem })
        Pair(itemsToDelete.size, 0)
      },
      onOperationComplete = { viewModel.refresh() },
    )

    // UI State
    val listState = rememberLazyListState()
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }
    var urlDialogContent by remember { mutableStateOf("") }

    // Reorder mode state
    var isReorderMode by rememberSaveable { mutableStateOf(false) }

    // Predictive back: Intercept when in selection mode or reorder mode
    BackHandler(enabled = selectionManager.isInSelectionMode || isReorderMode) {
      when {
        isReorderMode -> isReorderMode = false
        selectionManager.isInSelectionMode -> selectionManager.clear()
      }
    }

    Scaffold(
      topBar = {
        BrowserTopBar(
          title = playlist?.name ?: "Playlist",
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          totalCount = videos.size,
          onBackClick = {
            when {
              isReorderMode -> isReorderMode = false
              selectionManager.isInSelectionMode -> selectionManager.clear()
              else -> backStack.removeLastOrNull()
            }
          },
          onCancelSelection = { selectionManager.clear() },
          isSingleSelection = selectionManager.isSingleSelection,
          useRemoveIcon = true, // Show remove icon instead of delete for playlist
          onInfoClick =
            if (selectionManager.isSingleSelection) {
              {
                val item = selectionManager.getSelectedItems().firstOrNull()
                if (item != null) {
                  if (playlist?.isM3uPlaylist == true) {
                    // For M3U playlists, show URL dialog
                    urlDialogContent = item.video.path
                    showUrlDialog = true
                    selectionManager.clear()
                  } else {
                    // For regular playlists, show MediaInfo activity
                    val intent = Intent(context, app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity::class.java)
                    intent.action = Intent.ACTION_VIEW
                    intent.data = item.video.uri
                    context.startActivity(intent)
                    selectionManager.clear()
                  }
                }
              }
            } else {
              null
            },
          onShareClick = if (playlist?.isM3uPlaylist != true) {
            // Hide share button for M3U playlists
            {
              val videosToShare = selectionManager.getSelectedItems().map { it.video }
              MediaUtils.shareVideos(context, videosToShare)
            }
          } else {
            null
          },
          onPlayClick = null, // Don't show play icon in selection mode for playlist
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
          onDeleteClick = { deleteDialogOpen.value = true },
          additionalActions = {
            when {
              // Show done button when in reorder mode
              isReorderMode -> {
                IconButton(
                  onClick = { isReorderMode = false },
                ) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Done reordering",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              // Show reorder button and play button when not in selection mode
              !selectionManager.isInSelectionMode && videos.isNotEmpty() -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  // Reorder button (hide for M3U playlists)
                  if (playlist?.isM3uPlaylist != true) {
                    IconButton(
                      onClick = { isReorderMode = true },
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Reorder playlist",
                        tint = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                  }

                  // Play button
                  Button(
                    onClick = {
                      if (playlist?.isM3uPlaylist == true) {
                        // M3U playlists: Play only the first/most recent stream (no playlist navigation)
                        val mostRecentlyPlayedItem = videoItems
                          .filter { it.playlistItem.lastPlayedAt > 0 }
                          .maxByOrNull { it.playlistItem.lastPlayedAt }

                        val itemToPlay = mostRecentlyPlayedItem ?: videoItems.firstOrNull()

                        if (itemToPlay != null) {
                          coroutineScope.launch {
                            viewModel.updatePlayHistory(itemToPlay.video.path)
                          }

                          // Play single stream URL without playlist
                          MediaUtils.playFile(itemToPlay.video, context, "m3u_playlist")
                        }
                      } else {
                        // Regular playlists: Start playlist navigation
                        if (videos.isNotEmpty()) {
                          val firstVideo = videos.first()
                          val intent = Intent(Intent.ACTION_VIEW, firstVideo.uri)
                          intent.setClass(context, PlayerActivity::class.java)
                          intent.putExtra("internal_launch", true)
                          intent.putParcelableArrayListExtra("playlist", ArrayList(videos.map { it.uri }))
                          intent.putExtra("playlist_index", 0)
                          intent.putExtra("launch_source", "playlist")
                          // Pass pre-resolved metadata to avoid ContentResolver queries
                          intent.putExtra("title", firstVideo.displayName)
                          intent.putExtra("absolute_path", firstVideo.path)
                          intent.putExtra("video_id", firstVideo.id)
                          intent.putExtra("date_modified", firstVideo.dateModified)
                          intent.putExtra("size", firstVideo.size)
                          context.startActivity(intent)
                        }
                      }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                  ) {
                    Icon(
                      imageVector = Icons.Filled.PlayArrow,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Play All",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          },
        )
      },
      floatingActionButton = { },
    ) { padding ->
      val pullToRefreshEnabled =
        !selectionManager.isInSelectionMode && !isReorderMode

      PullRefreshBox(
        isRefreshing = isRefreshing,
        enabled = pullToRefreshEnabled,
        listState = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        onRefresh = {
          val isM3uPlaylist = playlist?.isM3uPlaylist == true
          if (isM3uPlaylist) {
            val result = viewModel.refreshM3UPlaylist()
            result
              .onSuccess {
                Toast.makeText(context, "Playlist refreshed successfully", Toast.LENGTH_SHORT).show()
              }
              .onFailure { error ->
                Toast.makeText(context, "Failed to refresh: ${error.message}", Toast.LENGTH_LONG).show()
              }
          } else {
            viewModel.refreshNow()
          }
        },
      ) {
        PlaylistVideoListContent(
          videoItems = videoItems,
          isLoading = isLoading && videoItems.isEmpty(),
          selectionManager = selectionManager,
          videoCardSettings = videoCardSettings,
          isM3uPlaylist = playlist?.isM3uPlaylist == true,
          isReorderMode = isReorderMode,
          onReorder = { fromIndex, toIndex ->
            coroutineScope.launch {
              viewModel.reorderPlaylistItems(fromIndex, toIndex)
            }
          },
          onVideoItemClick = { item ->
            if (selectionManager.isInSelectionMode) {
              selectionManager.toggle(item)
            } else {
              coroutineScope.launch {
                viewModel.updatePlayHistory(item.video.path)
              }

              val startIndex = videoItems.indexOfFirst { it.playlistItem.id == item.playlistItem.id }
              if (startIndex >= 0) {
                if (videos.size == 1) {
                  MediaUtils.playFile(item.video, context, "playlist_detail")
                } else {
                  val targetVideo = videos[startIndex]
                  val intent = Intent(Intent.ACTION_VIEW, targetVideo.uri)
                  intent.setClass(context, PlayerActivity::class.java)
                  intent.putExtra("internal_launch", true)
                  intent.putExtra("playlist_index", startIndex)
                  intent.putExtra("launch_source", "playlist")
                  intent.putExtra("playlist_id", playlistId)
                  // Pass pre-resolved metadata to avoid ContentResolver queries
                  intent.putExtra("title", targetVideo.displayName)
                  intent.putExtra("absolute_path", targetVideo.path)
                  intent.putExtra("video_id", targetVideo.id)
                  intent.putExtra("date_modified", targetVideo.dateModified)
                  intent.putExtra("size", targetVideo.size)
                  context.startActivity(intent)
                }
              } else {
                MediaUtils.playFile(item.video, context, "playlist_detail")
              }
            }
          },
          onVideoItemLongClick = { item ->
            selectionManager.toggle(item)
          },
          listState = listState,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    // Dialogs
    RemoveFromPlaylistDialog(
      isOpen = deleteDialogOpen.value,
      onDismiss = { deleteDialogOpen.value = false },
      onConfirm = { selectionManager.deleteSelected() },
      itemCount = selectionManager.selectedCount,
    )

    // URL Dialog for M3U streams
    if (showUrlDialog) {
      StreamUrlDialog(
        url = urlDialogContent,
        onDismiss = { showUrlDialog = false },
        onCopy = {
          val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText("Stream URL", urlDialogContent)
          clipboardManager.setPrimaryClip(clip)
          Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
        }
      )
    }
  }
}

@Composable
private fun PlaylistVideoListContent(
  videoItems: List<PlaylistVideoItem>,
  isLoading: Boolean,
  selectionManager: SelectionManager<PlaylistVideoItem, Int>,
  videoCardSettings: app.marlboroadvance.mpvex.ui.browser.cards.VideoCardSettings,
  isReorderMode: Boolean,
  onReorder: (Int, Int) -> Unit,
  onVideoItemClick: (PlaylistVideoItem) -> Unit,
  onVideoItemLongClick: (PlaylistVideoItem) -> Unit,
  listState: LazyListState,
  modifier: Modifier = Modifier,
  isM3uPlaylist: Boolean = false,
) {
  val gesturePreferences = koinInject<GesturePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()

  // Find the most recently played video (highest lastPlayedAt timestamp)
  val mostRecentlyPlayedItem = remember(videoItems) {
    videoItems.filter { it.playlistItem.lastPlayedAt > 0 }
      .maxByOrNull { it.playlistItem.lastPlayedAt }
  }

  when {
    isLoading -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(48.dp),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    videoItems.isEmpty() -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "No videos in playlist",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Add videos to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    else -> {
      // Only show scrollbar if list has more than 20 items
      val hasEnoughItems = videoItems.size > 20

      // Animate scrollbar alpha
      val scrollbarAlpha by animateFloatAsState(
        targetValue = if (!hasEnoughItems) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbarAlpha",
      )

      // Reorderable state
      val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        if (isReorderMode) {
          onReorder(from.index, to.index)
        }
      }

      LazyColumnScrollbar(
        state = listState,
        settings = ScrollbarSettings(
          thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
          thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
        ),
        modifier = modifier.fillMaxSize(),
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
        ) {
          items(
            count = videoItems.size,
            key = { index -> videoItems[index].playlistItem.id },
          ) { index ->
            ReorderableItem(reorderableLazyListState, key = videoItems[index].playlistItem.id) {
              val item = videoItems[index]

              val progressPercentage = if (item.playlistItem.lastPosition > 0 && item.video.duration > 0) {
                item.playlistItem.lastPosition.toFloat() / item.video.duration.toFloat() * 100f
              } else null

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                // Use M3UVideoCard for streaming URLs, VideoCard for local files
                if (isM3uPlaylist) {
                  M3UVideoCard(
                    title = item.video.displayName,
                    url = item.video.path,
                    onClick = { onVideoItemClick(item) },
                    onLongClick = { onVideoItemLongClick(item) },
                    settings = videoCardSettings,
                    isSelected = selectionManager.isSelected(item),
                    isRecentlyPlayed = item.playlistItem.id == mostRecentlyPlayedItem?.playlistItem?.id,
                    modifier = Modifier.weight(1f),
                  )
                } else {
                  VideoCard(
                    video = item.video,
                    settings = videoCardSettings,
                    progressPercentage = progressPercentage,
                    isRecentlyPlayed = item.playlistItem.id == mostRecentlyPlayedItem?.playlistItem?.id,
                    isSelected = selectionManager.isSelected(item),
                    onClick = { onVideoItemClick(item) },
                    onLongClick = { onVideoItemLongClick(item) },
                    onThumbClick = if (tapThumbnailToSelect) {
                      { onVideoItemLongClick(item) }
                    } else {
                      { onVideoItemClick(item) }
                    },
                    showSubtitleIndicator = showSubtitleIndicator,
                    modifier = Modifier.weight(1f),
                  )
                }

                // Drag handle - only show when in reorder mode, positioned at the end
                if (isReorderMode) {
                  IconButton(
                    onClick = { },
                    modifier = Modifier
                      .size(48.dp)
                      .draggableHandle(),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DragHandle,
                      contentDescription = "Drag to reorder",
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StreamUrlDialog(
  url: String,
  onDismiss: () -> Unit,
  onCopy: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Stream URL") },
    text = {
      Text(
        text = url,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          onCopy()
          onDismiss()
        }
      ) {
        Icon(
          imageVector = Icons.Filled.ContentCopy,
          contentDescription = null,
          modifier = Modifier.padding(end = 4.dp).size(18.dp)
        )
        Text("Copy")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

@Composable
private fun RemoveFromPlaylistDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  itemCount: Int,
) {
  if (!isOpen) return

  val itemText = if (itemCount == 1) "video" else "videos"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Remove $itemCount $itemText from playlist?",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ),
          shape = MaterialTheme.shapes.extraLarge,
        ) {
          Text(
            text = "The selected $itemText will be removed from this playlist. The original ${if (itemCount == 1) "file" else "files"} will not be deleted.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm()
          onDismiss()
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
          ),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(
          text = "Remove from Playlist",
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text("Cancel", fontWeight = FontWeight.Medium)
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}
