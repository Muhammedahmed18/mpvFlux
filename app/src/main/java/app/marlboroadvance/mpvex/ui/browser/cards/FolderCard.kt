package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import kotlin.math.pow

@Immutable
data class FolderCardSettings(
    val unlimitedNameLines: Boolean = false,
    val showTotalVideosChip: Boolean = true,
    val showTotalDurationChip: Boolean = true,
    val showTotalSizeChip: Boolean = true,
    val showDateChip: Boolean = true,
    val showFolderPath: Boolean = true,
)

@Composable
fun FolderCard(
    folder: VideoFolder,
    settings: FolderCardSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onThumbClick: () -> Unit = {},
    customIcon: ImageVector? = null,
    customChipContent: @Composable (() -> Unit)? = null,
) {
    val maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2

    val folderPath = remember(folder.path) {
        folder.path.trimEnd('/')
    }

    // Shapes
    val cardShape = RoundedCornerShape(28.dp)
    val iconShape = RoundedCornerShape(20.dp)
    val badgeShape = RoundedCornerShape(12.dp)

    // ── Animated State ────────────────────────────────────────────────────────

    val bouncySpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)

    val cardScale: Float by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = bouncySpring,
        label = "cardScale"
    )

    // Animated colors to prevent "flicker" during selection and theme switch
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )

    val secondaryContainerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.secondaryContainer,
        label = "secondaryContainerColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "iconTint"
    )

    val badgeAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.15f else 0.7f,
        label = "badgeAlpha"
    )

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .scale(cardScale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // ── Icon ────────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .combinedClickable(onClick = onThumbClick, onLongClick = onLongClick),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(iconShape)
                        .background(secondaryContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = customIcon ?: Icons.Filled.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconTint,
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    // Use the same spring and anchor growth to the bottom-right corner to stop "sliding"
                    enter = fadeIn(bouncySpring) + scaleIn(bouncySpring, transformOrigin = TransformOrigin(1f, 1f)),
                    exit = fadeOut(bouncySpring) + scaleOut(bouncySpring, transformOrigin = TransformOrigin(1f, 1f)),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(20.dp).offset(x = 4.dp, y = 4.dp),
                        border = BorderStroke(2.dp, containerColor)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.padding(3.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ── Info ────────────────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )

                if (settings.showFolderPath && folderPath.isNotEmpty()) {
                    Text(
                        text = folderPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) contentColor.copy(alpha = 0.8f) else onSurfaceVariantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                val metadataParts = remember(folder, settings) {
                    buildList {
                        if (settings.showTotalSizeChip && folder.totalSize > 0) add(formatFileSize(folder.totalSize))
                        if (settings.showTotalDurationChip && folder.totalDuration > 0) add(formatDuration(folder.totalDuration))
                        if (settings.showDateChip && folder.lastModified > 0) add(formatDate(folder.lastModified))
                    }
                }

                if (metadataParts.isNotEmpty()) {
                    Text(
                        text = metadataParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) contentColor.copy(alpha = 0.7f) else onSurfaceVariantColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // ── Badge ───────────────────────────────────────────────────────────────
            if (settings.showTotalVideosChip && folder.videoCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = secondaryContainerColor.copy(alpha = badgeAlpha),
                    shape = badgeShape,
                    modifier = Modifier.sizeIn(minWidth = 32.dp, minHeight = 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "${folder.videoCount}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = iconTint
                        )
                    }
                }
            }
        }
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds % 60}s"
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.getDefault(), "%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestampSeconds: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestampSeconds * 1000))
}

@Preview(showBackground = true)
@Composable
fun FolderCardPreview() {
    MaterialTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val folder = VideoFolder("1", "Movies", "/storage/Movies", 12, 500L * 1024 * 1024, 90L * 60 * 1000, System.currentTimeMillis() / 1000)
            FolderCard(folder, FolderCardSettings(), {})
            FolderCard(folder, FolderCardSettings(), {}, isSelected = true)
        }
    }
}
