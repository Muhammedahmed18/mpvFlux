package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
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

/**
 * Material 3 Folder Card
 *
 * Follows M3 design tokens and components:
 * - Card container with tonal elevation (surfaceContainerLowest)
 * - ListItem for standard layout structure
 * - AssistChip for primary metadata (video count)
 * - SuggestionChip for secondary metadata (size, duration, date)
 * - Standard icon container (40.dp) with secondaryContainer background
 * - Selection via primaryContainer + checked icon container pattern
 * - No manual gradients, glows, or alpha blending
 */
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

  Card(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
      }
    ),
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    ListItem(
      modifier = Modifier.padding(
        vertical = 4.dp,
        horizontal = 4.dp
      ),
      colors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
      ),
      headlineContent = {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
        )
      },
      supportingContent = {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (settings.showFolderPath && displayPath.isNotEmpty()) {
            Text(
              text = displayPath,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
          FolderMetadataChips(folder, settings, customChipContent)
        }
      },
      leadingContent = {
        FolderIconContainer(
          isSelected = isSelected,
          customIcon = customIcon,
          onThumbClick = onThumbClick,
        )
      }
    )
  }
}

@Composable
private fun FolderIconContainer(
  isSelected: Boolean,
  customIcon: ImageVector?,
  onThumbClick: () -> Unit,
) {
  val iconColor = if (isSelected) {
    MaterialTheme.colorScheme.onPrimaryContainer
  } else {
    MaterialTheme.colorScheme.onSecondaryContainer
  }

  Box(
    modifier = Modifier
      .size(40.dp),
    contentAlignment = Alignment.Center
  ) {
    // Background shape
    Box(
      modifier = Modifier
        .size(40.dp)
        .combinedClickable(
          onClick = onThumbClick,
          indication = ripple(bounded = true),
          interactionSource = remember { MutableInteractionSource() }
        ),
      contentAlignment = Alignment.Center
    ) {
      // Selection indicator: checked circle overlay
      if (isSelected) {
        Box(
          modifier = Modifier
            .size(40.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      } else {
        Icon(
          imageVector = customIcon ?: Icons.Filled.Folder,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = iconColor
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
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Primary metadata: video count (AssistChip)
    if (settings.showTotalVideosChip && folder.videoCount > 0) {
      AssistChip(
        onClick = { /* no action for read-only chip */ },
        label = {
          Text(
            text = "${folder.videoCount} ${if (folder.videoCount == 1) "Video" else "Videos"}",
            style = MaterialTheme.typography.labelLarge
          )
        },
        colors = AssistChipDefaults.assistChipColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = null,
      )
    }

    customChipContent?.invoke()

    // Secondary metadata: size, duration, date (SuggestionChips)
    if (settings.showTotalSizeChip && folder.totalSize > 0) {
      SuggestionChip(
        onClick = { },
        label = {
          Text(
            text = formatFileSize(folder.totalSize),
            style = MaterialTheme.typography.labelLarge
          )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = null,
      )
    }

    if (settings.showTotalDurationChip && folder.totalDuration > 0) {
      SuggestionChip(
        onClick = { },
        label = {
          Text(
            text = formatDuration(folder.totalDuration),
            style = MaterialTheme.typography.labelLarge
          )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = null,
      )
    }

    if (settings.showDateChip && folder.lastModified > 0) {
      SuggestionChip(
        onClick = { },
        label = {
          Text(
            text = formatDate(folder.lastModified),
            style = MaterialTheme.typography.labelLarge
          )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = null,
      )
    }
  }
}

private fun cleanPath(path: String, name: String): String {
  return path.removeSuffix("/").removeSuffix(name).removeSuffix("/")
}

private fun formatFileSize(size: Long): String {
  if (size <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
  return String.format(Locale.US, "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

private fun formatDuration(duration: Long): String {
  val hours = duration / 3600000
  val minutes = (duration % 3600000) / 60000
  return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatDate(timestamp: Long): String {
  val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
  return sdf.format(Date(timestamp))
}
