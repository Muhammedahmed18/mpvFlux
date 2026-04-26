package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.QueryBuilder
import androidx.compose.material.icons.outlined.SdStorage
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = bouncySpring,
        label = "cardScale"
    )

    val containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        ),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        ListItem(
            modifier = Modifier.padding(vertical = 6.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onSurface,
                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            headlineContent = {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (settings.showFolderPath && displayPath.isNotEmpty()) {
                        Text(
                            text = displayPath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.7f)
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
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    
    val iconColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.9f)
                    )
                )
            )
            .combinedClickable(onClick = onThumbClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.7f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.7f))
            },
            label = "iconSwap"
        ) { selected ->
            Icon(
                imageVector = if (selected) Icons.Filled.Check else (customIcon ?: Icons.Filled.Folder),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (settings.showTotalVideosChip && folder.videoCount > 0) {
            VideoCountPill(count = folder.videoCount)
        }

        val hasOtherMetadata = (settings.showTotalSizeChip && folder.totalSize > 0) ||
                (settings.showTotalDurationChip && folder.totalDuration > 0) ||
                (settings.showDateChip && folder.lastModified > 0) ||
                customChipContent != null

        if (hasOtherMetadata) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                customChipContent?.invoke()

                if (settings.showTotalSizeChip && folder.totalSize > 0) {
                    MetadataChip(
                        text = formatFileSize(folder.totalSize),
                        icon = Icons.Outlined.SdStorage
                    )
                }
                if (settings.showTotalDurationChip && folder.totalDuration > 0) {
                    MetadataChip(
                        text = formatDuration(folder.totalDuration),
                        icon = Icons.Outlined.QueryBuilder
                    )
                }
                if (settings.showDateChip && folder.lastModified > 0) {
                    MetadataChip(
                        text = formatDate(folder.lastModified),
                        icon = Icons.Outlined.CalendarToday
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCountPill(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
    ) {
        val label = if (count == 1) "VIDEO" else "VIDEOS"
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MetadataChip(
    text: String,
    icon: ImageVector? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                ),
            )
        }
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
