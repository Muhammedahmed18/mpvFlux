package app.marlboroadvance.mpvex.ui.player.controls

import androidx.activity.compose.BackHandler
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.AudioDelayPanel
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.SubtitleDelayPanel
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.SubtitleSettingsPanel
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.VideoSettingsPanel

/**
 * Panel content only. Presentation (anchoring, shape, scrim, motion, dismissal) is
 * owned by the shared PlayerOverlayHost that PlayerSheets mounts around this.
 */
@Composable
fun PlayerPanels(
  panelShown: Panels,
  onDismissRequest: () -> Unit,
  viewModel: PlayerViewModel,
  onBack: (() -> Unit)? = null,
) {
  // Registered inside the overlay's own composition so it outranks the sheet's
  // default back handling (which would dismiss everything).
  if (onBack != null) BackHandler(onBack = onBack)

  when (panelShown) {
    Panels.None -> {}
    Panels.SubtitleSettings -> {
      SubtitleSettingsPanel(onDismissRequest = onDismissRequest, onBack = onBack)
    }
    Panels.SubtitleDelay -> {
      SubtitleDelayPanel(
        onDismissRequest = onDismissRequest,
        onBack = onBack,
        subDelay = viewModel.subDelay,
        subSpeed = viewModel.subSpeed,
      )
    }
    Panels.AudioDelay -> {
      AudioDelayPanel(
        onDismissRequest = onDismissRequest,
        onBack = onBack,
        audioDelay = viewModel.audioDelay,
      )
    }
    Panels.VideoFilters -> {
      VideoSettingsPanel(onDismissRequest = onDismissRequest, onBack = onBack)
    }
  }
}

val CARDS_MAX_WIDTH = 420.dp
val panelCardsColors: @Composable () -> CardColors = {
  // Higher alpha for better readability in panels (less transparent)
  val alpha = 0.85f

  CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha),
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
  )
}
