package app.marlboroadvance.mpvex.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.update.UpdateManager
import app.marlboroadvance.mpvex.update.UpdateState
import org.koin.compose.koinInject

/**
 * "Updates" card at the top of Settings. A plain row when up to date; a highlighted card
 * when an update is available or a check failed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateSettingsCard(onClick: () -> Unit) {
  val manager = koinInject<UpdateManager>()
  val state by manager.state.collectAsState()
  val highlighted = state.needsAttention()

  val title: String
  val summary: String
  when (val s = state) {
    is UpdateState.Available -> {
      title = stringResource(R.string.update_available_title, s.release.version.toString())
      summary = stringResource(R.string.update_card_tap_to_view)
    }
    is UpdateState.Failed -> {
      title = stringResource(R.string.update_title)
      summary = updateErrorText(s.error)
    }
    UpdateState.Checking -> {
      title = stringResource(R.string.update_title)
      summary = stringResource(R.string.update_checking)
    }
    else -> {
      title = stringResource(R.string.update_title)
      summary = stringResource(R.string.update_card_up_to_date, manager.currentVersion)
    }
  }

  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    shape = MaterialTheme.shapes.extraLarge,
    color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(
          if (highlighted) R.drawable.ic_sym_system_update else R.drawable.ic_sym_sync,
        ),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
        Text(
          summary,
          style = MaterialTheme.typography.bodyMedium,
          color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.75f),
        )
      }
    }
  }
}

/**
 * Settings icon with a small dot when an update needs attention. Used in the browser top bars.
 */
@Composable
fun SettingsIconWithUpdateDot(icon: @Composable () -> Unit) {
  val manager = koinInject<UpdateManager>()
  val state by manager.state.collectAsState()
  if (state.needsAttention()) {
    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.primary) }) { icon() }
  } else {
    icon()
  }
}
