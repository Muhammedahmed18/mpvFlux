package app.marlboroadvance.mpvex.ui.browser.states

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "empty_state")

  // Combined scale + alpha for a more alive, M3-compliant motion feel
  val animatedScale by infiniteTransition.animateFloat(
    initialValue = 0.97f,
    targetValue = 1.00f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(2500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "icon_scale",
  )
  val animatedAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(2500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "icon_alpha",
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 48.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {

    // Layered icon container: decorative ring behind the main surface
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .scale(animatedScale)
        .alpha(animatedAlpha),
    ) {
      // Outer decorative ring
      Surface(
        modifier = Modifier.size(120.dp),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
      ) {}

      // Inner icon surface
      Surface(
        modifier = Modifier.size(96.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          modifier = Modifier.padding(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Title — upgraded to headlineSmall for stronger presence
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Message
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
    )

    // Optional action slot (e.g. FilledTonalButton)
    if (action != null) {
      Spacer(modifier = Modifier.height(24.dp))
      action()
    }
  }
}
