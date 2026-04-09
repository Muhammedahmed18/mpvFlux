package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.VideoAspect
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButton
import app.marlboroadvance.mpvex.ui.theme.controlColor
import app.marlboroadvance.mpvex.ui.theme.spacing
import dev.vivvvek.seeker.Segment

@Composable
fun TopLeftPlayerControlsLandscape(
  mediaTitle: String?,
  hideBackground: Boolean,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
) {
  val playlistModeEnabled = viewModel.hasPlaylistSupport()
  val clickEvent = LocalPlayerButtonsClickEvent.current

  Row(
    modifier = Modifier.width(IntrinsicSize.Max),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    ControlsButton(
      icon = Icons.AutoMirrored.Default.ArrowBack,
      onClick = onBackPress,
      color = controlColor,
      modifier = Modifier.size(48.dp),
    )

    Box(
      modifier =
        Modifier
          .height(48.dp)
          .clip(CircleShape)
          .clickable(
            enabled = playlistModeEnabled,
            onClick = {
              clickEvent()
              onOpenSheet(Sheets.Playlist)
            },
          ),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        modifier =
          Modifier.padding(
            horizontal = 8.dp,
            vertical = 8.dp,
          ),
      ) {
        val textShadow = Shadow(
          color = Color.Black.copy(alpha = 0.6f),
          offset = Offset(0f, 2f),
          blurRadius = 4f
        )

        viewModel.getPlaylistInfo()?.let { playlistInfo ->
          Text(
            text = playlistInfo,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(
              shadow = textShadow,
              fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "|",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
            maxLines = 1,
            color = controlColor.copy(alpha = 0.5f),
            overflow = TextOverflow.Clip,
          )
        }
        Text(
          text = mediaTitle ?: "",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleMedium.copy(
            shadow = textShadow,
            fontWeight = FontWeight.SemiBold
          ),
          color = controlColor,
          modifier = Modifier.weight(1f, fill = false),
        )
      }
    }
  }
}

@Composable
fun TopRightPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isPortrait = false,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        aspect = aspect,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        onOpenPanel = onOpenPanel,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 48.dp,
      )
    }
  }
}

@Composable
fun BottomRightPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isPortrait = false,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        aspect = aspect,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        onOpenPanel = onOpenPanel,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 48.dp,
      )
    }
  }
}

@Composable
fun BottomLeftPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isPortrait = false,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        aspect = aspect,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        onOpenPanel = onOpenPanel,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 48.dp,
      )
    }
  }
}
