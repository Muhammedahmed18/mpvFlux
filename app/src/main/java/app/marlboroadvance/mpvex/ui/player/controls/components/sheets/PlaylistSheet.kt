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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import java.io.File
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
// PlaylistSheet — sheet container
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSheet(
  playlist: ImmutableList<PlaylistItem>,
  onDismissRequest: () -> Unit,
  onItemClick: (PlaylistItem) -> Unit,
  playerPreferences: app.marlboroadvance.mpvex.preferences.PlayerPreferences,
  modifier: Modifier = Modifier,
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
  
  // Logic for toggling watched view
  var isShowingWatched by remember { mutableStateOf(false) }
  
  val queue = remember(playlist) {
    playlist.filterNot { it.isPlaying || it.isWatched }.toImmutableList()
  }
  
  val watchedList = remember(playlist) {
    playlist.filter { it.isWatched && !it.isPlaying }.toImmutableList()
  }

  val density    = LocalDensity.current
  val windowSize = LocalWindowInfo.current.containerSize
  val screenWidth  = with(density) { windowSize.width.toDp() }
  val screenHeight = with(density) { windowSize.height.toDp() }

  val sheetWidth = if (isListMode) {
    if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 640.dp else 420.dp
  } else screenWidth * 0.85f

  // Two-panel only in landscape when something is playing
  val useTwoPanelLayout = !isPortrait && currentItem != null

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier         = Modifier.fillMaxWidth(),
    customMaxWidth   = sheetWidth,
    customMaxHeight  = if (isPortrait) screenHeight * 0.7f else screenHeight * 0.85f,
  ) {
    // ── Drag handle — always full-width at the very top ──────────────────────
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

    if (useTwoPanelLayout) {
      // ── Landscape: hero card left | queue right ───────────────────────────
      Row(
        modifier = modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(bottom = MaterialTheme.spacing.smaller),
      ) {
        // Left panel: sidebar + One UI Toggle Pill
        Column(
          modifier         = Modifier
            .weight(0.38f)
            .fillMaxHeight()
            .padding(
              top    = MaterialTheme.spacing.small,
              bottom = MaterialTheme.spacing.medium,
            ),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          PlaylistNowPlayingPanel(
            item                = currentItem!!,
            thumbnailRepository = thumbnailRepository,
            onClick             = { onItemClick(currentItem) },
            accentColor         = accentColor,
            skipThumbnail       = isM3UPlaylist,
          )
          
          Spacer(modifier = Modifier.weight(1f))
          
          if (watchedList.isNotEmpty()) {
            OneUI8TogglePill(
              isShowingWatched = isShowingWatched,
              onClick = { isShowingWatched = !isShowingWatched }
            )
          }
        }

        VerticalDivider(
          modifier  = Modifier
            .fillMaxHeight()
            .padding(vertical = MaterialTheme.spacing.medium),
          thickness = 1.dp,
          color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        // Right panel: header + scrollable content
        Column(
          modifier = Modifier
            .weight(0.62f)
            .fillMaxHeight(),
        ) {
          val activeList = if (isShowingWatched) watchedList else queue
          val headerTitle = when {
            isShowingWatched -> "Watched"
            currentItem != null -> "Up Next"
            else -> "Playlist"
          }

          PlaylistQueueHeader(
            count       = activeList.size,
            title       = headerTitle,
            isListMode  = isListMode,
            isPortrait  = false,
            onListMode  = { isListMode = it },
          )
          PlaylistQueueContent(
            modifier            = Modifier.weight(1f),
            queue               = activeList,
            isListMode          = isListMode,
            lazyListState       = lazyListState,
            lazyGridState       = lazyGridState,
            thumbnailRepository = thumbnailRepository,
            accentColor         = accentColor,
            isM3UPlaylist       = isM3UPlaylist,
            loadingItemIndex    = loadingItemIndex,
            onItemClick         = onItemClick,
          )
        }
      }
    } else {
      // ── Portrait (or landscape with nothing playing): single column ────────
      Column(
        modifier = modifier
          .padding(bottom = MaterialTheme.spacing.smaller)
          .animateContentSize(),
      ) {
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

        if (currentItem != null) {
          HorizontalDivider(
            modifier  = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = 4.dp),
            thickness = 1.dp,
            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
          )
        }

        PlaylistQueueHeader(
          count       = queue.size,
          title       = if (currentItem != null) "Up Next" else "Playlist",
          isListMode  = isListMode,
          isPortrait  = isPortrait,
          onListMode  = { isListMode = it },
        )

        PlaylistQueueContent(
          queue               = queue,
          isListMode          = isListMode,
          lazyListState       = lazyListState,
          lazyGridState       = lazyGridState,
          thumbnailRepository = thumbnailRepository,
          accentColor         = accentColor,
          isM3UPlaylist       = isM3UPlaylist,
          loadingItemIndex    = loadingItemIndex,
          onItemClick         = onItemClick,
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------
// OneUI8TogglePill — Stylish pill-shaped switcher
// ---------------------------------------------------------------------------

@Composable
private fun OneUI8TogglePill(
  isShowingWatched: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  Surface(
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    modifier     = modifier
      .height(48.dp)
      .padding(horizontal = 16.dp),
    shape        = CircleShape,
    color        = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shadowElevation = 4.dp,
  ) {
    Row(
      modifier              = Modifier.padding(horizontal = 20.dp),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = if (isShowingWatched) Icons.AutoMirrored.Default.ViewList else Icons.Outlined.Check,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text  = if (isShowingWatched) "Next Up" else "Watched",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
      )
    }
  }
}

// ---------------------------------------------------------------------------
// PlaylistQueueHeader — "Up Next" / "Playlist" row with badge and view toggles
// ---------------------------------------------------------------------------

@Composable
private fun PlaylistQueueHeader(
  count: Int,
  title: String,
  isListMode: Boolean,
  isPortrait: Boolean,
  onListMode: (Boolean) -> Unit,
) {
  Row(
    modifier              = Modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
    verticalAlignment     = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      Text(
        text  = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Badge(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
      ) {
        Text(
          text  = "$count",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      FilledIconToggleButton(
        checked         = isListMode,
        onCheckedChange = { if (!isPortrait && !isListMode) onListMode(true) },
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
        onCheckedChange = { if (!isPortrait && isListMode) onListMode(false) },
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
}

// ---------------------------------------------------------------------------
// PlaylistQueueContent — animated list / grid switcher
// ---------------------------------------------------------------------------

@Composable
private fun PlaylistQueueContent(
  queue: ImmutableList<PlaylistItem>,
  isListMode: Boolean,
  lazyListState: LazyListState,
  lazyGridState: LazyGridState,
  thumbnailRepository: ThumbnailRepository,
  accentColor: Color,
  isM3UPlaylist: Boolean,
  loadingItemIndex: Int,
  onItemClick: (PlaylistItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedContent(
    targetState    = isListMode,
    modifier       = modifier,
    transitionSpec = {
      val enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
        slideInVertically(
          spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
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
        if (queue.isEmpty()) {
          item(key = "up-next-empty") {
            AllCaughtUpHint(accentColor = accentColor, modifier = Modifier.animateItem())
          }
        }
        itemsIndexed(queue, key = { _, item -> item.index }) { position, item ->
          PlaylistTrackListItem(
            item                = item,
            thumbnailRepository = thumbnailRepository,
            onClick             = { onItemClick(item) },
            accentColor         = accentColor,
            modifier            = Modifier.animateItem(),
            skipThumbnail       = isM3UPlaylist,
            isLoading           = item.index == loadingItemIndex,
            queuePosition       = position,
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
        if (queue.isEmpty()) {
          item(key = "up-next-empty", span = { GridItemSpan(maxLineSpan) }) {
            AllCaughtUpHint(accentColor = accentColor, modifier = Modifier.animateItem())
          }
        }
        itemsIndexed(queue, key = { _, item -> item.index }) { position, item ->
          PlaylistTrackGridItem(
            item                = item,
            thumbnailRepository = thumbnailRepository,
            onClick             = { onItemClick(item) },
            accentColor         = accentColor,
            modifier            = Modifier.animateItem(),
            skipThumbnail       = isM3UPlaylist,
            isLoading           = item.index == loadingItemIndex,
            queuePosition       = position,
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// AllCaughtUpHint — shown when nothing is left in "Up Next"
// ---------------------------------------------------------------------------

@Composable
private fun AllCaughtUpHint(
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier              = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
    verticalAlignment     = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
  ) {
    Icon(
      imageVector        = Icons.Outlined.Check,
      contentDescription = null,
      tint               = accentColor,
      modifier           = Modifier.size(18.dp),
    )
    Text(
      text  = "Nothing here yet",
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
// rememberPlaylistVideo — build a Video stub from a PlaylistItem
// ---------------------------------------------------------------------------
// Reads size + last-modified from the file at the item's path so the thumbnail key
// (size|dateModified) matches the one the browser computes — restoring cross-screen
// thumbnail reuse. Falls back to 0 for content URIs / network / missing files.
@Composable
private fun rememberPlaylistVideo(item: PlaylistItem): Video =
  remember(item.uri, item.path, item.title, item.index) {
    val cleanPath = run {
      val withoutPrefix = item.path.removePrefix("file://")
      try { URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString()) }
      catch (_: Exception) { withoutPrefix }
    }
    val file = runCatching { File(cleanPath) }.getOrNull()
    val exists = file?.exists() == true
    val size = if (exists) file!!.length() else 0L
    val dateModified = if (exists) file!!.lastModified() / 1000 else 0L
    Video(
      id = item.index.toLong(), uri = item.uri, displayName = item.title,
      title = item.title.substringBeforeLast("."), path = cleanPath,
      duration = 0L, durationFormatted = item.duration, size = size, sizeFormatted = "",
      dateModified = dateModified, dateAdded = 0L, mimeType = "", bucketId = "", bucketDisplayName = "",
      width = 0, height = 0, fps = 0f, resolution = item.resolution,
      subtitleCodec = "", hasEmbeddedSubtitles = false,
    )
  }

// ---------------------------------------------------------------------------
// PlaylistHeroCard — large "now playing" card pinned at the top / left panel
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
  val video = rememberPlaylistVideo(item)

  val thumbWidthPx  = with(LocalDensity.current) { 400.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 225.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path) {
    thumbnailRepository.thumbnailKey(video)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video)
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
    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border    = BorderStroke(1.5.dp, accentColor),
  ) {
    Box(
      modifier         = Modifier
        .fillMaxWidth()
        .padding(10.dp)
        .aspectRatio(16f / 9f)
        // outer 28dp − 10dp padding = 18dp for perfect nested radius
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
          shape = RoundedCornerShape(18.dp),
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

      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(0.65f)
          .background(
            Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))),
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

      // Title + resolution — bottom-start over scrim
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

      // Progress bar — flush to bottom edge, inset horizontally only
      if (item.progressPercent > 0f) {
        LinearProgressIndicator(
          progress   = { item.progressPercent / 100f },
          modifier   = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(start = 4.dp, end = 4.dp)
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
// PlaylistNowPlayingPanel — compact landscape sidebar card (thumbnail + info)
// ---------------------------------------------------------------------------

@Composable
private fun PlaylistNowPlayingPanel(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit,
  accentColor: Color,
  skipThumbnail: Boolean,
) {
  val video = rememberPlaylistVideo(item)

  val thumbWidthPx  = with(LocalDensity.current) { 300.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 169.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path) {
    thumbnailRepository.thumbnailKey(video)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video)
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
      .padding(horizontal = MaterialTheme.spacing.medium),
    shape     = RoundedCornerShape(20.dp),
    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border    = BorderStroke(1.5.dp, accentColor),
  ) {
    Column {
      // ── Thumbnail — fills card width, flush at bottom to meet info section ──
      Box(
        modifier         = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp, start = 8.dp, end = 8.dp)
          .aspectRatio(16f / 9f)
          // outer 20dp card − 8dp padding = 12dp top, 0dp bottom (flush to info)
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
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

        // Progress bar — flush to thumbnail bottom edge
        if (item.progressPercent > 0f) {
          LinearProgressIndicator(
            progress   = { item.progressPercent / 100f },
            modifier   = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .padding(horizontal = 2.dp)
              .align(Alignment.BottomCenter),
            color      = accentColor,
            trackColor = Color.White.copy(alpha = 0.25f),
            strokeCap  = StrokeCap.Round,
          )
        }
      }

      // ── Info section — all metadata below the thumbnail, never overlaid ────
      Column(
        modifier            = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // NOW PLAYING row with equalizer
        Row(
          verticalAlignment     = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PlayingAnimationIndicator(color = accentColor)
          Text(
            text  = "NOW PLAYING",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight    = FontWeight.Bold,
              letterSpacing = 1.5.sp,
            ),
            color = accentColor,
          )
        }

        // Title
        Text(
          text     = item.title,
          style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color    = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        // Resolution + duration on a single line, separated by a dot
        if (item.resolution.isNotEmpty() || item.duration.isNotEmpty()) {
          Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (item.resolution.isNotEmpty()) {
              Text(
                text  = item.resolution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
              )
            }
            if (item.resolution.isNotEmpty() && item.duration.isNotEmpty()) {
              Box(
                modifier = Modifier
                  .size(3.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)),
              )
            }
            if (item.duration.isNotEmpty()) {
              Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Icon(
                  imageVector        = Icons.Outlined.Schedule,
                  contentDescription = null,
                  tint               = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                  modifier           = Modifier.size(10.dp),
                )
                Text(
                  text  = item.duration,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
              }
            }
          }
        }
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
  queuePosition: Int = 0,
) {
  val itemAlpha = if (item.isWatched && !item.isPlaying) 0.7f else 1f

  val video = rememberPlaylistVideo(item)

  val thumbWidthPx  = with(LocalDensity.current) { 120.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 68.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path) {
    thumbnailRepository.thumbnailKey(video)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video)
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
    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
    label         = "list-card-scale",
  )

  // Enter animation — staggered by visual queue position, not playlist index
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay((queuePosition * 30L).coerceAtMost(180L))
    entered = true
  }
  val enterScale by animateFloatAsState(
    targetValue   = if (entered) 1f else 0.92f,
    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
    label         = "list-enter-scale",
  )
  val enterAlpha by animateFloatAsState(
    targetValue   = if (entered) 1f else 0f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium),
    label         = "list-enter-alpha",
  )

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
      shape             = RoundedCornerShape(20.dp),
      colors            = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
      shape             = RoundedCornerShape(20.dp),
      colors            = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
    // Thumbnail — 120×68dp, outer card 20dp − 10dp padding = 10dp ideal; 12dp is a good visual balance
    Box(
      modifier         = Modifier
        .width(120.dp)
        .height(68.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
          shape = RoundedCornerShape(12.dp),
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

      // Index pill — unified dark scrim style (same in both playing/non-playing states)
      Surface(
        modifier     = Modifier.align(Alignment.TopStart).padding(6.dp),
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

      // Progress bar — flush to bottom edge
      if (item.progressPercent > 0f) {
        LinearProgressIndicator(
          progress   = { item.progressPercent / 100f },
          modifier   = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(start = 2.dp, end = 2.dp)
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
        color    = if (item.isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurface,
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
      LoadingIndicator(modifier = Modifier.size(24.dp), color = accentColor)
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
  queuePosition: Int = 0,
) {
  val itemAlpha = if (item.isWatched && !item.isPlaying) 0.7f else 1f

  val video = rememberPlaylistVideo(item)

  val thumbWidthPx  = with(LocalDensity.current) { 200.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 112.dp.roundToPx() }
  val thumbnailKey  = remember(video.id, video.path) {
    thumbnailRepository.thumbnailKey(video)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video)
    }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) { thumbnail = null; return@LaunchedEffect }
    val mem = thumbnailRepository.getThumbnailFromMemory(video)
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
    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
    label         = "grid-card-scale",
  )

  // Enter animation — staggered by visual queue position
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay((queuePosition * 30L).coerceAtMost(180L))
    entered = true
  }
  val enterScale by animateFloatAsState(
    targetValue   = if (entered) 1f else 0.92f,
    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
    label         = "grid-enter-scale",
  )
  val enterAlpha by animateFloatAsState(
    targetValue   = if (entered) 1f else 0f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium),
    label         = "grid-enter-alpha",
  )

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
      colors            = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
      colors            = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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

      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(0.6f)
          .background(
            Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))),
          ),
      )

      // Index pill — unified dark scrim style
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

      // Progress bar — flush to bottom edge
      if (item.progressPercent > 0f) {
        LinearProgressIndicator(
          progress   = { item.progressPercent / 100f },
          modifier   = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(start = 2.dp, end = 2.dp)
            .align(Alignment.BottomCenter),
          color      = accentColor,
          trackColor = Color.White.copy(alpha = 0.2f),
          strokeCap  = StrokeCap.Round,
        )
      }
    }

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
        LoadingIndicator(modifier = Modifier.size(20.dp), color = accentColor)
      } else if (item.isPlaying) {
        PlayingAnimationIndicator(color = accentColor)
      } else if (item.isWatched) {
        WatchedCheckBadge()
      }
    }
  }
}
