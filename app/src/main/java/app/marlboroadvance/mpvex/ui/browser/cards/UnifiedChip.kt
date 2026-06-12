package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UnifiedChip(
  text: String,
  isAccent: Boolean = false,
  isPrimary: Boolean = false,
  isOutlined: Boolean = false,
  compact: Boolean = false,
) {
  val containerColor = when {
    isOutlined -> Color.Transparent
    isPrimary -> MaterialTheme.colorScheme.primaryContainer
    isAccent -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surfaceContainerHighest
  }
  val labelColor = when {
    isPrimary -> MaterialTheme.colorScheme.onPrimaryContainer
    isAccent -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  val border = if (isOutlined) {
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  } else null

  val horizontalPadding = if (compact) 8.dp else 12.dp
  val verticalPadding = if (compact) 3.dp else 6.dp
  val textStyle = if (compact) {
    MaterialTheme.typography.labelSmall
  } else {
    MaterialTheme.typography.labelLarge
  }

  Surface(
    color = containerColor,
    contentColor = labelColor,
    shape = MaterialTheme.shapes.extraLarge,
    border = border,
    tonalElevation = if (isOutlined) 0.dp else 1.dp,
  ) {
    Text(
      text = text,
      style = textStyle,
      modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
    )
  }
}

@Composable
fun SubtitleChip(text: String) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = MaterialTheme.shapes.extraLarge,
    tonalElevation = 1.dp,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
      ),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}
