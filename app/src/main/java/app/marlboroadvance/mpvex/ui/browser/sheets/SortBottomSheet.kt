@file:OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalFoundationApi::class,
)

package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

// ── Layout constants ──
private val SheetHorizontalPadding = 24.dp
private val CardShape = RoundedCornerShape(24.dp)
private val CardPadding = 16.dp
private val SectionSpacing = 16.dp
private val ItemSpacing = 8.dp
private val CardContentSpacing = 12.dp
private const val SortTypeColumns = 2
private const val ViewOptionColumns = 3

/**
 * A single sort choice. [key] is the stable identifier passed back through
 * [SortBottomSheet]'s callbacks; [label] is the localized text shown to the user.
 */
@Immutable
data class SortOption(
  val key: String,
  val label: String,
  val icon: ImageVector? = null,
)

/**
 * Localized names for each sort direction. Resolved during composition so the
 * non-composable [SortBottomSheet] getLabelForType lambdas can use them.
 */
@Immutable
class SortOrderLabels(
  val oldest: String,
  val newest: String,
  val smallest: String,
  val largest: String,
  val fewest: String,
  val most: String,
  val shortest: String,
  val longest: String,
  val asc: String,
  val desc: String,
)

@Composable
fun rememberSortOrderLabels(): SortOrderLabels {
  val oldest = stringResource(R.string.sort_oldest)
  val newest = stringResource(R.string.sort_newest)
  val smallest = stringResource(R.string.sort_smallest)
  val largest = stringResource(R.string.sort_largest)
  val fewest = stringResource(R.string.sort_fewest)
  val most = stringResource(R.string.sort_most)
  val shortest = stringResource(R.string.sort_shortest)
  val longest = stringResource(R.string.sort_longest)
  val asc = stringResource(R.string.sort_asc)
  val desc = stringResource(R.string.sort_desc)
  return remember(oldest, newest, smallest, largest, fewest, most, shortest, longest, asc, desc) {
    SortOrderLabels(oldest, newest, smallest, largest, fewest, most, shortest, longest, asc, desc)
  }
}

/**
 * SortBottomSheet — M3 Expressive
 *
 * Pinned header with a quiet Reset action, one "Sort" card (type grid + connected
 * order group) and an optional "View options" card. Labels stay on one line and
 * shrink to fit instead of truncating or wrapping, so the sheet height is stable.
 *
 * [sortType] is the [SortOption.key] of the selected option. [getLabelForType]
 * receives that key and the current direction.
 */
@Composable
fun SortBottomSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  title: String,
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  sortOrderAsc: Boolean,
  onSortOrderChange: (Boolean) -> Unit,
  options: List<SortOption>,
  getLabelForType: (String, Boolean) -> Pair<String, String>,
  modifier: Modifier = Modifier,
  visibilityToggles: List<VisibilityToggle> = emptyList(),
  showSortOptions: Boolean = true,
  onReset: (() -> Unit)? = null,
) {
  if (!isOpen) return

  val (ascLabel, descLabel) = getLabelForType(sortType, sortOrderAsc)
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val subtitle = if (showSortOptions) {
    val selectedLabel = options.firstOrNull { it.key == sortType }?.label ?: sortType
    "$selectedLabel · ${if (sortOrderAsc) ascLabel else descLabel}"
  } else {
    stringResource(R.string.sort_sheet_subtitle)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    contentWindowInsets = { WindowInsets.navigationBars },
  ) {
    Column(modifier = modifier.fillMaxWidth()) {
      // ── Pinned header ──
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = SheetHorizontalPadding, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (onReset != null) {
          TextButton(onClick = onReset) {
            Icon(
              imageVector = Icons.Filled.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(ToggleButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.generic_reset))
          }
        }
      }

      // ── Scrolling options ──
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = SheetHorizontalPadding)
          .padding(bottom = SheetHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
      ) {
        if (showSortOptions) {
          SortCard(
            sortType = sortType,
            onSortTypeChange = onSortTypeChange,
            options = options,
            sortOrderAsc = sortOrderAsc,
            onSortOrderChange = onSortOrderChange,
            ascLabel = ascLabel,
            descLabel = descLabel,
          )
        }

        if (visibilityToggles.isNotEmpty()) {
          ViewOptionsCard(toggles = visibilityToggles)
        }
      }
    }
  }
}

/**
 * Backwards-compatible overload for call sites that still pass display strings and a
 * parallel icon list. The string doubles as the key. Migrate callers to the
 * [SortOption] overload.
 */
@Deprecated("Pass a List<SortOption> instead of parallel types/icons lists.")
@Composable
fun SortBottomSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  title: String,
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  sortOrderAsc: Boolean,
  onSortOrderChange: (Boolean) -> Unit,
  types: List<String>,
  icons: List<ImageVector>,
  getLabelForType: (String, Boolean) -> Pair<String, String>,
  modifier: Modifier = Modifier,
  visibilityToggles: List<VisibilityToggle> = emptyList(),
  showSortOptions: Boolean = true,
  onReset: (() -> Unit)? = null,
) {
  val options = remember(types, icons) {
    types.mapIndexed { index, type -> SortOption(key = type, label = type, icon = icons.getOrNull(index)) }
  }
  SortBottomSheet(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = title,
    sortType = sortType,
    onSortTypeChange = onSortTypeChange,
    sortOrderAsc = sortOrderAsc,
    onSortOrderChange = onSortOrderChange,
    options = options,
    getLabelForType = getLabelForType,
    modifier = modifier,
    visibilityToggles = visibilityToggles,
    showSortOptions = showSortOptions,
    onReset = onReset,
  )
}

@Composable
private fun SectionContainer(
  title: String,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = CardShape,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier.padding(CardPadding),
      verticalArrangement = Arrangement.spacedBy(CardContentSpacing),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
      )
      content()
    }
  }
}

/** Sort type grid and order group merged into one card to keep the sheet short. */
@Composable
private fun SortCard(
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  options: List<SortOption>,
  sortOrderAsc: Boolean,
  onSortOrderChange: (Boolean) -> Unit,
  ascLabel: String,
  descLabel: String,
) {
  SectionContainer(title = stringResource(R.string.sort_by)) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
      options.chunked(SortTypeColumns).forEach { rowOptions ->
        ConnectedRow {
          rowOptions.forEachIndexed { index, option ->
            val checked = option.key == sortType
            ToggleButton(
              checked = checked,
              onCheckedChange = { if (it) onSortTypeChange(option.key) },
              shapes = connectedShapes(index, rowOptions.size),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .semantics {
                  role = Role.RadioButton
                  selected = checked
                },
            ) {
              // The selected type shows the current direction in place of its own icon.
              val icon = when {
                checked && sortOrderAsc -> Icons.Filled.ArrowUpward
                checked -> Icons.Filled.ArrowDownward
                else -> option.icon
              }
              ToggleContent(icon = icon, label = option.label)
            }
          }
          if (rowOptions.size < SortTypeColumns) {
            Spacer(Modifier.weight((SortTypeColumns - rowOptions.size).toFloat()))
          }
        }
      }
    }

    Text(
      text = stringResource(R.string.sort_order),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ConnectedRow {
      OrderToggle(
        checked = sortOrderAsc,
        onClick = { onSortOrderChange(true) },
        icon = Icons.Filled.ArrowUpward,
        label = ascLabel,
        shapes = connectedShapes(0, 2),
        modifier = Modifier.weight(1f),
      )
      OrderToggle(
        checked = !sortOrderAsc,
        onClick = { onSortOrderChange(false) },
        icon = Icons.Filled.ArrowDownward,
        label = descLabel,
        shapes = connectedShapes(1, 2),
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun OrderToggle(
  checked: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  shapes: ToggleButtonShapes,
  modifier: Modifier = Modifier,
) {
  ToggleButton(
    checked = checked,
    onCheckedChange = { if (it) onClick() },
    shapes = shapes,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .semantics {
        role = Role.RadioButton
        selected = checked
      },
  ) {
    ToggleContent(icon = icon, label = label)
  }
}

@Composable
private fun ViewOptionsCard(
  toggles: List<VisibilityToggle>,
) {
  SectionContainer(title = stringResource(R.string.view_options)) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
      toggles.chunked(ViewOptionColumns).forEach { rowToggles ->
        ConnectedRow {
          rowToggles.forEachIndexed { index, toggle ->
            ToggleButton(
              checked = toggle.checked,
              onCheckedChange = toggle.onCheckedChange,
              shapes = connectedShapes(index, rowToggles.size),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .semantics {
                  role = Role.Checkbox
                  selected = toggle.checked
                },
            ) {
              ToggleContent(icon = null, label = toggle.label)
            }
          }
          if (rowToggles.size < ViewOptionColumns) {
            Spacer(Modifier.weight((ViewOptionColumns - rowToggles.size).toFloat()))
          }
        }
      }
    }
  }
}

// ── Shared building blocks ──

/** A row whose buttons sit tightly together so connected shapes read as one group. */
@Composable
private fun ConnectedRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    content = content,
  )
}

@Composable
private fun connectedShapes(index: Int, count: Int): ToggleButtonShapes = when {
  count <= 1 -> ToggleButtonDefaults.shapes()
  index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
  index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
  else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

@Composable
private fun ToggleContent(
  icon: ImageVector?,
  label: String,
) {
  if (icon != null) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(ToggleButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
  }
  FitLabel(text = label)
}

/** One-line label that shrinks (down to 10sp) rather than truncating or wrapping. */
@Composable
private fun FitLabel(text: String) {
  val style = MaterialTheme.typography.labelLargeEmphasized
  val maxSize = if (style.fontSize.isSpecified) style.fontSize else 14.sp
  Text(
    text = text,
    style = style,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = maxSize),
  )
}
