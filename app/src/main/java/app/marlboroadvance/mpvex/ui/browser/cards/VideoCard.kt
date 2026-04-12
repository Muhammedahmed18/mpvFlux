package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

  val showProgressBar =
    settings.showProgressBar && progressPercentage != null && progressPercentage > 0.01f && !isCompleted

  val isNew = remember(video.dateModified, settings.unplayedOldVideoDays, isOldAndUnplayed, isCompleted) {
    if (!settings.showUnplayedOldVideoLabel || !isOldAndUnplayed || isCompleted) false
    else {
        val currentTime = System.currentTimeMillis()
        val videoAge = currentTime - (video.dateModified * 1000)
        val thresholdMillis = settings.unplayedOldVideoDays * 24 * 60 * 60 * 1000L
        videoAge <= thresholdMillis
    }
  }

  val animatedProgress: Float by animateFloatAsState(
    targetValue = progressPercentage ?: 0f,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "progress"
  )

  val cardScale: Float by animateFloatAsState(
    targetValue = if (isSelected) 0.98f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "cardScale"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp)
      .scale(cardScale)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else Color.Transparent,
    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val thumbnailRepository = koinInject<ThumbnailRepository>()
      val thumbWidthDp = 160.dp 
      val aspect = 16f / 9f
      val thumbWidthPx = with(LocalDensity.current) { thumbWidthDp.roundToPx() }
      val thumbHeightPx = (thumbWidthPx / aspect).roundToInt()

      val thumbnailKey =
        remember(video.id, video.dateModified, video.size, thumbWidthPx, thumbHeightPx) {
          thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
        }

      var thumbnail by remember(thumbnailKey) {
        mutableStateOf(
          thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
        )
      }

      LaunchedEffect(thumbnailKey) {
        thumbnailRepository.thumbnailReadyKeys
          .filter { it == thumbnailKey }
          .collect {
            thumbnail =
              thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
          }
      }

      LaunchedEffect(thumbnailKey, settings.showThumbnails, allowThumbnailGeneration) {
        if (!settings.showThumbnails) {
          thumbnail = null
          return@LaunchedEffect
        }
        val memoryThumbnail =
          thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
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

      // Thumbnail
      Box(
        modifier = Modifier
          .width(thumbWidthDp)
          .aspectRatio(aspect)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .combinedClickable(
            onClick = onThumbClick,
            onLongClick = onLongClick,
          ),
        contentAlignment = Alignment.Center,
      ) {
        if (thumbnail != null) {
          Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
          )
        }

        if (isSelected) {
          Box(
            modifier = Modifier
              .padding(6.dp)
              .align(Alignment.TopEnd)
              .size(20.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Check,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
        }

        if (showProgressBar) {
          LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(2.5.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt,
          )
        }

        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = video.durationFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      // Content
      Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        verticalArrangement = Arrangement.Center
      ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isRecentlyPlayed) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            
            Text(
                text = if (settings.showVideoExtension) video.displayName 
                       else video.displayName.substringBeforeLast("."),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
                color = if (isRecentlyPlayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            if (isNew) {
                Text(
                    text = stringResource(R.string.video_label_new).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
            
            if (isCompleted && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Subtitle Badge
            if (showSubtitleIndicator && (video.hasEmbeddedSubtitles || video.subtitleCodec.isNotEmpty())) {
                val subText = if (video.subtitleCodec.isNotEmpty()) {
                    video.subtitleCodec.uppercase().substringBefore("/")
                } else "SUB"
                
                VideoMetadataChip(
                    text = subText,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Other Metadata
            if (showSizeChip) {
                VideoMetadataChip(text = video.sizeFormatted)
            }
            
            if (showResolutionChip) {
                val res = if (settings.showFramerateInResolution && video.fps > 0) {
                    "${video.resolution}@${video.fps.roundToInt()}"
                } else video.resolution
                VideoMetadataChip(text = res)
            }
            
            if (settings.showDateChip) {
                val sdf = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
                val dateStr = remember(video.dateModified) { sdf.format(java.util.Date(video.dateModified * 1000)) }
                VideoMetadataChip(text = dateStr)
            }
        }
      }
    }
  }
}

@Composable
private fun VideoMetadataChip(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Surface(
    modifier = modifier,
    color = containerColor,
    shape = RoundedCornerShape(6.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      ),
      color = contentColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}
