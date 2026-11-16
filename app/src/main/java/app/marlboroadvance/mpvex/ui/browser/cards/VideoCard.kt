@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package app.marlboroadvance.mpvex.ui.browser.cards

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
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

/**
 * Row sizes for a video card, chosen from the screen width and the user's font size.
 *
 * Width tiers follow Material's window size classes (compact < 600dp, medium < 840dp,
 * expanded), with an extra tier for very small phones (< 360dp). Lists can call
 * [rememberVideoCardDimens] once and pass the result to every row.
 */
@Immutable
data class VideoCardDimens(
  val rowPadding: Dp,
  val thumbTextGap: Dp,
  // The thumbnail takes this share of the row, kept between the min and max widths.
  val thumbnailFraction: Float,
  val thumbnailMinWidth: Dp,
  val thumbnailMaxWidth: Dp,
  val detailsSpacing: Dp,
  val chipSpacing: Dp,
  val chipSize: UnifiedChipSize,
  // Very large fonts on a phone: thumbnail on top, text underneath at full width.
  val stackThumbnail: Boolean,
) {
  companion object {
    fun forScreen(screenWidthDp: Int, fontScale: Float): VideoCardDimens {
      val base = when {
        screenWidthDp < 360 -> VideoCardDimens(
          rowPadding = 6.dp, thumbTextGap = 10.dp,
          thumbnailFraction = 0.45f, thumbnailMinWidth = 112.dp, thumbnailMaxWidth = 150.dp,
          detailsSpacing = 6.dp, chipSpacing = 4.dp, chipSize = UnifiedChipSize.Compact,
          stackThumbnail = false,
        )
        screenWidthDp < 600 -> VideoCardDimens(
          rowPadding = 8.dp, thumbTextGap = 12.dp,
          thumbnailFraction = 0.45f, thumbnailMinWidth = 128.dp, thumbnailMaxWidth = 184.dp,
          detailsSpacing = 8.dp, chipSpacing = 6.dp, chipSize = UnifiedChipSize.Medium,
          stackThumbnail = false,
        )
        screenWidthDp < 840 -> VideoCardDimens(
          rowPadding = 10.dp, thumbTextGap = 16.dp,
          thumbnailFraction = 0.36f, thumbnailMinWidth = 176.dp, thumbnailMaxWidth = 240.dp,
          detailsSpacing = 8.dp, chipSpacing = 6.dp, chipSize = UnifiedChipSize.Medium,
          stackThumbnail = false,
        )
        else -> VideoCardDimens(
          rowPadding = 12.dp, thumbTextGap = 20.dp,
          thumbnailFraction = 0.30f, thumbnailMinWidth = 200.dp, thumbnailMaxWidth = 280.dp,
          detailsSpacing = 10.dp, chipSpacing = 8.dp, chipSize = UnifiedChipSize.Medium,
          stackThumbnail = false,
        )
      }
      return when {
        fontScale >= 1.7f && screenWidthDp < 600 -> base.copy(stackThumbnail = true)
        // Bigger text needs more room beside the thumbnail, so the thumbnail gives some up.
        fontScale >= 1.3f -> base.copy(
          thumbnailFraction = base.thumbnailFraction * 0.85f,
          thumbnailMinWidth = base.thumbnailMinWidth - 16.dp,
        )
        else -> base
      }
    }
  }
}

/** Card sizes for the current screen width and font size; updates on rotation or resize. */
@Composable
fun rememberVideoCardDimens(): VideoCardDimens {
  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val fontScale = LocalDensity.current.fontScale
  return remember(screenWidthDp, fontScale) { VideoCardDimens.forScreen(screenWidthDp, fontScale) }
}

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
  // Marked as New from the "Mark as" sheet: shows the NEW chip whatever the file date.
  isMarkedNew: Boolean = false,
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
  // True while multi-select is active: every row shows a selection circle at its end.
  inSelectionMode: Boolean = false,
  // Sizes for this screen. Lists can compute this once and pass it to every row.
  dimens: VideoCardDimens = rememberVideoCardDimens(),
) {
  val isNew = remember(video.dateModified, settings.showUnplayedOldVideoLabel, isOldAndUnplayed, isMarkedNew, settings.unplayedOldVideoDays) {
    if (isMarkedNew) {
      true
    } else if (settings.showUnplayedOldVideoLabel && isOldAndUnplayed) {
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
    val thumbnail: @Composable (Modifier) -> Unit = { thumbModifier ->
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
        modifier = thumbModifier,
      )
    }

    val details: @Composable (Modifier) -> Unit = { detailsModifier ->
      Column(
        modifier = detailsModifier,
        verticalArrangement = Arrangement.spacedBy(dimens.detailsSpacing),
      ) {
        Text(
          text = displayTitle,
          style = MaterialTheme.typography.titleMedium,
          color = if (isWatched) supportingColor else contentColor,
          maxLines = if (settings.unlimitedNameLines) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(dimens.chipSpacing),
          verticalArrangement = Arrangement.spacedBy(dimens.chipSpacing),
        ) {
          subtitleNames?.let {
            UnifiedChip(text = it, isTertiary = true, bold = true, size = dimens.chipSize)
          }
          quality?.let {
            UnifiedChip(text = it, isPrimary = is4K, bold = is4K, size = dimens.chipSize)
          }
          if (showSizeChip && video.size > 0) {
            UnifiedChip(text = video.sizeFormatted, size = dimens.chipSize)
          }
          date?.let { UnifiedChip(text = it, size = dimens.chipSize) }
        }
      }
    }

    // Selection lives at the row's end, away from the thumbnail's watched tick.
    // It is shown when either flag is set, so the long-pressed row appears already
    // checked instead of flashing empty first; the fill change is animated.
    val selection: @Composable (Modifier) -> Unit = { selectionModifier ->
      AnimatedVisibility(
        visible = inSelectionMode || isSelected,
        enter = fadeIn(tween(150)) + expandHorizontally(tween(150), expandFrom = Alignment.Start),
        exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150), shrinkTowards = Alignment.Start),
        modifier = selectionModifier,
      ) {
        SelectionCircle(
          checked = isSelected,
          modifier = Modifier.padding(start = dimens.thumbTextGap, end = 4.dp),
        )
      }
    }

    if (dimens.stackThumbnail) {
      // Very large fonts on a phone: full-width thumbnail on top, text gets the whole row.
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(dimens.rowPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.thumbTextGap),
      ) {
        thumbnail(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
          details(Modifier.weight(1f))
          selection(Modifier.align(Alignment.CenterVertically))
        }
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(dimens.rowPadding),
        verticalAlignment = Alignment.Top,
      ) {
        // A share of the row, kept within the limits for this screen size.
        thumbnail(
          Modifier
            .fillMaxWidth(dimens.thumbnailFraction)
            .widthIn(min = dimens.thumbnailMinWidth, max = dimens.thumbnailMaxWidth)
            .aspectRatio(16f / 9f),
        )

        Spacer(Modifier.width(dimens.thumbTextGap))

        details(Modifier.weight(1f).padding(top = 2.dp))

        selection(Modifier.align(Alignment.CenterVertically))
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

/** Material 3 style round checkbox: outlined when off, filled primary with a check when on. */
@Composable
private fun SelectionCircle(checked: Boolean, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  val fill by animateColorAsState(
    targetValue = if (checked) colors.primary else Color.Transparent,
    animationSpec = tween(150),
    label = "selection_fill",
  )
  val outline by animateColorAsState(
    targetValue = if (checked) colors.primary else colors.outline,
    animationSpec = tween(150),
    label = "selection_outline",
  )
  val checkAlpha by animateFloatAsState(
    targetValue = if (checked) 1f else 0f,
    animationSpec = tween(150),
    label = "selection_check",
  )
  Box(
    modifier = modifier
      .size(24.dp)
      .clip(CircleShape)
      .background(fill)
      .border(2.dp, outline, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Check,
      contentDescription = if (checked) stringResource(R.string.card_cd_selected) else null,
      tint = colors.onPrimary,
      modifier = Modifier
        .size(16.dp)
        .alpha(checkAlpha),
    )
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

@Deprecated("Video rows now use UnifiedChip; kept only so old references still compile.")
enum class InfoPillStyle { Neutral, Accent, Subtitles }

private enum class ThumbnailTier { Small, Regular, Large }

/** Sizes of everything drawn over the thumbnail, picked from the thumbnail's own width. */
@Immutable
private data class ThumbnailOverlayDimens(
  val tier: ThumbnailTier,
  val inset: Dp,
  val gap: Dp,
  val badgeSize: Dp,
  val badgeIconSize: Dp,
  val progressHeight: Dp,
  val placeholderIconSize: Dp,
  val pillHorizontalPadding: Dp,
) {
  companion object {
    fun forWidth(width: Dp): ThumbnailOverlayDimens = when {
      width < 136.dp -> ThumbnailOverlayDimens(
        tier = ThumbnailTier.Small, inset = 4.dp, gap = 4.dp, badgeSize = 22.dp, badgeIconSize = 14.dp,
        progressHeight = 4.dp, placeholderIconSize = 28.dp, pillHorizontalPadding = 6.dp,
      )
      width < 208.dp -> ThumbnailOverlayDimens(
        tier = ThumbnailTier.Regular, inset = 6.dp, gap = 6.dp, badgeSize = 26.dp, badgeIconSize = 16.dp,
        progressHeight = 5.dp, placeholderIconSize = 32.dp, pillHorizontalPadding = 8.dp,
      )
      else -> ThumbnailOverlayDimens(
        tier = ThumbnailTier.Large, inset = 8.dp, gap = 8.dp, badgeSize = 32.dp, badgeIconSize = 20.dp,
        progressHeight = 6.dp, placeholderIconSize = 40.dp, pillHorizontalPadding = 10.dp,
      )
    }
  }
}

/** Pill drawn over the thumbnail image. */
@Composable
private fun ThumbnailPill(
  text: String,
  container: Color,
  content: Color,
  overlay: ThumbnailOverlayDimens,
  modifier: Modifier = Modifier,
) {
  val baseStyle = when (overlay.tier) {
    ThumbnailTier.Small -> MaterialTheme.typography.labelSmall
    ThumbnailTier.Regular -> MaterialTheme.typography.labelMedium
    ThumbnailTier.Large -> MaterialTheme.typography.labelLarge
  }
  Surface(modifier = modifier, color = container, contentColor = content, shape = CircleShape) {
    Text(
      text = text,
      style = baseStyle.copy(fontFeatureSettings = "tnum"),
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = overlay.pillHorizontalPadding, vertical = 2.dp),
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
) {
  val isInspection = LocalInspectionMode.current
  val thumbnailRepository = if (isInspection) null else koinInject<ThumbnailRepository>()
  val density = LocalDensity.current
  val shape = MaterialTheme.shapes.medium
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
      val overlay = remember(maxWidth) { ThumbnailOverlayDimens.forWidth(maxWidth) }
      val thumbWidthPx = with(density) { maxWidth.roundToPx() }
      // Request a size rounded up to a 64px step, so tiny layout changes don't trigger
      // a new decode, but a real change (rotation, unfolding) does.
      val requestWidthPx = ((thumbWidthPx + 63) / 64) * 64
      val requestHeightPx = (requestWidthPx / aspect).roundToInt()

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

      // Single effect replaces two competing ones. It also re-runs when the thumbnail
      // gets noticeably bigger, so a cached small bitmap isn't stretched and blurry.
      LaunchedEffect(thumbnailKey, allowThumbnailGeneration, showThumbnails, requestWidthPx) {
        val current = thumbnail
        val tooSmall = current != null &&
          current.width < requestWidthPx * 0.75f &&
          current.height < requestHeightPx * 0.75f
        if ((current == null || tooSmall) && showThumbnails && thumbnailRepository != null) {
          // Upgrading a too-small bitmap keeps showing it meanwhile: no shimmer.
          if (current == null) {
            isLoading = true
            loadFailed = false
          }
          val result = withContext(Dispatchers.IO) {
            if (allowThumbnailGeneration) {
              thumbnailRepository.getThumbnail(video, requestWidthPx, requestHeightPx)
            } else {
              thumbnailRepository.getCachedThumbnail(video, requestWidthPx, requestHeightPx)
            }
          }
          if (result != null && (current == null || result.width > current.width)) {
            thumbnail = result
          }
          if (current == null) {
            isLoading = false
            if (thumbnail == null) {
              loadFailed = true
            }
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
            modifier = Modifier.size(overlay.placeholderIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.ic_sym_movie),
          contentDescription = null,
          modifier = Modifier.size(overlay.placeholderIconSize),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      val showProgress =
        progressPercentage != null && progressPercentage > 0f && showProgressBar && !isWatched && video.duration > 0
      // Top start: NEW pill.
      if (isNew && !isWatched) {
        ThumbnailPill(
          text = stringResource(R.string.video_status_new).uppercase(Locale.getDefault()),
          container = MaterialTheme.colorScheme.primary,
          content = MaterialTheme.colorScheme.onPrimary,
          overlay = overlay,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(overlay.inset),
        )
      }

      // Bottom start: watched tick in a Material 3 Expressive cookie shape.
      if (isWatched) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(overlay.inset)
            .size(overlay.badgeSize)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = stringResource(R.string.video_status_watched),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(overlay.badgeIconSize),
          )
        }
      }

      // Bottom: the length pill stacked above the progress bar. Laying them out together
      // keeps the pill clear of the bar whatever size either one is.
      val hasDuration = video.durationFormatted.isNotBlank()
      if (hasDuration || showProgress) {
        Column(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(overlay.inset),
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(overlay.gap),
        ) {
          if (hasDuration) {
            ThumbnailPill(
              text = video.durationFormatted,
              container = Color.Black.copy(alpha = 0.72f),
              content = Color.White,
              overlay = overlay,
            )
          }
          if (showProgress) {
            LinearProgressIndicator(
              progress = { progressPercentage!!.coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(overlay.progressHeight)
                .clip(CircleShape),
              color = MaterialTheme.colorScheme.primary,
              trackColor = Color.White.copy(alpha = 0.4f),
              strokeCap = StrokeCap.Round,
              gapSize = 0.dp,
              drawStopIndicator = {},
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

@Preview(showBackground = true, name = "Small phone (320dp)", device = "spec:width=320dp,height=640dp")
@Composable
fun VideoCardSmallPhonePreview() {
  MaterialTheme {
    VideoCard(video = getSampleVideo(), settings = VideoCardSettings(), onClick = {}, progressPercentage = 0.45f)
  }
}

@Preview(showBackground = true, name = "Large font (1.5x)", fontScale = 1.5f, device = "spec:width=411dp,height=891dp")
@Composable
fun VideoCardLargeFontPreview() {
  MaterialTheme {
    VideoCard(video = getSampleVideo(), settings = VideoCardSettings(), onClick = {})
  }
}

@Preview(showBackground = true, name = "Huge font (2x, stacked)", fontScale = 2f, device = "spec:width=411dp,height=891dp")
@Composable
fun VideoCardHugeFontPreview() {
  MaterialTheme {
    VideoCard(video = getSampleVideo(), settings = VideoCardSettings(), onClick = {}, progressPercentage = 0.45f)
  }
}

@Preview(showBackground = true, name = "Landscape / foldable", device = "spec:width=891dp,height=411dp")
@Composable
fun VideoCardLandscapePreview() {
  MaterialTheme {
    VideoCard(video = getSampleVideo(), settings = VideoCardSettings(), onClick = {}, isWatched = true)
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
