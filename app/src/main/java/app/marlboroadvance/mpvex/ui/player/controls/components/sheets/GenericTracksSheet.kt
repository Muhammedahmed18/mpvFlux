package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

enum class MetadataType {
  DEFAULT, PRIMARY, WARNING
}

data class TrackMetadata(
  val text: String,
  val type: MetadataType = MetadataType.DEFAULT
)

@Composable
fun getTrackTitle(track: TrackNode): String {
  val title = if (track.external == true) {
    track.title?.substringBeforeLast(".")
  } else {
    track.title
  }
  
  return if (!title.isNullOrBlank()) {
    title
  } else {
    if (track.type == "audio") {
      stringResource(R.string.player_sheets_chapter_title_substitute_audio, track.id)
    } else {
      stringResource(R.string.player_sheets_chapter_title_substitute_subtitle, track.id)
    }
  }
}

@Composable
fun <T> GenericTracksSheet(
  tracks: ImmutableList<T>,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  lazyListState: LazyListState? = null,
  customMaxWidth: androidx.compose.ui.unit.Dp? = null,
  header: @Composable () -> Unit = {},
  track: @Composable (T) -> Unit = {},
  footer: @Composable () -> Unit = {},
) {
  val listState = lazyListState ?: rememberLazyListState()
  val configuration = LocalConfiguration.current
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  
  val calculatedMaxWidth = customMaxWidth ?: if (configuration.orientation == ORIENTATION_PORTRAIT) {
    640.dp 
  } else {
    560.dp
  }

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    customMaxWidth = calculatedMaxWidth,
    surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f)
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium))
    ) {
      header()
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f, fill = false),
        contentPadding = PaddingValues(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.small
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(tracks) {
          track(it)
        }
      }
      
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium)
      ) {
        footer()
      }
    }
  }
}

@Composable
fun TrackHeaderPill(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(top = 12.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
        shape = CircleShape,
        border = BorderStroke(
            0.5.dp, 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackSelectableBar(
  id: Int,
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  metadata: List<TrackMetadata> = emptyList(),
  trailingContent: @Composable (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
  }

  val borderColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
  } else {
    Color.Transparent
  }

  Surface(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = id.toString().padStart(2, '0'),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        fontWeight = FontWeight.Black,
        modifier = Modifier.width(18.dp)
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        
        if (metadata.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            metadata.forEach { data ->
              TrackMetadataTag(data, isSelected)
            }
          }
        }
      }

      Box(contentAlignment = Alignment.Center) {
          if (isSelected) {
              Box(
                  modifier = Modifier
                      .size(28.dp)
                      .background(MaterialTheme.colorScheme.primary, CircleShape)
                      .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                  contentAlignment = Alignment.Center
              ) {
                  Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(18.dp)
                  )
              }
          } else if (trailingContent == null) {
              Box(
                  modifier = Modifier
                      .size(28.dp)
                      .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
              )
          }
      }

      if (trailingContent != null) {
        trailingContent()
      }
    }
  }
}

@Composable
fun TrackMetadataTag(
  metadata: TrackMetadata,
  isSelected: Boolean
) {
  val backgroundColor = when (metadata.type) {
    MetadataType.PRIMARY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    MetadataType.WARNING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    MetadataType.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
  }

  val contentColor = when (metadata.type) {
    MetadataType.PRIMARY -> MaterialTheme.colorScheme.primary
    MetadataType.WARNING -> MaterialTheme.colorScheme.error
    MetadataType.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
  }

  Surface(
    color = backgroundColor,
    shape = CircleShape,
    modifier = Modifier.border(
        width = 0.5.dp,
        color = contentColor.copy(alpha = 0.1f),
        shape = CircleShape
    )
  ) {
    Text(
      text = metadata.text.uppercase(),
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
      fontWeight = FontWeight.Bold,
      color = if (isSelected && metadata.type == MetadataType.DEFAULT) MaterialTheme.colorScheme.primary else contentColor
    )
  }
}

data class TrackAction(
  val label: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
)

@Composable
fun TrackActionsRow(
  actions: List<TrackAction>,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = MaterialTheme.spacing.smaller),
    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items(actions) { action ->
      AssistChip(
        onClick = action.onClick,
        label = {
          Text(
            text = action.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
        },
        leadingIcon = {
          Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
          labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
          leadingIconContentColor = MaterialTheme.colorScheme.primary,
        ),
        border = null,
      )
    }
  }
}

@Composable
fun AddTrackRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
  ) {
    FilledTonalButton(
      onClick = onClick,
      modifier = Modifier.weight(1f),
      shape = MaterialTheme.shapes.extraLarge,
      contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium, vertical = 12.dp)
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      actions()
    }
  }
}
