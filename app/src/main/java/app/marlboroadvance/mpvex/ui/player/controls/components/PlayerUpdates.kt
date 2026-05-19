package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.theme.spacing

@Composable
fun PlayerUpdate(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
) {
  Surface(
    shape = CircleShape,
    color = Color.Black.copy(alpha = 0.5f),
    contentColor = Color.White,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    modifier = modifier
      .height(45.dp)
      .animateContentSize(),
  ) {
    Box(
      modifier = Modifier.padding(
        vertical = MaterialTheme.spacing.small,
        horizontal = MaterialTheme.spacing.medium,
      ),
      contentAlignment = Alignment.Center,
    ) {
      content()
    }
  }
}

@Composable
fun TextPlayerUpdate(
  text: String,
  modifier: Modifier = Modifier,
) {
  PlayerUpdate(modifier) {
    Text(
      text = text,
      textAlign = TextAlign.Center,
      color = Color.White,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
fun MultipleSpeedPlayerUpdate(
  currentSpeed: Float,
  modifier: Modifier = Modifier,
) {
  CompactSpeedIndicator(currentSpeed = currentSpeed, modifier = modifier)
}

@Composable
fun SeekPlayerUpdate(
  currentTime: String,
  seekDelta: String,
  modifier: Modifier = Modifier,
) {
  PlayerUpdate(modifier) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "$currentTime ($seekDelta)",
        textAlign = TextAlign.Center,
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
