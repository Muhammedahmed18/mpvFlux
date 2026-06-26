package app.marlboroadvance.mpvex.ui.browser.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Serializable
data class PlaylistDetailScreen(val playlistId: Int) : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "PlaylistDetailViewModel_$playlistId",
        factory = PlaylistDetailViewModel.factory(context.applicationContext as android.app.Application, playlistId),
      )

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val browserPreferences = koinInject<BrowserPreferences>()
    val appearancePreferences = koinInject<app.marlboroadvance.mpvex.preferences.AppearancePreferences>()
    val videoItems by viewModel.videoItems.collectAsStateWithLifecycle()
    val videos = videoItems.map { it.video }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()

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

    val selectionManager = rememberSelectionManager(
      items = videoItems,
      getId = { it.playlistItem.id },
      onDeleteItems = { itemsToDelete, _ ->
        viewModel.removePlaylistItems(itemsToDelete.map { it.playlistItem })
        Pair(itemsToDelete.size, 0)
      },
      onOperationComplete = { viewModel.refresh() },
    )

    val listState = rememberLazyListState()
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }
    var urlDialogContent by remember { mutableStateOf("") }
    var isReorderMode by rememberSaveable { mutableStateOf(false) }

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
          useRemoveIcon = true,
          onInfoClick = if (selectionManager.isSingleSelection) {
              {
                val item = selectionManager.getSelectedItems().firstOrNull()
                if (item != null) {
                  if (playlist?.isM3uPlaylist == true) {
                    urlDialogContent = item.video.path
                    showUrlDialog = true
                    selectionManager.clear()
                  } else {
                    val intent = Intent(context, app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity::class.java)
                    intent.action = Intent.ACTION_VIEW
                    intent.data = item.video.uri
                    context.startActivity(intent)
                    selectionManager.clear()
                  }
                }
              }
            } else null,
          onShareClick = if (playlist?.isM3uPlaylist != true) {
            {
              val videosToShare = selectionManager.getSelectedItems().map { it.video }
              MediaUtils.shareVideos(context, videosToShare)
            }
          } else null,
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
          onDeleteClick = { deleteDialogOpen.value = true },
          additionalActions = {
            when {
              isReorderMode -> {
                IconButton(onClick = { isReorderMode = false }) {
                  Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
              !selectionManager.isInSelectionMode && videos.isNotEmpty() -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (playlist?.isM3uPlaylist != true) {
                    IconButton(onClick = { isReorderMode = true }) {
                      Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(8.dp))
                  }

                  Button(
                    onClick = {
                      if (playlist?.isM3uPlaylist == true) {
                        val mostRecentlyPlayedItem = videoItems.filter { it.playlistItem.lastPlayedAt > 0 }.maxByOrNull { it.playlistItem.lastPlayedAt }
                        val itemToPlay = mostRecentlyPlayedItem ?: videoItems.firstOrNull()
                        if (itemToPlay != null) {
                          coroutineScope.launch { viewModel.updatePlayHistory(itemToPlay.video.path) }
                          MediaUtils.playFile(itemToPlay.video, context, "m3u_playlist")
                        }
                      } else if (videos.isNotEmpty()) {
                          val firstVideo = videos.first()
                          val intent = Intent(Intent.ACTION_VIEW, firstVideo.uri).apply {
                              setClass(context, PlayerActivity::class.java)
                              putExtra("internal_launch", true)
                              putParcelableArrayListExtra("playlist", ArrayList(videos.map { it.uri }))
                              putExtra("playlist_index", 0)
                              putExtra("launch_source", "playlist")
                              putExtra("title", firstVideo.displayName)
                              putExtra("absolute_path", firstVideo.path)
                              putExtra("video_id", firstVideo.id)
                              putExtra("date_modified", firstVideo.dateModified)
                              putExtra("size", firstVideo.size)
                          }
                          context.startActivity(intent)
                      }
                    },
                    shape = MaterialTheme.shapes.medium
                  ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          },
        )
      }
    ) { padding ->
      PullRefreshBox(
        isRefreshing = remember { mutableStateOf(false) },
        enabled = !selectionManager.isInSelectionMode && !isReorderMode,
        listState = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        onRefresh = { viewModel.refreshNow() },
      ) {
        PlaylistVideoListContent(
          videoItems = videoItems,
          isLoading = isLoading && videoItems.isEmpty(),
          selectionManager = selectionManager,
          videoCardSettings = videoCardSettings,
          isM3uPlaylist = playlist?.isM3uPlaylist == true,
          isReorderMode = isReorderMode,
          onReorder = { from, to -> coroutineScope.launch { viewModel.reorderPlaylistItems(from, to) } },
          onVideoItemClick = { item ->
            if (selectionManager.isInSelectionMode) selectionManager.toggle(item)
            else {
              coroutineScope.launch { viewModel.updatePlayHistory(item.video.path) }
              val startIndex = videoItems.indexOfFirst { it.playlistItem.id == item.playlistItem.id }
              if (startIndex >= 0) {
                if (videos.size == 1) MediaUtils.playFile(item.video, context, "playlist_detail")
                else {
                  val targetVideo = videos[startIndex]
                  val intent = Intent(Intent.ACTION_VIEW, targetVideo.uri).apply {
                      setClass(context, PlayerActivity::class.java)
                      putExtra("internal_launch", true)
                      putExtra("playlist_index", startIndex)
                      putExtra("launch_source", "playlist")
                      putExtra("playlist_id", playlistId)
                      putExtra("title", targetVideo.displayName)
                      putExtra("absolute_path", targetVideo.path)
                      putExtra("video_id", targetVideo.id)
                      putExtra("date_modified", targetVideo.dateModified)
                      putExtra("size", targetVideo.size)
                  }
                  context.startActivity(intent)
                }
              } else MediaUtils.playFile(item.video, context, "playlist_detail")
            }
          },
          onVideoItemLongClick = { selectionManager.toggle(it) },
          listState = listState,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    if (deleteDialogOpen.value) {
        AlertDialog(
            onDismissRequest = { deleteDialogOpen.value = false },
            title = { Text("Remove from playlist?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = { Text("Selected items will be removed from this playlist. Files will not be deleted.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
              TextButton(onClick = { selectionManager.deleteSelected(); deleteDialogOpen.value = false }) {
                Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = { TextButton(onClick = { deleteDialogOpen.value = false }) { Text("Cancel") } },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    if (showUrlDialog) {
      StreamUrlDialog(
        url = urlDialogContent,
        onDismiss = { showUrlDialog = false },
        onCopy = {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          cm.setPrimaryClip(ClipData.newPlainText("URL", urlDialogContent))
          Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
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

  val mostRecentlyPlayedItem = remember(videoItems) {
    videoItems.filter { it.playlistItem.lastPlayedAt > 0 }.maxByOrNull { it.playlistItem.lastPlayedAt }
  }

  if (isLoading) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      LoadingIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary)
    }
  } else if (videoItems.isEmpty()) {
    EmptyState(icon = Icons.AutoMirrored.Outlined.PlaylistAdd, title = "Empty Playlist", message = "Add some videos to this playlist", modifier = modifier.fillMaxSize())
  } else {
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        if (isReorderMode) onReorder(from.index, to.index)
    }

    LazyColumnScrollbar(
      state = listState,
      settings = ScrollbarSettings(
          thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
          thumbSelectedColor = MaterialTheme.colorScheme.primary,
      ),
      modifier = modifier.fillMaxSize(),
    ) {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(count = videoItems.size, key = { videoItems[it].playlistItem.id }) { index ->
          ReorderableItem(reorderableState, key = videoItems[index].playlistItem.id) { isDragging ->
            val item = videoItems[index]
            val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")

            Surface(
                tonalElevation = elevation.dp,
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent
            ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val isSelected by remember(selectionManager, item.playlistItem.id) {
                    derivedStateOf { selectionManager.isSelected(item) }
                  }
                  VideoCard(
                    video = item.video,
                    settings = videoCardSettings,
                    progressPercentage = if (item.playlistItem.lastPosition > 0 && item.video.duration > 0) item.playlistItem.lastPosition.toFloat() / item.video.duration.toFloat() else null,
                    isRecentlyPlayed = item.playlistItem.id == mostRecentlyPlayedItem?.playlistItem?.id,
                    isSelected = isSelected,
                    onClick = { onVideoItemClick(item) },
                    onLongClick = { onVideoItemLongClick(item) },
                    onThumbClick = { if (tapThumbnailToSelect || selectionManager.isInSelectionMode) onVideoItemLongClick(item) else onVideoItemClick(item) },
                    showSubtitleIndicator = showSubtitleIndicator,
                    modifier = Modifier.weight(1f),
                  )

                  if (isReorderMode) {
                    IconButton(onClick = { }, modifier = Modifier.size(48.dp).draggableHandle()) {
                      Icon(Icons.Filled.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
private fun StreamUrlDialog(url: String, onDismiss: () -> Unit, onCopy: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Stream URL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
    text = { Text(text = url, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()) },
    confirmButton = {
      TextButton(onClick = { onCopy(); onDismiss() }) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Copy", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    shape = MaterialTheme.shapes.extraLarge
  )
}
