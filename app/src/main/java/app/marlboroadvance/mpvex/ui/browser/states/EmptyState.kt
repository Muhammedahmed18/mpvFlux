package app.marlboroadvance.mpvex.ui.browser.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
  val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 48.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Icon Container with Glow
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(140.dp) // Reduced from 160dp
        .drawBehind {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(glowColor, Color.Transparent),
              center = Offset(size.width / 2f, size.height / 2f),
              radius = size.width / 2f,
            ),
          )
        },
    ) {
      Surface(
        modifier = Modifier.size(80.dp), // Slightly smaller, standard M3 feel
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null, // Title is already read by Text below
          modifier = Modifier.padding(20.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    // Spacing: (140 - 80) / 2 = 30dp intrinsic padding. 
    // To get a ~24dp visual gap, we use a small offset or no spacer.
    // Here we use 0.dp because the Box already provides 30dp of empty space.
    Spacer(modifier = Modifier.height(0.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(8.dp)) // Standard M3 spacing

    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (action != null) {
      Spacer(modifier = Modifier.height(32.dp))
      action()
    }
  }
}
