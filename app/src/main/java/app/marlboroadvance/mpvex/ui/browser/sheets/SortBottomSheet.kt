package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

/**
 * Localized names for each sort direction. Resolved during composition so the
 * non-composable [SortBottomSheet] getLabelForType lambdas can use them.
 */
@androidx.compose.runtime.Immutable
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
 * Pinned header, tonal section containers, wrapping ToggleButtons and switch rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
  if (!isOpen) return

  val (ascLabel, descLabel) = getLabelForType(sortType, sortOrderAsc)
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val subtitle = if (showSortOptions) {
    "$sortType · ${if (sortOrderAsc) ascLabel else descLabel}"
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
          .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
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
          IconButton(
            onClick = onReset,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.RestartAlt,
              contentDescription = stringResource(R.string.generic_reset),
            )
          }
        }
      }

      // ── Scrolling options ──
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (showSortOptions) {
          SortByCard(
            sortType = sortType,
            onSortTypeChange = onSortTypeChange,
            types = types,
            icons = icons,
          )
          OrderCard(
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

@Composable
private fun SectionContainer(
  title: String,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortByCard(
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  types: List<String>,
  icons: List<ImageVector>,
) {
  SectionContainer(title = stringResource(R.string.sort_by)) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      types.forEachIndexed { index, type ->
        val selected = type == sortType
        ToggleButton(
          checked = selected,
          onCheckedChange = { if (it) onSortTypeChange(type) },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics { role = Role.RadioButton },
        ) {
          if (index < icons.size) {
            Icon(
              imageVector = icons[index],
              contentDescription = null,
              modifier = Modifier.size(ToggleButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
          }
          Text(
            text = type,
            style = MaterialTheme.typography.labelLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun OrderCard(
  sortOrderAsc: Boolean,
  onSortOrderChange: (Boolean) -> Unit,
  ascLabel: String,
  descLabel: String,
) {
  SectionContainer(title = stringResource(R.string.sort_order)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OrderToggle(
        selected = sortOrderAsc,
        onClick = { onSortOrderChange(true) },
        icon = Icons.Filled.ArrowUpward,
        label = ascLabel,
        modifier = Modifier.weight(1f),
      )
      OrderToggle(
        selected = !sortOrderAsc,
        onClick = { onSortOrderChange(false) },
        icon = Icons.Filled.ArrowDownward,
        label = descLabel,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun OrderToggle(
  selected: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  ToggleButton(
    checked = selected,
    onCheckedChange = { if (it) onClick() },
    modifier = modifier
      .minimumInteractiveComponentSize()
      .semantics { role = Role.RadioButton },
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(ToggleButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLargeEmphasized,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ViewOptionsCard(
  toggles: List<VisibilityToggle>,
) {
  SectionContainer(title = stringResource(R.string.view_options)) {
    Column {
      toggles.forEach { toggle ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
              value = toggle.checked,
              role = Role.Switch,
              onValueChange = { toggle.onCheckedChange(it) },
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = toggle.label,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Switch(
            checked = toggle.checked,
            onCheckedChange = null,
          )
        }
      }
    }
  }
}
