package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Chip sizes. At the default font size each matches the old fixed height; larger fonts make it taller. */
enum class UnifiedChipSize { Compact, Medium, Large }

@Composable
fun UnifiedChip(
  text: String,
  isAccent: Boolean = false,
  isPrimary: Boolean = false,
  isOutlined: Boolean = false,
  compact: Boolean = false,
  // Filled with the theme's tertiary colour (used for the subtitles chip).
  isTertiary: Boolean = false,
  bold: Boolean = false,
  size: UnifiedChipSize = if (compact) UnifiedChipSize.Compact else UnifiedChipSize.Large,
) {
  val colors = MaterialTheme.colorScheme
  val containerColor = when {
    isOutlined -> Color.Transparent
    isTertiary -> colors.tertiary
    isPrimary -> colors.primaryContainer
    isAccent -> colors.secondaryContainer
    else -> colors.surfaceContainerHighest
  }
  val labelColor = when {
    isTertiary && !isOutlined -> colors.onTertiary
    isPrimary -> colors.onPrimaryContainer
    isAccent -> colors.onSecondaryContainer
    else -> colors.onSurfaceVariant
  }
  val border = if (isOutlined) {
    BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
  } else null

  // A minimum height (instead of fixed vertical padding) keeps today's look at 1x font
  // and lets the chip grow with the text when the user picks a bigger font.
  val horizontalPadding = when (size) {
    UnifiedChipSize.Compact -> 8.dp
    UnifiedChipSize.Medium -> 10.dp
    UnifiedChipSize.Large -> 12.dp
  }
  val minHeight = when (size) {
    UnifiedChipSize.Compact -> 22.dp
    UnifiedChipSize.Medium -> 24.dp
    UnifiedChipSize.Large -> 32.dp
  }
  val textStyle = when (size) {
    UnifiedChipSize.Compact -> MaterialTheme.typography.labelSmall
    UnifiedChipSize.Medium -> MaterialTheme.typography.labelMedium
    UnifiedChipSize.Large -> MaterialTheme.typography.labelLarge
  }

  Surface(
    color = containerColor,
    contentColor = labelColor,
    shape = MaterialTheme.shapes.extraLarge,
    border = border,
  ) {
    Box(
      modifier = Modifier
        .heightIn(min = minHeight)
        .padding(horizontal = horizontalPadding, vertical = 2.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = text,
        style = textStyle,
        fontWeight = if (bold) FontWeight.SemiBold else null,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
