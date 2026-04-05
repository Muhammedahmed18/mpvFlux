package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.material3.ripple
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject

/**
 * Card for displaying M3U/M3U8 playlist items (streaming URLs)
 * Shows simple layout without thumbnail since no metadata is available
 */
@Composable
fun M3UVideoCard(
  title: String,
  url: String,
  settings: VideoCardSettings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  isRecentlyPlayed: Boolean = false,
) {
  val maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    label = "scale"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
        indication = ripple(
          bounded = true,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
      )
      .then(
        if (isSelected) Modifier.border(
          width = 2.dp,
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(20.dp)
        ) else Modifier
      ),
    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(20.dp),
    tonalElevation = if (isSelected) 4.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Square placeholder with streaming icon
      Box(
        modifier =
          Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
              shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center,
      ) {
        // Play icon overlay
        Icon(
          Icons.Filled.PlayArrow,
          contentDescription = "Play",
          modifier = Modifier.size(36.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
      ) {
        Text(
          title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            lineHeight = 20.sp
          ),
          color = if (isRecentlyPlayed) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Show URL like a file path
        Text(
          url,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 0.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}


