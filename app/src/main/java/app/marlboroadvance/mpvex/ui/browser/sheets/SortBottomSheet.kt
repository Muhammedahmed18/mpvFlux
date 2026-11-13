package app.marlboroadvance.mpvex.ui.browser.sheets

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

/**
 * SortBottomSheet — M3 Expressive
 * ToggleButtons with built-in shape morphing, arranged in a grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
fun rememberSortOrderLabels(): SortOrderLabels = SortOrderLabels(
  oldest = stringResource(R.string.sort_oldest),
  newest = stringResource(R.string.sort_newest),
  smallest = stringResource(R.string.sort_smallest),
  largest = stringResource(R.string.sort_largest),
  fewest = stringResource(R.string.sort_fewest),
  most = stringResource(R.string.sort_most),
  shortest = stringResource(R.string.sort_shortest),
  longest = stringResource(R.string.sort_longest),
  asc = stringResource(R.string.sort_asc),
  desc = stringResource(R.string.sort_desc),
)

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

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = BottomSheetDefaults.ExpandedShape,
    containerColor = BottomSheetDefaults.ContainerColor,
    tonalElevation = 3.dp,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets = { WindowInsets.navigationBars }
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      // ── Header ──
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.sort_sheet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (onReset != null) {
          IconButton(
            onClick = onReset,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.RestartAlt,
              contentDescription = stringResource(R.string.generic_reset),
            )
          }
        }
      }

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
        ViewOptionsCard(
          toggles = visibilityToggles,
        )
      }
    }
  }
}

@Composable
private fun SectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmallEmphasized,
      color = MaterialTheme.colorScheme.primary,
    )
    content()
  }
}

@Composable
private fun SortByCard(
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  types: List<String>,
  icons: List<ImageVector>,
) {
  SectionCard(title = stringResource(R.string.sort_by)) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      types.chunked(2).forEach { rowTypes ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          rowTypes.forEach { type ->
            val index = types.indexOf(type)
            val selected = type == sortType
            ToggleButton(
              checked = selected,
              onCheckedChange = { if (it) onSortTypeChange(type) },
              modifier = Modifier.weight(1f)
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
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
              )
            }
          }
          if (rowTypes.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
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
  SectionCard(title = stringResource(R.string.sort_order)) {
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
    modifier = modifier,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(ToggleButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
  }
}

@Composable
private fun ViewOptionsCard(
  toggles: List<VisibilityToggle>,
) {
  SectionCard(title = stringResource(R.string.view_options)) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      toggles.chunked(2).forEach { rowToggles ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          rowToggles.forEach { toggle ->
            val selected = toggle.checked
            ToggleButton(
              checked = selected,
              onCheckedChange = { toggle.onCheckedChange(it) },
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = toggle.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
              )
            }
          }
          if (rowToggles.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}
