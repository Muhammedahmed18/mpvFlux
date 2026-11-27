@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3Api::class,
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package app.marlboroadvance.mpvex.ui.mediainfo

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.marlboroadvance.mpvex.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies to the clipboard and always confirms with the app's own snackbar, so the feedback is
 * the same on every Android version (Android 13+ also shows its system preview).
 */
internal class CopyActions(
  private val context: Context,
  private val scope: CoroutineScope,
  private val snackbarHostState: SnackbarHostState,
) {
  fun copyValue(label: String, value: String) =
    copy(label, value, context.getString(R.string.media_info_copied, label))

  fun copy(clipLabel: String, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(clipLabel, text))
    showMessage(message)
  }

  fun showMessage(message: String) {
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(message)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MediaInfoScreen(
  viewModel: MediaInfoViewModel,
  onBack: () -> Unit,
) {
  val state by viewModel.state.collectAsState()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val copyActions = remember(context) { CopyActions(context, scope, snackbarHostState) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  // Lives here, above AnimatedContent, so the selected tab survives retry / reload.
  val pagerState = rememberPagerState(pageCount = { 2 })
  val details = (state as? MediaInfoUiState.Loaded)?.details
  val subtitleText = details?.subtitle?.takeIf { it.isNotBlank() }

  val surface = MaterialTheme.colorScheme.surface
  val scrolledSurface = MaterialTheme.colorScheme.surfaceContainer

  // Flips only when the bar crosses the halfway point, so scrolling doesn't recompose the title.
  val titleCollapsed by remember { derivedStateOf { scrollBehavior.state.collapsedFraction > 0.5f } }
  val titleFallback = stringResource(R.string.media_info_title_fallback)

  val onShareFailure: (Throwable) -> Unit = { e ->
    val message =
      if (e is ActivityNotFoundException) {
        R.string.media_info_share_no_app
      } else {
        R.string.media_info_share_failed_generic
      }
    copyActions.showMessage(context.getString(message))
  }

  Scaffold(
    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = surface,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      LargeFlexibleTopAppBar(
        title = {
          Text(
            text = details?.title?.takeIf { it.isNotBlank() } ?: titleFallback,
            // Emphasized M3 styles instead of the bar's oversized default: a readable
            // two lines while expanded, one compact line once collapsed.
            style =
              if (titleCollapsed) {
                MaterialTheme.typography.titleMediumEmphasized
              } else {
                MaterialTheme.typography.titleLargeEmphasized
              },
            maxLines = if (titleCollapsed) 1 else 2,
            overflow = TextOverflow.Ellipsis,
          )
        },
        subtitle =
          if (subtitleText != null) {
            { Text(text = subtitleText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
          } else {
            null
          },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.media_info_back),
            )
          }
        },
        actions = {
          if (details != null) {
            OverflowMenu(
              contentDescription = stringResource(R.string.media_info_more_options),
              groups =
                listOf(
                  listOf(
                    MenuEntry(stringResource(R.string.media_info_copy_summary), Icons.Filled.ContentCopy) {
                      copyActions.copy(
                        details.title,
                        details.summaryText,
                        context.getString(R.string.media_info_copied_summary),
                      )
                    },
                    MenuEntry(stringResource(R.string.media_info_copy_report), Icons.Filled.ContentCopy) {
                      copyActions.copy(
                        details.title,
                        details.reportText,
                        context.getString(R.string.media_info_copied_report),
                      )
                    },
                  ),
                  listOf(
                    MenuEntry(stringResource(R.string.media_info_share_summary), Icons.Filled.Share) {
                      shareText(context, details.title, details.summaryText).onFailure(onShareFailure)
                    },
                    MenuEntry(stringResource(R.string.media_info_share_report), Icons.Filled.Share) {
                      scope.launch {
                        shareReportFile(context, details.fileName, details.reportText).onFailure(onShareFailure)
                      }
                    },
                  ),
                ),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = surface,
            scrolledContainerColor = scrolledSurface,
          ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    val enterSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    AnimatedContent(
      targetState = state,
      modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
      transitionSpec = { fadeIn(enterSpec) togetherWith fadeOut(exitSpec) },
      contentKey = { it::class },
      label = "media_info_state",
    ) { current ->
      when (current) {
        MediaInfoUiState.Loading -> MediaInfoLoading()
        is MediaInfoUiState.Error -> MediaInfoError(current.message, onRetry = viewModel::retry)
        is MediaInfoUiState.Loaded ->
          MediaInfoTabs(
            details = current.details,
            copyActions = copyActions,
            pagerState = pagerState,
            // Same colour the large top bar fades to when collapsed, so bar and tabs match.
            tabColor = {
              lerp(surface, scrolledSurface, FastOutLinearInEasing.transform(scrollBehavior.state.collapsedFraction))
            },
          )
      }
    }
  }
}

@Composable
private fun MediaInfoTabs(
  details: MediaInfoDetails,
  copyActions: CopyActions,
  pagerState: PagerState,
  tabColor: () -> androidx.compose.ui.graphics.Color,
) {
  val scope = rememberCoroutineScope()
  val tabs = listOf(R.string.media_info_tab_overview, R.string.media_info_tab_report)

  Column(Modifier.fillMaxSize()) {
    SecondaryTabRow(
      selectedTabIndex = pagerState.currentPage,
      // The colour follows the top bar while it collapses. It's drawn behind a transparent
      // tab row so scrolling only redraws it, without recomposing the tabs and pager.
      modifier = Modifier.drawBehind { drawRect(tabColor()) },
      containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) {
      tabs.forEachIndexed { index, titleRes ->
        Tab(
          selected = pagerState.currentPage == index,
          onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
          text = { Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall) },
        )
      }
    }

    HorizontalPager(
      state = pagerState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalAlignment = Alignment.Top,
    ) { page ->
      when (page) {
        0 -> MediaInfoOverview(details, copyActions)
        else -> MediaInfoReport(details.reportSections, copyActions)
      }
    }
  }
}

/** One row in the overflow menu. */
private class MenuEntry(
  val label: String,
  val icon: ImageVector,
  val action: () -> Unit,
)

/** A single overflow menu; each inner list is a group, and groups are separated by a divider. */
@Composable
private fun OverflowMenu(
  contentDescription: String,
  groups: List<List<MenuEntry>>,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(Icons.Filled.MoreVert, contentDescription = contentDescription)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      groups.forEachIndexed { index, group ->
        if (index > 0) HorizontalDivider()
        group.forEach { entry ->
          DropdownMenuItem(
            text = { Text(entry.label) },
            leadingIcon = { Icon(entry.icon, contentDescription = null) },
            onClick = {
              expanded = false
              entry.action()
            },
          )
        }
      }
    }
  }
}

// ── Loading and error ──────────────────────────────────────────────────────

@Composable
private fun MediaInfoLoading() {
  // One animation drives every block, so the shimmer sweeps in sync. It's only read while
  // drawing, so it never recomposes the placeholders.
  val progress =
    rememberInfiniteTransition(label = "media_info_skeleton").animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
      label = "shimmer",
    )

  // Placeholders shaped like the content that's coming: chips, a summary card, then sections.
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      repeat(4) { SkeletonBlock(Modifier.width(72.dp).height(32.dp), 16.dp, progress) }
    }
    SkeletonBlock(Modifier.fillMaxWidth().height(96.dp), 28.dp, progress)
    repeat(2) { SkeletonSection(progress) }
  }
}

@Composable
private fun SkeletonSection(progress: State<Float>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SkeletonBlock(Modifier.width(120.dp).height(16.dp), 8.dp, progress)
    repeat(3) { SkeletonBlock(Modifier.fillMaxWidth().height(56.dp), 20.dp, progress) }
  }
}

@Composable
private fun SkeletonBlock(modifier: Modifier, radius: Dp, progress: State<Float>) {
  val base = MaterialTheme.colorScheme.surfaceContainerHigh
  val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
  Box(
    modifier
      .clip(RoundedCornerShape(radius))
      .drawBehind {
        val width = size.width
        val shift = (progress.value * 2f - 0.5f) * width
        drawRect(
          Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(shift - width, 0f),
            end = Offset(shift, 0f),
          ),
        )
      },
  )
}

@Composable
private fun MediaInfoError(message: String?, onRetry: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier
          .size(96.dp)
          .clip(MaterialShapes.Cookie9Sided.toShape())
          .background(MaterialTheme.colorScheme.errorContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        Icons.Filled.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(44.dp),
        tint = MaterialTheme.colorScheme.onErrorContainer,
      )
    }
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(R.string.media_info_error_title),
      style = MaterialTheme.typography.titleLargeEmphasized,
      textAlign = TextAlign.Center,
    )
    if (!message.isNullOrBlank()) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry) {
      Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(stringResource(R.string.media_info_retry))
    }
  }
}

// ── Sharing ────────────────────────────────────────────────────────────────

/** Fails with [ActivityNotFoundException] when no app can handle the share. */
private fun shareText(context: Context, subject: String, text: String): Result<Unit> =
  runCatching {
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
      }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.media_info_share_chooser)))
  }

/** The full report is shared as a .txt file: it can be long, and keeps its column layout. */
private suspend fun shareReportFile(context: Context, fileName: String, report: String): Result<Unit> =
  runCatching {
    val safeName = fileName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
    val file =
      withContext(Dispatchers.IO) {
        File(context.cacheDir, "mediainfo_$safeName.txt").apply { writeText(report) }
      }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.media_info_share_chooser)))
  }
