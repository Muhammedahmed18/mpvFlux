package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle

/**
 * A modern, One UI 7 / Android 16 inspired Sort Bottom Sheet.
 * Features:
 * - Extreme roundness (48dp corners)
 * - Large, centered, black-weight typography
 * - Spring-animated selection states
 * - High-contrast "pill" toggles
 * - Integrated haptic feedback for a premium feel
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
) {
    if (!isOpen) return

    val (ascLabel, descLabel) = getLabelForType(sortType, sortOrderAsc)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 64.dp,
                height = 3.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            )
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Android 16 Style Header: Centered, Bold
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 24.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

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
                    Spacer(modifier = Modifier.height(24.dp))
                }
                ViewOptionsSection(
                    toggles = visibilityToggles,
                    onToggle = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        // Sort Type Selection Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(text = "Sort by")

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                types.forEachIndexed { index, type ->
                    val isSelected = sortType == type
                    ModernSortTypeItem(
                        label = type,
                        icon = icons.getOrNull(index),
                        isSelected = isSelected,
                        onClick = { onSortTypeChange(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sort Order - Modern Pill Toggle
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(text = "Order")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OrderToggleItem(
                    label = ascLabel,
                    icon = Icons.Default.KeyboardArrowUp,
                    isSelected = sortOrderAsc,
                    onClick = { onSortOrderChange(true) },
                    modifier = Modifier.weight(1f)
                )
                OrderToggleItem(
                    label = descLabel,
                    icon = Icons.Default.KeyboardArrowDown,
                    isSelected = !sortOrderAsc,
                    onClick = { onSortOrderChange(false) },
                    modifier = Modifier.weight(1f)
                )
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
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun ModernSortTypeItem(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    val cornerSize by animateDpAsState(
        if (isSelected) 28.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cornerSize"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 60.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(cornerSize),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 1
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OrderToggleItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "toggleColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "toggleContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold
            )
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

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            toggles.forEach { toggle ->
                ModernViewOptionChip(
                    label = toggle.label,
                    checked = toggle.checked,
                    onCheckedChange = {
                        onToggle()
                        toggle.onCheckedChange(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernViewOptionChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipColor"
    )
    val contentColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContentColor"
    )

    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier
            .height(44.dp)
            .animateContentSize(),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
