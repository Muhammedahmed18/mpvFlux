package app.marlboroadvance.mpvex.ui.preferences

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.UpdatePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.update.ReleaseNotes
import app.marlboroadvance.mpvex.ui.update.formatBytes
import app.marlboroadvance.mpvex.ui.update.updateErrorText
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.update.ApkInstaller
import app.marlboroadvance.mpvex.update.GitHubReleases
import app.marlboroadvance.mpvex.update.ReleaseInfo
import app.marlboroadvance.mpvex.update.UpdateError
import app.marlboroadvance.mpvex.update.UpdateManager
import app.marlboroadvance.mpvex.update.UpdateState
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/**
 * Settings → Updates: current status with the main action (check / download / install),
 * release notes, the "Install unknown apps" permission, and updater settings.
 */
@Serializable
object UpdatesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val manager = koinInject<UpdateManager>()
    val preferences = koinInject<UpdatePreferences>()
    val context = LocalContext.current
    val state by manager.state.collectAsState()

    // Re-read the install permission each time the screen comes back (e.g. from Android's
    // settings page) and continue an install that was waiting for it.
    var canInstall by remember { mutableStateOf(ApkInstaller.canInstallPackages(context)) }
    // Set when "Download and install" sent the user to grant the permission first.
    var downloadAfterPermission by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
      canInstall = ApkInstaller.canInstallPackages(context)
      if (canInstall) {
        manager.retryAfterPermission()
        if (downloadAfterPermission) {
          downloadAfterPermission = false
          manager.download()
        }
      }
      onPauseOrDispose {}
    }
    val openPermissionSettings = {
      runCatching { context.startActivity(ApkInstaller.unknownAppSourcesIntent(context)) }
      Unit
    }

    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = backgroundColor,
      topBar = {
        PreferenceTopBar(
          title = {
            Text(
              text = stringResource(R.string.update_title),
              style = MaterialTheme.typography.titleLargeEmphasized,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                ImageVector.vectorResource(R.drawable.ic_sym_arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          scrollBehavior = scrollBehavior,
          containerColor = backgroundColor,
        )
      },
    ) { padding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize().readableContentWidth(),
        contentPadding = PaddingValues(
          top = padding.calculateTopPadding(),
          bottom = padding.calculateBottomPadding() + 24.dp,
          start = 8.dp,
          end = 8.dp,
        ),
      ) {
        item {
          StatusCard(
            state = state,
            manager = manager,
            canInstall = canInstall,
            onAllowInstall = openPermissionSettings,
            onDownloadNeedsPermission = {
              downloadAfterPermission = true
              openPermissionSettings()
            },
          )
        }

        releaseOf(state)?.let { release ->
          item { PreferenceSectionHeader(title = stringResource(R.string.update_whats_new_in, release.version.toString())) }
          item {
            PreferenceCard {
              ReleaseNotes(
                markdown = release.notes.ifBlank { stringResource(R.string.update_no_notes) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
              )
              PreferenceDivider()
              LinkRow(
                text = stringResource(R.string.update_view_on_github),
                url = release.htmlUrl.ifBlank { RELEASES_URL },
              )
            }
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.update_permission_header)) }
        item {
          PreferenceCard {
            PermissionRow(canInstall = canInstall, onAllow = openPermissionSettings)
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.update_settings_header)) }
        item {
          val autoCheck by preferences.autoCheck.collectAsState()
          val includePre by preferences.includePrereleases.collectAsState()
          val lastCheck by preferences.lastCheckTime.collectAsState()
          PreferenceCard {
            SwitchRow(
              title = stringResource(R.string.update_auto_check),
              summary = stringResource(R.string.update_auto_check_summary),
              checked = autoCheck,
              onCheckedChange = { preferences.autoCheck.set(it) },
            )
            PreferenceDivider()
            SwitchRow(
              title = stringResource(R.string.update_beta),
              summary = stringResource(R.string.update_beta_summary),
              checked = includePre,
              onCheckedChange = {
                preferences.includePrereleases.set(it)
                manager.check()
              },
            )
            PreferenceDivider()
            Row(
              modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (lastCheck > 0) {
                  stringResource(
                    R.string.update_last_checked,
                    DateUtils.getRelativeTimeSpanString(lastCheck, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                  )
                } else {
                  stringResource(R.string.update_never_checked)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = { manager.check() },
                enabled = manager.isSupported && state !is UpdateState.Checking && state !is UpdateState.Downloading,
              ) { Text(stringResource(R.string.update_check_now)) }
            }
          }
        }
      }
    }
  }

  private const val RELEASES_URL = "https://github.com/${GitHubReleases.OWNER}/${GitHubReleases.REPO}/releases"
}

private fun releaseOf(state: UpdateState): ReleaseInfo? =
  when (state) {
    is UpdateState.Available -> state.release
    is UpdateState.Downloading -> state.release
    is UpdateState.ReadyToInstall -> state.release
    is UpdateState.Installing -> state.release
    is UpdateState.Failed -> state.release
    else -> null
  }

// ---------------------------------------------------------------------------
// Status card
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusCard(
  state: UpdateState,
  manager: UpdateManager,
  canInstall: Boolean,
  onAllowInstall: () -> Unit,
  onDownloadNeedsPermission: () -> Unit,
) {
  val highlighted = releaseOf(state) != null
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    shape = MaterialTheme.shapes.extraLarge,
    color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      when {
        !manager.isSupported -> {
          StatusIcon(R.drawable.ic_sym_system_update)
          StatusTitle(stringResource(R.string.update_not_supported_title))
          StatusText(stringResource(R.string.update_not_supported_text))
        }

        else -> when (state) {
          UpdateState.Idle, UpdateState.UpToDate -> {
            StatusIcon(R.drawable.ic_sym_check_circle_fill)
            StatusTitle(stringResource(R.string.update_up_to_date))
            StatusText(stringResource(R.string.update_installed_version, manager.currentVersion))
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = { manager.check() }) {
              Icon(ImageVector.vectorResource(R.drawable.ic_sym_sync), null, Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.update_check_now))
            }
          }

          UpdateState.Checking -> {
            LoadingIndicator(modifier = Modifier.size(48.dp))
            StatusTitle(stringResource(R.string.update_checking))
          }

          is UpdateState.Available -> {
            StatusIcon(R.drawable.ic_sym_system_update)
            StatusTitle(stringResource(R.string.update_available_title, state.release.version.toString()))
            ReleaseMeta(state.release, manager.currentVersion)
            Spacer(Modifier.height(4.dp))
            Button(onClick = {
              if (canInstall) manager.download() else onDownloadNeedsPermission()
            }) {
              Icon(ImageVector.vectorResource(R.drawable.ic_sym_download), null, Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.update_download_install))
            }
            if (!canInstall) StatusText(stringResource(R.string.update_permission_needed_first))
            if (!state.skipped) {
              TextButton(onClick = { manager.skip(state.release) }) {
                Text(stringResource(R.string.update_skip_version))
              }
            } else {
              StatusText(stringResource(R.string.update_skipped))
            }
          }

          is UpdateState.Downloading -> {
            StatusIcon(R.drawable.ic_sym_download)
            StatusTitle(stringResource(R.string.update_downloading, state.release.version.toString()))
            LinearWavyProgressIndicator(
              progress = { state.progress },
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            StatusText(
              stringResource(
                R.string.update_download_progress,
                (state.progress * 100).toInt(),
                formatBytes(state.downloaded),
                formatBytes(state.total),
              ),
            )
            OutlinedButton(onClick = { manager.cancelDownload() }) {
              Text(stringResource(android.R.string.cancel))
            }
          }

          is UpdateState.ReadyToInstall -> {
            StatusIcon(R.drawable.ic_sym_check_circle_fill)
            StatusTitle(stringResource(R.string.update_ready_title, state.release.version.toString()))
            StatusText(stringResource(R.string.update_ready_text))
            Spacer(Modifier.height(4.dp))
            Button(onClick = { manager.install() }) {
              Text(stringResource(R.string.update_install))
            }
          }

          is UpdateState.Installing -> {
            LoadingIndicator(modifier = Modifier.size(48.dp))
            StatusTitle(stringResource(R.string.update_installing))
            StatusText(stringResource(R.string.update_installing_text))
          }

          is UpdateState.Failed -> {
            StatusIcon(R.drawable.ic_sym_error)
            StatusTitle(stringResource(R.string.update_failed_title))
            StatusText(updateErrorText(state.error))
            Spacer(Modifier.height(4.dp))
            when {
              state.error == UpdateError.PermissionMissing -> Button(onClick = onAllowInstall) {
                Text(stringResource(R.string.update_allow))
              }
              state.release != null && state.error != UpdateError.SignatureMismatch -> Button(onClick = { manager.download() }) {
                Text(stringResource(R.string.update_try_again))
              }
              else -> FilledTonalButton(onClick = { manager.check() }) {
                Text(stringResource(R.string.update_check_now))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReleaseMeta(release: ReleaseInfo, installed: String) {
  StatusText(
    stringResource(R.string.update_release_meta, installed, release.abi, formatBytes(release.apkSize)) +
      if (release.isPreRelease) " · " + stringResource(R.string.update_beta_label) else "",
  )
}

@Composable
private fun StatusIcon(icon: Int) {
  Icon(
    imageVector = ImageVector.vectorResource(icon),
    contentDescription = null,
    modifier = Modifier.size(48.dp),
    tint = MaterialTheme.colorScheme.primary,
  )
}

@Composable
private fun StatusTitle(text: String) {
  Text(text = text, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
}

@Composable
private fun StatusText(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
    color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.8f),
  )
}

// ---------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------

@Composable
private fun PermissionRow(canInstall: Boolean, onAllow: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(
      ImageVector.vectorResource(R.drawable.ic_sym_verified_user),
      contentDescription = null,
      tint = if (canInstall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(stringResource(R.string.update_permission_title), style = MaterialTheme.typography.titleMediumEmphasized)
      Text(
        text = stringResource(
          if (canInstall) R.string.update_permission_allowed else R.string.update_permission_not_allowed,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = if (canInstall) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
      )
    }
    if (canInstall) {
      Icon(
        ImageVector.vectorResource(R.drawable.ic_sym_check_circle_fill),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    } else {
      FilledTonalButton(onClick = onAllow) { Text(stringResource(R.string.update_allow)) }
    }
  }
}

@Composable
private fun SwitchRow(
  title: String,
  summary: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  // The whole row toggles, and TalkBack reads title + summary + on/off as one switch.
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
      .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
      Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = null)
  }
}

@Composable
private fun LinkRow(text: String, url: String) {
  val context = LocalContext.current
  Surface(
    onClick = {
      try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      } catch (_: ActivityNotFoundException) {
      }
    },
    color = Color.Transparent,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
      Icon(
        ImageVector.vectorResource(R.drawable.ic_sym_open_in_new),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
