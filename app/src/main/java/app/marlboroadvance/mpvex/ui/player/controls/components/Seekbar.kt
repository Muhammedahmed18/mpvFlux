package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.indication
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import app.marlboroadvance.mpvex.ui.player.controls.LocalPlayerButtonsClickEvent
import app.marlboroadvance.mpvex.ui.player.controls.playerChromeContentColor
import app.marlboroadvance.mpvex.ui.player.controls.playerGlass
import app.marlboroadvance.mpvex.ui.theme.spacing
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun SeekbarWithTimers(
    positionFlow: StateFlow<Float>,
    durationFlow: StateFlow<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    timersInverted: Pair<Boolean, Boolean>,
    positionTimerOnClick: (() -> Unit)?,
    durationTimerOnCLick: () -> Unit,
    chapters: ImmutableList<Segment>,
    paused: Boolean,
    seekbarStyle: SeekbarStyle = SeekbarStyle.Thick,
    loopStart: Float? = null,
    loopEnd: Float? = null,
    modifier: Modifier = Modifier,
    hideBackground: Boolean = false,
) {
    val clickEvent = LocalPlayerButtonsClickEvent.current
    val haptics = LocalHapticFeedback.current
    var isUserInteracting by remember { mutableStateOf(false) }
    var userPosition by remember { mutableFloatStateOf(0f) }
    // Where the current drag started, for the "+0:45" in the bubble.
    var dragStartPosition by remember { mutableFloatStateOf(0f) }

    val position by positionFlow.collectAsState()
    val duration by durationFlow.collectAsState()

    LaunchedEffect(isUserInteracting) {
        if (isUserInteracting) dragStartPosition = position
    }
    // A light tick when a drag crosses into another chapter.
    val dragChapter = if (isUserInteracting && chapters.size > 1) {
        chapters.indexOfLast { it.start <= userPosition }
    } else {
        -1
    }
    var lastDragChapter by remember { mutableStateOf(-1) }
    LaunchedEffect(dragChapter) {
        if (dragChapter >= 0 && lastDragChapter >= 0 && dragChapter != lastDragChapter) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastDragChapter = dragChapter
    }

    val scope = rememberCoroutineScope()
    val animatedPosition = remember { Animatable(positionFlow.value) }

    // Position now arrives at ~1 Hz (whole-second ticks) to save battery. Bridge the gap
    // with a 1s linear tween so the thumb glides at playback rate between ticks. Large
    // deltas (user seeks, playlist/file changes) snap instantly instead of slow-crawling
    // across the bar.
    LaunchedEffect(position, isUserInteracting) {
        if (!isUserInteracting && position != animatedPosition.value) {
            if (kotlin.math.abs(position - animatedPosition.value) > 2f) {
                animatedPosition.snapTo(position)
            } else {
                animatedPosition.animateTo(
                    targetValue = position,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                )
            }
        }
    }

    Row(
        modifier = modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        VideoTimer(
            value = if (isUserInteracting) userPosition else position,
            isInverted = timersInverted.first,
            // Only tappable when it has a job; otherwise plain text with no ripple.
            onClick = positionTimerOnClick?.let { handler ->
                {
                    clickEvent()
                    handler()
                }
            },
            hideBackground = hideBackground,
            modifier = Modifier.widthIn(min = 72.dp),
        )

        Box(
            // The whole 48dp row height is the touch area: the Slider itself handles
            // taps and drags, with no extra gesture layer underneath.
            modifier = Modifier.weight(1f).height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            // animatedPosition changes on every frame of the 1 s glide between position ticks.
            // Reading it here re-ran this whole composable (timers, gesture modifiers and all)
            // at the display refresh rate while the controls were visible. It's now read only
            // inside AnimatedSeekbar, so just the seekbar itself recomposes per frame.
            AnimatedSeekbar(
                position = { if (isUserInteracting) userPosition else animatedPosition.value },
                duration = duration,
                chapters = chapters,
                isPaused = paused,
                isScrubbing = isUserInteracting,
                seekbarStyle = seekbarStyle,
                onSeek = { newPos -> if (!isUserInteracting) isUserInteracting = true; userPosition = newPos; onValueChange(newPos) },
                onSeekFinished = { scope.launch { animatedPosition.snapTo(userPosition) }; isUserInteracting = false; onValueChangeFinished() },
                loopStart = loopStart,
                loopEnd = loopEnd,
            )

            // Time bubble above the thumb while dragging.
            androidx.compose.animation.AnimatedVisibility(
                visible = isUserInteracting && duration > 0f,
                enter = androidx.compose.animation.fadeIn(tween(120)),
                exit = androidx.compose.animation.fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-36).dp),
            ) {
                SeekBubble(
                    position = userPosition,
                    duration = duration,
                    startPosition = dragStartPosition,
                    chapters = chapters,
                    thumbSlotWidth = seekbarThumbSlotWidth(seekbarStyle == SeekbarStyle.Thick),
                )
            }
        }

        VideoTimer(
            value = if (timersInverted.second) position - duration else duration,
            isInverted = timersInverted.second,
            onClick = {
                clickEvent()
                durationTimerOnCLick()
            },
            hideBackground = hideBackground,
            modifier = Modifier.widthIn(min = 72.dp),
        )
    }
}

/** Reads the (per-frame) animated position in its own recomposition scope. */
@Composable
private fun AnimatedSeekbar(
    position: () -> Float,
    duration: Float,
    chapters: ImmutableList<Segment>,
    isPaused: Boolean,
    isScrubbing: Boolean,
    seekbarStyle: SeekbarStyle,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    loopStart: Float?,
    loopEnd: Float?,
) {
    StandardSeekbar(
        position = position(),
        duration = duration,
        chapters = chapters,
        isPaused = isPaused,
        isScrubbing = isScrubbing,
        seekbarStyle = seekbarStyle,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
        loopStart = loopStart,
        loopEnd = loopEnd,
    )
}

@Composable
fun VideoTimer(
    value: Float,
    isInverted: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hideBackground: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Same round Standard-glass pill as the player's icon buttons; follows
    // "hide button background" like every other control.
    val shape = CircleShape
    // Outer box = 48dp touch target; inner pill keeps its compact look and shows the
    // ripple. With no onClick the timer is plain text (no ripple, nothing to press).
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .playerGlass(shape, hideBackground)
                .then(if (onClick != null) Modifier.indication(interactionSource, ripple()) else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = Utils.prettyTime(value.toInt(), isInverted),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = playerChromeContentColor(hideBackground),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun StandardSeekbar(position: Float, duration: Float, chapters: ImmutableList<Segment>, isPaused: Boolean = false, isScrubbing: Boolean = false, seekbarStyle: SeekbarStyle = SeekbarStyle.Standard, onSeek: (Float) -> Unit, onSeekFinished: () -> Unit, loopStart: Float? = null, loopEnd: Float? = null, modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.primary
    // Classic look: the unplayed part is the accent colour at 30%.
    val inactiveColor = seekbarInactiveColor(activeColor)
    // A-B loop markers use the theme's tertiary so they follow dynamic colour.
    val loopColor = MaterialTheme.colorScheme.tertiary
    val interactionSource = remember { MutableInteractionSource() }
    val isThick = seekbarStyle == SeekbarStyle.Thick
    // Reuse a single Animatable across pause/scrub toggles instead of allocating a new
    // one each time the effect re-runs.
    val heightAnimatable = remember { Animatable(1f) }
    // Classic behaviour: both styles ease the track to 70% height while paused or dragging.
    LaunchedEffect(isPaused, isScrubbing) {
        val shouldFlatten = isPaused || isScrubbing
        val targetHeight = if (shouldFlatten) 0.7f else 1f
        delay(if (shouldFlatten) 0L else 60L)
        heightAnimatable.animateTo(targetValue = targetHeight, animationSpec = tween(durationMillis = if (shouldFlatten) 550 else 800, easing = LinearEasing))
    }
    val trackHeightDp = (if (isThick) ThickTrackHeight else StandardTrackHeight) * heightAnimatable.value
    // Chapter start times as fractions of the video (the first chapter at 0 isn't a cut).
    val chapterFractions = remember(chapters, duration) { chapterFractions(chapters, duration) }

    Slider(value = position, onValueChange = onSeek, onValueChangeFinished = onSeekFinished, valueRange = 0f..duration.coerceAtLeast(0.1f), modifier = modifier.fillMaxWidth().height(48.dp), interactionSource = interactionSource,
        track = { sliderState ->
            Canvas(modifier = Modifier.fillMaxWidth().height(trackHeightDp)) {
                val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).takeIf { it > 0f } ?: 1f
                val fraction = ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                drawSeekTrack(
                    fraction = fraction,
                    chapterFractions = chapterFractions,
                    isThick = isThick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                )
                if (loopStart != null || loopEnd != null) {
                    if (loopStart != null) { val startPx = (loopStart / duration).coerceIn(0f, 1f) * size.width; drawLine(color = loopColor, start = Offset(startPx, 0f), end = Offset(startPx, size.height), strokeWidth = 2.dp.toPx()) }
                    if (loopEnd != null) { val endPx = (loopEnd / duration).coerceIn(0f, 1f) * size.width; drawLine(color = loopColor, start = Offset(endPx, 0f), end = Offset(endPx, size.height), strokeWidth = 2.dp.toPx()) }
                    if (loopStart != null && loopEnd != null) { val minPx = (minOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width; val maxPx = (maxOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width; drawRect(color = loopColor.copy(alpha = 0.3f), topLeft = Offset(minPx, 0f), size = Size(maxPx - minPx, size.height)) }
                }
            }
        },
        thumb = {
            // Classic fixed-size handle (doesn't change while dragging), 6dp wide.
            // Standard: a 24dp-tall rounded pill standing above the 8dp track.
            // Thick: a 16dp block (same height as the track) with 3dp corners.
            Box(
                modifier = Modifier
                    .width(SeekbarThumbWidth)
                    .height(if (isThick) ThickTrackHeight else 24.dp)
                    .background(activeColor, if (isThick) RoundedCornerShape(3.dp) else CircleShape),
            )
        }
    )
}

// Seekbar sizes (classic style). Standard = 8dp track with a 6×24dp pill handle;
// Thick = 16dp track with a 6×16dp block handle. Both leave 7dp either side of the
// handle's centre clear.
internal val StandardTrackHeight = 8.dp
internal val ThickTrackHeight = 16.dp
internal val SeekbarThumbWidth = 6.dp
private val SeekbarThumbGapHalf = 7.dp

/** Handle width, used to line the drag bubble up with it (same for both styles). */
@Suppress("UNUSED_PARAMETER")
internal fun seekbarThumbSlotWidth(isThick: Boolean) = SeekbarThumbWidth

/** Unplayed-track colour: the accent at 30% (classic style). */
internal fun seekbarInactiveColor(activeColor: Color) = activeColor.copy(alpha = 0.3f)

/** Chapter starts as fractions of [duration], skipping a chapter at the very start. */
internal fun chapterFractions(chapters: List<Segment>, duration: Float): List<Float> {
    if (duration <= 0f || chapters.size < 2) return emptyList()
    return chapters.map { it.start / duration }.filter { it > 0.005f && it < 0.995f }.sorted()
}

/**
 * Draws the seek track: played part in [activeColor], the rest in [inactiveColor], a gap
 * around the handle, small gaps where chapters start, and a stop dot. Outer ends are
 * fully rounded. Edges next to a gap: Standard gets small 2dp corners, Thick is fully
 * rounded so each piece reads as its own pill (classic style).
 */
internal fun DrawScope.drawSeekTrack(
    fraction: Float,
    chapterFractions: List<Float>,
    isThick: Boolean,
    activeColor: Color,
    inactiveColor: Color,
) {
    val width = size.width
    val trackHeight = size.height
    val outerRadius = trackHeight / 2f
    val innerRadius = if (isThick) outerRadius else 2.dp.toPx()
    val playedPx = width * fraction
    // 7dp either side of the handle's centre (6dp handle → 4dp visible gap each side).
    val thumbGapHalf = SeekbarThumbGapHalf.toPx()
    val chapterGapHalf = if (isThick) 2.dp.toPx() else 1.5.dp.toPx()

    // Cuts: the thumb gap and one small gap per chapter start.
    val cuts = buildList {
        if (thumbGapHalf > 0f) add((playedPx - thumbGapHalf) to (playedPx + thumbGapHalf))
        chapterFractions.forEach { f -> add((width * f - chapterGapHalf) to (width * f + chapterGapHalf)) }
    }.sortedBy { it.first }

    // Pieces of the track between cuts, split at the played position for colouring.
    val pieces = mutableListOf<Pair<Float, Float>>()
    var cursor = 0f
    for ((cutStart, cutEnd) in cuts) {
        if (cutStart > cursor) pieces += cursor to cutStart.coerceAtMost(width)
        cursor = maxOf(cursor, cutEnd)
    }
    if (cursor < width) pieces += cursor to width

    fun corner(x: Float): CornerRadius =
        if (x <= 0.5f || x >= width - 0.5f) CornerRadius(outerRadius) else CornerRadius(innerRadius)

    fun drawPiece(startX: Float, endX: Float, color: Color, roundLeft: Boolean, roundRight: Boolean) {
        if (endX - startX < 0.5f) return
        val left = if (roundLeft) corner(startX) else CornerRadius.Zero
        val right = if (roundRight) corner(endX) else CornerRadius.Zero
        val path = Path()
        path.addRoundRect(
            RoundRect(
                left = startX, top = 0f, right = endX, bottom = trackHeight,
                topLeftCornerRadius = left, bottomLeftCornerRadius = left,
                topRightCornerRadius = right, bottomRightCornerRadius = right,
            ),
        )
        drawPath(path, color)
    }

    for ((start, end) in pieces) {
        when {
            end <= playedPx -> drawPiece(start, end, activeColor, roundLeft = true, roundRight = true)
            start >= playedPx -> drawPiece(start, end, inactiveColor, roundLeft = true, roundRight = true)
            else -> {
                // Defensive: with a handle gap a piece shouldn't span the play position,
                // but split it cleanly if it ever does.
                drawPiece(start, playedPx, activeColor, roundLeft = true, roundRight = false)
                drawPiece(playedPx, end, inactiveColor, roundLeft = false, roundRight = true)
            }
        }
    }

    // Stop indicator: a small dot at the end of the track while there is track left.
    val dotRadius = 2.dp.toPx()
    val dotX = width - outerRadius
    if (playedPx + thumbGapHalf < dotX - dotRadius) {
        drawCircle(color = activeColor, radius = dotRadius, center = Offset(dotX, trackHeight / 2f))
    }
}

/**
 * Small drawing of a seekbar style for the settings picker: sample chapter gaps,
 * 40% played and the style's thumb.
 */
@Composable
fun SeekbarStylePreview(style: SeekbarStyle, modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = seekbarInactiveColor(activeColor)
    val isThick = style == SeekbarStyle.Thick
    Canvas(modifier = modifier.width(120.dp).height(28.dp)) {
        val trackHeight = (if (isThick) 12.dp else 6.dp).toPx()
        val top = (size.height - trackHeight) / 2f
        val fraction = 0.4f
        inset(left = 0f, top = top, right = 0f, bottom = top) {
            drawSeekTrack(fraction, listOf(0.18f, 0.72f), isThick, activeColor, inactiveColor)
        }
        // Standard: pill handle taller than the track. Thick: block handle as tall as
        // the track, with small rounded corners.
        val x = size.width * fraction
        val handleWidth = 4.dp.toPx()
        val handleHeight = if (isThick) trackHeight else size.height * 0.75f
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(x - handleWidth / 2f, (size.height - handleHeight) / 2f),
            size = Size(handleWidth, handleHeight),
            cornerRadius = CornerRadius(if (isThick) 2.dp.toPx() else handleWidth / 2f),
        )
    }
}

/**
 * Bubble shown above the thumb while dragging: target time, chapter name (if any) and how
 * far the drag moved, e.g. "12:34 · Part B" and "+0:45". Stays inside the seekbar's width.
 */
@Composable
internal fun SeekBubble(
    position: Float,
    duration: Float,
    startPosition: Float,
    chapters: List<Segment>,
    thumbSlotWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val fraction = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    val chapterName = chapters.lastOrNull { it.start <= position }?.name?.takeIf { it.isNotBlank() && chapters.size > 1 }
    val delta = position - startPosition
    val deltaText = (if (delta >= 0) "+" else "-") + Utils.prettyTime(kotlin.math.abs(delta).toInt())
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val slot = thumbSlotWidth.roundToPx()
            val trackWidth = (constraints.maxWidth - slot).coerceAtLeast(0)
            val centerX = slot / 2 + (trackWidth * fraction).toInt()
            val x = (centerX - placeable.width / 2).coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
            layout(constraints.maxWidth, placeable.height) { placeable.place(x, 0) }
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = Utils.prettyTime(position.toInt()) + (chapterName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = deltaText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.inversePrimary,
                maxLines = 1,
            )
        }
    }
}
