package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

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
) {
    if (!isOpen) return

    val (ascLabel, descLabel) = getLabelForType(sortType, sortOrderAsc)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (showSortOptions) {
                SortTypeSelectorExpressive(
                    sortType = sortType,
                    onSortTypeChange = onSortTypeChange,
                    types = types,
                    icons = icons,
                    modifier = Modifier.fillMaxWidth(),
                )

                SortOrderSelectorExpressive(
                    sortOrderAsc = sortOrderAsc,
                    onSortOrderChange = onSortOrderChange,
                    ascLabel = ascLabel,
                    descLabel = descLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (visibilityToggles.isNotEmpty()) {
                VisibilityTogglesSectionExpressive(
                    toggles = visibilityToggles,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SortTypeSelectorExpressive(
    sortType: String,
    onSortTypeChange: (String) -> Unit,
    types: List<String>,
    icons: List<ImageVector>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = (types.size + 1) / 2
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(2) { colIndex ->
                        val index = rowIndex * 2 + colIndex
                        if (index < types.size) {
                            val type = types[index]
                            val icon = icons[index]
                            val selected = sortType == type
                            SortTypeCard(
                                type = type,
                                icon = icon,
                                selected = selected,
                                onClick = { onSortTypeChange(type) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortTypeCard(
    type: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer 
                       else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = type,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SortOrderSelectorExpressive(
    sortOrderAsc: Boolean,
    onSortOrderChange: (Boolean) -> Unit,
    ascLabel: String,
    descLabel: String,
    modifier: Modifier = Modifier,
) {
    val options = listOf(ascLabel, descLabel)
    val selectedIndex = if (sortOrderAsc) 0 else 1

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        
        SingleChoiceSegmentedButtonRow(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    onClick = { onSortOrderChange(index == 0) },
                    selected = selected,
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        activeBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    icon = {
                        Icon(
                            if (index == 0) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun VisibilityTogglesSectionExpressive(
    toggles: List<VisibilityToggle>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Display Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                toggles.forEach { toggle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { toggle.onCheckedChange(!toggle.checked) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = toggle.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = toggle.checked,
                            onCheckedChange = { toggle.onCheckedChange(it) }
                        )
                    }
                }
            }
        }
    }
}
