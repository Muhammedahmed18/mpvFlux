package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// ---------------------------------------------------------------------------
// Data
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

/** Which part of the playlist the list shows. */
private enum class PlaylistFilter(val labelRes: Int) {
  All(R.string.playlist_filter_all),
  UpNext(R.string.playlist_filter_up_next),
  Watched(R.string.playlist_filter_watched),
}

// ---------------------------------------------------------------------------
// PlaylistSheet
// ---------------------------------------------------------------------------

/**
 * Playlist for the player.
 *
 * Portrait: bottom sheet. Landscape: side sheet from the right, so the video stays visible.
 * Layout: header with the episode count and an *All · Up next · Watched* segmented button,
 * a "Now playing" card, then one row per episode (thumbnail with duration and progress, a
 * cleaned-up title, resolution · length, watched check). Opens scrolled to the current
 * episode. Watched episodes are always reachable (they used to be hidden in portrait).
 *
 * [playerPreferences] is kept for the call site; the old grid view is gone.
 */
@Suppress("UNUSED_PARAMETER")
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
  val isPortrait =
    LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
  val windowSize = LocalWindowInfo.current.containerSize
  val density = LocalDensity.current
  val screenWidth = with(density) { windowSize.width.toDp() }
  val screenHeight = with(density) { windowSize.height.toDp() }

  if (isPortrait) {
    PlayerSheet(
      onDismissRequest = onDismissRequest,
      modifier = Modifier.fillMaxWidth(),
      customMaxWidth = 560.dp,
      customMaxHeight = screenHeight * 0.75f,
    ) {
      Column(modifier = modifier) {
        DragHandle()
        PlaylistContent(playlist, onItemClick, isM3UPlaylist, loadingItemIndex)
      }
    }
  } else {
    PlaylistSideSheet(
      onDismissRequest = onDismissRequest,
      width = minOf(400.dp, screenWidth * 0.45f),
    ) {
      PlaylistContent(playlist, onItemClick, isM3UPlaylist, loadingItemIndex, Modifier.padding(top = 12.dp))
    }
  }
}

@Composable
private fun PlaylistContent(
  playlist: ImmutableList<PlaylistItem>,
  onItemClick: (PlaylistItem) -> Unit,
  skipThumbnails: Boolean,
  loadingItemIndex: Int,
  modifier: Modifier = Modifier,
) {
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val current = remember(playlist) { playlist.firstOrNull { it.isPlaying } }
  var filter by rememberSaveable { mutableStateOf(PlaylistFilter.All) }

  val visible = remember(playlist, filter, current) {
    when (filter) {
      PlaylistFilter.All -> playlist.filterNot { it.isPlaying }
      PlaylistFilter.UpNext -> playlist.filter {
        !it.isPlaying && (if (current != null) it.index > current.index else !it.isWatched)
      }
      PlaylistFilter.Watched -> playlist.filter { it.isWatched && !it.isPlaying }
    }
  }

  // Open (and switch filters) with the episode after the current one at the top.
  val listState = rememberLazyListState()
  LaunchedEffect(filter, current?.index) {
    val target = if (filter == PlaylistFilter.All && current != null) {
      visible.indexOfFirst { it.index > current.index }.takeIf { it > 0 } ?: 0
    } else {
      0
    }
    listState.scrollToItem(target)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    PlaylistHeader(count = playlist.size)

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      PlaylistFilter.entries.forEachIndexed { i, option ->
        SegmentedButton(
          selected = filter == option,
          onClick = { filter = option },
          shape = SegmentedButtonDefaults.itemShape(index = i, count = PlaylistFilter.entries.size),
          label = { Text(stringResource(option.labelRes), maxLines = 1) },
        )
      }
    }

    if (current != null) {
      NowPlayingCard(
        item = current,
        thumbnailRepository = thumbnailRepository,
        skipThumbnail = skipThumbnails,
        onClick = { onItemClick(current) },
      )
    }

    if (visible.isEmpty()) {
      EmptyState(filter)
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        items(visible, key = { it.index }) { item ->
          PlaylistRow(
            item = item,
            thumbnailRepository = thumbnailRepository,
            skipThumbnail = skipThumbnails,
            isLoading = item.index == loadingItemIndex,
            onClick = { onItemClick(item) },
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

@Composable
private fun DragHandle() {
  Box(
    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .width(32.dp)
        .height(4.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
    )
  }
}

@Composable
private fun PlaylistHeader(count: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = stringResource(R.string.playlist_title),
      style = MaterialTheme.typography.titleLargeEmphasized,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowPlayingCard(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  skipThumbnail: Boolean,
  onClick: () -> Unit,
) {
  val thumbnail = rememberPlaylistThumbnail(item, thumbnailRepository, skipThumbnail)
  val haptics = LocalHapticFeedback.current
  Surface(
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Thumbnail(
        bitmap = thumbnail,
        duration = "",
        progressPercent = item.progressPercent,
        modifier = Modifier.width(128.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.playlist_now_playing).uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = remember(item.title) { cleanVideoTitle(item.title) },
          style = MaterialTheme.typography.titleSmallEmphasized,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        DetailsLine(item, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistRow(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  skipThumbnail: Boolean,
  isLoading: Boolean,
  onClick: () -> Unit,
) {
  val thumbnail = rememberPlaylistThumbnail(item, thumbnailRepository, skipThumbnail)
  val haptics = LocalHapticFeedback.current
  Surface(
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = Color.Transparent,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Thumbnail(
        bitmap = thumbnail,
        duration = item.duration,
        progressPercent = item.progressPercent,
        modifier = Modifier.width(120.dp),
        dimmed = item.isWatched,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = remember(item.title) { cleanVideoTitle(item.title) },
          style = MaterialTheme.typography.bodyLarge,
          color = if (item.isWatched) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        DetailsLine(item, MaterialTheme.colorScheme.onSurfaceVariant)
      }
      when {
        isLoading -> LoadingIndicator(modifier = Modifier.size(24.dp))
        item.isWatched -> Icon(
          imageVector = ImageVector.vectorResource(R.drawable.ic_sym_check_circle_fill),
          contentDescription = stringResource(R.string.playlist_filter_watched),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

/** "1080p · 23:40" under a title; leaves out parts that are unknown. */
@Composable
private fun DetailsLine(item: PlaylistItem, color: Color) {
  val text = listOf(item.resolution, item.duration).filter { it.isNotBlank() }.joinToString(" · ")
  if (text.isNotEmpty()) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
  }
}

/** 16:9 thumbnail with an optional duration badge and a progress line along the bottom. */
@Composable
private fun Thumbnail(
  bitmap: Bitmap?,
  duration: String,
  progressPercent: Float,
  modifier: Modifier = Modifier,
  dimmed: Boolean = false,
) {
  Box(
    modifier = modifier
      .aspectRatio(16f / 9f)
      .clip(MaterialTheme.shapes.medium)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = if (dimmed) 0.6f else 1f },
        contentScale = ContentScale.Crop,
      )
    } else {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_sym_movie),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
      )
    }

    if (duration.isNotEmpty()) {
      Surface(
        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.65f),
        contentColor = Color.White,
      ) {
        Text(
          text = duration,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
      }
    }

    if (progressPercent > 0f) {
      LinearProgressIndicator(
        progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.White.copy(alpha = 0.25f),
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
      )
    }
  }
}

@Composable
private fun EmptyState(filter: PlaylistFilter) {
  Box(
    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(
        when (filter) {
          PlaylistFilter.Watched -> R.string.playlist_empty_watched
          else -> R.string.playlist_empty_up_next
        },
      ),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

// ---------------------------------------------------------------------------
// Landscape side sheet
// ---------------------------------------------------------------------------

/**
 * Side sheet sliding in from the right edge, full height. Tap outside, or Back, to close.
 * Used in landscape so the playlist doesn't cover most of the video.
 */
@Composable
private fun PlaylistSideSheet(
  onDismissRequest: () -> Unit,
  width: androidx.compose.ui.unit.Dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scope = rememberCoroutineScope()
  // 1 = fully off screen to the right, 0 = fully shown.
  val hidden = remember { Animatable(1f) }
  var closing by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    hidden.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
  }
  val dismiss: () -> Unit = {
    if (!closing) {
      closing = true
      scope.launch {
        hidden.animateTo(1f, tween(200))
        onDismissRequest()
      }
    }
  }
  BackHandler(onBack = dismiss)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.4f * (1f - hidden.value)))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = dismiss,
      ),
  ) {
    Surface(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .fillMaxHeight()
        .width(width)
        .graphicsLayer { translationX = size.width * hidden.value }
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {},
        ),
      shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      Column(
        modifier = Modifier.windowInsetsPadding(
          WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
        ),
        content = content,
      )
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Loads the thumbnail for a playlist item from the shared cache (the same one the browser
 * uses), generating it only if it isn't cached yet.
 */
@Composable
private fun rememberPlaylistThumbnail(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  skipThumbnail: Boolean,
): Bitmap? {
  val video = rememberPlaylistVideo(item)
  val density = LocalDensity.current
  val widthPx = with(density) { 160.dp.roundToPx() }
  val heightPx = with(density) { 90.dp.roundToPx() }
  val key = remember(video.id, video.path) { thumbnailRepository.thumbnailKey(video) }
  var thumbnail by remember(key) { mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video)) }

  LaunchedEffect(key) {
    thumbnailRepository.thumbnailReadyKeys.filter { it == key }.collect {
      thumbnail = thumbnailRepository.getThumbnailFromMemory(video)
    }
  }
  LaunchedEffect(key, skipThumbnail) {
    if (skipThumbnail) {
      thumbnail = null
      return@LaunchedEffect
    }
    if (thumbnail != null) return@LaunchedEffect
    val loaded = withContext(Dispatchers.IO) { thumbnailRepository.getThumbnail(video, widthPx, heightPx) }
    if (loaded != null) thumbnail = loaded
  }
  return thumbnail
}

// Reads size + last-modified from the file so the thumbnail key (size|dateModified) matches
// the one the browser computes, which lets the playlist reuse the browser's thumbnails.
@Composable
private fun rememberPlaylistVideo(item: PlaylistItem): Video =
  remember(item.uri, item.path, item.title, item.index) {
    val cleanPath = run {
      val withoutPrefix = item.path.removePrefix("file://")
      try {
        URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString())
      } catch (_: Exception) {
        withoutPrefix
      }
    }
    val file = runCatching { File(cleanPath) }.getOrNull()
    val exists = file?.exists() == true
    Video(
      id = item.index.toLong(), uri = item.uri, displayName = item.title,
      title = item.title.substringBeforeLast("."), path = cleanPath,
      duration = 0L, durationFormatted = item.duration,
      size = if (exists) file!!.length() else 0L, sizeFormatted = "",
      dateModified = if (exists) file!!.lastModified() / 1000 else 0L,
      dateAdded = 0L, mimeType = "", bucketId = "", bucketDisplayName = "",
      width = 0, height = 0, fps = 0f, resolution = item.resolution,
      subtitleCodec = "", hasEmbeddedSubtitles = false,
    )
  }

private val LEADING_TAGS = Regex("^\\s*(\\[[^\\]]*]\\s*)+")
private val TRAILING_TAGS = Regex("(\\s*[\\[(][^\\])]*[\\])])+\\s*$")
private val FILE_EXTENSION = Regex("\\.[A-Za-z0-9]{2,4}$")
private val SPACES = Regex("\\s{2,}")

/**
 * Turns a release file name into a readable title:
 * "[SubsPlease] Frieren - 05 (1080p) [A1B2C3D4].mkv" → "Frieren - 05".
 * Falls back to the original name if nothing is left.
 */
internal fun cleanVideoTitle(raw: String): String {
  var t = raw.replace(FILE_EXTENSION, "")
  t = t.replace(LEADING_TAGS, "")
  t = t.replace(TRAILING_TAGS, "")
  // Names like "Frieren.S01E05.1080p" or "Frieren_-_05" use dots/underscores as spaces.
  if (!t.contains(' ')) t = t.replace('_', ' ').replace('.', ' ')
  t = t.replace(SPACES, " ").trim()
  return t.ifBlank { raw }
}
