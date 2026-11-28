package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.preferences.icon
import app.marlboroadvance.mpvex.preferences.getPlayerButtonLabel

/**
 * A simple "Quick Settings" style chip for a player button.
 * Renders text or icons based on the button type.
 */
/**
 * Tile for one player button in Settings → Player layout. Uses the same icon as the player
 * ([PlayerButton.icon]). [badgeIcon] sits on the bottom-right corner (add / remove);
 * [elevation] lifts the tile while it is dragged.
 */
@Composable
fun PlayerButtonChip(
  button: PlayerButton,
  enabled: Boolean,
  onClick: (() -> Unit)? = null,
  badgeIcon: ImageVector? = null,
  badgeColor: Color? = null,
  elevation: Dp = 0.dp,
  compact: Boolean = false,
) {
  val label = getPlayerButtonLabel(button) // Kept for accessibility
  val shape = MaterialTheme.shapes.large
  // Compact chips are used in the layout overview, where no badge is shown.
  val outerPadding = if (compact) 0.dp else 6.dp
  val minSize = if (compact) 46.dp else 56.dp
  val horizontalPadding = if (compact) 10.dp else 12.dp
  val iconSize = if (compact) 20.dp else 24.dp
  val textSize = if (compact) 13.sp else 15.sp

  Box(
    // Room around the tile so the corner badge is never cut off.
    modifier = Modifier.padding(outerPadding),
  ) {
    Card(
      modifier = Modifier,
      shape = shape,
      // A brighter tile plus a thin outline keeps the edges visible on dark and black themes.
      border = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else 0.4f),
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = elevation),
      colors =
        CardDefaults.cardColors(
          containerColor =
            if (enabled) {
              MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
            },
          contentColor =
            if (enabled) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
        ),
      onClick = { onClick?.invoke() },
      enabled = enabled && onClick != null,
    ) {
      Box(
        modifier =
          Modifier
            .defaultMinSize(minWidth = minSize, minHeight = minSize)
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        when (button) {
          PlayerButton.VIDEO_TITLE -> {
            Text(
              text = stringResource(R.string.player_button_video_title),
              fontSize = textSize,
              textAlign = TextAlign.Center,
              lineHeight = 14.sp,
            )
          }
          PlayerButton.CURRENT_CHAPTER -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = button.icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize),
              )
              Text(
                text = stringResource(R.string.player_button_chapter_preview),
                fontSize = textSize,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
              )
            }
          }
          else -> {
            // Default: Icon only
            Icon(
              imageVector = button.icon,
              contentDescription = label,
              modifier = Modifier.size(iconSize).then(
                if (button == PlayerButton.VERTICAL_FLIP) Modifier.rotate(90f) else Modifier
              ),
            )
          }
        }
      }
    }

    // Badge on the tile's bottom-right corner, with a ring in the card colour behind it.
    if (badgeIcon != null && badgeColor != null) {
      Icon(
        imageVector = badgeIcon,
        contentDescription = null, // Decorative
        tint = badgeColor,
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 4.dp, y = 4.dp)
            .size(22.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
            .padding(1.dp),
      )
    }
  }
}
