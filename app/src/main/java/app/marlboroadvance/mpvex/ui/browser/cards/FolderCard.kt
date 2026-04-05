package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject
import kotlin.math.pow

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

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

  // Remove the redundant folder name from the path
  val parentPath = remember(folder.path) {
    folder.path.substringBeforeLast("/", folder.path)
  }

  // Step 4: Animation and Selection States
  val cardScale by animateFloatAsState(if (isSelected) 0.98f else 1f, label = "scale")
  val containerColor by animateColorAsState(
    targetValue = if (isSelected) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
      MaterialTheme.colorScheme.surface
    },
    label = "color"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .scale(cardScale)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = containerColor
    ),
    border = if (isSelected) {
      androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
      null
    },
    elevation = CardDefaults.cardElevation(
      defaultElevation = if (isSelected) 2.dp else 0.dp
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .padding(start = 4.dp)
          .combinedClickable(
            onClick = onThumbClick,
            onLongClick = onLongClick,
          ),
        contentAlignment = Alignment.Center,
      ) {
        BadgedBox(
          badge = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(bottom = 2.dp, end = 2.dp)
                )
            } else if (folder.videoCount > 0) {
              Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 2.dp, end = 2.dp)
              ) {
                Text(
                  text = folder.videoCount.toString(),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                )
              }
            }
          }
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            },
            modifier = Modifier.size(56.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = customIcon ?: Icons.Filled.Folder,
                contentDescription = "Folder",
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )

        if (settings.showFolderPath && parentPath.isNotEmpty()) {
          Text(
            text = parentPath,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FolderMetadataRow(
            folder = folder,
            settings = settings,
            customChipContent = customChipContent
        )
      }
    }
  }
}

@Composable
private fun FolderMetadataRow(
    folder: VideoFolder,
    settings: FolderCardSettings,
    customChipContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (customChipContent != null || folder.totalSize > 0 || folder.totalDuration > 0 || folder.lastModified > 0) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier
        ) {
            customChipContent?.invoke()

            if (settings.showTotalSizeChip && folder.totalSize > 0) {
                MetadataChip(
                    text = formatFileSize(folder.totalSize),
                    icon = Icons.Default.Storage
                )
            }

            if (settings.showTotalDurationChip && folder.totalDuration > 0) {
                MetadataChip(
                    text = formatDuration(folder.totalDuration),
                    icon = Icons.Default.Schedule
                )
            }

            if (settings.showDateChip && folder.lastModified > 0) {
                MetadataChip(
                    text = formatDate(folder.lastModified),
                    icon = Icons.Default.Event
                )
            }
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    icon: ImageVector,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
  val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
  return sdf.format(java.util.Date(timestampSeconds * 1000))
}
