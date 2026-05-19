package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.network.NetworkFile

/**
 * M3 Expressive Network Folder Card
 * Consistent with local FolderCard for a unified media player experience.
 */
@Composable
fun NetworkFolderCard(
  file: NetworkFile,
  settings: FolderCardSettings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
) {
  val maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val cardScale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.96f
      isSelected -> 0.98f
      else -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "network_folder_card_scale"
  )

  val containerColor = when {
    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    isPressed -> MaterialTheme.colorScheme.surfaceContainerHigh
    else -> MaterialTheme.colorScheme.surfaceContainerLow
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .scale(cardScale)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
    tonalElevation = if (isSelected) 4.dp else 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(MaterialTheme.shapes.large)
          .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Filled.Folder,
          contentDescription = "Folder",
          modifier = Modifier.size(32.dp),
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      
      Spacer(modifier = Modifier.width(16.dp))
      
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          file.name,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )
        
        if (file.path.isNotEmpty()) {
            Text(
                text = file.path,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
      }
    }
  }
}
