package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.marlboroadvance.mpvex.R
import androidx.compose.ui.res.stringResource
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
  val internalLabel = stringResource(R.string.card_storage_internal)
  val sdCardLabel = stringResource(R.string.card_storage_sd_card)
  val displayPath = remember(folder.path, folder.name, internalLabel, sdCardLabel) {
    shortenPath(folder.path, folder.name, internalLabel, sdCardLabel)
  }

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val cardScale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.96f
      else -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "folder_card_scale"
  )


  // Selection: solid tonal fill + check badge (no border or shrink).
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.secondaryContainer
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .scale(cardScale)
      .semantics { selected = isSelected }
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
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
          style = MaterialTheme.typography.titleMediumEmphasized,
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
        // The selected card turns secondaryContainer, so the tile switches to
        // primaryContainer to stay visible against it.
        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
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
        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
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
          contentDescription = stringResource(R.string.card_cd_selected),
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
        text = pluralStringResource(R.plurals.card_video_count, folder.videoCount, folder.videoCount),
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

/**
 * Turns "/storage/emulated/0/Anime/Frieren" (folder "Frieren") into "Internal › Anime".
 * Paths that aren't under /storage are shown as-is, minus the folder name.
 */
private fun shortenPath(
  path: String,
  name: String,
  internalLabel: String,
  sdCardLabel: String,
): String {
  val parent = path.removeSuffix("/").removeSuffix(name).removeSuffix("/")
  if (parent.isEmpty()) return ""

  val internalRoot = "/storage/emulated/0"
  val (rootLabel, rest) = when {
    parent == internalRoot || parent.startsWith("$internalRoot/") ->
      internalLabel to parent.removePrefix(internalRoot)
    parent.startsWith("/storage/") -> {
      val afterStorage = parent.removePrefix("/storage/")
      sdCardLabel to afterStorage.substringAfter('/', "")
    }
    else -> return parent
  }

  return (listOf(rootLabel) + rest.split('/'))
    .filter { it.isNotBlank() }
    .joinToString(" › ")
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
