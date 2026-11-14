package app.marlboroadvance.mpvex.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.update.ReleaseInfo
import app.marlboroadvance.mpvex.update.UpdateManager
import app.marlboroadvance.mpvex.update.UpdateState
import org.koin.compose.koinInject

/**
 * "Updates" card at the top of Settings. A plain row when up to date; a highlighted card
 * while an update is available, downloading or ready to install.
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
    is UpdateState.Downloading -> {
      title = stringResource(R.string.update_downloading, s.release.version.toString())
      summary = "${(s.progress * 100).toInt()}%"
    }
    is UpdateState.ReadyToInstall -> {
      title = stringResource(R.string.update_ready_title, s.release.version.toString())
      summary = stringResource(R.string.update_card_tap_to_install)
    }
    is UpdateState.Installing -> {
      title = stringResource(R.string.update_ready_title, s.release.version.toString())
      summary = stringResource(R.string.update_installing)
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
    Column {
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
      if (state is UpdateState.Downloading) {
        LinearWavyProgressIndicator(
          progress = { (state as? UpdateState.Downloading)?.progress ?: 0f },
          modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
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

/**
 * "Update available" dialog shown when the app opens and a newer version is on GitHub.
 * M3 basic dialog: icon on top, centred title, version line, scrollable release notes.
 * [onUpdate] starts the download, [onLater] hides it until the next app launch and
 * [onSkip] stops asking about this version.
 */
@Composable
fun UpdateAvailableDialog(
  release: ReleaseInfo,
  installedVersion: String,
  onUpdate: () -> Unit,
  onLater: () -> Unit,
  onSkip: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onLater,
    icon = {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_sym_new_releases),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = stringResource(R.string.update_dialog_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = stringResource(R.string.update_dialog_version, release.version.toString()),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.update_release_meta, installedVersion, release.abi, formatBytes(release.apkSize)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
        if (release.notes.isNotBlank()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Box(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
            ReleaseNotes(markdown = release.notes)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onUpdate) { Text(stringResource(R.string.update_dialog_update)) }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip_version)) }
        TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
      }
    },
  )
}

/** "What's new" sheet shown once after the app was updated from inside the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
  version: String,
  notes: String,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        stringResource(R.string.update_whats_new_in, version),
        style = MaterialTheme.typography.titleLargeEmphasized,
      )
      Box(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        ReleaseNotes(markdown = notes)
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        Button(onClick = onDismiss) { Text(stringResource(R.string.update_got_it)) }
      }
    }
  }
}
