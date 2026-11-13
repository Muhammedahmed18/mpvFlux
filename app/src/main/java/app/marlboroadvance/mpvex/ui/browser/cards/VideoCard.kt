@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package app.marlboroadvance.mpvex.ui.browser.cards

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.marlboroadvance.mpvex.ui.utils.videoFileTitle
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_FORMATTER by lazy { SimpleDateFormat("MMM d", Locale.getDefault()) }

@Immutable
data class VideoCardSettings(
  val unlimitedNameLines: Boolean = false,
  val showThumbnails: Boolean = true,
  val showVideoExtension: Boolean = false,
  val showSizeChip: Boolean = true,
  val showResolutionChip: Boolean = true,
  val showFramerateInResolution: Boolean = false,
  val showProgressBar: Boolean = true,
  val showDateChip: Boolean = true,
  val showUnplayedOldVideoLabel: Boolean = true,
  val unplayedOldVideoDays: Int = 7
)

@Composable
fun VideoCard(
  video: Video,
  settings: VideoCardSettings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  progressPercentage: Float? = null,
  isOldAndUnplayed: Boolean = false,
  isWatched: Boolean = false,
  isRecentlyPlayed: Boolean = false,
  onThumbClick: () -> Unit = {},
  showSubtitleIndicator: Boolean = true,
  overrideShowSizeChip: Boolean? = null,
  overrideShowResolutionChip: Boolean? = null,
  useFolderNameStyle: Boolean = false, // Ignored, kept for API compatibility
  allowThumbnailGeneration: Boolean = true,
  // Kept for API compatibility; rows no longer draw a grouped background.
  groupPosition: VideoCardGroupPosition = VideoCardGroupPosition.Single,
  // Show the ⋮ menu (Play / Info / Select). Pass false while multi-select is active.
  showMenu: Boolean = true,
  // True while multi-select is active: unselected rows show an empty check circle.
  inSelectionMode: Boolean = false,
) {
  val isNew = remember(video.dateModified, settings.showUnplayedOldVideoLabel, isOldAndUnplayed, settings.unplayedOldVideoDays) {
    if (settings.showUnplayedOldVideoLabel && isOldAndUnplayed) {
      val currentTime = System.currentTimeMillis()
      val videoAge = currentTime - (video.dateModified * 1000)
      val thresholdMillis = settings.unplayedOldVideoDays * 24 * 60 * 60 * 1000L
      videoAge <= thresholdMillis
    } else false
  }

  // The raw file name as stored on disk; the extension follows the "Show file extension" option.
  val displayTitle = remember(video.displayName, settings.showVideoExtension) {
    videoFileTitle(video.displayName, settings.showVideoExtension)
  }
  val showSizeChip = overrideShowSizeChip ?: settings.showSizeChip
  val showResolutionChip = overrideShowResolutionChip ?: settings.showResolutionChip

  // Material 3 media list item with no container: rows sit directly on the screen.
  // Selection adds a soft accent highlight behind the row; nothing changes size.
  val colors = MaterialTheme.colorScheme
  val containerColor by animateColorAsState(
    targetValue = if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
    animationSpec = tween(durationMillis = 150),
    label = "video_row_selection",
  )
  val contentColor = colors.onSurface
  val supportingColor = colors.onSurfaceVariant
  val shape = MaterialTheme.shapes.large

  val date = if (settings.showDateChip && video.dateModified > 0) {
    remember(video.dateModified) { formatDate(video.dateModified) }
  } else {
    null
  }
  val quality = if (showResolutionChip) {
    remember(video.width, video.height, video.fps, settings.showFramerateInResolution) {
      qualityLabel(video, settings.showFramerateInResolution)
    }
  } else {
    null
  }
  val is4K = video.width >= 3840 || video.height >= 2160
  // Embedded subtitle names as reported by the file ("SRT · ASS").
  val subtitleNames = if (showSubtitleIndicator && video.hasEmbeddedSubtitles) {
    remember(video.subtitleCodec) {
      video.subtitleCodec.split(" ", ",").filter { it.isNotBlank() }.joinToString(" · ")
    }.ifBlank { stringResource(R.string.cd_embedded_subtitles) }
  } else {
    null
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .semantics { selected = isSelected }
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = shape,
    color = containerColor,
    contentColor = contentColor,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = if (showMenu) 0.dp else 8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      VideoThumbnail(
        video = video,
        isNew = isNew,
        isSelected = isSelected,
        inSelectionMode = inSelectionMode,
        isWatched = isWatched,
        progressPercentage = progressPercentage,
        showThumbnails = settings.showThumbnails,
        showProgressBar = settings.showProgressBar,
        allowThumbnailGeneration = allowThumbnailGeneration,
        onThumbClick = onThumbClick,
        onLongClick = onLongClick,
        // Steady size: 160dp wide (90dp tall) on phones, never below 144dp on very narrow screens.
        modifier = Modifier
          .fillMaxWidth(0.5f)
          .widthIn(min = 144.dp, max = 160.dp)
          .aspectRatio(16f / 9f),
      )

      Spacer(Modifier.width(12.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = displayTitle,
          style = MaterialTheme.typography.titleMedium,
          color = if (isWatched) supportingColor else contentColor,
          maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          subtitleNames?.let { InfoPill(text = it, style = InfoPillStyle.Subtitles) }
          quality?.let { InfoPill(text = it, style = if (is4K) InfoPillStyle.Accent else InfoPillStyle.Neutral) }
          if (showSizeChip && video.size > 0) InfoPill(text = video.sizeFormatted)
          date?.let { InfoPill(text = it) }
        }
      }

      if (showMenu) {
        VideoCardMenu(video = video, onPlay = onClick, onSelect = onLongClick, tint = supportingColor)
      }
    }
  }
}

/** Where a row sits in a run of consecutive video rows (Material 3 connected list). */
enum class VideoCardGroupPosition { Single, First, Middle, Last }

/** Group position from whether the rows above and below are also video rows. */
fun videoCardGroupPosition(hasPrevious: Boolean, hasNext: Boolean): VideoCardGroupPosition = when {
  hasPrevious && hasNext -> VideoCardGroupPosition.Middle
  hasPrevious -> VideoCardGroupPosition.Last
  hasNext -> VideoCardGroupPosition.First
  else -> VideoCardGroupPosition.Single
}

/** ⋮ button with the per-video menu: Play, Info, Select (enters multi-select). */
@Composable
private fun VideoCardMenu(
  video: Video,
  onPlay: () -> Unit,
  onSelect: (() -> Unit)?,
  tint: Color,
) {
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(
        imageVector = Icons.Rounded.MoreVert,
        contentDescription = stringResource(R.string.cd_more_options),
        tint = tint,
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.cd_play)) },
        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
        onClick = {
          expanded = false
          onPlay()
        },
      )
      DropdownMenuItem(
        text = { Text(stringResource(R.string.info)) },
        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        onClick = {
          expanded = false
          val intent = Intent(context, MediaInfoActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = video.uri
          }
          context.startActivity(intent)
        },
      )
      if (onSelect != null) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.select)) },
          leadingIcon = { Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null) },
          onClick = {
            expanded = false
            onSelect()
          },
        )
      }
    }
  }
}

/** Quality tag text: "4K", "1080p", with the frame rate appended when enabled ("1080p60"). */
private fun qualityLabel(video: Video, showFramerate: Boolean): String? {
  if (video.height <= 0 && video.width <= 0) return null
  val base = when {
    video.width >= 3840 || video.height >= 2160 -> "4K"
    video.width >= 2560 || video.height >= 1440 -> "1440p"
    video.width >= 1920 || video.height >= 1080 -> "1080p"
    video.width >= 1280 || video.height >= 720 -> "720p"
    video.width >= 854 || video.height >= 480 -> "480p"
    else -> "${video.height}p"
  }
  return if (showFramerate && video.fps > 0) "$base${video.fps.roundToInt()}" else base
}

enum class InfoPillStyle { Neutral, Accent, Subtitles }

/** Pill-shaped, non-interactive label (One UI / M3 style) for video details. */
@Composable
private fun InfoPill(text: String, style: InfoPillStyle = InfoPillStyle.Neutral) {
  val colors = MaterialTheme.colorScheme
  val (container, content) = when (style) {
    InfoPillStyle.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    InfoPillStyle.Accent -> colors.primaryContainer to colors.onPrimaryContainer
    // Subtitles stand out: filled with the theme's tertiary accent.
    InfoPillStyle.Subtitles -> colors.tertiary to colors.onTertiary
  }
  Surface(color = container, contentColor = content, shape = CircleShape) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (style == InfoPillStyle.Neutral) FontWeight.Normal else FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

/** Pill drawn over the thumbnail image. */
@Composable
private fun ThumbnailPill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
  Surface(modifier = modifier, color = container, contentColor = content, shape = CircleShape) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
  }
}

@Composable
fun VideoThumbnail(
  video: Video,
  isNew: Boolean,
  isSelected: Boolean,
  isWatched: Boolean,
  progressPercentage: Float?,
  showThumbnails: Boolean,
  showProgressBar: Boolean,
  allowThumbnailGeneration: Boolean,
  onThumbClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  aspect: Float = 16f / 9f,
  inSelectionMode: Boolean = false,
) {
  val isInspection = LocalInspectionMode.current
  val thumbnailRepository = if (isInspection) null else koinInject<ThumbnailRepository>()
  val density = LocalDensity.current
  val shape = MaterialTheme.shapes.medium
  // Selection fades in a border and check; the thumbnail never changes size.
  val selectionAlpha by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = tween(durationMillis = 150),
    label = "thumbnail_selection",
  )
  // Vertical videos (phone recordings) are fitted on black instead of cropped.
  val isPortrait = video.width > 0 && video.height > video.width

  Box(modifier = modifier) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .clip(shape)
        .background(if (isPortrait) Color.Black else MaterialTheme.colorScheme.surfaceVariant)
        .combinedClickable(
          onClick = onThumbClick,
          onLongClick = onLongClick,
        ),
      contentAlignment = Alignment.Center,
    ) {
      val thumbWidthPx = with(density) { maxWidth.roundToPx() }
      val thumbHeightPx = (thumbWidthPx / aspect).roundToInt()

      val thumbnailKey = remember(video.size, video.dateModified) {
        thumbnailRepository?.thumbnailKey(video) ?: ""
      }

      var thumbnail by remember(thumbnailKey) {
        mutableStateOf(thumbnailRepository?.getThumbnailFromMemory(video))
      }

      var isLoading by remember(thumbnailKey) {
        mutableStateOf(thumbnail == null && showThumbnails && thumbnailRepository != null)
      }

      var loadFailed by remember(thumbnailKey) { mutableStateOf(false) }

      // Start already-visible when the bitmap is present synchronously from the memory
      // cache, so re-entering the folder (e.g. returning from the player) shows it
      // instantly with no shimmer/fade replay. Only thumbnails that must be loaded or
      // generated (thumbnail == null here) fade in.
      var thumbnailVisible by remember(thumbnailKey) { mutableStateOf(thumbnail != null) }

      // Step 3.2 — Animate alpha on the Image
      val imageAlpha by animateFloatAsState(
        targetValue = if (thumbnailVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "thumbnail_fade"
      )

      // Single effect replaces two competing ones
      LaunchedEffect(thumbnailKey, allowThumbnailGeneration, showThumbnails) {
        if (thumbnail == null && showThumbnails && thumbnailRepository != null) {
          isLoading = true
          loadFailed = false
          val result = withContext(Dispatchers.IO) {
            if (allowThumbnailGeneration) {
              thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
            } else {
              thumbnailRepository.getCachedThumbnail(video, thumbWidthPx, thumbHeightPx)
            }
          }
          thumbnail = result
          isLoading = false
          if (thumbnail == null) {
            loadFailed = true
          }
        }
        // Guarantees Compose sees false -> true transition
        if (thumbnail != null) {
          thumbnailVisible = true
        }
      }

      if (showThumbnails) {
        // Step 3.3 — Ensure shimmer exits cleanly
        // Shimmer stays as long as we're loading OR the fade is in progress.
        // We also check !loadFailed and thumbnail != null to avoid indefinite shimmer if loading never starts or fails.
        if ((isLoading || (thumbnail != null && imageAlpha < 1f)) && !loadFailed) {
          ThumbnailShimmer(modifier = Modifier.matchParentSize())
        }

        if (thumbnail != null) {
          Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
              .matchParentSize()
              .alpha(imageAlpha * (if (isWatched) 0.6f else 1f)),
            contentScale = if (isPortrait) ContentScale.Fit else ContentScale.Crop,
          )
        } else if (!isLoading || loadFailed) {
          Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_sym_movie),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.ic_sym_movie),
          contentDescription = null,
          modifier = Modifier.size(32.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      val showProgress =
        progressPercentage != null && progressPercentage > 0f && showProgressBar && !isWatched && video.duration > 0
      // Top start: NEW pill (the selection check takes this corner while selected).
      if (isNew && !isWatched && !isSelected && !inSelectionMode) {
        ThumbnailPill(
          text = stringResource(R.string.video_status_new).uppercase(Locale.getDefault()),
          container = MaterialTheme.colorScheme.primary,
          content = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp),
        )
      }

      // Bottom start: watched tick in a Material 3 Expressive cookie shape.
      if (isWatched) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(6.dp)
            .size(26.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = stringResource(R.string.video_status_watched),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
          )
        }
      }

      // Bottom end: length, lifted above the progress bar when there is one.
      if (video.durationFormatted.isNotBlank()) {
        ThumbnailPill(
          text = video.durationFormatted,
          container = Color.Black.copy(alpha = 0.72f),
          content = Color.White,
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 6.dp, bottom = if (showProgress) 17.dp else 6.dp),
        )
      }

      // Bottom: watch progress as a thick rounded bar across the image.
      if (showProgress) {
        LinearProgressIndicator(
          progress = { progressPercentage!!.coerceIn(0f, 1f) },
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape),
          color = MaterialTheme.colorScheme.primary,
          trackColor = Color.White.copy(alpha = 0.4f),
          strokeCap = StrokeCap.Round,
          gapSize = 0.dp,
          drawStopIndicator = {},
        )
      }
    }

    // Selection border, drawn inside the thumbnail's rounded corners.
    if (selectionAlpha > 0f) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .alpha(selectionAlpha)
          .border(3.dp, MaterialTheme.colorScheme.primary, shape),
      )
    }

    // Top start: filled check when selected, empty circle for other rows in selection mode.
    if (isSelected || inSelectionMode) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 6.dp, start = 6.dp)
          .size(24.dp),
      ) {
        if (selectionAlpha < 1f) {
          Box(
            modifier = Modifier
              .matchParentSize()
              .alpha(1f - selectionAlpha)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.32f))
              .border(2.dp, Color.White, CircleShape),
          )
        }
        if (selectionAlpha > 0f) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
              .matchParentSize()
              .alpha(selectionAlpha),
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = stringResource(R.string.card_cd_selected),
              modifier = Modifier.padding(4.dp),
              tint = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ThumbnailShimmer(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
  val xOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_offset"
  )

  val colors = listOf(
    MaterialTheme.colorScheme.surfaceContainerLow,
    MaterialTheme.colorScheme.surfaceContainerHighest,
    MaterialTheme.colorScheme.surfaceContainerLow,
  )

  // The animated offset is read inside drawBehind, so each frame only redraws this box.
  // Building the brush in composition (inside BoxWithConstraints) re-ran composition and
  // subcomposition for every loading card on every frame.
  Box(
    modifier = modifier.drawBehind {
      val widthPx = size.width
      val heightPx = size.height
      drawRect(
        brush = Brush.linearGradient(
          colors = colors,
          start = Offset(xOffset * widthPx - widthPx, 0f),
          end = Offset(xOffset * widthPx, heightPx),
        ),
      )
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoMetadataChips(
  video: Video,
  showSubtitleIndicator: Boolean,
  showSizeChip: Boolean,
  showResolutionChip: Boolean,
  showFramerate: Boolean,
  showDateChip: Boolean,
  modifier: Modifier = Modifier
) {
  // 4K is the only quality tier we can derive — Video carries no HDR/dynamic-range
  // field, so HDR is intentionally out of scope until MediaStore provides it.
  val is4K = video.width >= 3840 || video.height >= 2160

  // Resolution as a normal chip; 4K gets its own highlighted chip instead.
  val resolutionLabel = if (showResolutionChip && video.height > 0 && !is4K) {
    val baseRes = when {
      video.width >= 2560 || video.height >= 1440 -> "1440p"
      video.width >= 1920 || video.height >= 1080 -> "1080p"
      video.width >= 1280 || video.height >= 720 -> "720p"
      video.width >= 854 || video.height >= 480 -> "480p"
      else -> "${video.height}p"
    }
    if (showFramerate && video.fps > 0) "$baseRes@${video.fps.roundToInt()}" else baseRes
  } else null

  // 4K becomes an emphasized "is this the version I want" badge, gated by the same
  // resolution toggle. Framerate rides along when the user opts in.
  val qualityLabel = if (showResolutionChip && is4K) {
    if (showFramerate && video.fps > 0) "4K@${video.fps.roundToInt()}" else "4K"
  } else null

  val subtitleCodecs = if (
    showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()
  ) {
    video.subtitleCodec.split(" ").filter { it.isNotBlank() }
  } else {
    emptyList()
  }

  if (resolutionLabel == null && qualityLabel == null && subtitleCodecs.isEmpty() &&
    !(showSizeChip && video.size > 0) && !(showDateChip && video.dateModified > 0)
  ) return

  // One row of pills, the same chips as the folder card; wraps on narrow screens.
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    qualityLabel?.let { UnifiedChip(text = it, isAccent = true, compact = true) }
    resolutionLabel?.let { UnifiedChip(text = it, compact = true) }
    if (showSizeChip && video.size > 0) UnifiedChip(text = video.sizeFormatted, compact = true)
    if (showDateChip && video.dateModified > 0) {
      UnifiedChip(text = remember(video.dateModified) { formatDate(video.dateModified) }, compact = true)
    }
    if (subtitleCodecs.isNotEmpty()) UnifiedChip(text = subtitleCodecs.joinToString(" · "), compact = true)
  }
}

private fun formatDate(timestampSeconds: Long): String {
  return DATE_FORMATTER.format(Date(timestampSeconds * 1000))
}

@Preview(showBackground = true, name = "Video Card Default State")
@Composable
fun VideoCardDefaultPreview() {
  val sampleVideo = getSampleVideo()
  MaterialTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {})
    }
  }
}

@Preview(showBackground = true, name = "Video Card Selected")
@Composable
fun VideoCardSelectedPreview() {
  val sampleVideo = getSampleVideo()
  MaterialTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isSelected = true)
    }
  }
}

@Preview(showBackground = true, name = "Video Card Watched + Progress + Subtitles")
@Composable
fun VideoCardFullPreview() {
  val sampleVideo = getSampleVideo()
  MaterialTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      VideoCard(
        video = sampleVideo,
        settings = VideoCardSettings(),
        onClick = {},
        isWatched = true,
        progressPercentage = 0.7f
      )
    }
  }
}

@Preview(showBackground = true, name = "Video Card Continue Watching")
@Composable
fun VideoCardContinueWatchingPreview() {
  val sampleVideo = getSampleVideo()
  MaterialTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      VideoCard(
        video = sampleVideo,
        settings = VideoCardSettings(),
        onClick = {},
        progressPercentage = 0.45f
      )
    }
  }
}

@Preview(showBackground = true, name = "Video Card Long Title")
@Composable
fun VideoCardLongTitlePreview() {
  val sampleVideo = getSampleVideo().copy(
    displayName = "This is a very very long video title that should definitely truncate after two lines of text in the information panel.mp4"
  )
  MaterialTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {})
    }
  }
}

private fun getSampleVideo() = Video(
  id = 1,
  title = "Sample Video File",
  displayName = "Sample Video File.mp4",
  path = "/storage/emulated/0/Movies/Sample.mp4",
  uri = Uri.EMPTY,
  duration = 3600000,
  durationFormatted = "01:00:00",
  size = 1024 * 1024 * 500,
  sizeFormatted = "500 MB",
  dateModified = 1714564800L, // Approx May 1 2024
  dateAdded = System.currentTimeMillis() / 1000,
  mimeType = "video/mp4",
  bucketId = "1",
  bucketDisplayName = "Movies",
  width = 1920,
  height = 1080,
  fps = 24f,
  resolution = "1920x1080 @ 24",
  hasEmbeddedSubtitles = true,
  subtitleCodec = "SRT ASS"
)
