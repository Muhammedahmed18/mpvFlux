package app.marlboroadvance.mpvex.ui.player.controls.components.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.theme.spacing

@Composable
fun VideoSettingsPanel(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null,
) {
  OverlayPanelContent(
    modifier = modifier,
    title = stringResource(R.string.player_sheets_video_settings_title),
    onClose = onDismissRequest,
    onBack = onBack,
  ) {
    Column(
      Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.extraSmall),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      VideoSettingsFilterPresetsCard()
      VideoSettingsFiltersCard()
      VideoSettingsDebandCard()
    }
  }
}
