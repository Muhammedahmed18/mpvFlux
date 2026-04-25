package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

/**
 * A redesigned Material 3 Sort Bottom Sheet.
 * Features:
 * - Headline typography for clarity
 * - Card-based 2x2 grid for sort selection
 * - Tonal checkmark indicator
 * - 2-column grid for View options
 * - Animated selection transitions
 * - Full Dynamic Color support
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                if (onReset != null) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset()
                        }
                    ) {
                        Text("Reset")
                    }
                }
            }

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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Sort Type Selection
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
                    SortItemCard(
                        text = type,
                        icon = icons.getOrNull(index),
                        isSelected = isSelected,
                        onClick = { onSortTypeChange(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (types.size % 2 != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Order Selection
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(text = "Order")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = sortOrderAsc,
                    onClick = { onSortOrderChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { SegmentedButtonDefaults.Icon(sortOrderAsc) {
                        Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(20.dp))
                    }}
                ) {
                    Text(ascLabel, style = MaterialTheme.typography.bodyMedium)
                }
                SegmentedButton(
                    selected = !sortOrderAsc,
                    onClick = { onSortOrderChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { SegmentedButtonDefaults.Icon(!sortOrderAsc) {
                        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp))
                    }}
                ) {
                    Text(descLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SortItemCard(
    text: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                // Tonal background for the check icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
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

        // 2-Column Grid for View Options
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
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = toggle.checked,
                            onCheckedChange = {
                                onToggle()
                                toggle.onCheckedChange(it)
                            },
                            modifier = Modifier.scale(0.8f)
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
