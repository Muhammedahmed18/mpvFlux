package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    customChipContent: (@Composable () -> Unit)? = null,
) {
    val displayPath = remember(folder.path, folder.name) {
        cleanPath(folder.path, folder.name)
    }

    val bouncySpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)

    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = bouncySpring,
        label = "cardScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "containerColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
    ) {
        ListItem(
            modifier = Modifier.padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onSurface,
                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            headlineContent = {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (settings.showFolderPath && displayPath.isNotEmpty()) {
                        Text(
                            text = displayPath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FolderMetadataChips(folder, settings, customChipContent)
                }
            },
            leadingContent = {
                FolderIconBox(
                    isSelected = isSelected,
                    customIcon = customIcon,
                    onThumbClick = onThumbClick,
                    onLongClick = onLongClick
                )
            },
            trailingContent = {
                if (settings.showTotalVideosChip && folder.videoCount > 0) {
                    Badge(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = folder.videoCount.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun FolderIconBox(
    isSelected: Boolean,
    customIcon: ImageVector?,
    onThumbClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
        label = "iconContainerColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer,
        label = "iconColor"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .combinedClickable(onClick = onThumbClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
            },
            label = "iconSwap"
        ) { selected ->
            Icon(
                imageVector = if (selected) Icons.Filled.Check else (customIcon ?: Icons.Filled.Folder),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderMetadataChips(
    folder: VideoFolder,
    settings: FolderCardSettings,
    customChipContent: (@Composable () -> Unit)?
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        customChipContent?.invoke()

        if (settings.showTotalSizeChip && folder.totalSize > 0) {
            MetadataChip(text = formatFileSize(folder.totalSize))
        }
        if (settings.showTotalDurationChip && folder.totalDuration > 0) {
            MetadataChip(text = formatDuration(folder.totalDuration))
        }
        if (settings.showDateChip && folder.lastModified > 0) {
            MetadataChip(text = formatDate(folder.lastModified))
        }
    }
}

@Composable
private fun MetadataChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun cleanPath(path: String, folderName: String): String {
    val trimmed = path.trimEnd('/')
    val parentPath = if (trimmed.endsWith(folderName, ignoreCase = true)) {
        trimmed.substringBeforeLast(folderName).trimEnd('/')
    } else {
        trimmed
    }
    return parentPath
        .replace(Regex("^/storage/emulated/\\d+"), "Internal Storage")
        .replace(Regex("^/storage/[A-Z0-9-]+"), "SD Card")
}

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
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestampSeconds: Long): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(Date(timestampSeconds * 1000))
}

@Preview(showBackground = true)
@Composable
fun FolderCardPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FolderCard(
                folder = VideoFolder(
                    bucketId = "1",
                    name = "Downloaded Movies",
                    path = "/storage/emulated/0/Download/Movies",
                    videoCount = 12,
                    totalSize = 5L * 1024 * 1024 * 1024,
                    totalDuration = 36000000L,
                    lastModified = System.currentTimeMillis() / 1000
                ),
                settings = FolderCardSettings(),
                onClick = {}
            )
        }
    }
}
