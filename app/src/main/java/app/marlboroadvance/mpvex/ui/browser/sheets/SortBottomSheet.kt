package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

/**
 * SortBottomSheet — M3 Standard Implementation
 * Uses canonical M3 components, tokens, and motion without custom physics or glassmorphism.
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
  icons: List<androidx.compose.ui.graphics.vector.ImageVector>,
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
        .padding(bottom = 32.dp)
    ) {
      // ── Header ──
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface
        )

        if (onReset != null) {
          TextButton(onClick = onReset) {
            Text(
              text = "Reset",
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }

      if (showSortOptions) {
        SortOptionsSection(
          sortType = sortType,
          onSortTypeChange = onSortTypeChange,
          types = types,
          icons = icons,
          sortOrderAsc = sortOrderAsc,
          onSortOrderChange = onSortOrderChange,
          ascLabel = ascLabel,
          descLabel = descLabel
        )
      }

      if (visibilityToggles.isNotEmpty()) {
        if (showSortOptions) {
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 24.dp),
            thickness = DividerDefaults.Thickness
          )
        }
        ViewOptionsSection(
          toggles = visibilityToggles,
          onToggle = { index, checked ->
            visibilityToggles[index].onCheckedChange(checked)
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsSection(
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  types: List<String>,
  icons: List<androidx.compose.ui.graphics.vector.ImageVector>,
  sortOrderAsc: Boolean,
  onSortOrderChange: (Boolean) -> Unit,
  ascLabel: String,
  descLabel: String
) {
  Column(
    modifier = Modifier.padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // ── Sort By: 2x2 Grid of ElevatedFilterChips ──
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SectionHeader(text = "Sort by")

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(2).forEachIndexed { rowIndex, rowTypes ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            rowTypes.forEachIndexed { colIndex, type ->
              val index = rowIndex * 2 + colIndex
              ElevatedFilterChip(
                modifier = Modifier.weight(1f),
                selected = type == sortType,
                onClick = { onSortTypeChange(type) },
                label = {
                  Text(
                    text = type,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge
                  )
                },
                leadingIcon = if (index < icons.size) {
                  {
                    Icon(
                      imageVector = icons[index],
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                } else null
              )
            }
            if (rowTypes.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    // ── Order: SegmentedButton ──
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SectionHeader(text = "Order")

      SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
      ) {
        val options = listOf(ascLabel to true, descLabel to false)
        val orderIcons = listOf(
          Icons.Filled.ArrowUpward,
          Icons.Filled.ArrowDownward
        )

        options.forEachIndexed { index, (label, isAsc) ->
          SegmentedButton(
            selected = (isAsc == sortOrderAsc),
            onClick = { onSortOrderChange(isAsc) },
            shape = SegmentedButtonDefaults.itemShape(
              index = index,
              count = options.size
            ),
            icon = {
              SegmentedButtonDefaults.Icon(
                active = (isAsc == sortOrderAsc)
              )
            },
            label = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = orderIcons[index],
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Text(label)
              }
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewOptionsSection(
  toggles: List<VisibilityToggle>,
  onToggle: (Int, Boolean) -> Unit
) {
  Column(
    modifier = Modifier.padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    SectionHeader(text = "View options")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      toggles.chunked(2).forEachIndexed { rowIndex, rowToggles ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          rowToggles.forEachIndexed { colIndex, toggle ->
            val index = rowIndex * 2 + colIndex
            FilterChip(
              modifier = Modifier.weight(1f),
              selected = toggle.checked,
              onClick = { onToggle(index, !toggle.checked) },
              label = {
                Text(
                  text = toggle.label,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.Center,
                  style = MaterialTheme.typography.labelLarge
                )
              },
              leadingIcon = if (toggle.checked) {
                {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                }
              } else null
            )
          }
          if (rowToggles.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 4.dp)
  )
}
