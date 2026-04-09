package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A minimalist icon for preference items. 
 * Updated with a colored rounded square background for One UI 7 / Material 3 Expressive style.
 */
@Composable
fun PreferenceIcon(
  imageVector: ImageVector,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
  tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
) {
  Box(
    modifier = modifier
      .padding(end = 16.dp) // Expressive Spacing: Added breathing space between icon and text
      .size(40.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(24.dp), // Keeps a standard hit area but light visual weight
    )
  }
}

/**
 * A grouped container for preference items.
 * Updated to use a Surface with rounded corners and a container color to match One UI 7 grouped style.
 */
@Composable
fun PreferenceCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 6.dp),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      content()
    }
  }
}

/**
 * A subtle divider to separate preferences.
 * Reduced padding to align with the new grouped card edges.
 */
@Composable
fun PreferenceDivider(
  modifier: Modifier = Modifier,
) {
  HorizontalDivider(
    modifier = modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    thickness = 0.5.dp,
  )
}

/**
 * A modern section header using Material 3 Expressive style.
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
    modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
  )
}
