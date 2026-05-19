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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ripple
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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

  // ── Interaction State ───────────────────────────────────────────────────
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  // ── Animated State ────────────────────────────────────────────────────────

  val selectionBorderWidth by animateDpAsState(
    targetValue = if (isSelected) 1.dp else 0.dp,
    label = "selectionBorderWidth"
  )

  val containerColor = when {
    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    isPressed -> MaterialTheme.colorScheme.surfaceContainerLow
    else -> MaterialTheme.colorScheme.surfaceContainerLow
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = if (isSelected) BorderStroke(selectionBorderWidth, MaterialTheme.colorScheme.outlineVariant) else null,
    elevation = CardDefaults.cardElevation(
      defaultElevation = if (isPressed) 4.dp else 1.dp,
      pressedElevation = 4.dp
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
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
        modifier = Modifier.requiredSize(width = 172.dp, height = 97.dp)
      )

      Spacer(modifier = Modifier.width(20.dp))

      VideoInfoPanel(
        displayTitle = displayTitle,
        maxLines = maxLines,
        useFolderNameStyle = useFolderNameStyle,
        isWatched = isWatched,
        isRecentlyPlayed = isRecentlyPlayed,
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
  isWatched: Boolean,
  isRecentlyPlayed: Boolean,
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
        MaterialTheme.typography.bodyLarge
      } else {
        MaterialTheme.typography.bodyLarge
      },
      fontWeight = FontWeight.Medium,
      color = when {
        isRecentlyPlayed -> MaterialTheme.colorScheme.primary
        isWatched -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
      },
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
      lineHeight = 1.4.em,
      modifier = if (isWatched) Modifier.alpha(0.6f) else Modifier
    )

    Spacer(modifier = Modifier.height(12.dp))

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
        .background(MaterialTheme.colorScheme.surfaceVariant)
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
        thumbnailRepository?.thumbnailReadyKeys
          ?.filter { it == thumbnailKey }
          ?.collect {
            thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
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
          modifier = Modifier.matchParentSize().alpha(if (isWatched) 0.4f else 1f),
          contentScale = ContentScale.Crop,
        )
      } else {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      // Selection scrim overlay
      if (isSelected) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        )
      }

      // Overlays
      val showProgress = progressPercentage != null && showProgressBar && !isWatched

      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(
            bottom = if (showProgress) 16.dp else 8.dp,
            start = 8.dp
          )
      ) {
        Text(
          text = video.durationFormatted,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
      }

      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
      ) {
        if (isWatched) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = "Watched",
              modifier = Modifier.padding(3.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else if (isNew) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(0.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(4.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.secondary)
              )
              Text(
                text = stringResource(R.string.video_label_new),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }
      }

      // Progress Bar (Optimized Draw Phase)
      if (showProgress) {
        val animatedProgress by animateFloatAsState(
          targetValue = progressPercentage,
          label = "VideoProgressAnimation"
        )
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        val indicatorColor = MaterialTheme.colorScheme.secondary

        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
              val barWidth = size.width
              val barHeight = size.height

              drawRect(
                color = trackColor,
                size = Size(barWidth, barHeight)
              )

              drawRect(
                color = indicatorColor,
                size = Size(barWidth * animatedProgress, barHeight)
              )
            }
        )
      }
    }

    // Selection Badge
    AnimatedVisibility(
      visible = isSelected,
      enter = fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + scaleIn(
        spring(dampingRatio = Spring.DampingRatioNoBouncy),
        transformOrigin = TransformOrigin(1f, 1f)
      ),
      exit = fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + scaleOut(
        spring(dampingRatio = Spring.DampingRatioNoBouncy),
        transformOrigin = TransformOrigin(1f, 1f)
      ),
      modifier = Modifier.align(Alignment.BottomEnd)
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier
          .size(24.dp)
          .padding(end = 8.dp, bottom = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = "Selected",
          modifier = Modifier.padding(3.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
  val onSurface = MaterialTheme.colorScheme.onSurface
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
  val secondary = MaterialTheme.colorScheme.secondary

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        video.subtitleCodec.split(" ").forEach { codec ->
          MetadataChip(
            text = codec,
            containerColor = Color.Transparent,
            contentColor = secondary,
            isOutlined = true
          )
        }
      }
    }

    val metadataText = buildAnnotatedString {
      var first = true
      fun appendSeparator() {
        if (!first) {
          withStyle(SpanStyle(color = onSurfaceVariant.copy(alpha = 0.38f))) {
            append("  •  ")
          }
        }
        first = false
      }

      if (showResolutionChip && video.height > 0) {
        appendSeparator()
        val resText = when {
          video.width >= 3840 || video.height >= 2160 -> "4K"
          video.width >= 2560 || video.height >= 1440 -> "1440p"
          video.width >= 1920 || video.height >= 1080 -> "1080p"
          video.width >= 1280 || video.height >= 720 -> "720p"
          video.width >= 854 || video.height >= 480 -> "480p"
          else -> "${video.height}p"
        }

        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = onSurface)) {
          append(resText)
        }

        if (showFramerateInResolution && video.fps > 0) {
          withStyle(SpanStyle(color = onSurfaceVariant, fontWeight = FontWeight.Normal)) {
            append("@${video.fps.toInt()}")
          }
        }
      }

      if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
        appendSeparator()
        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = onSurface)) {
          append(video.sizeFormatted)
        }
      }

      if (showDateChip && video.dateModified > 0) {
        appendSeparator()
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
          append(formatDate(video.dateModified))
        }
      }
    }

    if (metadataText.isNotEmpty()) {
      Text(
        text = metadataText,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun MetadataChip(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  isOutlined: Boolean = false
) {
  Surface(
    color = if (isOutlined) Color.Transparent else containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(8.dp),
    border = if (isOutlined) {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    } else {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
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

      StateLabel("Selected State (Tonal)")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isSelected = true)

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
      StateLabel("Recently Played")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isRecentlyPlayed = true)

      StateLabel("New / Unplayed Label")
      VideoCard(video = sampleVideo, settings = VideoCardSettings(), onClick = {}, isOldAndUnplayed = true)

      StateLabel("Watched (Dimmed + Strikethrough)")
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
