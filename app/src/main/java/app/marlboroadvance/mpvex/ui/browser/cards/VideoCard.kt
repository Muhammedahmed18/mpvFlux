package app.marlboroadvance.mpvex.ui.browser.cards

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.aspectRatio
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

  val selectionBackgroundColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0f),
    label = "selectionBackgroundColor"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .scale(cardScale)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(selectionBackgroundColor)
        .padding(12.dp),
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
        modifier = Modifier.requiredSize(width = 160.dp, height = 90.dp) // LOCKED: Cannot be compressed by Row height
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
    modifier = modifier,
  ) {
    Text(
      text = displayTitle,
      style = if (useFolderNameStyle) {
        MaterialTheme.typography.titleMedium
      } else {
        MaterialTheme.typography.titleSmall
      },
      color = when {
        isRecentlyPlayed -> MaterialTheme.colorScheme.tertiary
        isWatched -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
      },
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(6.dp))

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
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
          modifier = Modifier.matchParentSize(),
          contentScale = ContentScale.Crop,
        )
      } else {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }

      // Overlays - Top Row
      Row(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        // Left side: Watched or New
        Box {
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
              Text(
                text = stringResource(R.string.video_label_new),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }

        // Right side: Duration pill
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Text(
            text = video.durationFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
          )
        }
      }

      // Progress Bar - Floating Pill
      if (progressPercentage != null && showProgressBar && !isWatched) {
        val animatedProgress by animateFloatAsState(
          targetValue = progressPercentage,
          label = "VideoProgressAnimation"
        )

        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .height(6.dp)
        ) {
          // Track
          Box(
            modifier = Modifier
              .matchParentSize()
              .clip(RoundedCornerShape(50))
              .background(Color.White.copy(alpha = 0.25f))
          )

          // Progress
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
      // Use the same spring and anchor growth to the bottom-right corner to stop "sliding"
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
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    // Subtitles
    if (showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
      video.subtitleCodec.split(" ").forEach { codec ->
        MetadataChip(
          text = codec,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      }
    }

    // Size
    if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
      MetadataChip(text = video.sizeFormatted)
    }

    // Resolution & FPS
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

    // Date
    if (showDateChip && video.dateModified > 0) {
      MetadataChip(text = formatDate(video.dateModified))
    }
  }
}

@Composable
private fun MetadataChip(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier
      .background(containerColor, CircleShape)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    color = contentColor,
  )
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

            StateLabel("Selected State (Hanging Badge)")
            VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isSelected = true)

            StateLabel("Recently Played (No Italic)")
            VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isRecentlyPlayed = true)

            StateLabel("Progress Bar (50%)")
            VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, progressPercentage = 0.5f
            )
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

            StateLabel("Watched (Dimmed + Tick)")
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
    dateModified = 1776686400L, // Apr 20, 2026
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
