package app.marlboroadvance.mpvex.ui.browser.cards

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontStyle
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

  Card(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val thumbnailRepository = koinInject<ThumbnailRepository>()
      // Rectangular thumbnail (16:9) with fixed width; height derives from aspect ratio
      val thumbWidthDp = 128.dp
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
            .clip(RoundedCornerShape(12.dp))
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
              modifier = Modifier.size(32.dp),
              tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            )
          }
        } else {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Play",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
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
                .height(4.dp),
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
                  .fillMaxWidth(progressPercentage)
                  .background(MaterialTheme.colorScheme.primary),
            )
          }
        }
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          video.displayName,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
          ).let { baseStyle ->
            if (isRecentlyPlayed) baseStyle.copy(fontStyle = FontStyle.Italic) else baseStyle
          },
          color = if (isRecentlyPlayed) {
            MaterialTheme.colorScheme.tertiary 
          } else if (isWatched) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
          verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
          if (showSubtitleIndicator) {
            if (video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
              video.subtitleCodec.split(" ").forEach { codec ->
                Text(
                  text = codec,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                  ),
                  modifier = Modifier
                    .background(
                      MaterialTheme.colorScheme.primaryContainer,
                      RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
          }
          if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
            Text(
              video.sizeFormatted,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier
                .background(
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          // Resolution and Framerate logic (List view)
            val fpsOnly = video.resolution.substringAfter("@", "")
            val hasFps = fpsOnly.isNotEmpty()

            if (showResolutionChip) {
              if (video.resolution != "--") {
                val displayResolution = if (showFramerateInResolution) {
                  video.resolution
                } else {
                  video.resolution.substringBefore("@")
                }

                Text(
                  displayResolution,
                  style = MaterialTheme.typography.labelSmall,
                  modifier =
                    Modifier
                      .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp),
                      )
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else if (showFramerateInResolution && hasFps) {
              // Resolution is hidden, but framerate is enabled -> show only framerate
              Text(
                "$fpsOnly FPS",
                style = MaterialTheme.typography.labelSmall,
                modifier =
                  Modifier
                    .background(
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                      RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            
            if (showDateChip && video.dateModified > 0) {
              Text(
                formatDate(video.dateModified),
                style = MaterialTheme.typography.labelSmall,
                modifier =
                  Modifier
                    .background(
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                      RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
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
