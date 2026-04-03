package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun VideoCard(
  video: Video,
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
  val appearancePreferences = koinInject<AppearancePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val showSizeChipPref by browserPreferences.showSizeChip.collectAsState()
  val showResolutionChipPref by browserPreferences.showResolutionChip.collectAsState()
  val showFramerateInResolution by browserPreferences.showFramerateInResolution.collectAsState()
  val showProgressBar by browserPreferences.showProgressBar.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showUnplayedOldVideoLabel by appearancePreferences.showUnplayedOldVideoLabel.collectAsState()
  val unplayedOldVideoDays by appearancePreferences.unplayedOldVideoDays.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2
  
  // Use override parameters if provided, otherwise use preferences
  val showSizeChip = overrideShowSizeChip ?: showSizeChipPref
  val showResolutionChip = overrideShowResolutionChip ?: showResolutionChipPref

  val animatedProgress by animateFloatAsState(
    targetValue = progressPercentage ?: 0f,
    animationSpec = tween(durationMillis = 600),
    label = "VideoProgress"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp) // Outer gap for selection
        .clip(RoundedCornerShape(16.dp))
        .background(
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
        )
        .padding(horizontal = 12.dp, vertical = 12.dp), // Inner content padding
      verticalAlignment = Alignment.CenterVertically
    ) {
      val thumbnailRepository = koinInject<ThumbnailRepository>()
      // Rectangular thumbnail (16:9) with fixed width; height derives from aspect ratio
      val thumbWidthDp = 160.dp // Increased from 128.dp
      val aspect = 16f / 9f
      val thumbWidthPx = with(LocalDensity.current) { thumbWidthDp.roundToPx() }
      val thumbHeightPx = (thumbWidthPx / aspect).roundToInt()

      // Load thumbnail with optimized state management
      // Key includes video identity to prevent reloading same thumbnail
      val thumbnailKey =
        remember(video.id, video.dateModified, video.size, thumbWidthPx, thumbHeightPx) {
          thumbnailRepository.thumbnailKey(video, thumbWidthPx, thumbHeightPx)
        }

      // Try to get from memory cache immediately (synchronous, no flicker)
      var thumbnail by remember(thumbnailKey) {
        mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
      }

      // Update thumbnail when the repository emits that this key became ready (folder prefetch or any other source).
      LaunchedEffect(thumbnailKey) {
        thumbnailRepository.thumbnailReadyKeys
          .filter { it == thumbnailKey }
          .collect {
            thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
          }
      }

      // Optional immediate generation (used on screens that don't run folder-wide sequential generation).
      LaunchedEffect(thumbnailKey, allowThumbnailGeneration, showThumbnails) {
        if (thumbnail == null && showThumbnails) {
          thumbnail =
            withContext(Dispatchers.IO) {
              if (allowThumbnailGeneration) {
                thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
              } else {
                thumbnailRepository.getCachedThumbnail(video, thumbWidthPx, thumbHeightPx)
              }
            }
        }
      }

      Box(
        modifier =
          Modifier
            .width(thumbWidthDp)
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
              onClick = onThumbClick,
              onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
      ) {
        if (showThumbnails) {
          thumbnail?.let {
            Image(
              bitmap = it.asImageBitmap(),
              contentDescription = "Thumbnail",
              modifier = Modifier.matchParentSize(),
              contentScale = ContentScale.Crop,
            )
          } ?: run {
            Icon(
              Icons.Filled.PlayArrow,
              contentDescription = "Play",
              modifier = Modifier.size(40.dp), // Increased from 32.dp
              tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            )
          }
        } else {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Play",
            modifier = Modifier.size(40.dp), // Increased from 32.dp
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
          )
        }

        // Show tick icon for watched videos
        if (isWatched) {
          Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp)
              .size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
          )
        }

        // Show "NEW" label for recently added unplayed videos if enabled (top-left corner)
        // Like MX Player: show NEW for videos added within threshold days that haven't been played
        if (showUnplayedOldVideoLabel && isOldAndUnplayed) {
          // Check if video is recently modified (within threshold days)
          val currentTime = System.currentTimeMillis()
          val videoAge = currentTime - (video.dateModified * 1000) // dateModified is in seconds
          val thresholdMillis = unplayedOldVideoDays * 24 * 60 * 60 * 1000L

          if (videoAge <= thresholdMillis) {
            Box(
              modifier =
                Modifier
                  .align(Alignment.TopStart)
                  .padding(8.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.error)
                  .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = stringResource(R.string.video_label_new),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onError,
              )
            }
          }
        }


        // Duration timestamp overlay at bottom-right of the thumbnail
        Box(
          modifier =
            Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color.Black.copy(alpha = 0.5f))
              .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = video.durationFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium
            ),
            color = Color.White,
          )
        }

        // Progress bar at bottom of thumbnail
        if (progressPercentage != null && showProgressBar) {
          Box(
            modifier =
              Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.5.dp),
          ) {
            // Background (unwatched portion)
            Box(
              modifier =
                Modifier
                  .matchParentSize()
                  .background(Color.Black.copy(alpha = 0.6f)),
            )
            // Progress (watched portion)
            Box(
              modifier =
                Modifier
                  .fillMaxHeight()
                  .fillMaxWidth(animatedProgress)
                  .background(MaterialTheme.colorScheme.primary),
            )
          }
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          video.displayName,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (isRecentlyPlayed) FontWeight.Bold else FontWeight.SemiBold
          ),
          color = if (isRecentlyPlayed) {
            MaterialTheme.colorScheme.primary
          } else if (isWatched) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        val metadataParts = remember(video, showSizeChip, showResolutionChip, showDateChip, showFramerateInResolution) {
          buildList {
            if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
              add(video.sizeFormatted)
            }
            if (showResolutionChip && video.resolution != "--") {
              val res = if (showFramerateInResolution) video.resolution else video.resolution.substringBefore("@")
              add(res)
            } else if (showFramerateInResolution && video.resolution.substringAfter("@", "").isNotEmpty()) {
              add("${video.resolution.substringAfter("@")} FPS")
            }
            if (showDateChip && video.dateModified > 0) {
              add(formatDate(video.dateModified))
            }
          }
        }

        if (metadataParts.isNotEmpty() || (showSubtitleIndicator && video.hasEmbeddedSubtitles)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
          ) {
            if (showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
                video.subtitleCodec.split(" ").forEach { codec ->
                  Text(
                    text = codec,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                      .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
            }
            
            if (metadataParts.isNotEmpty()) {
              Text(
                text = metadataParts.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

private fun formatDate(timestampSeconds: Long): String {
  val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
  return sdf.format(java.util.Date(timestampSeconds * 1000))
}
