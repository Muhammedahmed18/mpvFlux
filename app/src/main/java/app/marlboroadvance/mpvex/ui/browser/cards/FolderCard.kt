package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

  val cardScale: Float by animateFloatAsState(
    targetValue = if (isSelected) 0.98f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "cardScale"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp)
      .scale(cardScale)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else Color.Transparent,
    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {

      // ── Icon Container ──────────────────────────────────────────────────────
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .combinedClickable(
            onClick = onThumbClick,
            onLongClick = onLongClick,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = customIcon ?: Icons.Filled.Folder,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.primary,
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      // ── Text Content ──────────────────────────────────────────────────────
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )

        if (settings.showFolderPath && folderPath.isNotEmpty()) {
          Text(
            text = folderPath,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        val metadataParts = remember(folder, settings) {
          mutableListOf<String>().apply {
            if (settings.showTotalSizeChip && folder.totalSize > 0) {
              add(formatFileSize(folder.totalSize))
            }
            if (settings.showTotalDurationChip && folder.totalDuration > 0) {
              add(formatDuration(folder.totalDuration))
            }
            if (settings.showDateChip && folder.lastModified > 0) {
              add(formatDate(folder.lastModified))
            }
          }
        }

        val hasVideos = settings.showTotalVideosChip && folder.videoCount > 0
        val hasMetadata = metadataParts.isNotEmpty() || customChipContent != null

        if (hasVideos || hasMetadata) {
          FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (hasVideos) {
              FolderMetadataChip(
                text = "${folder.videoCount} videos",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            }

            if (hasVideos && hasMetadata) {
              Text(
                text = "|",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                  .padding(horizontal = 2.dp)
                  .align(Alignment.CenterVertically)
              )
            }

            metadataParts.forEach { part ->
              FolderMetadataChip(
                text = part,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            }

            customChipContent?.let {
              Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                it()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FolderMetadataChip(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Surface(
    modifier = modifier,
    color = containerColor,
    shape = RoundedCornerShape(6.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      ),
      color = contentColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

// ── Formatters ────────────────────────────────────────────────────────────────

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
