package app.marlboroadvance.mpvex.ui.preferences

import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import app.marlboroadvance.mpvex.ui.preferences.components.PreferenceSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.UpdatePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.update.CollapsibleReleaseNotes
import app.marlboroadvance.mpvex.ui.update.updateErrorText
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.update.GitHubReleases
import app.marlboroadvance.mpvex.update.ReleaseInfo
import app.marlboroadvance.mpvex.update.UpdateManager
import app.marlboroadvance.mpvex.update.UpdateState
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private const val RELEASES_URL = "https://github.com/${GitHubReleases.OWNER}/${GitHubReleases.REPO}/releases"

/**
 * Settings → Updates: whether a newer stable version exists, its release notes, a link to the
 * release page on GitHub, and the auto-check setting. Nothing is downloaded or installed here;
 * updating happens from GitHub in the browser.
 */
@Serializable
object UpdatesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val manager = koinInject<UpdateManager>()
    val preferences = koinInject<UpdatePreferences>()
    val state by manager.state.collectAsState()

    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = backgroundColor,
      topBar = {
        PreferenceTopBar(
          title = {
            Text(
              text = stringResource(R.string.update_title),
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
          large = true,
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
        item { StatusCard(state = state, manager = manager) }

        (state as? UpdateState.Available)?.release?.let { release ->
          item { PreferenceSectionHeader(title = stringResource(R.string.update_whats_new_in, release.version.toString())) }
          item {
            PreferenceCard {
              CollapsibleReleaseNotes(
                markdown = release.notes.ifBlank { stringResource(R.string.update_no_notes) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
              )
            }
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.update_settings_header)) }
        item {
          val autoCheck by preferences.autoCheck.collectAsState()
          val lastCheck by preferences.lastCheckTime.collectAsState()
          PreferenceCard {
            SwitchRow(
              title = stringResource(R.string.update_auto_check),
              summary = stringResource(R.string.update_auto_check_summary),
              checked = autoCheck,
              onCheckedChange = { preferences.autoCheck.set(it) },
            )
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
                enabled = manager.isSupported && state !is UpdateState.Checking,
              ) { Text(stringResource(R.string.update_check_now)) }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Status card
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusCard(state: UpdateState, manager: UpdateManager) {
  val context = LocalContext.current
  val highlighted = manager.isSupported && state is UpdateState.Available
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
            StatusText(stringResource(R.string.update_you_have, manager.currentVersion))
            Spacer(Modifier.height(4.dp))
            Button(onClick = { openInBrowser(context, releasePageUrl(state.release)) }) {
              Text(stringResource(R.string.update_view_on_github))
              Spacer(Modifier.width(8.dp))
              Icon(ImageVector.vectorResource(R.drawable.ic_sym_open_in_new), null, Modifier.size(18.dp))
            }
          }

          is UpdateState.Failed -> {
            StatusIcon(R.drawable.ic_sym_error)
            StatusTitle(stringResource(R.string.update_failed_title))
            StatusText(updateErrorText(state.error))
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = { manager.check() }) {
              Text(stringResource(R.string.update_check_now))
            }
          }
        }
      }
    }
  }
}

private fun releasePageUrl(release: ReleaseInfo): String = release.htmlUrl.ifBlank { RELEASES_URL }

/** Opens [url] in the user's browser. Only https links are opened. */
private fun openInBrowser(context: Context, url: String) {
  val uri = Uri.parse(url)
  if (uri.scheme != "https") return
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: ActivityNotFoundException) {
  }
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
    PreferenceSwitch(checked = checked, onCheckedChange = null)
  }
}
