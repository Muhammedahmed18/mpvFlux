package app.marlboroadvance.mpvex.ui.preferences

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.UpdatePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.components.PreferenceSwitch
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
 *
 * The status card changes color, badge shape and content with the update state.
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
    val lastCheck by preferences.lastCheckTime.collectAsState()

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
      Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        LazyColumn(
          modifier = Modifier.fillMaxSize().readableContentWidth(),
          contentPadding = PaddingValues(
            bottom = padding.calculateBottomPadding() + 24.dp,
            start = 8.dp,
            end = 8.dp,
          ),
        ) {
          item { StatusCard(state = state, manager = manager, lastCheck = lastCheck) }

          (state as? UpdateState.Available)?.release?.let { release ->
            item { ReleaseNotesCard(release = release) }
          }

          item { PreferenceSectionHeader(title = stringResource(R.string.update_settings_header)) }
          item {
            val autoCheck by preferences.autoCheck.collectAsState()
            PreferenceCard {
              SwitchRow(
                icon = R.drawable.ic_sym_sync,
                title = stringResource(R.string.update_auto_check),
                summary = stringResource(R.string.update_auto_check_summary),
                checked = autoCheck,
                onCheckedChange = { preferences.autoCheck.set(it) },
              )
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

private enum class StatusKind { Unsupported, UpToDate, Checking, Available, Failed }

private fun statusKindOf(state: UpdateState, supported: Boolean): StatusKind = when {
  !supported -> StatusKind.Unsupported
  state is UpdateState.Checking -> StatusKind.Checking
  state is UpdateState.Available -> StatusKind.Available
  state is UpdateState.Failed -> StatusKind.Failed
  else -> StatusKind.UpToDate
}

private data class StatusColors(
  val container: Color,
  val content: Color,
  val badge: Color,
  val onBadge: Color,
)

/** One color role per state, so the state reads before any text does. */
@Composable
private fun statusColors(kind: StatusKind): StatusColors {
  val c = MaterialTheme.colorScheme
  return when (kind) {
    StatusKind.Available -> StatusColors(c.primaryContainer, c.onPrimaryContainer, c.primary, c.onPrimary)
    StatusKind.Failed -> StatusColors(c.errorContainer, c.onErrorContainer, c.error, c.onError)
    StatusKind.UpToDate -> StatusColors(c.tertiaryContainer, c.onTertiaryContainer, c.tertiary, c.onTertiary)
    StatusKind.Checking -> StatusColors(c.surfaceContainerHigh, c.onSurface, c.secondaryContainer, c.onSecondaryContainer)
    StatusKind.Unsupported -> StatusColors(c.surfaceContainerLow, c.onSurface, c.surfaceContainerHighest, c.onSurfaceVariant)
  }
}

@DrawableRes
private fun statusIcon(kind: StatusKind): Int = when (kind) {
  StatusKind.Available, StatusKind.Unsupported, StatusKind.Checking -> R.drawable.ic_sym_system_update
  StatusKind.UpToDate -> R.drawable.ic_sym_check_circle_fill
  StatusKind.Failed -> R.drawable.ic_sym_error
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun statusShape(kind: StatusKind): Shape {
  val polygon = when (kind) {
    StatusKind.Available -> MaterialShapes.SoftBurst
    StatusKind.UpToDate -> MaterialShapes.Cookie9Sided
    StatusKind.Failed -> MaterialShapes.Pentagon
    StatusKind.Checking, StatusKind.Unsupported -> MaterialShapes.Cookie6Sided
  }
  // toShape() is itself @Composable (it remembers the shape), so it must not be called inside remember { }.
  return polygon.toShape()
}

@Composable
private fun StatusCard(state: UpdateState, manager: UpdateManager, lastCheck: Long) {
  val kind = statusKindOf(state, manager.isSupported)
  val target = statusColors(kind)
  val containerColor by animateColorAsState(target.container, label = "statusContainer")
  val contentColor by animateColorAsState(target.content, label = "statusContent")
  val badgeColor by animateColorAsState(target.badge, label = "statusBadge")
  val onBadgeColor by animateColorAsState(target.onBadge, label = "statusOnBadge")

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp)
      // Screen readers announce the new state when it changes.
      .semantics { liveRegion = LiveRegionMode.Polite },
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
    contentColor = contentColor,
  ) {
    AnimatedContent(
      targetState = state,
      contentKey = { statusKindOf(it, manager.isSupported) },
      label = "statusBody",
    ) { current ->
      StatusBody(
        state = current,
        manager = manager,
        lastCheck = lastCheck,
        badgeColor = badgeColor,
        onBadgeColor = onBadgeColor,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun StatusBody(
  state: UpdateState,
  manager: UpdateManager,
  lastCheck: Long,
  badgeColor: Color,
  onBadgeColor: Color,
) {
  val context = LocalContext.current
  val kind = statusKindOf(state, manager.isSupported)
  val actionColors = ButtonDefaults.buttonColors(containerColor = badgeColor, contentColor = onBadgeColor)

  Column(
    modifier = Modifier.fillMaxWidth().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Badge (or spinner) next to the title and supporting text, left-aligned.
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (kind == StatusKind.Checking) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
          LoadingIndicator(modifier = Modifier.size(48.dp))
        }
      } else {
        TonalBadge(
          icon = statusIcon(kind),
          containerColor = badgeColor,
          contentColor = onBadgeColor,
          shape = statusShape(kind),
          size = 56.dp,
          iconSize = 28.dp,
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        when {
          !manager.isSupported -> {
            StatusTitle(stringResource(R.string.update_not_supported_title))
            StatusText(stringResource(R.string.update_not_supported_text))
          }

          state is UpdateState.Available -> {
            StatusTitle(stringResource(R.string.update_available_title, state.release.version.toString()))
            StatusText(stringResource(R.string.update_you_have, manager.currentVersion))
          }

          state is UpdateState.Failed -> {
            StatusTitle(stringResource(R.string.update_failed_title))
            StatusText(updateErrorText(state.error))
          }

          state is UpdateState.Checking -> {
            StatusTitle(stringResource(R.string.update_checking))
          }

          else -> {
            StatusTitle(stringResource(R.string.update_up_to_date))
            StatusText(stringResource(R.string.update_installed_version, manager.currentVersion))
          }
        }
      }
    }

    // Actions. "Check now" lives only here; the settings section no longer repeats it.
    when {
      !manager.isSupported || state is UpdateState.Checking -> Unit

      state is UpdateState.Available -> {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Button(
            onClick = { openInBrowser(context, releasePageUrl(state.release)) },
            colors = actionColors,
          ) {
            Text(stringResource(R.string.update_view_on_github))
            Spacer(Modifier.width(8.dp))
            Icon(ImageVector.vectorResource(R.drawable.ic_sym_open_in_new), null, Modifier.size(18.dp))
          }
          TextButton(onClick = { manager.check() }) {
            Text(stringResource(R.string.update_check_now))
          }
        }
      }

      else -> {
        Button(onClick = { manager.check() }, colors = actionColors) {
          Icon(ImageVector.vectorResource(R.drawable.ic_sym_sync), null, Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.update_check_now))
        }
      }
    }

    // Last checked, folded into the card.
    if (manager.isSupported && state !is UpdateState.Checking) {
      val lastCheckedText = if (lastCheck > 0) {
        stringResource(
          R.string.update_last_checked,
          DateUtils.getRelativeTimeSpanString(lastCheck, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
        )
      } else {
        stringResource(R.string.update_never_checked)
      }
      Text(
        text = lastCheckedText,
        style = MaterialTheme.typography.labelMedium,
        color = LocalContentColor.current.copy(alpha = 0.7f),
      )
    }
  }
}

/** "What's new in X": a tonal card with a badge + title header above the collapsible notes. */
@Composable
private fun ReleaseNotesCard(release: ReleaseInfo) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        TonalBadge(
          icon = Icons.Outlined.AutoAwesome,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          shape = CircleShape,
          size = 40.dp,
          iconSize = 22.dp,
        )
        Text(
          text = stringResource(R.string.update_whats_new_in, release.version.toString()),
          style = MaterialTheme.typography.titleMediumEmphasized,
          modifier = Modifier.weight(1f),
        )
      }
      CollapsibleReleaseNotes(
        markdown = release.notes.ifBlank { stringResource(R.string.update_no_notes) },
        modifier = Modifier.fillMaxWidth(),
      )
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
private fun TonalBadge(
  @DrawableRes icon: Int,
  containerColor: Color,
  contentColor: Color,
  shape: Shape,
  size: Dp,
  iconSize: Dp,
) {
  TonalBadge(
    icon = ImageVector.vectorResource(icon),
    containerColor = containerColor,
    contentColor = contentColor,
    shape = shape,
    size = size,
    iconSize = iconSize,
  )
}

@Composable
private fun TonalBadge(
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  shape: Shape,
  size: Dp,
  iconSize: Dp,
) {
  Box(
    modifier = Modifier.size(size).background(containerColor, shape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(iconSize),
      tint = contentColor,
    )
  }
}

@Composable
private fun StatusTitle(text: String) {
  Text(text = text, style = MaterialTheme.typography.titleLargeEmphasized)
}

@Composable
private fun StatusText(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = LocalContentColor.current.copy(alpha = 0.8f),
  )
}

// ---------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------

@Composable
private fun SwitchRow(
  @DrawableRes icon: Int,
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
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TonalBadge(
      icon = icon,
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
      size = 40.dp,
      iconSize = 22.dp,
    )
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
      Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    PreferenceSwitch(checked = checked, onCheckedChange = null)
  }
}
