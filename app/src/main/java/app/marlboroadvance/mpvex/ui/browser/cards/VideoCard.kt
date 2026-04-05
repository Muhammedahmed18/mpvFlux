package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
    
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage ?: 0f,
        label = "VideoProgressAnimation"
    )
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "ContainerColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "ContentColor"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        label = "BorderWidth"
    )
    val animatedThumbnailPadding by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 0.dp,
        label = "ThumbnailPadding"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
        ),
        border = if (isSelected) BorderStroke(animatedBorderWidth, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    .padding(animatedThumbnailPadding)
                    .clip(RoundedCornerShape(12.dp))
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

                // Selection Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
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
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
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
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.video_label_new),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
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
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Completed",
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                if (settings.showProgressBar && progressPercentage != null && progressPercentage > 0.01f && !isCompleted) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                            .height(4.dp)
                            .drawBehind {
                                // Subtle glow effect for the active progress
                                if (animatedProgress > 0f) {
                                    drawRoundRect(
                                        color = primaryColor.copy(alpha = 0.3f),
                                        size = size.copy(width = size.width * animatedProgress, height = size.height * 2f),
                                        topLeft = Offset(0f, -size.height * 0.5f),
                                        cornerRadius = CornerRadius(size.height, size.height)
                                    )
                                }
                            },
                        color = primaryColor,
                        trackColor = Color.Black.copy(alpha = 0.4f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (settings.showVideoExtension) video.displayName else video.displayName.substringBeforeLast("."),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isRecentlyPlayed) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.sp
                    ),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isRecentlyPlayed && !isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        animatedContentColor
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val chipContainerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                    val chipContentColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        animatedContentColor.copy(alpha = 0.8f)
                    }

                    if (showSizeChip) {
                        MetadataChip(text = video.sizeFormatted, containerColor = chipContainerColor, contentColor = chipContentColor)
                    }

                    if (showResolutionChip) {
                        val resText = if (settings.showFramerateInResolution && video.fps > 0) {
                            "${video.resolution} • ${video.fps.roundToInt()}fps"
                        } else {
                            video.resolution
                        }
                        MetadataChip(text = resText, containerColor = chipContainerColor, contentColor = chipContentColor)
                    }

                    if (settings.showDateChip) {
                        val formattedDate = remember(video.dateModified) {
                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(video.dateModified * 1000))
                        }
                        MetadataChip(text = formattedDate, containerColor = chipContainerColor, contentColor = chipContentColor)
                    }

                    if (showSubtitleIndicator && (video.hasEmbeddedSubtitles || video.subtitleCodec.isNotEmpty())) {
                        val subText = if (video.subtitleCodec.isNotEmpty()) {
                            video.subtitleCodec.uppercase().substringBefore("/")
                        } else "SUB"

                        MetadataChip(
                            text = subText,
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            isBold = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    isBold: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = contentColor
        )
    }
}
