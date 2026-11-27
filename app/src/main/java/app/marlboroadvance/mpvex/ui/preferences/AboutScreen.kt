package app.marlboroadvance.mpvex.ui.preferences

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import `is`.xyz.mpv.Utils
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object AboutScreen : Screen {

  private const val GITHUB_URL = "https://github.com/MpvFlux/mpvFlux"
  private const val DISCUSSIONS_URL = "$GITHUB_URL/discussions"
  private const val LATEST_RELEASE_URL = "$GITHUB_URL/releases/latest"
  private const val FEATURE_REQUEST_URL = "$GITHUB_URL/issues/new?template=feature_request.md"

  private val CardShape = RoundedCornerShape(28.dp)
  private val TileShape = RoundedCornerShape(24.dp)

  /** New bug report with the template and the app version already in the title. */
  private fun bugReportUrl(): String {
    val version = BuildConfig.VERSION_NAME.substringBefore('-')
    return "$GITHUB_URL/issues/new?template=bug_report.md&title=" + Uri.encode("[v$version] ")
  }

  /** Opens a link in the browser. Returns false (instead of crashing) if no browser exists. */
  private fun openLink(context: Context, url: String): Boolean =
    try {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      )
      true
    } catch (_: ActivityNotFoundException) {
      false
    }

  private data class LinkItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
  )

  private data class MetricItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
  )

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val noBrowserMessage = stringResource(R.string.about_no_browser)
    val debugCopiedMessage = stringResource(R.string.about_debug_info_copied)

    val showMessage: (String) -> Unit = { message ->
      scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
      }
    }
    val open: (String) -> Unit = { url ->
      if (!openLink(context, url)) showMessage(noBrowserMessage)
    }
    val copyText: (String, suspend () -> String, String) -> Unit = { label, text, message ->
      scope.launch {
        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, text())))
        showMessage(message)
      }
    }

    val links = listOf(
      LinkItem(Icons.Outlined.BugReport, stringResource(R.string.about_report_bug)) {
        // Copy debug info first so it can be pasted into the report.
        scope.launch {
          clipboard.setClipEntry(
            ClipEntry(ClipData.newPlainText("Debug Info", collectDeviceInfo())),
          )
          showMessage(debugCopiedMessage)
          open(bugReportUrl())
        }
      },
      LinkItem(Icons.Outlined.Lightbulb, stringResource(R.string.about_request_feature)) {
        open(FEATURE_REQUEST_URL)
      },
      LinkItem(Icons.Outlined.Forum, stringResource(R.string.about_discussions)) {
        open(DISCUSSIONS_URL)
      },
      LinkItem(Icons.Outlined.NewReleases, stringResource(R.string.about_whats_new)) {
        open(LATEST_RELEASE_URL)
      },
      LinkItem(Icons.Outlined.StarOutline, stringResource(R.string.about_star_github)) {
        open(GITHUB_URL)
      },
      LinkItem(Icons.Outlined.Code, stringResource(R.string.about_source_code)) {
        open(GITHUB_URL)
      },
    )

    val metrics = listOf(
      MetricItem(
        Icons.Outlined.Android,
        stringResource(R.string.about_metric_android),
        "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
      ),
      MetricItem(
        Icons.Outlined.PhoneAndroid,
        stringResource(R.string.about_metric_device),
        Build.MODEL,
      ),
      MetricItem(
        Icons.Outlined.Memory,
        stringResource(R.string.about_metric_hardware),
        Build.HARDWARE,
      ),
      MetricItem(
        Icons.Outlined.DeveloperBoard,
        "ABI",
        Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
      ),
      MetricItem(Icons.Outlined.Code, "MPV", Utils.VERSIONS.mpv),
      MetricItem(Icons.Outlined.Movie, "FFmpeg", Utils.VERSIONS.ffmpeg),
    )
    val libPlacebo = MetricItem(Icons.Outlined.Settings, "libplacebo", Utils.VERSIONS.libPlacebo)

    val onCopyMetric: (MetricItem) -> Unit = { metric ->
      copyText(
        metric.label,
        { metric.value },
        resources.getString(R.string.about_metric_copied, metric.label),
      )
    }

    Surface(
      modifier = Modifier.fillMaxSize(),
      color = backgroundColor,
    ) {
      Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          PreferenceTopBar(
            title = {
              Text(
                text = stringResource(R.string.pref_about_title),
                color = MaterialTheme.colorScheme.primary,
              )
            },
            navigationIcon = {
              IconButton(onClick = backstack::removeLastOrNull) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
      ) { paddingValues ->
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .readableContentWidth(),
          contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = paddingValues.calculateTopPadding() + 4.dp,
            bottom = 24.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item { HeroSection() }

          item { SectionHeader(text = stringResource(R.string.about_core_highlights)) }
          item { HighlightsGrid() }

          item { SectionHeader(text = stringResource(R.string.about_community)) }
          item { LinksCard(links = links) }

          item { SectionHeader(text = stringResource(R.string.about_system_info)) }
          item { SystemGrid(metrics = metrics, onCopy = onCopyMetric) }
          item { MetricTile(metric = libPlacebo, onCopy = onCopyMetric, modifier = Modifier.fillMaxWidth()) }

          item {
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(
              onClick = {
                copyText("Debug Info", { collectDeviceInfo() }, debugCopiedMessage)
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
              shape = CircleShape,
            ) {
              Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.about_copy_debug_info),
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
          item {
            Text(
              text = stringResource(R.string.about_forked_from),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              textAlign = TextAlign.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            )
          }
        }
      }
    }
  }

  @Composable
  private fun HeroSection() {
    val cleanVersion = BuildConfig.VERSION_NAME.substringBefore('-')
    val channel = stringResource(
      if (BuildConfig.DEBUG) R.string.about_channel_debug else R.string.about_channel_stable,
    )
    val colors = MaterialTheme.colorScheme

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(36.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(
              colors.primaryContainer,
              colors.tertiaryContainer.copy(alpha = 0.55f),
            ),
          ),
        )
        .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_about_logo),
          contentDescription = stringResource(R.string.about_app_icon),
          modifier = Modifier.size(104.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = "mpvFlux",
          style = MaterialTheme.typography.headlineLargeEmphasized,
          color = colors.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          InfoChip(
            text = "v$cleanVersion",
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
          )
          InfoChip(
            text = channel,
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
          )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = stringResource(R.string.about_description),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onPrimaryContainer.copy(alpha = 0.85f),
          textAlign = TextAlign.Center,
        )
      }
    }
  }

  @Composable
  private fun InfoChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
  ) {
    Surface(
      shape = CircleShape,
      color = containerColor,
      contentColor = contentColor,
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      )
    }
  }

  @Composable
  private fun SectionHeader(text: String) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMediumEmphasized,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 8.dp, top = 12.dp),
    )
  }

  @Composable
  private fun HighlightsGrid() {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        HighlightCard(
          modifier = Modifier.weight(1f),
          icon = Icons.Outlined.Bolt,
          title = stringResource(R.string.about_highlight_high_performance),
          subtitle = stringResource(R.string.about_highlight_libmpv),
          containerColor = colors.primaryContainer,
          contentColor = colors.onPrimaryContainer,
        )
        HighlightCard(
          modifier = Modifier.weight(1f),
          icon = Icons.Outlined.AutoAwesome,
          title = "libplacebo",
          subtitle = stringResource(R.string.about_highlight_libplacebo),
          containerColor = colors.secondaryContainer,
          contentColor = colors.onSecondaryContainer,
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        HighlightCard(
          modifier = Modifier.weight(1f),
          icon = Icons.Outlined.Movie,
          title = stringResource(R.string.about_highlight_formats),
          subtitle = "MKV · MP4 · AVI · HEVC · AV1",
          containerColor = colors.tertiaryContainer,
          contentColor = colors.onTertiaryContainer,
        )
        HighlightCard(
          modifier = Modifier.weight(1f),
          icon = Icons.Outlined.Palette,
          title = "Material You",
          subtitle = stringResource(R.string.about_highlight_material_you),
          containerColor = colors.surfaceContainerHigh,
          contentColor = colors.onSurface,
        )
      }
    }
  }

  @Composable
  private fun HighlightCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
  ) {
    Surface(
      modifier = modifier.fillMaxHeight(),
      shape = CardShape,
      color = containerColor,
      contentColor = contentColor,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(contentColor.copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
          )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmallEmphasized,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = contentColor.copy(alpha = 0.8f),
        )
      }
    }
  }

  @Composable
  private fun LinksCard(links: List<LinkItem>) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = CardShape,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      Column {
        links.forEachIndexed { index, link ->
          LinkRow(link = link)
          if (index != links.lastIndex) {
            HorizontalDivider(
              modifier = Modifier.padding(start = 72.dp, end = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
          }
        }
      }
    }
  }

  @Composable
  private fun LinkRow(link: LinkItem) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = link.onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = link.icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(22.dp),
        )
      }
      Text(
        text = link.label,
        style = MaterialTheme.typography.bodyLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
      Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
      )
    }
  }

  @Composable
  private fun SystemGrid(
    metrics: List<MetricItem>,
    onCopy: (MetricItem) -> Unit,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      metrics.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          pair.forEach { metric ->
            MetricTile(
              metric = metric,
              onCopy = onCopy,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            )
          }
          if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }

  @Composable
  private fun MetricTile(
    metric: MetricItem,
    onCopy: (MetricItem) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Surface(
      onClick = { onCopy(metric) },
      modifier = modifier,
      shape = TileShape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = metric.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = metric.value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
