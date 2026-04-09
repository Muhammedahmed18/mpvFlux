package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

/**
 * A modern, Material 3 "Contextual" Sort Bottom Sheet.
 * Features:
 * - Centered M3 Typography
 * - Reset functionality
 * - Grid-based selection for sort types (2x2)
 * - Segmented Buttons for order
 * - Grid-based View Options (2-columns)
 * - Integrated haptic feedback
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
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step 1: M3 Centered Header with Reset Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (onReset != null) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Reset")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showSortOptions) {
                SortOptionsSection(
                    sortType = sortType,
                    onSortTypeChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSortTypeChange(it)
                    },
                    types = types,
                    icons = icons,
                    sortOrderAsc = sortOrderAsc,
                    onSortOrderChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSortOrderChange(it)
                    },
                    ascLabel = ascLabel,
                    descLabel = descLabel
                )
            }

            if (visibilityToggles.isNotEmpty()) {
                if (showSortOptions) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                ViewOptionsSection(
                    toggles = visibilityToggles,
                    onToggle = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsSection(
    sortType: String,
    onSortTypeChange: (String) -> Unit,
    types: List<String>,
    icons: List<ImageVector>,
    sortOrderAsc: Boolean,
    onSortOrderChange: (Boolean) -> Unit,
    ascLabel: String,
    descLabel: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Step 2: Sort Type Selection using a Grid-like 2x2 FlowRow
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(text = "Sort by")

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                types.forEachIndexed { index, type ->
                    val isSelected = sortType == type
                    // Use a Box with weight to force 2-column grid behavior
                    Box(modifier = Modifier.weight(1f)) {
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSortTypeChange(type) },
                            label = {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            leadingIcon = if (icons.getOrNull(index) != null) {
                                {
                                    Icon(
                                        imageVector = icons[index],
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }

        // Step 3: Sort Order - Standard M3 Segmented Button
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(text = "Order")

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = sortOrderAsc,
                    onClick = { onSortOrderChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { SegmentedButtonDefaults.Icon(active = sortOrderAsc) {
                        Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(18.dp))
                    }}
                ) {
                    Text(ascLabel, style = MaterialTheme.typography.bodyMedium)
                }
                SegmentedButton(
                    selected = !sortOrderAsc,
                    onClick = { onSortOrderChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { SegmentedButtonDefaults.Icon(active = !sortOrderAsc) {
                        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                    }}
                ) {
                    Text(descLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewOptionsSection(
    toggles: List<VisibilityToggle>,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(text = "View options")

        // Step 4: 2-Column Grid for View Options
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            toggles.forEach { toggle ->
                Surface(
                    onClick = {
                        onToggle()
                        toggle.onCheckedChange(!toggle.checked)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = toggle.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = toggle.checked,
                            onCheckedChange = {
                                onToggle()
                                toggle.onCheckedChange(it)
                            },
                            modifier = Modifier.scale(0.7f) // Even smaller to fit better in grid
                        )
                    }
                }
            }
            
            // Spacer for odd numbers of items to maintain grid alignment
            if (toggles.size % 2 != 0) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
