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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = bouncySpring,
        label = "cardScale"
    )

    val selectionBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else Color.Transparent,
        label = "selectionBackgroundColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(selectionBackgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Leading Icon ──────────────────────────────────────────────────
            FolderIconBox(
                isSelected = isSelected,
                customIcon = customIcon,
                onThumbClick = onThumbClick,
                onLongClick = onLongClick
            )

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (settings.showFolderPath && displayPath.isNotEmpty()) {
                    Text(
                        text = displayPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                FolderMetadataChips(folder, settings, customChipContent)
            }

            // ── Trailing Badge ────────────────────────────────────────────────
            if (settings.showTotalVideosChip && folder.videoCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = folder.videoCount.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderIconBox(
    isSelected: Boolean,
    customIcon: ImageVector?,
    onThumbClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    Box(modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                .combinedClickable(onClick = onThumbClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = customIcon ?: Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        // Selection Badge (consistent with VideoCard)
        val bouncySpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(bouncySpring) + scaleIn(bouncySpring, transformOrigin = TransformOrigin(1f, 1f)),
            exit = fadeOut(bouncySpring) + scaleOut(bouncySpring, transformOrigin = TransformOrigin(1f, 1f)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = 4.dp, y = 4.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainerLow)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderMetadataChips(
    folder: VideoFolder,
    settings: FolderCardSettings,
    customChipContent: (@Composable () -> Unit)?
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

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
