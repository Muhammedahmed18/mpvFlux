package app.marlboroadvance.mpvex.ui.player.controls.components.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.theme.spacing

/**
 * Content layout for player panels: a compact fixed header above scrollable content.
 *
 * Presentation (anchoring, shape, scrim, drag and dismissal) belongs to the shared
 * PlayerOverlayHost, so this holds only the panel's own layout.
 *
 * @param title Panel title shown in the header
 * @param onClose Dismisses the overlay (close icon, shown only when there is no [onBack])
 * @param modifier Modifier for the panel
 * @param onBack When set, the header shows a back arrow that calls it (return to the
 * sheet the panel was opened from); otherwise a close icon is shown at the end
 * @param pinned Optional content kept below the header while the body scrolls
 * (e.g. a live preview)
 * @param content The scrollable content of the panel
 */
@Composable
fun OverlayPanelContent(
  title: String,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null,
  pinned: (@Composable () -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.small),
    ) {
      if (onBack != null) {
        IconButton(onClick = onBack) {
          Icon(
            Icons.AutoMirrored.Default.ArrowBack,
            stringResource(R.string.back),
            modifier = Modifier.size(24.dp),
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .weight(1f)
          .padding(start = if (onBack != null) 0.dp else MaterialTheme.spacing.smaller),
      )
      if (onBack == null) {
        IconButton(onClick = onClose) {
          Icon(
            Icons.Default.Close,
            stringResource(R.string.close),
            modifier = Modifier.size(24.dp),
          )
        }
      }
    }

    pinned?.invoke()

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
