package app.marlboroadvance.mpvex.ui.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

/**
 * M3 Expressive Browser Top Bar
 * Optimized for performance by reducing layout-phase tracking.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowserTopBar(
  title: String,
  isInSelectionMode: Boolean,
  selectedCount: Int,
  totalCount: Int,
  onCancelSelection: () -> Unit,
  modifier: Modifier = Modifier,
  onBackClick: (() -> Unit)? = null,
  onSortClick: (() -> Unit)? = null,
  onSettingsClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
  onRenameClick: (() -> Unit)? = null,
  isSingleSelection: Boolean = false,
  onInfoClick: (() -> Unit)? = null,
  onShareClick: (() -> Unit)? = null,
  onPlayClick: (() -> Unit)? = null,
  onBlacklistClick: (() -> Unit)? = null,
  onSelectAll: (() -> Unit)? = null,
  onInvertSelection: (() -> Unit)? = null,
  onDeselectAll: (() -> Unit)? = null,
  additionalActions: @Composable RowScope.() -> Unit = { },
  onTitleLongPress: (() -> Unit)? = null,
  useRemoveIcon: Boolean = false,
  onAddToPlaylistClick: (() -> Unit)? = null,
  subtitle: String? = null,
  onMarkAsClick: (() -> Unit)? = null,
  onExtractAudioClick: (() -> Unit)? = null,
  extractAudioEnabled: Boolean = true,
) {
  if (isInSelectionMode) {
    SelectionTopBar(
      selectedCount = selectedCount,
      totalCount = totalCount,
      onCancel = onCancelSelection,
      onDelete = onDeleteClick,
      onRename = onRenameClick,
      isSingleSelection = isSingleSelection,
      onInfo = onInfoClick,
      onShare = onShareClick,
      onPlay = onPlayClick,
      onBlacklist = onBlacklistClick,
      onSelectAll = onSelectAll,
      onInvertSelection = onInvertSelection,
      onDeselectAll = onDeselectAll,
      modifier = modifier,
      useRemoveIcon = useRemoveIcon,
      onAddToPlaylist = onAddToPlaylistClick,
      onMarkAs = onMarkAsClick,
      onExtractAudio = onExtractAudioClick,
      extractAudioEnabled = extractAudioEnabled,
    )
  } else {
    NormalTopBar(
      title = title,
      subtitle = subtitle,
      onBackClick = onBackClick,
      onSortClick = onSortClick,
      onSettingsClick = onSettingsClick,
      additionalActions = additionalActions,
      modifier = modifier,
      onTitleLongPress = onTitleLongPress,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NormalTopBar(
  title: String,
  subtitle: String?,
  onBackClick: (() -> Unit)?,
  onSortClick: (() -> Unit)?,
  onSettingsClick: (() -> Unit)?,
  additionalActions: @Composable RowScope.() -> Unit,
  modifier: Modifier = Modifier,
  onTitleLongPress: (() -> Unit)?,
) {
  val colors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
  )
  val titleContent: @Composable () -> Unit = {
    Text(
      text = title,
      // Expressive emphasized styles replace the manual ExtraBold on the base style.
      style = if (onBackClick == null) {
        MaterialTheme.typography.headlineMediumEmphasized
      } else {
        MaterialTheme.typography.titleLargeEmphasized
      },
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      // A plain tap on the title does nothing (it used to silently toggle dark mode).
      modifier = if (onTitleLongPress != null) {
        Modifier.pointerInput(onTitleLongPress) {
          detectTapGestures(onLongPress = { onTitleLongPress() })
        }
      } else {
        Modifier
      },
    )
  }
  val navigationIconContent: @Composable () -> Unit = {
      if (onBackClick != null) {
        IconButton(onClick = onBackClick) {
          Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }
  val actionsContent: @Composable RowScope.() -> Unit = {
      additionalActions()
      if (onSortClick != null) {
        IconButton(onClick = onSortClick) {
          Icon(
            painter = painterResource(R.drawable.sort_by_alpha_24px),
            contentDescription = stringResource(R.string.sort),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
      if (onSettingsClick != null) {
        IconButton(onClick = onSettingsClick) {
          // Small dot when an app update is waiting.
          app.marlboroadvance.mpvex.ui.update.SettingsIconWithUpdateDot {
            Icon(
              Icons.Rounded.Settings,
              contentDescription = stringResource(R.string.settings),
              tint = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    }

  if (subtitle.isNullOrBlank()) {
    TopAppBar(
      title = titleContent,
      navigationIcon = navigationIconContent,
      actions = actionsContent,
      colors = colors,
      modifier = modifier,
    )
  } else {
    // M3 Expressive top app bar with a subtitle line (e.g. "12 videos · 4.2 GB").
    TopAppBar(
      title = titleContent,
      subtitle = {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      navigationIcon = navigationIconContent,
      actions = actionsContent,
      colors = colors,
      modifier = modifier,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionTopBar(
  selectedCount: Int,
  totalCount: Int,
  onCancel: () -> Unit,
  onDelete: (() -> Unit)?,
  onRename: (() -> Unit)?,
  isSingleSelection: Boolean,
  onInfo: (() -> Unit)?,
  onShare: (() -> Unit)?,
  onPlay: (() -> Unit)?,
  onBlacklist: (() -> Unit)?,
  onSelectAll: (() -> Unit)?,
  onInvertSelection: (() -> Unit)?,
  onDeselectAll: (() -> Unit)?,
  modifier: Modifier = Modifier,
  useRemoveIcon: Boolean = false,
  onAddToPlaylist: (() -> Unit)? = null,
  onMarkAs: (() -> Unit)? = null,
  onExtractAudio: (() -> Unit)? = null,
  extractAudioEnabled: Boolean = true,
) {
  var showDropdown by remember { mutableStateOf(false) }

  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { showDropdown = true },
      ) {
        Text(
          stringResource(R.string.selected_items, selectedCount, totalCount),
          style = MaterialTheme.typography.titleLargeEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(
          Icons.Rounded.ArrowDropDown,
          contentDescription = stringResource(R.string.selection_options),
          modifier = Modifier.size(28.dp)
        )

        DropdownMenu(
          expanded = showDropdown,
          onDismissRequest = { showDropdown = false },
        ) {
          onSelectAll?.let { DropdownMenuItem(text = { Text(stringResource(R.string.select_all)) }, onClick = { it(); showDropdown = false }) }
          onInvertSelection?.let { DropdownMenuItem(text = { Text(stringResource(R.string.invert_selection)) }, onClick = { it(); showDropdown = false }) }
          onDeselectAll?.let { DropdownMenuItem(text = { Text(stringResource(R.string.deselect_all)) }, onClick = { it(); showDropdown = false }) }
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = onCancel) {
        Icon(
          Icons.Rounded.Close,
          contentDescription = stringResource(R.string.generic_cancel),
          modifier = Modifier.size(28.dp)
        )
      }
    },
    actions = {
      // Material 3: keep at most three actions in the bar so the selection count stays
      // readable on narrow phones; anything beyond that moves into a "More options" menu.
      val actions = buildList {
        onPlay?.let { add(SelectionAction(Icons.Rounded.PlayArrow, stringResource(R.string.cd_play), it)) }
        // Extract audio sits before Info; both act on a single video only.
        if (onExtractAudio != null && isSingleSelection) {
          add(
            SelectionAction(
              Icons.Rounded.MusicNote,
              stringResource(R.string.extract_audio),
              onExtractAudio,
              enabled = extractAudioEnabled,
            ),
          )
        }
        if (onInfo != null && isSingleSelection) {
          add(SelectionAction(Icons.Rounded.Info, stringResource(R.string.info), onInfo))
        }
        onMarkAs?.let {
          add(SelectionAction(ImageVector.vectorResource(R.drawable.ic_sym_done_all), stringResource(R.string.mark_as_title), it))
        }
        onRename?.let {
          add(SelectionAction(Icons.Rounded.DriveFileRenameOutline, stringResource(R.string.rename), it, enabled = isSingleSelection))
        }
        onShare?.let { add(SelectionAction(Icons.Rounded.Share, stringResource(R.string.generic_share), it)) }
        onDelete?.let {
          add(
            SelectionAction(
              icon = if (useRemoveIcon) Icons.Rounded.RemoveCircle else Icons.Rounded.Delete,
              label = stringResource(if (useRemoveIcon) R.string.remove else R.string.delete),
              onClick = it,
              destructive = true,
            ),
          )
        }
      }
      val visible = if (actions.size <= 3) actions else actions.take(2)
      val overflow = actions.drop(visible.size)

      visible.forEach { action ->
        IconButton(onClick = action.onClick, enabled = action.enabled) {
          Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = if (action.destructive) MaterialTheme.colorScheme.error else LocalContentColor.current,
          )
        }
      }
      if (overflow.isNotEmpty()) {
        var showMore by remember { mutableStateOf(false) }
        Box {
          IconButton(onClick = { showMore = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
          }
          DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
            overflow.forEach { action ->
              DropdownMenuItem(
                text = { Text(action.label) },
                leadingIcon = {
                  Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = if (action.destructive) MaterialTheme.colorScheme.error else LocalContentColor.current,
                  )
                },
                enabled = action.enabled,
                onClick = {
                  showMore = false
                  action.onClick()
                },
              )
            }
          }
        }
      }
    },
    modifier = modifier.clip(MaterialTheme.shapes.extraLarge),
  )
}

/** One action in the multi-select top bar (shown as an icon or in the overflow menu). */
private class SelectionAction(
  val icon: ImageVector,
  val label: String,
  val onClick: () -> Unit,
  val enabled: Boolean = true,
  val destructive: Boolean = false,
)
