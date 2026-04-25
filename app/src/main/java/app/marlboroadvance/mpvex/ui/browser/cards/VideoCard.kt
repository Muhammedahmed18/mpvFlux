package app.marlboroadvance.mpvex.ui.browser.cards

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_FORMATTER by lazy { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

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
  isRecentlyPlayed: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  progressPercentage: Float? = null,
  isOldAndUnplayed: Boolean = false,
  isWatched: Boolean = false,
  onThumbClick: () -> Unit = {},
  showSubtitleIndicator: Boolean = true,
  overrideShowSizeChip: Boolean? = null,
  overrideShowResolutionChip: Boolean? = null,
  useFolderNameStyle: Boolean = false,
  allowThumbnailGeneration: Boolean = true,
) {
  // Shared calculations
  val isNew = remember(video.dateModified, settings.showUnplayedOldVideoLabel, isOldAndUnplayed, settings.unplayedOldVideoDays) {
    if (settings.showUnplayedOldVideoLabel && isOldAndUnplayed) {
      val currentTime = System.currentTimeMillis()
      val videoAge = currentTime - (video.dateModified * 1000)
      val thresholdMillis = settings.unplayedOldVideoDays * 24 * 60 * 60 * 1000L
      videoAge <= thresholdMillis
    } else false
  }

  val displayTitle = remember(video.displayName, settings.showVideoExtension) {
    if (settings.showVideoExtension) {
      video.displayName
    } else {
      video.displayName.substringBeforeLast('.')
    }
  }

  val maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2
  val showSizeChip = overrideShowSizeChip ?: settings.showSizeChip
  val showResolutionChip = overrideShowResolutionChip ?: settings.showResolutionChip

  // ── Animated State ────────────────────────────────────────────────────────

  val bouncySpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)

  val cardScale by animateFloatAsState(
    targetValue = if (isSelected) 0.98f else 1f,
    animationSpec = bouncySpring,
    label = "cardScale"
  )

  val selectionBorderWidth by animateDpAsState(
    targetValue = if (isSelected) 2.dp else 0.dp,
    label = "selectionBorderWidth"
  )

  // Removed animateColorAsState to prevent flickering during theme changes
  val containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow

  Card(
    modifier = modifier
      .fillMaxWidth()
      .scale(cardScale)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    // M3 \"Container-Transform\" influenced: card expands and gains border on selection
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = if (isSelected) BorderStroke(selectionBorderWidth, MaterialTheme.colorScheme.primary) else null,
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      VideoThumbnail(
        video = video,
        isNew = isNew,
        isSelected = isSelected,
        isWatched = isWatched,
        progressPercentage = progressPercentage,
        showThumbnails = settings.showThumbnails,
        showProgressBar = settings.showProgressBar,
        allowThumbnailGeneration = allowThumbnailGeneration,
        onThumbClick = onThumbClick,
        onLongClick = onLongClick,
        modifier = Modifier.requiredSize(width = 160.dp, height = 90.dp)
      )

      Spacer(modifier = Modifier.width(16.dp))

      VideoInfoPanel(
        displayTitle = displayTitle,
        maxLines = maxLines,
        useFolderNameStyle = useFolderNameStyle,
        isRecentlyPlayed = isRecentlyPlayed,
        isWatched = isWatched,
        video = video,
        showSubtitleIndicator = showSubtitleIndicator,
        showSizeChip = showSizeChip,
        showResolutionChip = showResolutionChip,
        showFramerateInResolution = settings.showFramerateInResolution,
        showDateChip = settings.showDateChip,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun VideoInfoPanel(
  displayTitle: String,
  maxLines: Int,
  useFolderNameStyle: Boolean,
  isRecentlyPlayed: Boolean,
  isWatched: Boolean,
  video: Video,
  showSubtitleIndicator: Boolean,
  showSizeChip: Boolean,
  showResolutionChip: Boolean,
  showFramerateInResolution: Boolean,
  showDateChip: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.alpha(if (isWatched) 0.6f else 1f),
  ) {
    Text(
      text = displayTitle,
      style = if (useFolderNameStyle) {
        MaterialTheme.typography.titleMedium
      } else {
        MaterialTheme.typography.titleMedium // Boosted from titleSmall
      },
      fontWeight = FontWeight.SemiBold, // Modern M3 emphasis
      color = when {
        isRecentlyPlayed -> MaterialTheme.colorScheme.tertiary
        isWatched -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
      },
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
    )

    // Duration moved to thumbnail overlay in Refined B style
    Spacer(modifier = Modifier.height(8.dp))

    VideoMetadataChips(
      video = video,
      showSubtitleIndicator = showSubtitleIndicator,
      showSizeChip = showSizeChip,
      showResolutionChip = showResolutionChip,
      showFramerateInResolution = showFramerateInResolution,
      showDateChip = showDateChip
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
  aspect: Float = 16f / 9f
) {
  val isInspection = LocalInspectionMode.current
  val thumbnailRepository = if (isInspection) null else koinInject<ThumbnailRepository>()
  val density = LocalDensity.current

  Box(modifier = modifier) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        // M3 Large shape: 16dp
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .combinedClickable(
          onClick = onThumbClick,
          onLongClick = onLongClick,
        ),
      contentAlignment = Alignment.Center,
    ) {
      val thumbWidthPx = with(density) { maxWidth.roundToPx() }
      val thumbHeightPx = (thumbWidthPx / aspect).roundToInt()

      val thumbnailKey = remember(video.id, video.dateModified, video.size, thumbWidthPx, thumbHeightPx) {
        thumbnailRepository?.thumbnailKey(video, thumbWidthPx, thumbHeightPx) ?: ""
      }

      var thumbnail by remember(thumbnailKey) {
        mutableStateOf(thumbnailRepository?.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
      }

      LaunchedEffect(thumbnailKey) {
        if (thumbnailRepository != null) {
          thumbnailRepository.thumbnailReadyKeys
            .filter { it == thumbnailKey }
            .collect {
              thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
            }
        }
      }

      LaunchedEffect(thumbnailKey, allowThumbnailGeneration, showThumbnails) {
        if (thumbnail == null && showThumbnails && thumbnailRepository != null) {
          thumbnail = withContext(Dispatchers.IO) {
            if (allowThumbnailGeneration) {
              thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
            } else {
              thumbnailRepository.getCachedThumbnail(video, thumbWidthPx, thumbHeightPx)
            }
          }
        }
      }

      if (showThumbnails && thumbnail != null) {
        Image(
          bitmap = thumbnail!!.asImageBitmap(),
          contentDescription = null,
          modifier = Modifier.matchParentSize().alpha(if (isWatched) 0.6f else 1f),
          contentScale = ContentScale.Crop,
        )
      } else {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(32.dp).alpha(if (isWatched) 0.6f else 1f),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Overlays
      val showProgress = progressPercentage != null && showProgressBar && !isWatched
      
      // Duration Overlay (Bottom End) - Adjusted to avoid collision with progress bar
      Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(
            bottom = if (showProgress) 16.dp else 8.dp,
            end = 8.dp
          )
      ) {
        Text(
          text = video.durationFormatted,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }

      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp),
      ) {
        if (isWatched) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              modifier = Modifier.padding(4.dp),
              tint = MaterialTheme.colorScheme.onPrimary,
            )
          }
        } else if (isNew) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.primary)
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Text(
              text = stringResource(R.string.video_label_new),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }
      }

      // Progress Bar
      if (showProgress) {
        val animatedProgress by animateFloatAsState(
          targetValue = progressPercentage ?: 0f,
          label = "VideoProgressAnimation"
        )

        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .height(3.dp)
        ) {
          Box(
            modifier = Modifier
              .matchParentSize()
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
          )
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(animatedProgress)
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.primary)
          )
        }
      }
    }

    // Selection Badge
    val bouncySpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)
    AnimatedVisibility(
      visible = isSelected,
      enter = fadeIn(bouncySpring) + scaleIn(bouncySpring, transformOrigin = TransformOrigin(1f, 1f)),
      exit = fadeOut(bouncySpring) + scaleOut(bouncySpring, transformOrigin = TransformOrigin(1f, 1f)),
      modifier = Modifier.align(Alignment.BottomEnd)
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        modifier = Modifier
          .size(28.dp)
          .offset(x = 6.dp, y = 6.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = "Selected",
          modifier = Modifier.padding(4.dp),
          tint = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
  }
}

@Composable
fun VideoMetadataChips(
  video: Video,
  showSubtitleIndicator: Boolean,
  showSizeChip: Boolean,
  showResolutionChip: Boolean,
  showFramerateInResolution: Boolean,
  showDateChip: Boolean,
  modifier: Modifier = Modifier
) {
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
      video.subtitleCodec.split(" ").forEach { codec ->
        MetadataChip(
          text = codec,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      }
    }
    if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
      MetadataChip(text = video.sizeFormatted)
    }
    if (showResolutionChip && video.height > 0) {
      val resText = when {
        video.width >= 3840 || video.height >= 2160 -> "4K"
        video.width >= 2560 || video.height >= 1440 -> "1440p"
        video.width >= 1920 || video.height >= 1080 -> "1080p"
        video.width >= 1280 || video.height >= 720 -> "720p"
        video.width >= 854 || video.height >= 480 -> "480p"
        else -> "${video.height}p"
      }
      val displayResolution = if (showFramerateInResolution && video.fps > 0) {
        "$resText@${video.fps.toInt()}"
      } else {
        resText
      }
      MetadataChip(text = displayResolution)
    }
    if (showDateChip && video.dateModified > 0) {
      MetadataChip(text = formatDate(video.dateModified))
    }
  }
}

@Composable
private fun MetadataChip(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

private fun formatDate(timestampSeconds: Long): String {
  return DATE_FORMATTER.format(Date(timestampSeconds * 1000))
}

@Preview(showBackground = true, name = "Video Card Primary States")
@Composable
fun VideoCardPrimaryPreview() {
  val sampleVideo = getSampleVideo()

  MaterialTheme {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      StateLabel("Extension: ON")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(showVideoExtension = true), onClick = {})

      StateLabel("Extension: OFF")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(showVideoExtension = false), onClick = {})

      StateLabel("Selected State (Outlined + Tonal)")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isSelected = true)

      StateLabel("Recently Played")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isRecentlyPlayed = true)

      StateLabel("Progress Bar (50%)")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, progressPercentage = 0.5f)
    }
  }
}

@Preview(showBackground = true, name = "Video Card Status States")
@Composable
fun VideoCardStatusPreview() {
  val sampleVideo = getSampleVideo()

  MaterialTheme {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      StateLabel("New / Unplayed Label")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isOldAndUnplayed = true)

      StateLabel("Watched (Dimmed Content + Bright Tick)")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isWatched = true)
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
  dateModified = 1776686400L,
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

@Composable
private fun StateLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 8.dp)
  )
}
