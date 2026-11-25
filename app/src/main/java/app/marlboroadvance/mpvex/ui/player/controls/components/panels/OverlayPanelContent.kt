package app.marlboroadvance.mpvex.ui.player.controls.components.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.marlboroadvance.mpvex.ui.theme.spacing

/**
 * Content layout for player panels: an optional fixed header above scrollable content.
 *
 * Presentation (anchoring, shape, scrim, drag and dismissal) belongs to the shared
 * PlayerOverlayHost, so this holds only the panel's own layout.
 *
 * @param modifier Modifier for the panel
 * @param header Optional composable for the fixed header that stays constant during scroll
 * @param content The scrollable content of the panel
 */
@Composable
fun OverlayPanelContent(
  modifier: Modifier = Modifier,
  header: (@Composable () -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Column(modifier = modifier.fillMaxWidth()) {
    header?.invoke()

    Column(
      modifier = Modifier
        .weight(1f, fill = false)
        .verticalScroll(rememberScrollState())
        .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium)),
    ) {
      content()
    }
  }
}
