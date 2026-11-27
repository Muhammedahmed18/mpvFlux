package app.marlboroadvance.mpvex.ui.browser.cards

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder

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
  // Sizes for this screen, shared with video rows. Lists can compute this once.
  dimens: BrowserCardDimens = rememberBrowserCardDimens(),
) {
  val internalLabel = stringResource(R.string.card_storage_internal)
  val externalLabel = stringResource(R.string.generic_external)
  val displayPath = remember(folder.path, folder.name, internalLabel, externalLabel) {
    shortenPath(folder.path, folder.name, internalLabel, externalLabel)
  }

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val cardScale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "folder_card_scale",
  )

  // One transition drives every selection colour (row tint, tile, icon tint).
  val selectionTransition = updateTransition(targetState = isSelected, label = "folder_selection")

  // No container, same as the video rows: folders sit directly on the screen.
  // Selection adds a soft tonal highlight behind the row (plus the tile turning into a check).
  // Only the alpha is animated (never colour from Transparent, which blends through black and
  // flickers on light themes). It is read while drawing, so the row doesn't recompose.
  val rowTintAlpha by selectionTransition.animateFloat(
    transitionSpec = { tween(durationMillis = 150) },
    label = "folder_row_tint",
  ) { selected -> if (selected) 0.5f else 0f }
  val rowTintColor = MaterialTheme.colorScheme.primaryContainer
  val shape = MaterialTheme.shapes.large

  Surface(
    modifier = modifier
      .fillMaxWidth()
      // Read in the graphics layer, so the press animation doesn't recompose the row.
      .graphicsLayer {
        scaleX = cardScale
        scaleY = cardScale
      }
      .clip(shape)
      .drawBehind { drawRect(rowTintColor, alpha = rowTintAlpha) }
      .semantics { selected = isSelected }
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = shape,
    color = Color.Transparent,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(dimens.rowPadding),
      verticalAlignment = Alignment.Top,
    ) {
      FolderIconContainer(
        isSelected = isSelected,
        selectionTransition = selectionTransition,
        customIcon = customIcon,
        onThumbClick = onThumbClick,
        onLongClick = onLongClick,
        dimens = dimens,
      )

      Spacer(modifier = Modifier.width(dimens.thumbTextGap))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(dimens.detailsSpacing),
      ) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
        )

        if (settings.showFolderPath && displayPath.isNotEmpty()) {
          Text(
            text = displayPath,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = dimens.folderPathMaxLines,
            overflow = TextOverflow.Ellipsis,
          )
        }

        FolderMetadataChips(folder, settings, customChipContent, dimens)
      }
    }
  }
}

@Composable
private fun FolderIconContainer(
  isSelected: Boolean,
  selectionTransition: Transition<Boolean>,
  customIcon: ImageVector?,
  onThumbClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  dimens: BrowserCardDimens,
) {
  // When selected, the whole tile becomes the tick: it fills with the accent
  // colour and the folder icon swaps to a check, instead of a badge on top.
  val tileColor by selectionTransition.animateColor(
    transitionSpec = { tween(durationMillis = 150) },
    label = "folder_tile_color",
  ) { selected ->
    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
  }
  val iconTint by selectionTransition.animateColor(
    transitionSpec = { tween(durationMillis = 150) },
    label = "folder_tile_icon_tint",
  ) { selected ->
    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
  }
  val selectLabel = stringResource(R.string.select)

  Box(
    modifier = Modifier
      .size(dimens.folderTileSize)
      .clip(MaterialTheme.shapes.large)
      .drawBehind { drawRect(tileColor) }
      .combinedClickable(
        onClick = onThumbClick,
        onClickLabel = selectLabel,
        role = Role.Button,
        // Long press on the tile behaves like long press on the row.
        onLongClick = onLongClick,
        indication = ripple(bounded = true),
        interactionSource = remember { MutableInteractionSource() },
      ),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedContent(
      targetState = isSelected,
      transitionSpec = {
        val enter = fadeIn(tween(durationMillis = 150)) + scaleIn(
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
          ),
          initialScale = 0.6f,
        )
        val exit = fadeOut(tween(durationMillis = 90)) + scaleOut(
          animationSpec = tween(durationMillis = 90),
          targetScale = 0.6f,
        )
        enter togetherWith exit
      },
      contentAlignment = Alignment.Center,
      label = "folder_tile_icon",
    ) { selected ->
      Icon(
        imageVector = if (selected) Icons.Filled.Check else customIcon ?: Icons.Filled.Folder,
        // The row itself already reports "selected" to screen readers.
        contentDescription = null,
        modifier = Modifier.size(dimens.folderIconSize),
        tint = iconTint,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderMetadataChips(
  folder: VideoFolder,
  settings: FolderCardSettings,
  customChipContent: (@Composable () -> Unit)?,
  dimens: BrowserCardDimens,
) {
  val context = LocalContext.current

  // Formatted once per value instead of on every recomposition.
  val sizeText = remember(folder.totalSize, settings.showTotalSizeChip) {
    if (settings.showTotalSizeChip && folder.totalSize > 0) {
      Formatter.formatShortFileSize(context, folder.totalSize)
    } else {
      null
    }
  }
  val durationText = remember(folder.totalDuration, settings.showTotalDurationChip) {
    if (settings.showTotalDurationChip && folder.totalDuration > 0) {
      formatDuration(context, folder.totalDuration)
    } else {
      null
    }
  }
  val dateText = remember(folder.lastModified, settings.showDateChip) {
    if (settings.showDateChip && folder.lastModified > 0) {
      formatDate(context, folder.lastModified)
    } else {
      null
    }
  }

  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(dimens.chipSpacing),
    verticalArrangement = Arrangement.spacedBy(dimens.chipSpacing),
  ) {
    if (settings.showTotalVideosChip && folder.videoCount > 0) {
      UnifiedChip(
        text = pluralStringResource(R.plurals.card_video_count, folder.videoCount, folder.videoCount),
        isAccent = true,
        size = dimens.chipSize,
      )
    }

    customChipContent?.invoke()

    if (sizeText != null) {
      UnifiedChip(text = sizeText, size = dimens.chipSize)
    }

    if (durationText != null) {
      UnifiedChip(text = durationText, size = dimens.chipSize)
    }

    if (dateText != null) {
      UnifiedChip(text = dateText, size = dimens.chipSize)
    }
  }
}

/**
 * Turns "/storage/emulated/0/Anime/Frieren" (folder "Frieren") into "Internal › Anime".
 * Any other volume under /storage (SD card, USB drive) is shown as [externalLabel].
 * Paths that aren't under /storage are shown as-is, minus the folder name.
 */
private fun shortenPath(
  path: String,
  name: String,
  internalLabel: String,
  externalLabel: String,
): String {
  val parent = path.removeSuffix("/").removeSuffix(name).removeSuffix("/")
  if (parent.isEmpty()) return ""
  if (!parent.startsWith("/storage/")) return parent

  // "emulated/0/Anime" -> volume "emulated", rest "0/Anime".
  val afterStorage = parent.removePrefix("/storage/")
  val volume = afterStorage.substringBefore('/')
  val rest = afterStorage.substringAfter('/', "")

  val (rootLabel, folders) = if (volume == "emulated") {
    // Internal storage of any user (/storage/emulated/0, /storage/emulated/10, ...):
    // the first segment is the user id, not a folder.
    internalLabel to rest.substringAfter('/', "")
  } else {
    externalLabel to rest
  }

  return (listOf(rootLabel) + folders.split('/'))
    .filter { it.isNotBlank() }
    .joinToString(" › ")
}

private fun formatDuration(context: Context, duration: Long): String {
  val hours = duration / 3_600_000
  val minutes = (duration % 3_600_000) / 60_000
  return when {
    hours > 0 -> context.getString(R.string.card_duration_hours_minutes, hours, minutes)
    minutes > 0 -> context.getString(R.string.card_duration_minutes, minutes)
    else -> context.getString(R.string.card_duration_less_than_minute)
  }
}

private fun formatDate(context: Context, timestampSeconds: Long): String {
  return DateUtils.formatDateTime(
    context,
    timestampSeconds * 1000,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
  )
}
