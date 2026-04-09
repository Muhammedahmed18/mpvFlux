package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Immutable
data class VideoCardSettings(
  val unlimitedNameLines: Boolean = false,
  val showThumbnails: Boolean = true,
  val showVideoExtension: Boolean = true,
  val showSizeChip: Boolean = true,
  val showResolutionChip: Boolean = true,
  val showFramerateInResolution: Boolean = true,
  val showProgressBar: Boolean = true,
  val showDateChip: Boolean = true,
  val showUnplayedOldVideoLabel: Boolean = true,
  val unplayedOldVideoDays: Int = 7,
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
  val maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2
  val showSizeChip = overrideShowSizeChip ?: settings.showSizeChip
  val showResolutionChip = overrideShowResolutionChip ?: settings.showResolutionChip

  val isCompleted by remember(progressPercentage, isWatched) {
    derivedStateOf { isWatched || (progressPercentage ?: 0f) >= 0.95f }
  }

  val showProgressBar = settings.showProgressBar && progressPercentage != null && progressPercentage > 0.01f && !isCompleted

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer
      else
        MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = if (isSelected)
        MaterialTheme.colorScheme.onSecondaryContainer
      else
        MaterialTheme.colorScheme.onSurface,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
    border = null
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val thumbnailRepository = koinInject<ThumbnailRepository>()
      val thumbWidthDp = 168.dp
      val aspect = 16f / 9f
      val thumbWidthPx = with(LocalDensity.current) { thumbWidthDp.roundToPx() }
      val thumbHeightPx = (thumbWidthPx / aspect).roundToInt()

      val thumbnailKey =
        remember(video.id, video.dateModified, video.size, thumbWidthPx, thumbHeightPx) {
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

      LaunchedEffect(thumbnailKey, settings.showThumbnails, allowThumbnailGeneration) {
        if (!settings.showThumbnails) {
          thumbnail = null
          return@LaunchedEffect
        }

        val memoryThumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
        if (memoryThumbnail != null) {
          thumbnail = memoryThumbnail
          return@LaunchedEffect
        }

        val loadedThumbnail = withContext(Dispatchers.IO) {
          if (allowThumbnailGeneration) {
            thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
          } else {
            thumbnailRepository.getCachedThumbnail(video, thumbWidthPx, thumbHeightPx)
          }
        }
        if (loadedThumbnail != null) {
          thumbnail = loadedThumbnail
        }
      }

      Box(
        modifier = Modifier
          .width(thumbWidthDp)
          .aspectRatio(aspect)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .combinedClickable(
            onClick = onThumbClick,
            onLongClick = onLongClick,
          ),
        contentAlignment = Alignment.Center,
      ) {
        thumbnail?.let { bmp ->
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Thumbnail",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
        } ?: run {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Play",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }

        if (isSelected) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = if (showProgressBar) 10.dp else 6.dp, end = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = video.durationFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.sp
            ),
            color = Color.White,
          )
        }

        if (showProgressBar) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progressPercentage ?: 0f)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          if (settings.showUnplayedOldVideoLabel && isOldAndUnplayed && !isCompleted) {
            val isNew = remember(video.dateModified, settings.unplayedOldVideoDays) {
              val currentTime = System.currentTimeMillis()
              val videoAge = currentTime - (video.dateModified * 1000)
              val thresholdMillis = settings.unplayedOldVideoDays * 24 * 60 * 60 * 1000L
              videoAge <= thresholdMillis
            }
            if (isNew) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.secondaryContainer)
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(
                  text = stringResource(R.string.video_label_new),
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                  )
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          if (isCompleted && !isSelected) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(4.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Filled.Check,
                contentDescription = "Completed",
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        verticalArrangement = Arrangement.Center
      ) {
        val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

        Text(
          text = if (settings.showVideoExtension) video.displayName else video.displayName.substringBeforeLast("."),
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = if (isRecentlyPlayed) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            lineHeight = 20.sp
          ),
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
          color = if (isRecentlyPlayed && !isSelected) {
            MaterialTheme.colorScheme.primary
          } else {
            contentColor
          }
        )

        Spacer(modifier = Modifier.height(4.dp))

        val metadataText = remember(video, settings, showSizeChip, showResolutionChip, showSubtitleIndicator) {
          val parts = mutableListOf<String>()
          if (showSizeChip) parts.add(video.sizeFormatted)
          if (showResolutionChip) {
            val resText = if (settings.showFramerateInResolution && video.fps > 0) {
              "${video.resolution} • ${video.fps.roundToInt()}fps"
            } else {
              video.resolution
            }
            parts.add(resText)
          }
          if (settings.showDateChip) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            parts.add(sdf.format(java.util.Date(video.dateModified * 1000)))
          }
          if (showSubtitleIndicator && (video.hasEmbeddedSubtitles || video.subtitleCodec.isNotEmpty())) {
            parts.add(if (video.subtitleCodec.isNotEmpty()) video.subtitleCodec.uppercase().substringBefore("/") else "SUB")
          }
          parts.joinToString(" • ")
        }

        Text(
          text = metadataText,
          style = MaterialTheme.typography.bodySmall,
          color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
