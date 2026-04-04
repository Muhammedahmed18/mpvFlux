package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
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
    val showVideoExtension by browserPreferences.showVideoExtension.collectAsState()
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

    val isCompleted = (progressPercentage ?: 0f) >= 0.95f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage ?: 0f,
        label = "VideoProgressAnimation"
    )

    // 1. Card Architecture: Tonal Card (surfaceContainerLow) with MaterialTheme.shapes.large (16dp)
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
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            } else {
                Color.Transparent
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnailRepository = koinInject<ThumbnailRepository>()
            // Adjusted size for better M3 balance
            val thumbWidthDp = 140.dp
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

            // 2. Thumbnail & Overlays
            Box(
                modifier = Modifier
                    .width(thumbWidthDp)
                    .aspectRatio(aspect)
                    .clip(RoundedCornerShape(12.dp)) // Refinement: Nested 12dp corners
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
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }

                // Status Badges using M3 system
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isCompleted) {
                            // "Completed" would be a Tertiary tonal icon badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Completed",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        if (showUnplayedOldVideoLabel && isOldAndUnplayed && !isCompleted) {
                            val currentTime = System.currentTimeMillis()
                            val videoAge = currentTime - (video.dateModified * 1000)
                            val thresholdMillis = unplayedOldVideoDays * 24 * 60 * 60 * 1000L

                            if (videoAge <= thresholdMillis) {
                                // "New" indicator would be a subtle Primary pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.video_label_new),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Selection State: Integrated checkmark badge on thumbnail
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Duration Badge (M3 style pill with surfaceContainer background)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = video.durationFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Integrated Progress: Linear Progress Indicator perfectly flush with the bottom edge
                if (progressPercentage != null && showProgressBar && !isCompleted) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        strokeCap = StrokeCap.Butt
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp)) // 4. Spacing: Standard M3 "Large" spacing

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 3. Typography: Title using titleMedium, color logic for played/unplayed
                Text(
                    if (showVideoExtension) video.displayName else video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRecentlyPlayed || isWatched || isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant // played videos
                    } else {
                        MaterialTheme.colorScheme.onSurface // unplayed videos
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Metadata Row: Using bodySmall and pipe separators with wrapping support
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val metadataStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val separatorStyle = metadataStyle.copy(
                        color = metadataStyle.color.copy(alpha = 0.5f)
                    )

                    val items = buildList {
                        if (showSizeChip && video.sizeFormatted != "0 B" && video.sizeFormatted != "--") {
                            add(video.sizeFormatted)
                        }

                        if (showResolutionChip && video.resolution != "--") {
                            val baseResolution = if (video.resolution.all { it.isDigit() }) {
                                "${video.resolution}p"
                            } else {
                                video.resolution
                            }

                            val displayResolution = if (showFramerateInResolution && video.fps > 0f) {
                                "$baseResolution@${video.fps.toInt()}"
                            } else {
                                baseResolution
                            }
                            add(displayResolution)
                        }

                        if (showDateChip && video.dateModified > 0) {
                            add(formatDate(video.dateModified))
                        }
                    }

                    items.forEachIndexed { index, text ->
                        Text(text, style = metadataStyle)
                        if (index < items.size - 1) {
                            Text("|", style = separatorStyle)
                        }
                    }
                }

                // 3. Subtitle Labels: M3 Assist Chips
                if (showSubtitleIndicator && video.hasEmbeddedSubtitles && video.subtitleCodec.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        video.subtitleCodec.split(" ").take(2).forEach { codec ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        codec.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                    )
                                },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
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
