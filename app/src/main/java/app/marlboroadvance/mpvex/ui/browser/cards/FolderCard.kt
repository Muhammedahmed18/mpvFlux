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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import java.text.SimpleDateFormat
import java.util.Calendar
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
 * Shared look for the browser cards (folders, playlists and videos) so selection,
 * press feedback and spacing behave the same everywhere.
 */
internal object BrowserCardDefaults {
  val ContentPadding = 12.dp
  val ThumbnailGap = 14.dp
  const val PressedScale = 0.98f

  /** Selected cards get a solid tonal fill + check badge instead of border/shrink/tint stacking. */
  @Composable
  fun containerColor(isSelected: Boolean): Color =
    if (isSelected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerLow
    }

  @Composable
  fun pressScale(interactionSource: MutableInteractionSource, label: String): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
      targetValue = if (isPressed) PressedScale else 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
      label = label,
    )
    return scale
  }
}

/** Check badge shown on a selected card's thumbnail/icon (bottom-end). */
@Composable
internal fun SelectionCheckBadge(
  visible: Boolean,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + scaleIn(
      spring(dampingRatio = Spring.DampingRatioNoBouncy),
      transformOrigin = TransformOrigin(1f, 1f),
    ),
    exit = fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + scaleOut(
      spring(dampingRatio = Spring.DampingRatioNoBouncy),
      transformOrigin = TransformOrigin(1f, 1f),
    ),
    modifier = modifier,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primary,
      shape = CircleShape,
      modifier = Modifier.size(22.dp),
      border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
    ) {
      Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = stringResource(R.string.card_cd_selected),
        modifier = Modifier.padding(3.dp),
        tint = MaterialTheme.colorScheme.onPrimary,
      )
    }
  }
}

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
  val cardScale = BrowserCardDefaults.pressScale(interactionSource, "folder_card_scale")

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
    color = BrowserCardDefaults.containerColor(isSelected),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(BrowserCardDefaults.ContentPadding),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FolderIconContainer(
        isSelected = isSelected,
        customIcon = customIcon,
        onThumbClick = onThumbClick,
        onLongClick = onLongClick,
      )

      Spacer(modifier = Modifier.width(BrowserCardDefaults.ThumbnailGap))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        FolderMetadataRow(folder, settings, customChipContent)
      }
    }
  }
}

@Composable
private fun FolderIconContainer(
  isSelected: Boolean,
  customIcon: ImageVector?,
  onThumbClick: () -> Unit,
  onLongClick: (() -> Unit)?,
) {
  // The selected card itself turns secondaryContainer, so the icon tile switches to
  // primaryContainer to stay visible against it.
  val tileColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
  val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

  Box(modifier = Modifier.size(56.dp)) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(MaterialTheme.shapes.large)
        .background(tileColor)
        .combinedClickable(
          onClick = onThumbClick,
          onLongClick = onLongClick,
          indication = ripple(bounded = true),
          interactionSource = remember { MutableInteractionSource() },
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = customIcon ?: Icons.Filled.Folder,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = iconTint,
      )
    }

    SelectionCheckBadge(
      visible = isSelected,
      modifier = Modifier.align(Alignment.BottomEnd),
    )
  }
}

/**
 * One accent chip for the video count (plus any custom chip), followed by the passive
 * attributes on a single muted dot-separated line — the same pattern VideoCard uses —
 * so a folder card stays two rows shorter than the old four-chip FlowRow.
 */
@Composable
private fun FolderMetadataRow(
  folder: VideoFolder,
  settings: FolderCardSettings,
  customChipContent: (@Composable () -> Unit)?,
) {
  val showCount = settings.showTotalVideosChip && folder.videoCount > 0
  val countLabel = if (showCount) {
    pluralStringResource(R.plurals.card_video_count, folder.videoCount, folder.videoCount)
  } else {
    null
  }

  val attributes = buildList {
    if (settings.showTotalSizeChip && folder.totalSize > 0) add(formatFileSize(folder.totalSize))
    if (settings.showTotalDurationChip && folder.totalDuration > 0) add(formatDuration(folder.totalDuration))
    if (settings.showDateChip && folder.lastModified > 0) add(formatCardDate(folder.lastModified))
  }

  if (countLabel == null && customChipContent == null && attributes.isEmpty()) return

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    countLabel?.let { UnifiedChip(text = it, isAccent = true, compact = true) }
    customChipContent?.invoke()
    if (attributes.isNotEmpty()) {
      Text(
        text = attributes.joinToString("  ·  "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
      )
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
  val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
  return String.format(Locale.getDefault(), "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

private fun formatDuration(durationMs: Long): String {
  val hours = durationMs / 3_600_000
  val minutes = (durationMs % 3_600_000) / 60_000
  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m"
    else -> "<1m"
  }
}

private val SHORT_DATE_FORMATTER by lazy { SimpleDateFormat("MMM d", Locale.getDefault()) }
private val FULL_DATE_FORMATTER by lazy { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

/**
 * Card date shared by folder and video cards: "Jun 26" for this year,
 * "Jun 26, 2025" otherwise, so older files are never ambiguous.
 */
internal fun formatCardDate(timestampSeconds: Long): String {
  val date = Date(timestampSeconds * 1000)
  val calendar = Calendar.getInstance()
  val currentYear = calendar.get(Calendar.YEAR)
  calendar.time = date
  return if (calendar.get(Calendar.YEAR) == currentYear) {
    SHORT_DATE_FORMATTER.format(date)
  } else {
    FULL_DATE_FORMATTER.format(date)
  }
}
