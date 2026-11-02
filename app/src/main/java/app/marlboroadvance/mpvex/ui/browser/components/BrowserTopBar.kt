package app.marlboroadvance.mpvex.ui.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
  val preferences = koinInject<AppearancePreferences>()
  val darkMode by preferences.darkMode.collectAsState()
  val darkTheme = isSystemInDarkTheme()
  val coroutineScope = rememberCoroutineScope()

  fun toggleDarkMode() {
    when (darkMode) {
      DarkMode.System -> preferences.darkMode.set(if (darkTheme) DarkMode.Light else DarkMode.Dark)
      DarkMode.Light -> preferences.darkMode.set(if (darkTheme) DarkMode.System else DarkMode.Dark)
      DarkMode.Dark -> preferences.darkMode.set(if (darkTheme) DarkMode.Light else DarkMode.System)
    }
  }

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
      modifier = Modifier
        .pointerInput(onTitleLongPress) {
          detectTapGestures(
            onTap = { coroutineScope.launch { toggleDarkMode() } },
            onLongPress = { onTitleLongPress?.invoke() },
          )
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
          Icon(
            Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.settings),
            tint = MaterialTheme.colorScheme.secondary,
          )
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
      onPlay?.let { IconButton(onClick = it) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp)) } }
      
      if (onInfo != null && isSingleSelection) {
        Surface(
          onClick = onInfo,
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
          modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = "Info", style = MaterialTheme.typography.labelLargeEmphasized, modifier = Modifier.padding(start = 4.dp))
          }
        }
      }

      onRename?.let { IconButton(onClick = it, enabled = isSingleSelection) { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) } }
      onShare?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Share, contentDescription = null) } }
      onDelete?.let { IconButton(onClick = it) { Icon(if (useRemoveIcon) Icons.Rounded.RemoveCircle else Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) } }
    },
    modifier = modifier.clip(MaterialTheme.shapes.extraLarge),
  )
}
