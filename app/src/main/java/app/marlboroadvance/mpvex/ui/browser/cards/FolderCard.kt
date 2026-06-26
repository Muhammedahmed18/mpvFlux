package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val cardScale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.96f
      isSelected -> 0.98f
      else -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "folder_card_scale"
  )

  val tonalElevation = if (isPressed) 4.dp else 1.dp

  val selectionBorderWidth = if (isSelected) 1.dp else 0.dp

  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .scale(cardScale)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
    tonalElevation = tonalElevation,
    border = if (isSelected) {
      BorderStroke(selectionBorderWidth, MaterialTheme.colorScheme.outlineVariant)
    } else null,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.Top,
    ) {
      FolderIconContainer(
        isSelected = isSelected,
        customIcon = customIcon,
        onThumbClick = onThumbClick,
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
          maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
        )

        if (settings.showFolderPath && displayPath.isNotEmpty()) {
          Text(
            text = displayPath,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        FolderMetadataChips(folder, settings, customChipContent)
      }
    }
  }
}

@Composable
private fun FolderIconContainer(
  isSelected: Boolean,
  customIcon: ImageVector?,
  onThumbClick: () -> Unit,
) {
  Box(
    modifier = Modifier.size(56.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .clip(MaterialTheme.shapes.large)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .combinedClickable(
          onClick = onThumbClick,
          indication = ripple(bounded = true),
          interactionSource = remember { MutableInteractionSource() }
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = customIcon ?: Icons.Filled.Folder,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }

    AnimatedVisibility(
      visible = isSelected,
      enter = fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + scaleIn(
        spring(dampingRatio = Spring.DampingRatioNoBouncy),
        transformOrigin = TransformOrigin(1f, 1f)
      ),
      exit = fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + scaleOut(
        spring(dampingRatio = Spring.DampingRatioNoBouncy),
        transformOrigin = TransformOrigin(1f, 1f)
      ),
      modifier = Modifier.align(Alignment.BottomEnd),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = "Selected",
          modifier = Modifier.padding(3.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
    if (settings.showTotalVideosChip && folder.videoCount > 0) {
      UnifiedChip(
        text = "${folder.videoCount} ${if (folder.videoCount == 1) "Video" else "Videos"}",
        isAccent = true,
      )
    }

    customChipContent?.invoke()

    if (settings.showTotalSizeChip && folder.totalSize > 0) {
      UnifiedChip(text = formatFileSize(folder.totalSize))
    }

    if (settings.showTotalDurationChip && folder.totalDuration > 0) {
      UnifiedChip(text = formatDuration(folder.totalDuration))
    }

    if (settings.showDateChip && folder.lastModified > 0) {
      UnifiedChip(text = formatDate(folder.lastModified))
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

private val FOLDER_DATE_FORMATTER by lazy { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

private fun formatDate(timestampSeconds: Long): String {
  return FOLDER_DATE_FORMATTER.format(Date(timestampSeconds * 1000))
}
