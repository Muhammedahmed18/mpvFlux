package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject
import kotlin.math.pow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
fun FolderCard(
  folder: VideoFolder,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isRecentlyPlayed: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  onThumbClick: () -> Unit = {},
  showDateModified: Boolean = false,
  customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  newVideoCount: Int = 0,
  customChipContent: @Composable (() -> Unit)? = null,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  // Optimize expensive calculations with remember
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2
  val parentPath = remember(folder.path) { folder.path.substringBeforeLast("/", folder.path) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(28.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      )
      .then(
        if (isSelected) Modifier.border(
          width = 1.5.dp,
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(28.dp)
        ) else Modifier
      ),
    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(28.dp),
    tonalElevation = if (isSelected) 2.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            customIcon ?: Icons.Rounded.Folder,
            contentDescription = "Folder",
            modifier = Modifier
              .size(26.dp)
              .semantics { contentDescription = "Folder icon" },
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          
          if (newVideoCount > 0) {
            Surface(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp),
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(100)
            ) {
              Text(
                text = newVideoCount.toString(),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            folder.name,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.sp
            ),
            color = if (isRecentlyPlayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { contentDescription = "Folder: ${folder.name}" },
          )
          if (showFolderPath && parentPath.isNotEmpty()) {
            Text(
              parentPath,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          
          FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Video count chip
            if (showTotalVideosChip && folder.videoCount > 0) {
              MetadataPill(
                text = if (folder.videoCount == 1) "1 Video" else "${folder.videoCount} Videos",
                isPrimary = true
              )
            }

            if (showTotalSizeChip && folder.totalSize > 0) {
              MetadataPill(text = formatFileSize(folder.totalSize))
            }
            if (showTotalDurationChip && folder.totalDuration > 0) {
              MetadataPill(text = formatDuration(folder.totalDuration))
            }
            if (showDateChip && folder.lastModified > 0) {
              MetadataPill(text = formatDate(folder.lastModified))
            }
            
            // Invoke custom content if any
            customChipContent?.invoke()
          }
        }
      }
    }
  }

@Composable
private fun MetadataPill(
  text: String,
  isPrimary: Boolean = false
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.padding(vertical = 2.dp)
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
        fontSize = 10.sp
      ),
      color = if (isPrimary) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant
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
  val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
  return sdf.format(java.util.Date(timestampSeconds * 1000))
}
