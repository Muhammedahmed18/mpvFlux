package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
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
  
  val showSizeChip = overrideShowSizeChip ?: showSizeChipPref
  val showResolutionChip = overrideShowResolutionChip ?: showResolutionChipPref

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(12.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      )
      .then(
        if (isSelected) Modifier.border(
          width = 1.5.dp,
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(12.dp)
        ) else Modifier
      ),
    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    tonalElevation = if (isSelected) 2.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.Top
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
        mutableStateOf(thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
      }

      LaunchedEffect(thumbnailKey) {
        thumbnailRepository.thumbnailReadyKeys
          .filter { it == thumbnailKey }
          .collect {
            thumbnail = thumbnailRepository.getThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx)
          }
      }

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
            .border(
              width = 0.5.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp)
            )
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
              modifier = Modifier.size(40.dp),
              tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            )
          }
        } else {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Play",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
          )
        }

        if (isWatched) {
          Surface(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(4.dp)
          ) {
            Icon(
              Icons.Rounded.CheckCircle,
              contentDescription = null,
              modifier = Modifier.padding(2.dp).size(12.dp),
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
        }

        if (showUnplayedOldVideoLabel && isOldAndUnplayed) {
          val currentTime = System.currentTimeMillis()
          val videoAge = currentTime - (video.dateModified * 1000)
          val thresholdMillis = unplayedOldVideoDays * 24 * 60 * 60 * 1000L

          if (videoAge <= thresholdMillis) {
            Surface(
              modifier =
                Modifier
                  .align(Alignment.TopStart)
                  .padding(6.dp),
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = stringResource(R.string.video_label_new),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }

        Surface(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp),
          color = Color.Black.copy(alpha = 0.7f),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = video.durationFormatted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp
            ),
            color = Color.White,
          )
        }

        if (progressPercentage != null && showProgressBar) {
          Box(
            modifier =
              Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.CenterStart
          ) {
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
      Spacer(modifier = Modifier.width(20.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          video.displayName,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
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
        
        val metadataParts = remember(video, showSizeChip, showResolutionChip, showDateChip, showFramerateInResolution) {
          buildList {
            if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
              add(video.sizeFormatted)
            }
            if (showResolutionChip && video.resolution != "--") {
                val res = if (showFramerateInResolution && video.fps > 0) {
                    "${video.resolution} (${video.fps.roundToInt()}fps)"
                } else video.resolution
                add(res)
            }
            if (showDateChip && video.dateModified > 0) {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                add(sdf.format(java.util.Date(video.dateModified * 1000)))
            }
          }
        }

        if (metadataParts.isNotEmpty()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = metadataParts.joinToString("  •  "),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              letterSpacing = 0.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
