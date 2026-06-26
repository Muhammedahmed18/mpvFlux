package app.marlboroadvance.mpvex.ui.browser.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.NavigationBarState
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCardSettings
import app.marlboroadvance.mpvex.ui.browser.cards.PlaylistCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlaylistActionSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object PlaylistScreen : Screen {
  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val appearancePreferences = koinInject<AppearancePreferences>()
    val repository = koinInject<PlaylistRepository>()
    
    val viewModel: PlaylistViewModel = viewModel(
      factory = PlaylistViewModel.factory(context.applicationContext as android.app.Application)
    )

    val playlistsWithCount by viewModel.playlistsWithCount.collectAsStateWithLifecycle()
    val selectionManager = rememberSelectionManager(
      items = playlistsWithCount,
      getId = { it.playlist.id },
      onDeleteItems = { playlistsToDelete, _ -> 
        playlistsToDelete.forEach { viewModel.deletePlaylist(it.playlist) }
        Pair(playlistsToDelete.size, 0)
      },
      onRenameItem = { _, _ -> 
        Result.success(Unit)
      },
      onOperationComplete = { viewModel.refresh() }
    )

    val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
    val folderCardSettings = remember(unlimitedNameLines) {
      FolderCardSettings(
        unlimitedNameLines = unlimitedNameLines,
        showTotalVideosChip = true,
        showTotalDurationChip = false,
        showTotalSizeChip = false,
        showDateChip = true,
        showFolderPath = false
      )
    }

    var showFloatingBottomBar by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    
    val navBarHeight = LocalNavigationBarHeight.current

    LaunchedEffect(selectionManager.isInSelectionMode) {
      showFloatingBottomBar = selectionManager.isInSelectionMode
      NavigationBarState.updateBottomBarVisibility(!selectionManager.isInSelectionMode)
      NavigationBarState.updateSelectionState(
        inSelectionMode = selectionManager.isInSelectionMode,
        onlyVideos = false
      )
    }

    BackHandler(enabled = selectionManager.isInSelectionMode) { selectionManager.clear() }

    Scaffold(
      topBar = {
        BrowserTopBar(
          title = "Playlists",
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          totalCount = playlistsWithCount.size,
          onCancelSelection = { selectionManager.clear() },
          onDeleteClick = { deleteDialogOpen.value = true },
          onRenameClick = null,
          isSingleSelection = selectionManager.isSingleSelection,
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
        )
      },
      floatingActionButton = {
        if (!selectionManager.isInSelectionMode) {
          val fabScale by animateFloatAsState(
              targetValue = 1f,
              animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
              label = "fab_scale"
          )
          FloatingActionButton(
            onClick = { showActionSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(bottom = navBarHeight)
                .scale(fabScale)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Playlist", modifier = Modifier.size(28.dp))
          }
        }
      }
    ) { padding ->
      Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        if (playlistsWithCount.isEmpty()) {
          EmptyState(
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            title = "No Playlists",
            message = "Create a playlist to see it here",
            modifier = Modifier.fillMaxSize()
          )
        } else {
          val listState = rememberLazyListState()
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              start = 8.dp,
              top = 8.dp,
              end = 8.dp,
              bottom = navBarHeight + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(items = playlistsWithCount, key = { it.playlist.id }) { item ->
              val isSelected by remember(selectionManager, item.playlist.id) {
                derivedStateOf { selectionManager.isSelected(item) }
              }
              PlaylistCard(
                playlist = item.playlist,
                itemCount = item.itemCount,
                settings = folderCardSettings,
                isSelected = isSelected,
                onClick = {
                  if (selectionManager.isInSelectionMode) selectionManager.toggle(item)
                  else backstack.add(PlaylistDetailScreen(item.playlist.id))
                },
                onLongClick = { selectionManager.toggle(item) },
                onThumbClick = { 
                  if (selectionManager.isInSelectionMode) selectionManager.toggle(item)
                  else backstack.add(PlaylistDetailScreen(item.playlist.id))
                }
              )
            }
          }
        }

        AnimatedVisibility(
          visible = showFloatingBottomBar,
          enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium), initialOffsetY = { it }),
          exit = slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium), targetOffsetY = { it }),
          modifier = Modifier.align(Alignment.BottomCenter)
        ) {
          BrowserBottomBar(
            isSelectionMode = true,
            onRenameClick = { },
            onDeleteClick = { deleteDialogOpen.value = true },
            showRename = false,
            onCopyClick = {},
            onMoveClick = {},
            onAddToPlaylistClick = {},
            showCopy = false,
            showMove = false,
            showAddToPlaylist = false
          )
        }
      }

      // Gated so getSelectedItems()/map only runs while the sheet is open.
      if (deleteDialogOpen.value) {
        val selectedForDelete = selectionManager.getSelectedItems()
        DeleteConfirmationDialog(
          isOpen = true,
          onDismiss = { deleteDialogOpen.value = false },
          onConfirm = { selectionManager.deleteSelected() },
          itemType = "playlist",
          itemCount = selectedForDelete.size,
          itemNames = selectedForDelete.map { it.playlist.name }
        )
      }

      PlaylistActionSheet(
        isOpen = showActionSheet,
        onDismiss = { showActionSheet = false },
        repository = repository,
        context = context
      )
    }
  }
}
