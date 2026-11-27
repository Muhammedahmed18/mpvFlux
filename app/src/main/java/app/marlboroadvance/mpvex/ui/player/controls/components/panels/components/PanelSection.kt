package app.marlboroadvance.mpvex.ui.player.controls.components.panels.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.theme.spacing

/**
 * Labeled group for player panels: a small title above a solid tonal surface.
 *
 * The surface is intentionally solid (not the translucent panel card colors) so dense
 * controls stay readable over the video behind the sheet.
 *
 * @param title Optional label shown above the surface
 * @param icon Optional icon shown before the title
 */
@Composable
fun PanelSection(
  modifier: Modifier = Modifier,
  title: String? = null,
  icon: ImageVector? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    if (title != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
          start = MaterialTheme.spacing.small,
          bottom = MaterialTheme.spacing.extraSmall,
        ),
      ) {
        if (icon != null) {
          Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
              .padding(end = MaterialTheme.spacing.smaller)
              .size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(content = content)
    }
  }
}
