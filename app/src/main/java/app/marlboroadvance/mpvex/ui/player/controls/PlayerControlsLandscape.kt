package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import dev.vivvvek.seeker.Segment

@Composable
fun TopLeftPlayerControlsLandscape(
  mediaTitle: String?,
  hideBackground: Boolean,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(20.dp), // Increased spacing for Expressive UI
  ) {
    RenderPlayerButton(
      button = PlayerButton.BACK_ARROW,
      chapters = emptyList(),
      currentChapter = null,
      isSpeedNonOne = false,
      currentZoom = 1f,
      mediaTitle = mediaTitle,
      hideBackground = hideBackground,
      decoder = app.marlboroadvance.mpvex.ui.player.Decoder.Auto,
      playbackSpeed = 1f,
      onBackPress = onBackPress,
      onOpenSheet = onOpenSheet,
      viewModel = viewModel,
      activity = activity,
    )

    RenderPlayerButton(
      button = PlayerButton.VIDEO_TITLE,
      chapters = emptyList(),
      currentChapter = null,
      isSpeedNonOne = false,
      currentZoom = 1f,
      mediaTitle = mediaTitle,
      hideBackground = hideBackground,
      decoder = app.marlboroadvance.mpvex.ui.player.Decoder.Auto,
      playbackSpeed = 1f,
      onBackPress = onBackPress,
      onOpenSheet = onOpenSheet,
      viewModel = viewModel,
      activity = activity,
      modifier = Modifier.weight(1f, fill = false)
    )
  }
}

@Composable
fun TopRightPlayerControlsLandscape(
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
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
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
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
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
  ) {
    buttons.forEach { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        viewModel = viewModel,
        activity = activity,
        buttonSize = 48.dp,
      )
    }
  }
}
