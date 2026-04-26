package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Video.Thumbnails
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext

data class PlaylistItem(
  val uri: Uri,
  val title: String,
  val index: Int,
  val isPlaying: Boolean,
  val progressPercent: Float = 0f, // 0-100, progress of video watched
  val isWatched: Boolean = false,  // True if video is fully watched (100%)
  val path: String = "", // Video path for thumbnail loading
  val duration: String = "", // Duration in formatted string (e.g., "10:30")
  val resolution: String = "", // Resolution (e.g., "1920x1080")
)

/**
 * LRU (Least Recently Used) cache for Bitmap thumbnails with a maximum size limit.
 * This prevents memory issues when dealing with large playlists (100+ videos).
 */
class LRUBitmapCache(private val maxSize: Int) {
  private val cache = LinkedHashMap<String, Bitmap?>(maxSize + 1, 1f, true)

  operator fun get(key: String): Bitmap? = synchronized(this) { cache[key] }

  operator fun set(key: String, value: Bitmap?) = synchronized(this) {
    cache[key] = value
    if (cache.size > maxSize) {
      // Remove the least recently used item
      cache.remove(cache.keys.firstOrNull())
    }
  }

  fun containsKey(key: String): Boolean = synchronized(this) { cache.containsKey(key) }

  fun clear() = synchronized(this) { cache.clear() }
}

/**
 * Loads a thumbnail from MediaStore cache (much faster than generating new thumbnails).
 * Uses the modern loadThumbnail API on Android Q+ for better performance.
 * Falls back to null if no cached thumbnail exists (in which case a placeholder will be shown).
 */
private suspend fun loadMediaStoreThumbnail(context: Context, uri: Uri): Bitmap? {
  return withContext(Dispatchers.IO) {
    try {
      // 1. Try MediaStore first (fastest)
      val mediaStoreBmp = when (uri.scheme) {
        // For content:// URIs, try direct load or extract ID
        "content" -> {
          // On Android Q+, try to load directly from URI first (most reliable)
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
              context.contentResolver.loadThumbnail(
                uri,
                android.util.Size(512, 512),
                null
              )
            } catch (e: Exception) {
              // If direct load fails, try ID extraction as backup
              val videoId = extractVideoId(uri, context)
              if (videoId != null) {
                val contentUri = android.content.ContentUris.withAppendedId(
                  MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                  videoId
                )
                context.contentResolver.loadThumbnail(
                  contentUri,
                  android.util.Size(512, 512),
                  null
                )
              } else null
            }
          } else {
            // Legacy ID extraction for older Android versions
            val videoId = extractVideoId(uri, context)
            if (videoId != null) {
              @Suppress("DEPRECATION")
              Thumbnails.getThumbnail(
                context.contentResolver,
                videoId,
                Thumbnails.MINI_KIND,
                null
              )
            } else null
          }
        }
        // For file:// URIs, try to find the corresponding MediaStore entry
        "file" -> {
          val filePath = uri.path ?: return@withContext null
          val projection = arrayOf(MediaStore.Video.Media._ID)
          val selection = "${MediaStore.Video.Media.DATA} = ?"
          val selectionArgs = arrayOf(filePath)

          context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
              val videoId = cursor.getLong(idColumn)

              // Use modern API on Android Q+
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentUri = android.content.ContentUris.withAppendedId(
                  MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                  videoId
                )
                context.contentResolver.loadThumbnail(
                  contentUri,
                  android.util.Size(512, 512),
                  null
                )
              } else {
                @Suppress("DEPRECATION")
                Thumbnails.getThumbnail(
                  context.contentResolver,
                  videoId,
                  Thumbnails.MINI_KIND,
                  null
                )
              }
            } else null
          }
        }
        else -> null
      }

      if (mediaStoreBmp != null) return@withContext mediaStoreBmp

      // 2. Fallback: Extract frame directly from video file if MediaStore fails
      // This is crucial for new files or files not in MediaStore
      val retriever = MediaMetadataRetriever()
      try {
        if (uri.scheme == "file") {
          retriever.setDataSource(uri.path)
        } else {
          retriever.setDataSource(context, uri)
        }
        // Extract a frame at 1 second mark (or 0 if it fails)
        retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          ?: retriever.frameAtTime
      } catch (e: Exception) {
        null
      } finally {
        try {
          retriever.release()
        } catch (e: Exception) { /* Ignore */ }
      }
    } catch (e: Exception) {
      // Fallback with placeholder if thumbnail loading fails
      android.util.Log.w("PlaylistSheet", "Failed to load thumbnail for $uri", e)
      null
    }
  }
}

/**
 * Extracts the video ID from a content:// URI.
 */
private fun extractVideoId(uri: Uri, context: Context): Long? {
  return try {
    val path = uri.path ?: return null
    // Extract ID from path like /external/video/media/123 or video:123
    // Handle both / and : as delimiters (DocumentProvider URIs often use :)
    val idString = if (path.contains(':')) {
      path.substringAfterLast(':')
    } else {
      path.substringAfterLast('/')
    }

    val videoId = idString.toLongOrNull() ?: return null

    // Verify this ID exists in MediaStore
    val projection = arrayOf(MediaStore.Video.Media._ID)
    val selection = "${MediaStore.Video.Media._ID} = ?"
    val selectionArgs = arrayOf(videoId.toString())

    context.contentResolver.query(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      projection,
      selection,
      selectionArgs,
      null
    )?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        cursor.getLong(idColumn)
      } else {
        null
      }
    }
  } catch (e: Exception) {
    null
  }
}

@Composable
fun PlaylistSheet(
  playlist: ImmutableList<PlaylistItem>,
  onDismissRequest: () -> Unit,
  onItemClick: (PlaylistItem) -> Unit,
  totalCount: Int = playlist.size,
  isM3UPlaylist: Boolean = false,
  playerPreferences: app.marlboroadvance.mpvex.preferences.PlayerPreferences,
  loadingItemIndex: Int = -1,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current

  val accentColor = MaterialTheme.colorScheme.primary

  // Check portrait mode
  val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

  // Portrait mode => list mode
  val isListModePreference by playerPreferences.playlistViewMode.collectAsState()
  var isListMode by remember { mutableStateOf(if (isPortrait) true else isListModePreference) }

  LaunchedEffect(isPortrait) {
    if (isPortrait && !isListMode) {
      isListMode = true
    }
  }

  // Update preference when view mode changes (only in landscape)
  LaunchedEffect(isListMode) {
    if (!isPortrait && isListMode != isListModePreference) {
      playerPreferences.playlistViewMode.set(isListMode)
    }
  }

  // Thumbnail cache with LRU eviction - limited size to prevent memory issues with large playlists
  // Note: This is now only used for UI state persistence. ThumbnailRepository handles actual caching.
  val thumbnailCache by remember {
    mutableStateOf(LRUBitmapCache(maxSize = 50))
  }

  // Get ThumbnailRepository for consistent thumbnail loading

  val thumbnailRepository = koinInject<ThumbnailRepository>()

  // Scroll state for the playlist
  val lazyListState = rememberLazyListState()

  // Find the currently playing item index - tracks changes in playlist items
  val playingItemIndex by remember {
    derivedStateOf {
      playlist.indexOfFirst { it.isPlaying }
    }
  }

  // Scroll to the currently playing item when the playing item changes or when sheet opens
  LaunchedEffect(playingItemIndex) {
    if (playingItemIndex >= 0) {
      lazyListState.animateScrollToItem(playingItemIndex)
    }
  }

  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val sheetWidth = if (isListMode) {
    if (LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
      640.dp
    } else {
      420.dp
    }
  } else {
    screenWidth * 0.85f
  }

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier = Modifier.fillMaxWidth(),
    customMaxWidth = sheetWidth,
    customMaxHeight = if (isPortrait) LocalConfiguration.current.screenHeightDp.dp * 0.5f else null,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.Transparent,
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
      ),
      tonalElevation = 0.dp,
    ) {
      Column(
        modifier = modifier.padding(
          vertical = MaterialTheme.spacing.smaller,
          horizontal = if (!isListMode) MaterialTheme.spacing.medium else 0.dp
        )
      ) {
        // Header showing current playlist info with toggle button
        val currentItem = playlist.find { it.isPlaying }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              horizontal = if (isListMode) MaterialTheme.spacing.medium else 0.dp,
              vertical = MaterialTheme.spacing.small,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            modifier = Modifier.weight(1f)
          ) {
            if (currentItem != null) {
              Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = accentColor,
                ),
              )
              Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = "$totalCount items",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Toggle button for list/grid view (only in landscape)
          if (!isPortrait) {
            IconButton(
              onClick = { isListMode = !isListMode }
            ) {
              Icon(
                imageVector = if (isListMode) Icons.Default.GridView else Icons.AutoMirrored.Default.ViewList,
                contentDescription = if (isListMode) "Switch to Grid View" else "Switch to List View",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Conditional rendering based on view mode
        if (isListMode) {
          // Vertical list mode (original implementation)
          LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth()
          ) {
            items(playlist) { item ->
              PlaylistTrackListItem(
                item = item,
                thumbnailRepository = thumbnailRepository,
                onClick = { onItemClick(item) },
                skipThumbnail = isM3UPlaylist,
                accentColor = accentColor,
                isLoading = item.index == loadingItemIndex
              )
            }
          }
        } else {
          // Horizontal grid mode
          LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(
              horizontal = if (isListMode) MaterialTheme.spacing.medium else 0.dp,
              vertical = MaterialTheme.spacing.small
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
          ) {
            items(playlist) { item ->
              PlaylistTrackGridItem(
                item = item,
                thumbnailRepository = thumbnailRepository,
                onClick = { onItemClick(item) },
                skipThumbnail = isM3UPlaylist,
                isLoading = item.index == loadingItemIndex
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun PlayingAnimationIndicator(
  color: Color,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "playing")
  
  val height1 by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar1"
  )
  val height2 by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar2"
  )
  val height3 by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(700, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar3"
  )

  Row(
    modifier = modifier.height(14.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.Bottom
  ) {
    Box(Modifier.width(3.dp).fillMaxHeight(height1).background(color, RoundedCornerShape(1.dp)))
    Box(Modifier.width(3.dp).fillMaxHeight(height2).background(color, RoundedCornerShape(1.dp)))
    Box(Modifier.width(3.dp).fillMaxHeight(height3).background(color, RoundedCornerShape(1.dp)))
  }
}

@Composable
fun PlaylistTrackListItem(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit,
  skipThumbnail: Boolean = false,
  accentColor: Color,
  isLoading: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
    label = "list_item_scale"
  )

  val itemAlpha by animateFloatAsState(
    targetValue = if (item.isWatched && !item.isPlaying) 0.6f else 1f,
    label = "item_alpha"
  )

  val itemShape = RoundedCornerShape(18.dp)
  val glassBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))

  // Convert PlaylistItem to Video for ThumbnailRepository
  val cleanPath = remember(item.path) {
    val withoutPrefix = item.path.removePrefix("file://")
    try {
      URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) {
      withoutPrefix
    }
  }
  val video = remember(item.uri, cleanPath, item.title) {
    Video(
      id = item.index.toLong(),
      uri = item.uri,
      displayName = item.title,
      title = item.title.substringBeforeLast("."),
      path = cleanPath,
      duration = 0L,
      durationFormatted = item.duration,
      size = 0L,
      sizeFormatted = "",
      dateModified = 0L,
      dateAdded = 0L,
      mimeType = "",
      bucketId = "",
      bucketDisplayName = "",
      width = 0,
      height = 0,
      fps = 0f,
      resolution = item.resolution,
      subtitleCodec = "",
      hasEmbeddedSubtitles = false,
    )
  }

  val thumbWidthPx = with(LocalDensity.current) { 100.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 56.dp.roundToPx() }
  val thumbnailKey = remember(video.id, video.path, thumbWidthPx, thumbHeightPx) {
    thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
  }

  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }

  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys
      .filter { it == thumbnailKey }
      .collect {
        thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
      }
  }

  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) {
      thumbnail = null
      return@LaunchedEffect
    }
    val memoryThumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    if (memoryThumbnail != null) {
      thumbnail = memoryThumbnail
      return@LaunchedEffect
    }
    val loadedThumbnail = withContext(Dispatchers.IO) {
      thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
    }
    if (loadedThumbnail != null) {
      thumbnail = loadedThumbnail
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .graphicsLayer { 
          scaleX = scale
          scaleY = scale 
          alpha = itemAlpha
      }
      .clip(itemShape)
      .then(if (item.isPlaying) Modifier.border(1.dp, accentColor.copy(alpha = 0.4f), itemShape) else Modifier)
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(color = Color.White),
        onClick = onClick
      ),
    color = if (item.isPlaying) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
    shape = itemShape,
    border = glassBorder,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Thumbnail Box
      Box(
        modifier = Modifier
          .width(100.dp)
          .height(56.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
      ) {
        thumbnail?.let { bmp ->
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
        } ?: run {
          Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp),
          )
        }

        // Top-left index badge
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = "${item.index + 1}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = Color.White,
          )
        }

        // Glass pills inside thumbnail
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (item.duration.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = item.duration,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White,
                    )
                }
            }
        }

        // Integrated progress line at bottom
        if (item.progressPercent > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressPercent / 100f)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }
      }

      // Title and status
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            color = if (item.isPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (item.resolution.isNotEmpty()) {
                Text(
                    text = item.resolution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (item.isWatched && !item.isPlaying) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
      }

      // Status Indicator
      if (isLoading) {
        LinearProgressIndicator(
          modifier = Modifier.width(32.dp).height(2.dp),
          color = accentColor,
          trackColor = accentColor.copy(alpha = 0.2f)
        )
      } else if (item.isPlaying) {
          PlayingAnimationIndicator(color = accentColor)
      }
    }
  }
}

@Composable
fun PlaylistTrackGridItem(
  item: PlaylistItem,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit,
  skipThumbnail: Boolean = false,
  isLoading: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val accentColor = MaterialTheme.colorScheme.primary
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
    label = "grid_item_scale"
  )

  val itemAlpha by animateFloatAsState(
    targetValue = if (item.isWatched && !item.isPlaying) 0.6f else 1f,
    label = "grid_alpha"
  )

  val itemShape = RoundedCornerShape(20.dp)
  val glassBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))

  // Thumbnail loading logic
  val cleanPath = remember(item.path) {
    val withoutPrefix = item.path.removePrefix("file://")
    try {
      URLDecoder.decode(withoutPrefix, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) {
      withoutPrefix
    }
  }
  val video = remember(item.uri, cleanPath, item.title) {
    Video(
      id = item.index.toLong(),
      uri = item.uri,
      displayName = item.title,
      title = item.title.substringBeforeLast("."),
      path = cleanPath,
      duration = 0L,
      durationFormatted = item.duration,
      size = 0L,
      sizeFormatted = "",
      dateModified = 0L,
      dateAdded = 0L,
      mimeType = "",
      bucketId = "",
      bucketDisplayName = "",
      width = 0,
      height = 0,
      fps = 0f,
      resolution = item.resolution,
      subtitleCodec = "",
      hasEmbeddedSubtitles = false,
    )
  }

  val thumbWidthPx = with(LocalDensity.current) { 200.dp.roundToPx() }
  val thumbHeightPx = with(LocalDensity.current) { 112.dp.roundToPx() }
  val thumbnailKey = remember(video.id, video.path, thumbWidthPx, thumbHeightPx) {
    thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
  }
  var thumbnail by remember(thumbnailKey) {
    mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }
  LaunchedEffect(thumbnailKey) {
    thumbnailRepository.thumbnailReadyKeys
      .filter { it == thumbnailKey }
      .collect {
        thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
      }
  }
  LaunchedEffect(thumbnailKey, skipThumbnail) {
    if (skipThumbnail) {
      thumbnail = null
      return@LaunchedEffect
    }
    val memoryThumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
    if (memoryThumbnail != null) {
      thumbnail = memoryThumbnail
      return@LaunchedEffect
    }
    val loadedThumbnail = withContext(Dispatchers.IO) {
      thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
    }
    if (loadedThumbnail != null) {
      thumbnail = loadedThumbnail
    }
  }

  Surface(
    modifier = modifier
      .width(200.dp)
      .graphicsLayer { 
          scaleX = scale
          scaleY = scale 
          alpha = itemAlpha
      }
      .clip(itemShape)
      .then(if (item.isPlaying) Modifier.border(1.2.dp, accentColor.copy(alpha = 0.5f), itemShape) else Modifier)
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(color = Color.White),
        onClick = onClick
      ),
    color = if (item.isPlaying) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
    shape = itemShape,
    border = glassBorder,
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(112.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
      ) {
        thumbnail?.let { bmp ->
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
        } ?: run {
          Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(32.dp),
          )
        }

        // Overlay badges
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Text(
            text = "${item.index + 1}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = Color.White,
          )
        }

        if (item.duration.isNotEmpty()) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
              .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Text(
              text = item.duration,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = Color.White,
            )
          }
        }
        
        // Progress line
        if (item.progressPercent > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressPercent / 100f)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }

        if (item.isPlaying) {
            Box(Modifier.matchParentSize().background(accentColor.copy(alpha = 0.1f)))
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = item.title,
          modifier = Modifier.height(40.dp),
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            color = if (item.isPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = item.resolution,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          )
          
          if (isLoading) {
             LinearProgressIndicator(modifier = Modifier.width(30.dp).height(2.dp), color = accentColor)
          } else if (item.isPlaying) {
             PlayingAnimationIndicator(color = accentColor)
          } else if (item.isWatched) {
             Icon(Icons.Outlined.Check, null, Modifier.size(12.dp), tint = accentColor.copy(alpha = 0.6f))
          }
        }
      }
    }
  }
}

@Composable
fun LoadingChip(
  width: androidx.compose.ui.unit.Dp,
  height: androidx.compose.ui.unit.Dp = 18.dp,
  isDark: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
  val shimmerTranslate = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer"
  )

  val baseColor = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest
  val shimmerColor = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh

  Box(
    modifier = modifier
      .width(width)
      .height(height)
      .clip(RoundedCornerShape(6.dp))
      .background(
        brush = Brush.linearGradient(
          colors = listOf(baseColor, shimmerColor, baseColor),
          start = Offset(shimmerTranslate.value - 200f, 0f),
          end = Offset(shimmerTranslate.value, 0f)
        )
      )
  )
}
