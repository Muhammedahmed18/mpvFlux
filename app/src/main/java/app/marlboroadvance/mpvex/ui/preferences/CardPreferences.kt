package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A circular icon container for preference items, mimicking modern Android settings UI.
 */
@Composable
fun PreferenceIcon(
  imageVector: ImageVector,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
  tint: Color = MaterialTheme.colorScheme.primary,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
) {
  Box(
    modifier = modifier
      .size(40.dp)
      .background(color = containerColor, shape = CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(24.dp),
    )
  }
}

/**
 * A card container for grouping related preferences, mimicking modern Android settings UI.
 */
@Composable
fun PreferenceCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 0.dp,
    ),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 4.dp), // Minimal top/bottom padding for card container
      verticalArrangement = Arrangement.spacedBy(0.dp), // Let preference items control their own spacing
    ) {
      content()
    }
  }
}

/**
 * A divider to separate preferences within a card.
 */
@Composable
fun PreferenceDivider(
  modifier: Modifier = Modifier,
) {
  HorizontalDivider(
    modifier = modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
  )
}

/**
 * A section header for preferences, displayed outside cards.
 */
@Composable
fun PreferenceSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier.padding(horizontal = 32.dp, vertical = 16.dp),
  )
}
