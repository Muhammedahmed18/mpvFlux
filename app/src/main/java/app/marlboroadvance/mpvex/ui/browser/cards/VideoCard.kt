package app.marlboroadvance.mpvex.ui.browser.cards

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
) {
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

  val showSizeChip = overrideShowSizeChip ?: settings.showSizeChip
  val showResolutionChip = overrideShowResolutionChip ?: settings.showResolutionChip
  
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val cardScale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.96f
      isSelected -> 0.98f
      else -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "video_card_scale"
  )

  val tonalElevation = if (isPressed) 4.dp else 1.dp
  val selectionBorderWidth = if (isSelected) 1.dp else 0.dp

  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .scale(cardScale)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
    tonalElevation = tonalElevation,
    border = if (isSelected) {
      BorderStroke(selectionBorderWidth, MaterialTheme.colorScheme.outlineVariant)
    } else null,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
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

      Spacer(modifier = Modifier.width(16.dp))

      VideoInfoPanel(
        displayTitle = displayTitle,
        isSelected = isSelected,
        isWatched = isWatched,
        isRecentlyPlayed = isRecentlyPlayed,
        video = video,
        showSubtitleIndicator = showSubtitleIndicator,
        showSizeChip = showSizeChip,
        showResolutionChip = showResolutionChip,
        showFramerate = settings.showFramerateInResolution,
        showDateChip = settings.showDateChip,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun VideoInfoPanel(
  displayTitle: String,
  isSelected: Boolean,
  isWatched: Boolean,
  isRecentlyPlayed: Boolean,
  video: Video,
  showSubtitleIndicator: Boolean,
  showSizeChip: Boolean,
  showResolutionChip: Boolean,
  showFramerate: Boolean,
  showDateChip: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = displayTitle,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isRecentlyPlayed -> MaterialTheme.colorScheme.primary
        isWatched -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
      },
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = if (isWatched) Modifier.alpha(0.6f) else Modifier
    )

    VideoMetadataChips(
      video = video,
      showSubtitleIndicator = showSubtitleIndicator,
      showSizeChip = showSizeChip,
      showResolutionChip = showResolutionChip,
      showFramerate = showFramerate,
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
  val shape = RoundedCornerShape(8.dp)

  Box(modifier = modifier) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .clip(shape)
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
          modifier = Modifier
            .matchParentSize()
            .alpha(if (isWatched) 0.4f else 1f),
          contentScale = ContentScale.Crop,
        )
      } else {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(40.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Duration pill
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
      ) {
        Text(
          text = video.durationFormatted,
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      // Status Badge (Top Right)
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
      ) {
        if (isWatched) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(22.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = "Watched",
              modifier = Modifier.padding(4.dp),
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        } else if (isNew) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = stringResource(R.string.video_label_new),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Progress Bar
      if (progressPercentage != null && showProgressBar && !isWatched) {
        val animatedProgress by animateFloatAsState(
          targetValue = progressPercentage,
          label = "VideoProgressAnimation"
        )
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(4.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
          strokeCap = StrokeCap.Round,
        )
      }
    }

    // Selection Check Badge (Bottom End)
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
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 8.dp, end = 8.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
      video.subtitleCodec.split(" ").forEach { codec ->
        SubtitleChip(text = codec)
      }
    }

    if (showResolutionChip && video.height > 0) {
      val baseRes = when {
        video.width >= 3840 || video.height >= 2160 -> "4K"
        video.width >= 2560 || video.height >= 1440 -> "1440p"
        video.width >= 1920 || video.height >= 1080 -> "1080p"
        video.width >= 1280 || video.height >= 720 -> "720p"
        video.width >= 854 || video.height >= 480 -> "480p"
        else -> "${video.height}p"
      }
      val resText = if (showFramerate && video.fps > 0) {
        "$baseRes@${video.fps.roundToInt()}"
      } else {
        baseRes
      }
      UnifiedChip(text = resText, compact = true)
    }

    if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
      UnifiedChip(text = video.sizeFormatted, compact = true)
    }

    if (showDateChip && video.dateModified > 0) {
      UnifiedChip(text = formatDate(video.dateModified), compact = true)
    }
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
