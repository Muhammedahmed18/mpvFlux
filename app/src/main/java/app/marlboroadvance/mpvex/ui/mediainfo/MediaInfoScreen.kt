@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3Api::class,
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package app.marlboroadvance.mpvex.ui.mediainfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
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
 * Copies to the clipboard and confirms with a snackbar. Android 13+ shows its own
 * confirmation whenever an app copies something, so the snackbar is skipped there.
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) showMessage(message)
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
  val thumbnail by viewModel.thumbnail.collectAsState()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val copyActions = remember(context) { CopyActions(context, scope, snackbarHostState) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val details = (state as? MediaInfoUiState.Loaded)?.details
  val subtitleText = details?.subtitle?.takeIf { it.isNotBlank() }

  val surface = MaterialTheme.colorScheme.surface
  val scrolledSurface = MaterialTheme.colorScheme.surfaceContainer

  Scaffold(
    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = surface,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      LargeFlexibleTopAppBar(
        title = {
          Text(
            text = details?.title.orEmpty(),
            maxLines = 1,
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
            ActionMenu(
              icon = Icons.Filled.ContentCopy,
              contentDescription = stringResource(R.string.media_info_copy),
              items =
                listOf(
                  menuItem(stringResource(R.string.media_info_copy_summary)) {
                    copyActions.copy(
                      details.title,
                      details.summaryText,
                      context.getString(R.string.media_info_copied_summary),
                    )
                  },
                  menuItem(stringResource(R.string.media_info_copy_report)) {
                    copyActions.copy(
                      details.title,
                      details.reportText,
                      context.getString(R.string.media_info_copied_report),
                    )
                  },
                ),
            )
            ActionMenu(
              icon = Icons.Filled.Share,
              contentDescription = stringResource(R.string.media_info_share),
              items =
                listOf(
                  menuItem(stringResource(R.string.media_info_share_summary)) {
                    shareText(context, details.title, details.summaryText)
                  },
                  menuItem(stringResource(R.string.media_info_share_report)) {
                    scope.launch {
                      shareReportFile(context, details.fileName, details.reportText)
                        .onFailure { e ->
                          copyActions.showMessage(
                            context.getString(R.string.media_info_share_failed, e.message.orEmpty()),
                          )
                        }
                    }
                  },
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
            thumbnail = thumbnail,
            copyActions = copyActions,
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
  thumbnail: android.graphics.Bitmap?,
  copyActions: CopyActions,
  tabColor: () -> androidx.compose.ui.graphics.Color,
) {
  val pagerState = rememberPagerState(pageCount = { 2 })
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
      key = { it },
    ) { page ->
      when (page) {
        0 -> MediaInfoOverview(details, thumbnail, copyActions)
        else -> MediaInfoReport(details.reportSections, copyActions)
      }
    }
  }
}

/** A menu entry; the action is typed `() -> Unit` explicitly so any body works. */
private fun menuItem(label: String, action: () -> Unit): Pair<String, () -> Unit> = label to action

@Composable
private fun ActionMenu(
  icon: ImageVector,
  contentDescription: String,
  items: List<Pair<String, () -> Unit>>,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(icon, contentDescription = contentDescription)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      items.forEach { (label, action) ->
        DropdownMenuItem(
          text = { Text(label) },
          onClick = {
            expanded = false
            action()
          },
        )
      }
    }
  }
}

// ── Loading and error ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MediaInfoLoading() {
  Box(Modifier.fillMaxSize()) {
    // Placeholders shaped like the content that's coming.
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(16f / 9f), 28.dp)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) { SkeletonBlock(Modifier.width(64.dp).height(32.dp), 12.dp) }
      }
      SkeletonBlock(Modifier.fillMaxWidth().height(88.dp), 28.dp)
      SkeletonBlock(Modifier.fillMaxWidth().height(200.dp), 28.dp)
    }
    ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
private fun SkeletonBlock(modifier: Modifier, radius: Dp) {
  Box(
    modifier
      .graphicsLayer { alpha = 0.7f }
      .clip(RoundedCornerShape(radius))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

private fun shareText(context: Context, subject: String, text: String) {
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
