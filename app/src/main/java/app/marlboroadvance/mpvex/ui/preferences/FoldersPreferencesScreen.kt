package app.marlboroadvance.mpvex.ui.preferences

import android.app.Application
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.FoldersPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionState
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object FoldersPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<FoldersPreferences>()
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val blacklistedFolders by preferences.blacklistedFolders.collectAsState()
    var availableFolders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var isLoading by remember { mutableStateOf(false) }
    var selectionState by remember { mutableStateOf(SelectionState<String>()) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val blacklistedFoldersList = remember(blacklistedFolders) { blacklistedFolders.toList() }

    Scaffold(
      topBar = {
        BrowserTopBar(
            title = stringResource(R.string.pref_folders_title),
            isInSelectionMode = selectionState.isInSelectionMode,
            selectedCount = selectionState.selectedCount,
            totalCount = blacklistedFoldersList.size,
            onCancelSelection = { selectionState = selectionState.clear() },
            onBackClick = backstack::removeLastOrNull,
            onDeleteClick = {
              val updated = blacklistedFolders.toMutableSet().apply {
                removeAll(selectionState.selectedIds)
              }
              preferences.blacklistedFolders.set(updated)
              selectionState = selectionState.clear()
            },
            onSelectAll = { selectionState = selectionState.selectAll(blacklistedFoldersList) },
            onInvertSelection = { selectionState = selectionState.invertSelection(blacklistedFoldersList) },
            onDeselectAll = { selectionState = selectionState.clear() },
            useRemoveIcon = true,
            additionalActions = {
                if (!selectionState.isInSelectionMode && blacklistedFolders.isNotEmpty()) {
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(
                            Icons.Rounded.Restore,
                            contentDescription = stringResource(R.string.pref_folders_clear_all),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        )
      },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(16.dp),
      ) {
        if (!selectionState.isInSelectionMode) {
          Text(
            text = stringResource(R.string.pref_folders_summary),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          Spacer(modifier = Modifier.height(24.dp))
        }

        if (blacklistedFolders.isEmpty()) {
          EmptyState(
            icon = Icons.Rounded.FolderOff,
            title = stringResource(R.string.pref_folders_empty_title),
            message = stringResource(R.string.pref_folders_empty_message),
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(blacklistedFoldersList) { folderPath ->
              BlacklistedFolderItem(
                folderPath = folderPath,
                isSelected = selectionState.isSelected(folderPath),
                isInSelectionMode = selectionState.isInSelectionMode,
                onRemove = {
                  val updated = blacklistedFolders.toMutableSet().apply { remove(folderPath) }
                  preferences.blacklistedFolders.set(updated)
                },
                onLongClick = {
                  selectionState = selectionState.toggle(folderPath)
                },
                onClick = {
                  if (selectionState.isInSelectionMode) {
                    selectionState = selectionState.toggle(folderPath)
                  }
                },
              )
            }
          }
        }

        if (!selectionState.isInSelectionMode) {
          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .height(64.dp),
            onClick = {
                showAddSheet = true
                isLoading = true
                coroutineScope.launch(Dispatchers.IO) {
                  try {
                    availableFolders = scanAllVideoFolders(context.applicationContext as Application)
                  } finally {
                    isLoading = false
                  }
                }
            },
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Spacer(modifier = Modifier.padding(8.dp))
              Text(
                text = stringResource(R.string.pref_folders_add_folder),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }
    }

    if (showAddSheet) {
      AddFolderBottomSheet(
        folders = availableFolders,
        blacklistedFolders = blacklistedFolders,
        isLoading = isLoading,
        onDismiss = {
          coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) showAddSheet = false
          }
        },
        onAddFolders = { folderPaths ->
          val updated = blacklistedFolders.toMutableSet().apply { addAll(folderPaths) }
          preferences.blacklistedFolders.set(updated)
        },
        sheetState = sheetState,
      )
    }

    if (showClearAllDialog) {
      AlertDialog(
        onDismissRequest = { showClearAllDialog = false },
        title = { Text(stringResource(R.string.pref_folders_clear_all_confirm_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.pref_folders_clear_all_confirm_message), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
          TextButton(
            onClick = {
              preferences.blacklistedFolders.set(emptySet())
              showClearAllDialog = false
            },
          ) {
            Text(stringResource(R.string.generic_confirm), fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearAllDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
        shape = MaterialTheme.shapes.extraLarge
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlacklistedFolderItem(
  folderPath: String,
  isSelected: Boolean,
  isInSelectionMode: Boolean,
  onRemove: () -> Unit,
  onLongClick: () -> Unit,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
    label = "folder_item_scale"
  )

  Surface(
    modifier = Modifier
        .fillMaxWidth()
        .scale(scale),
    shape = MaterialTheme.shapes.large,
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = if (isSelected) 4.dp else 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
          onLongClick = onLongClick,
        )
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isInSelectionMode) {
          Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            modifier = Modifier.padding(end = 12.dp),
          )
        }
        Column {
          Text(
            text = folderPath.substringAfterLast('/'),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = folderPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      if (!isInSelectionMode) {
        IconButton(onClick = onRemove) {
          Icon(
            imageVector = Icons.Rounded.RemoveCircle,
            contentDescription = stringResource(R.string.delete),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFolderBottomSheet(
  folders: List<VideoFolder>,
  blacklistedFolders: Set<String>,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onAddFolders: (Set<String>) -> Unit,
  sheetState: androidx.compose.material3.SheetState,
) {
  var selectionState by remember { mutableStateOf(SelectionState<String>()) }
  var showDropdown by remember { mutableStateOf(false) }

  val availableFolders = remember(folders, blacklistedFolders) {
    folders.filter { it.path !in blacklistedFolders }
  }

  val availableFolderPaths = remember(availableFolders) {
    availableFolders.map { it.path }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = MaterialTheme.shapes.extraLarge,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surfaceContainer
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable(enabled = !isLoading && availableFolders.isNotEmpty()) {
            showDropdown = true
          },
        ) {
          Text(
            text = if (selectionState.isInSelectionMode) {
              stringResource(R.string.selected_items, selectionState.selectedCount, availableFolders.size)
            } else {
              stringResource(R.string.pref_folders_select_folders)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (!isLoading && availableFolders.isNotEmpty()) {
            Icon(
              Icons.Rounded.ArrowDropDown,
              contentDescription = stringResource(R.string.selection_options),
              modifier = Modifier.size(28.dp),
              tint = MaterialTheme.colorScheme.primary
            )
          }

          DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
          ) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.select_all)) },
              onClick = {
                selectionState = selectionState.selectAll(availableFolderPaths)
                showDropdown = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.invert_selection)) },
              onClick = {
                selectionState = selectionState.invertSelection(availableFolderPaths)
                showDropdown = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.deselect_all)) },
              onClick = {
                selectionState = selectionState.clear()
                showDropdown = false
              },
            )
          }
        }

        IconButton(
          onClick = {
            onAddFolders(selectionState.selectedIds)
            onDismiss()
          },
          enabled = selectionState.isInSelectionMode && !isLoading,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = stringResource(R.string.generic_confirm),
            tint = if (selectionState.isInSelectionMode && !isLoading) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(32.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(stringResource(R.string.pref_folders_loading), style = MaterialTheme.typography.bodyLarge)
        }
      } else if (availableFolders.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(stringResource(R.string.pref_folders_no_folders), style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(availableFolders) { folder ->
            Surface(
                onClick = { selectionState = selectionState.toggle(folder.path) },
                shape = MaterialTheme.shapes.large,
                color = if (selectionState.isSelected(folder.path)) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                    checked = selectionState.isSelected(folder.path),
                    onCheckedChange = {
                      selectionState = selectionState.toggle(folder.path)
                    },
                  )
                  Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                      text = folder.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = folder.path,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
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

private suspend fun scanAllVideoFolders(context: Application): List<VideoFolder> {
  return app.marlboroadvance.mpvex.repository.MediaFileRepository
    .getAllVideoFoldersFast(
      context = context
    )
}
