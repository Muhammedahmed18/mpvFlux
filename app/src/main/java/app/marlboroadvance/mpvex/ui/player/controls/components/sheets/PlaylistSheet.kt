package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Data & cache
// ---------------------------------------------------------------------------

data class PlaylistItem(
  val uri: Uri,
  val title: String,
  val index: Int,
  val isPlaying: Boolean,
  val progressPercent: Float = 0f,
  val isWatched: Boolean = false,
  val path: String = "",
  val duration: String = "",
  val resolution: String = "",
)

// ---------------------------------------------------------------------------
// PlaylistSheet — sheet container + header
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSheet(
  playlist: ImmutableList<PlaylistItem>,
  onDismissRequest: () -> Unit,
  onItemClick: (PlaylistItem) -> Unit,
  playerPreferences: app.marlboroadvance.mpvex.preferences.PlayerPreferences,
  modifier: Modifier = Modifier,
  totalCount: Int = playlist.size,
  isM3UPlaylist: Boolean = false,
  loadingItemIndex: Int = -1,
) {
  val configuration = LocalConfiguration.current
  val accentColor   = MaterialTheme.colorScheme.primary
  val isPortrait    = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

  val isListModePreference by playerPreferences.playlistViewMode.collectAsState()
  var isListMode by remember { mutableStateOf(if (isPortrait) true else isListModePreference) }

  LaunchedEffect(isPortrait) { if (isPortrait && !isListMode) isListMode = true }
  LaunchedEffect(isListMode) {
    if (!isPortrait && isListMode != isListModePreference) playerPreferences.playlistViewMode.set(isListMode)
  }

  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val lazyListState       = rememberLazyListState()
  val lazyGridState       = rememberLazyGridState()

  val currentItem = remember(playlist) { playlist.find { it.isPlaying } }
  val queue = remember(playlist) {
    playlist.filterNot { it.isPlaying }.toImmutableList()
  }

  val density = LocalDensity.current
  val windowSize = LocalWindowInfo.current.containerSize
  val screenWidth = with(density) { windowSize.width.toDp() }
  val screenHeight = with(density) { windowSize.height.toDp() }

  val sheetWidth  = if (isListMode) {
    if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 640.dp else 420.dp
  } else screenWidth * 0.85f

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier         = Modifier.fillMaxWidth(),
    customMaxWidth   = sheetWidth,
    customMaxHeight  = when {
      isPortrait  -> screenHeight * 0.7f
      !isListMode -> screenHeight * 0.85f
      else        -> null
    },
  ) {
    Column(
      modifier = modifier
        .padding(vertical = MaterialTheme.spacing.smaller)
        .animateContentSize(),
    ) {
      // ── Drag handle ─────────────────────────────────────────────────────────
      Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.medium),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .width(32.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
      }

      // ── Hero card — currently-playing track pulled to the top ──────────────
      AnimatedVisibility(
        visible = currentItem != null,
        enter   = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(
          spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        ),
        exit    = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + shrinkVertically(
          spring(stiffness = Spring.StiffnessMediumLow),
        ),
      ) {
        currentItem?.let {
          PlaylistHeroCard(
            item                = it,
            thumbnailRepository = thumbnailRepository,
            onClick             = { onItemClick(it) },
            accentColor         = accentColor,
            skipThumbnail       = isM3UPlaylist,
          )
        }
      }

      // ── Up Next / Playlist row + count + view-mode toggles ─────────────────
      Row(
        modifier              = Modifier
          .fillMaxWidth()
          .padding(
            horizontal = MaterialTheme.spacing.medium,
            vertical   = MaterialTheme.spacing.small,
          ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment     = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Text(
            text  = if (currentItem != null) "Up Next" else "Playlist",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Badge(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
          ) {
            Text(
              text  = "${queue.size}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }

        // View-mode toggles — disabled in portrait (forced list mode)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilledIconToggleButton(
            checked         = isListMode,
            onCheckedChange = { if (!isPortrait && !isListMode) isListMode = true },
            enabled         = !isPortrait,
            modifier        = Modifier.size(40.dp),
          ) {
            Icon(
              imageVector        = Icons.AutoMirrored.Default.ViewList,
              contentDescription = "List view",
              modifier           = Modifier.size(18.dp),
            )
          }
          FilledIconToggleButton(
            checked         = !isListMode,
            onCheckedChange = { if (!isPortrait && isListMode) isListMode = false },
            enabled         = !isPortrait,
            modifier        = Modifier.size(40.dp),
          ) {
            Icon(
              imageVector        = Icons.Default.GridView,
              contentDescription = "Grid view",
              modifier           = Modifier.size(18.dp),
            )
          }
        }
      }

      // ── Content ───────────────────────────────────────────────────────────
      AnimatedContent(
        targetState    = isListMode,
        transitionSpec = {
          val enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(
              spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness    = Spring.StiffnessMediumLow,
              ),
            ) { 16 }
          val exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
            slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -16 }
          enter togetherWith exit
        },
        label = "view-mode-switch",
      ) { listMode ->
        if (listMode) {
          LazyColumn(
            state    = lazyListState,
            modifier = Modifier.fillMaxWidth(),
          ) {
            items(queue, key = { it.index }) { item ->
              PlaylistTrackListItem(
                item                = item,
                thumbnailRepository = thumbnailRepository,
                onClick             = { onItemClick(item) },
                accentColor         = accentColor,
                modifier            = Modifier.animateItem(),
                skipThumbnail       = isM3UPlaylist,
                isLoading           = item.index == loadingItemIndex,
              )
            }
          }
        } else {
          LazyVerticalGrid(
            state                 = lazyGridState,
            columns               = GridCells.Adaptive(minSize = 200.dp),
            contentPadding        = PaddingValues(
              horizontal = MaterialTheme.spacing.medium,
              vertical   = MaterialTheme.spacing.small,
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement   = Arrangement.spacedBy(MaterialTheme.spacing.small),
          ) {
            items(queue, key = { it.index }) { item ->
              PlaylistTrackGridItem(
                item                = item,
                thumbnailRepository = thumbnailRepository,
                onClick             = { onItemClick(item) },
                accentColor         = accentColor,
                modifier            = Modifier.animateItem(),
                skipThumbnail       = isM3UPlaylist,
                isLoading           = item.index == loadingItemIndex,
              )
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// PlayingAnimationIndicator — three animated bars
// ---------------------------------------------------------------------------

@Composable
fun PlayingAnimationIndicator(
  color: Color,
  modifier: Modifier = Modifier,
) {
  val bar1 = remember { Animatable(0.3f) }
  val bar2 = remember { Animatable(0.55f) }
  val bar3 = remember { Animatable(0.2f) }

  LaunchedEffect(Unit) {
    launch {
      while (true) {
        bar1.animateTo(0.85f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        bar1.animateTo(0.30f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
      }
    }
    launch {
      while (true) {
        bar2.animateTo(0.95f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
        bar2.animateTo(0.45f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
      }
    }
    launch {
      while (true) {
        bar3.animateTo(0.75f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
        bar3.animateTo(0.20f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
      }
    }
  }

  Row(
    modifier              = modifier.height(14.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment     = Alignment.Bottom,
  ) {
    Box(Modifier.width(3.dp).fillMaxHeight(bar1.value).background(color, RoundedCornerShape(1.5.dp)))
    Box(Modifier.width(3.dp).fillMaxHeight(bar2.value).background(color, RoundedCornerShape(1.5.dp)))
    Box(Modifier.width(3.dp).fillMaxHeight(bar3.value).background(color, RoundedCornerShape(1.5.dp)))
  }
}

// ---------------------------------------------------------------------------
// WatchedCheckBadge — filled circular check for finished items
// ---------------------------------------------------------------------------

@Composable
private fun WatchedCheckBadge(modifier: Modifier = Modifier) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { visible = true }
  val scale by animateFloatAsState(
    targetValue   = if (visible) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness    = Spring.StiffnessMedium,
    ),
    label = "watched-scale",
  )
  Box(
    modifier         = modifier
      .size(20.dp)
      .scale(scale)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.primary),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector        = Icons.Outlined.Check,
      contentDescription = "Watched",
      tint               = MaterialTheme.colorScheme.onPrimary,
      modifier           = Modifier.size(13.dp),
    )
  }
}

// ---------------------------------------------------------------------------
// PlaylistHeroCard — large "now playing" card pinned at the top of the sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistHeroCard(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit,
  accentColor: Color,
  skipThumbnail: Boolean,
) {
  val cleanPath = remember(item.path) {
    val withoutPrefix = item.path.removePrefix("file://")
    try { URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString()) }
    catch (_: Exception) { withoutPrefix }
  }
  val video = remember(item.uri, cleanPath, item.title) {
    Video(
      id = item.index.toLong(), uri = item.uri, displayName = item.title,
      title = item.title.substringBeforeLast("."), path = cleanPath,
      duration = 0L, durationFormatted = item.duration, size = 0L, sizeFormatted = "",
      dateModified = 0L, dateAdded = 0L, mimeType = "", bucketId = "", bucketDisplayName = "",
      width = 0, height = 0, fps = 0f, resolution = item.resolution,
      subtitleCodec = "", hasEmbeddedSubtitles = false,
    )
  }

  val thumbWidthPx  = with(LocalDensity.current) { 400.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 225.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path, thumbWidthPx, thumbHeightPx) {
    thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    if (mem != null) { thumbnail = mem; return@LaunchedEffect }
    val loaded = withContext(Dispatchers.IO) { thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx) }
    if (loaded != null) thumbnail = loaded
  }

  val haptics = LocalHapticFeedback.current
  val onClickWithHaptic = {
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    onClick()
  }

  Card(
    onClick   = onClickWithHaptic,
    modifier  = Modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = 6.dp),
    shape     = RoundedCornerShape(28.dp),
    colors    = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border    = BorderStroke(1.5.dp, accentColor),
  ) {
    Box(
      modifier         = Modifier
        .fillMaxWidth()
        .padding(10.dp)
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
          shape = RoundedCornerShape(20.dp),
        ),
      contentAlignment = Alignment.Center,
    ) {
      thumbnail?.let { bmp ->
        Image(
          bitmap             = bmp.asImageBitmap(),
          contentDescription = null,
          modifier           = Modifier.matchParentSize(),
          contentScale       = ContentScale.Crop,
        )
      } ?: Icon(
        imageVector        = Icons.Outlined.Movie,
        contentDescription = null,
        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier           = Modifier.size(48.dp),
      )

      // Bottom scrim — keeps title legible regardless of thumbnail content
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(0.65f)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
            ),
          ),
      )

      // NOW PLAYING eyebrow + equalizer — top-start
      Surface(
        modifier     = Modifier.align(Alignment.TopStart).padding(10.dp),
        color        = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        shape        = CircleShape,
      ) {
        Row(
          modifier              = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment     = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PlayingAnimationIndicator(color = Color.White)
          Text(
            text  = "NOW PLAYING",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight    = FontWeight.Bold,
              letterSpacing = 1.5.sp,
            ),
          )
        }
      }

      // Duration pill — top-end
      if (item.duration.isNotEmpty()) {
        Surface(
          modifier     = Modifier.align(Alignment.TopEnd).padding(10.dp),
          color        = Color.Black.copy(alpha = 0.55f),
          contentColor = Color.White,
          shape        = CircleShape,
        ) {
          Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector        = Icons.Outlined.Schedule,
              contentDescription = null,
              modifier           = Modifier.size(12.dp),
            )
            Text(
              text  = item.duration,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            )
          }
        }
      }

      // Title overlay — bottom-start
      Column(
        modifier            = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text     = item.title,
          style    = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color    = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (item.resolution.isNotEmpty()) {
          Text(
            text  = item.resolution,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f),
          )
        }
      }

      // Progress bar — bottom, inset, rounded ends, slightly thicker than card progress
      if (item.progressPercent > 0f) {
        LinearProgressIndicator(
          progress   = { item.progressPercent / 100f },
          modifier   = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
            .align(Alignment.BottomCenter),
          color      = accentColor,
          trackColor = Color.White.copy(alpha = 0.25f),
          strokeCap  = StrokeCap.Round,
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------
// PlaylistTrackListItem — M3 Card-based list row
// ---------------------------------------------------------------------------

@Composable
fun PlaylistTrackListItem(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit,
  accentColor: Color,
  modifier: Modifier = Modifier,
  skipThumbnail: Boolean = false,
  isLoading: Boolean = false,
) {
  val itemAlpha = if (item.isWatched && !item.isPlaying) 0.7f else 1f

  val cleanPath = remember(item.path) {
    val withoutPrefix = item.path.removePrefix("file://")
    try { URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString()) }
    catch (_: Exception) { withoutPrefix }
  }
  val video = remember(item.uri, cleanPath, item.title) {
    Video(
      id = item.index.toLong(), uri = item.uri, displayName = item.title,
      title = item.title.substringBeforeLast("."), path = cleanPath,
      duration = 0L, durationFormatted = item.duration, size = 0L, sizeFormatted = "",
      dateModified = 0L, dateAdded = 0L, mimeType = "", bucketId = "", bucketDisplayName = "",
      width = 0, height = 0, fps = 0f, resolution = item.resolution,
      subtitleCodec = "", hasEmbeddedSubtitles = false,
    )
  }

  val thumbWidthPx  = with(LocalDensity.current) { 100.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 56.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path, thumbWidthPx, thumbHeightPx) {
    thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    if (mem != null) { thumbnail = mem; return@LaunchedEffect }
    val loaded = withContext(Dispatchers.IO) { thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx) }
    if (loaded != null) thumbnail = loaded
  }

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val baseScale by animateFloatAsState(
    targetValue   = when {
      isPressed      -> 0.98f
      item.isPlaying -> 1.02f
      else           -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness    = Spring.StiffnessMediumLow,
    ),
    label = "list-card-scale",
  )

  // Enter animation — staggered for the first viewport, instant fade-in past that
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay((item.index * 25L).coerceAtMost(200L))
    entered = true
  }
  val enterScale by animateFloatAsState(
    targetValue   = if (entered) 1f else 0.92f,
    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
    label         = "list-enter-scale",
  )
  val enterAlpha by animateFloatAsState(
    targetValue   = if (entered) 1f else 0f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label         = "list-enter-alpha",
  )

  // One-shot pulse when this card becomes the currently-playing item
  val pulse = remember { Animatable(1f) }
  LaunchedEffect(item.isPlaying) {
    if (item.isPlaying) {
      pulse.snapTo(1f)
      pulse.animateTo(1.04f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
      pulse.animateTo(1f,    spring(Spring.DampingRatioLowBouncy,   Spring.StiffnessMediumLow))
    }
  }

  val haptics = LocalHapticFeedback.current
  val onClickWithHaptic = {
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    onClick()
  }

  val finalScale = enterScale * baseScale * pulse.value
  val finalAlpha = enterAlpha * itemAlpha

  val cardModifier = modifier
    .fillMaxWidth()
    .padding(horizontal = MaterialTheme.spacing.medium, vertical = 6.dp)
    .scale(finalScale)
    .alpha(finalAlpha)

  if (item.isPlaying) {
    Card(
      onClick           = onClickWithHaptic,
      modifier          = cardModifier,
      shape             = RoundedCornerShape(28.dp),
      colors            = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
      ),
      elevation         = CardDefaults.cardElevation(defaultElevation = 0.dp),
      border            = BorderStroke(1.5.dp, accentColor),
      interactionSource = interactionSource,
    ) {
      PlaylistListItemContent(item, thumbnail, accentColor, isLoading)
    }
  } else {
    Card(
      onClick           = onClickWithHaptic,
      modifier          = cardModifier,
      shape             = RoundedCornerShape(24.dp),
      colors            = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
      elevation         = CardDefaults.cardElevation(defaultElevation = 0.dp),
      interactionSource = interactionSource,
    ) {
      PlaylistListItemContent(item, thumbnail, accentColor, isLoading)
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistListItemContent(
  item: PlaylistItem,
  thumbnail: Bitmap?,
  accentColor: Color,
  isLoading: Boolean,
) {
  Row(
    modifier              = Modifier.fillMaxWidth().padding(10.dp),
    verticalAlignment     = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier         = Modifier
        .width(100.dp)
        .height(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
          shape = RoundedCornerShape(16.dp),
        ),
      contentAlignment = Alignment.Center,
    ) {
      thumbnail?.let { bmp ->
        Image(
          bitmap             = bmp.asImageBitmap(),
          contentDescription = null,
          modifier           = Modifier.matchParentSize(),
          contentScale       = ContentScale.Crop,
        )
      } ?: Icon(
        imageVector        = Icons.Outlined.Movie,
        contentDescription = null,
        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier           = Modifier.size(24.dp),
      )

      // Index pill — primaryContainer for accent pop on both card states
      Surface(
        modifier     = Modifier.align(Alignment.TopStart).padding(6.dp),
        color        = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape        = CircleShape,
      ) {
        Text(
          text     = "${item.index + 1}",
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
      }

      if (item.duration.isNotEmpty()) {
        Surface(
          modifier     = Modifier.align(Alignment.BottomEnd).padding(4.dp),
          color        = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
          contentColor = Color.White,
          shape        = CircleShape,
        ) {
          Row(
            modifier              = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector        = Icons.Outlined.Schedule,
              contentDescription = null,
              modifier           = Modifier.size(10.dp),
            )
            Text(
              text  = item.duration,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }

      if (item.progressPercent > 0f) {
        LinearProgressIndicator(
          progress   = { item.progressPercent / 100f },
          modifier   = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(start = 2.dp, end = 2.dp, bottom = 4.dp)
            .align(Alignment.BottomCenter),
          color      = accentColor,
          trackColor = Color.White.copy(alpha = 0.2f),
          strokeCap  = StrokeCap.Round,
        )
      }
    }

    Column(
      modifier            = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text     = item.title,
        style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color    = if (item.isPlaying) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (item.resolution.isNotEmpty()) {
        Text(
          text  = item.resolution,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (isLoading) {
      LoadingIndicator(
        modifier = Modifier.size(24.dp),
        color    = accentColor,
      )
    } else if (item.isPlaying) {
      PlayingAnimationIndicator(color = accentColor)
    } else if (item.isWatched) {
      WatchedCheckBadge()
    }
  }
}

// ---------------------------------------------------------------------------
// PlaylistTrackGridItem — M3 Card-based grid card
// ---------------------------------------------------------------------------

@Composable
fun PlaylistTrackGridItem(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit,
  accentColor: Color,
  modifier: Modifier = Modifier,
  skipThumbnail: Boolean = false,
  isLoading: Boolean = false,
) {
  val itemAlpha = if (item.isWatched && !item.isPlaying) 0.7f else 1f

  val cleanPath = remember(item.path) {
    val withoutPrefix = item.path.removePrefix("file://")
    try { URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString()) }
    catch (_: Exception) { withoutPrefix }
  }
  val video = remember(item.uri, cleanPath, item.title) {
    Video(
      id = item.index.toLong(), uri = item.uri, displayName = item.title,
      title = item.title.substringBeforeLast("."), path = cleanPath,
      duration = 0L, durationFormatted = item.duration, size = 0L, sizeFormatted = "",
      dateModified = 0L, dateAdded = 0L, mimeType = "", bucketId = "", bucketDisplayName = "",
      width = 0, height = 0, fps = 0f, resolution = item.resolution,
      subtitleCodec = "", hasEmbeddedSubtitles = false,
    )
  }

  val thumbWidthPx  = with(LocalDensity.current) { 200.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 112.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path, thumbWidthPx, thumbHeightPx) {
    thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    if (mem != null) { thumbnail = mem; return@LaunchedEffect }
    val loaded = withContext(Dispatchers.IO) { thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx) }
    if (loaded != null) thumbnail = loaded
  }

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val baseScale by animateFloatAsState(
    targetValue   = when {
      isPressed      -> 0.98f
      item.isPlaying -> 1.02f
      else           -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness    = Spring.StiffnessMediumLow,
    ),
    label = "grid-card-scale",
  )

  // Enter animation — staggered for the first viewport, instant past that
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay((item.index * 25L).coerceAtMost(200L))
    entered = true
  }
  val enterScale by animateFloatAsState(
    targetValue   = if (entered) 1f else 0.92f,
    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
    label         = "grid-enter-scale",
  )
  val enterAlpha by animateFloatAsState(
    targetValue   = if (entered) 1f else 0f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label         = "grid-enter-alpha",
  )

  // One-shot pulse when becoming the currently-playing item
  val pulse = remember { Animatable(1f) }
  LaunchedEffect(item.isPlaying) {
    if (item.isPlaying) {
      pulse.snapTo(1f)
      pulse.animateTo(1.04f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
      pulse.animateTo(1f,    spring(Spring.DampingRatioLowBouncy,   Spring.StiffnessMediumLow))
    }
  }

  val haptics = LocalHapticFeedback.current
  val onClickWithHaptic = {
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    onClick()
  }

  val finalScale = enterScale * baseScale * pulse.value
  val finalAlpha = enterAlpha * itemAlpha

  val cardModifier = modifier
    .fillMaxWidth()
    .scale(finalScale)
    .alpha(finalAlpha)

  if (item.isPlaying) {
    Card(
      onClick           = onClickWithHaptic,
      modifier          = cardModifier,
      shape             = RoundedCornerShape(28.dp),
      colors            = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
      ),
      elevation         = CardDefaults.cardElevation(defaultElevation = 0.dp),
      border            = BorderStroke(1.5.dp, accentColor),
      interactionSource = interactionSource,
    ) {
      PlaylistGridItemContent(item, thumbnail, accentColor, isLoading)
    }
  } else {
    Card(
      onClick           = onClickWithHaptic,
      modifier          = cardModifier,
      shape             = RoundedCornerShape(24.dp),
      colors            = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
      elevation         = CardDefaults.cardElevation(defaultElevation = 0.dp),
      interactionSource = interactionSource,
    ) {
      PlaylistGridItemContent(item, thumbnail, accentColor, isLoading)
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistGridItemContent(
  item: PlaylistItem,
  thumbnail: Bitmap?,
  accentColor: Color,
  isLoading: Boolean,
) {
  Column(
    modifier            = Modifier.padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Thumbnail — title overlaid on a bottom scrim gradient (Netflix/Plex pattern)
    Box(
      modifier         = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
          shape = RoundedCornerShape(20.dp),
        ),
      contentAlignment = Alignment.Center,
    ) {
      thumbnail?.let { bmp ->
        Image(
          bitmap             = bmp.asImageBitmap(),
          contentDescription = null,
          modifier           = Modifier.matchParentSize(),
          contentScale       = ContentScale.Crop,
        )
      } ?: Icon(
        imageVector        = Icons.Outlined.Movie,
        contentDescription = null,
        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier           = Modifier.size(32.dp),
      )

      // Bottom scrim — keeps title legible regardless of thumbnail content
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(0.6f)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
            ),
          ),
      )

      // Index pill — top-left, semi-transparent black with white text
      Surface(
        modifier     = Modifier.align(Alignment.TopStart).padding(8.dp),
        color        = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        shape        = CircleShape,
      ) {
        Text(
          text     = "${item.index + 1}",
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
      }

      // Duration pill — top-right with schedule icon
      if (item.duration.isNotEmpty()) {
        Surface(
          modifier     = Modifier.align(Alignment.TopEnd).padding(8.dp),
          color        = Color.Black.copy(alpha = 0.55f),
          contentColor = Color.White,
          shape        = CircleShape,
        ) {
          Row(
            modifier              = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector        = Icons.Outlined.Schedule,
              contentDescription = null,
              modifier           = Modifier.size(10.dp),
            )
            Text(
              text  = item.duration,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }

      // Title overlay — bottom-start, sits over the scrim
      Text(
        text     = item.title,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color    = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      // Progress bar — bottom, inset, rounded ends
      if (item.progressPercent > 0f) {
        LinearProgressIndicator(
          progress   = { item.progressPercent / 100f },
          modifier   = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(start = 2.dp, end = 2.dp, bottom = 4.dp)
            .align(Alignment.BottomCenter),
          color      = accentColor,
          trackColor = Color.White.copy(alpha = 0.2f),
          strokeCap  = StrokeCap.Round,
        )
      }
    }

    // Bottom row — resolution chip + status indicator
    Row(
      modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      if (item.resolution.isNotEmpty()) {
        Surface(
          color        = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          shape        = CircleShape,
        ) {
          Text(
            text     = item.resolution,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          )
        }
      } else {
        Spacer(modifier = Modifier.width(1.dp))
      }

      if (isLoading) {
        LoadingIndicator(
          modifier = Modifier.size(20.dp),
          color    = accentColor,
        )
      } else if (item.isPlaying) {
        PlayingAnimationIndicator(color = accentColor)
      } else if (item.isWatched) {
        WatchedCheckBadge()
      }
    }
  }
}
