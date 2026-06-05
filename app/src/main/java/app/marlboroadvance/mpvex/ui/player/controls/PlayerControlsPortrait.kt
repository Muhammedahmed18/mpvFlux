package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.theme.spacing
import dev.vivvvek.seeker.Segment

@Composable
fun TopPlayerControlsPortrait(
  mediaTitle: String?,
  hideBackground: Boolean,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      // spacing.medium (16dp) from the spacing token — consistent with landscape rows
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
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
        buttonSize = 48.dp,
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
        modifier = Modifier.weight(1f, fill = false),
      )
    }
  }
}

@Composable
fun BottomPlayerControlsPortrait(
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
  if (buttons.isEmpty()) return
  // Per-button backgrounds — each button carries its own glass chip via
  // glassIconButtonColors(hideBackground). LazyRow keeps lazy composition.
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = MaterialTheme.spacing.large),
    horizontalArrangement = Arrangement.spacedBy(
      space     = MaterialTheme.spacing.medium,
      alignment = Alignment.CenterHorizontally,
    ),
    verticalAlignment  = Alignment.CenterVertically,
    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
  ) {
    items(
      items = buttons,
      key   = { button -> button.name },
    ) { button ->
      RenderPlayerButton(
        button = button,
        chapters = chapters,
        currentChapter = currentChapter,
        isSpeedNonOne = isSpeedNonOne,
        currentZoom = currentZoom,
        mediaTitle = mediaTitle,
        hideBackground = hideBackground,
        onBackPress = onBackPress,
        onOpenSheet = onOpenSheet,
        viewModel = viewModel,
        activity = activity,
        decoder = decoder,
        playbackSpeed = playbackSpeed,
        buttonSize = 48.dp,
      )
    }
  }
}
