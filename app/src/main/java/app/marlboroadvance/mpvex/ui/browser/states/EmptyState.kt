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
  val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 48.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {

    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(160.dp)
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
        modifier = Modifier.size(88.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          modifier = Modifier.padding(22.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    Spacer(modifier = Modifier.height(40.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (action != null) {
      Spacer(modifier = Modifier.height(28.dp))
      action()
    }
  }
}
