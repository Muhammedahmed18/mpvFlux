package app.marlboroadvance.mpvex.ui.player.controls

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import app.marlboroadvance.mpvex.ui.player.controls.components.StandardSeekbar
import app.marlboroadvance.mpvex.ui.player.controls.components.ThumbZoneUnlock
import app.marlboroadvance.mpvex.ui.player.controls.components.VerticalSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.VideoTimer
import kotlinx.collections.immutable.persistentListOf

// ---------------------------------------------------------------------------
// Design previews for the player chrome. They render the shared components over a
// stand-in "video" gradient in light/dark theme, with large font, and with
// "hide button background" on and off, so style regressions show up in the IDE
// without running the player.
//
// They use the plain Material 3 colour schemes (no dynamic colour, no app theme) so
// they render without Koin or preferences.
// ---------------------------------------------------------------------------

@Composable
private fun PreviewPlayerSurface(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    Box(
      modifier = Modifier
        .background(
          // Stand-in for a video frame: a bright-to-dark sweep, so contrast is checked
          // against both ends.
          Brush.horizontalGradient(listOf(Color(0xFFB9D6F2), Color(0xFF3A4F63), Color(0xFF0B0F14))),
        )
        .padding(16.dp),
    ) {
      content()
    }
  }
}

@Composable
private fun ChromeRow(hideBackground: Boolean) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayerIconButton(
      icon = Icons.Default.Settings,
      onClick = {},
      hideBackground = hideBackground,
      buttonSize = 48.dp,
      contentDescription = null,
    )
    PlayerIconButton(
      icon = Icons.Default.Refresh,
      onClick = {},
      hideBackground = hideBackground,
      buttonSize = 48.dp,
      checked = true,
      contentDescription = null,
    )
    PlayerValueChip(
      icon = Icons.Default.FavoriteBorder,
      text = "1.50x",
      onClick = {},
      hideBackground = hideBackground,
      buttonSize = 48.dp,
    )
    VideoTimer(value = 754f, isInverted = false, hideBackground = hideBackground)
    VideoTimer(value = 2391f, isInverted = false, onClick = {}, hideBackground = hideBackground)
  }
}

@Preview(name = "Chrome · light", uiMode = UI_MODE_NIGHT_NO, widthDp = 640)
@Preview(name = "Chrome · dark", uiMode = UI_MODE_NIGHT_YES, widthDp = 640)
@Preview(name = "Chrome · large font", uiMode = UI_MODE_NIGHT_YES, widthDp = 640, fontScale = 1.5f)
@Composable
private fun PlayerChromePreview() {
  PreviewPlayerSurface {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      ChromeRow(hideBackground = false)
      ChromeRow(hideBackground = true)
    }
  }
}

@Preview(name = "Seekbar styles · light", uiMode = UI_MODE_NIGHT_NO, widthDp = 480)
@Preview(name = "Seekbar styles · dark", uiMode = UI_MODE_NIGHT_YES, widthDp = 480)
@Composable
private fun SeekbarStylesPreview() {
  PreviewPlayerSurface {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Standard: thin track + bar thumb with a gap around it.
      StandardSeekbar(
        position = 400f,
        duration = 1000f,
        chapters = persistentListOf(),
        seekbarStyle = SeekbarStyle.Standard,
        onSeek = {},
        onSeekFinished = {},
      )
      // Thick: thick track, no thumb.
      StandardSeekbar(
        position = 400f,
        duration = 1000f,
        chapters = persistentListOf(),
        seekbarStyle = SeekbarStyle.Thick,
        onSeek = {},
        onSeekFinished = {},
        loopStart = 550f,
        loopEnd = 700f,
      )
    }
  }
}

@Preview(name = "Strong glass · light", uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "Strong glass · dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun StrongGlassPreview() {
  PreviewPlayerSurface {
    Row(
      horizontalArrangement = Arrangement.spacedBy(24.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      VerticalSlider(value = 0.6f, range = 0f..1f, icon = Icons.Default.Lock, label = "60")
      Box(modifier = Modifier.width(96.dp)) { ThumbZoneUnlock(onUnlock = {}) }
    }
  }
}
