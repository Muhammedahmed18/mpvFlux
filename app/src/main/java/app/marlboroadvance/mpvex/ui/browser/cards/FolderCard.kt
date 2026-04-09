package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import kotlin.math.pow
import androidx.compose.runtime.Immutable

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

  val parentPath = remember(folder.path) {
    folder.path.substringBeforeLast("/", folder.path)
  }

  val metadataText = remember(folder, settings) {
    val parts = mutableListOf<String>()
    if (settings.showTotalVideosChip && folder.videoCount > 0) {
      parts.add("${folder.videoCount} videos")
    }
    if (settings.showTotalSizeChip && folder.totalSize > 0) {
      parts.add(formatFileSize(folder.totalSize))
    }
    if (settings.showTotalDurationChip && folder.totalDuration > 0) {
      parts.add(formatDuration(folder.totalDuration))
    }
    if (settings.showDateChip && folder.lastModified > 0) {
      parts.add(formatDate(folder.lastModified))
    }
    parts.joinToString(" · ")
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
          onClick = onClick,
          onLongClick = onLongClick,
        ),
      color = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        Color.Transparent
      },
      tonalElevation = 0.dp,
      shadowElevation = 0.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Icon Section
        Box(
          modifier = Modifier
            .combinedClickable(
              onClick = onThumbClick,
              onLongClick = onLongClick,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) {
              MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            } else {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            },
            modifier = Modifier.size(52.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else (customIcon ?: Icons.Filled.Folder),
                contentDescription = "Folder",
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                },
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
          modifier = Modifier.weight(1f),
        ) {
          if (settings.showFolderPath && parentPath.isNotEmpty()) {
            Text(
              text = parentPath.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium
              ),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
              maxLines = Int.MAX_VALUE,
              overflow = TextOverflow.Clip,
            )
          }

          Text(
            text = folder.name,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
          )

          if (metadataText.isNotEmpty() || customChipContent != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              customChipContent?.invoke()
              if (metadataText.isNotEmpty()) {
                Text(
                  text = metadataText,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 16.dp),
      thickness = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
  }
}

private fun formatDuration(durationMs: Long): String {
  val seconds = durationMs / 1000
  val hours = seconds / 3600
  val minutes = (seconds % 3600) / 60
  val secs = seconds % 60

  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m"
    else -> "${secs}s"
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
  val value = bytes / 1024.0.pow(digitGroups.toDouble())
  return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}

private fun formatDate(timestampSeconds: Long): String {
  val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
  return sdf.format(java.util.Date(timestampSeconds * 1000))
}
