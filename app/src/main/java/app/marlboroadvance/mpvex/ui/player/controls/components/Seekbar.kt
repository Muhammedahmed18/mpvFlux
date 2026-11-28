package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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

            // Time bubble above the thumb while dragging. It pops out of the handle: the
            // bubble itself (not the full-width row) springs up from its bottom centre.
            androidx.compose.animation.AnimatedVisibility(
                visible = isUserInteracting && duration > 0f,
                enter = androidx.compose.animation.fadeIn(tween(120)),
                exit = androidx.compose.animation.fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-36).dp),
            ) {
                @OptIn(ExperimentalAnimationApi::class)
                SeekBubble(
                    position = userPosition,
                    duration = duration,
                    startPosition = dragStartPosition,
                    chapters = chapters,
                    thumbSlotWidth = seekbarMetrics(seekbarStyle).thumbWidth,
                    popModifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            animationSpec = spring(dampingRatio = SeekbarSprings.FastSpatialDamping, stiffness = SeekbarSprings.FastSpatialStiffness),
                            initialScale = 0.5f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                        ),
                        exit = scaleOut(
                            animationSpec = tween(150),
                            targetScale = 0.8f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                        ),
                    ),
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
            // Clean M3 label type with tabular numerals so digits don't jitter as they tick.
            Text(
                text = Utils.prettyTime(value.toInt(), isInverted),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge.copy(
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
    // The unplayed part is a lower-emphasis take on the same accent, so it follows
    // dynamic colour in both light and dark themes.
    val inactiveColor = seekbarInactiveColor(activeColor)
    // A-B loop markers use the theme's tertiary so they follow dynamic colour.
    val loopColor = MaterialTheme.colorScheme.tertiary
    val interactionSource = remember { MutableInteractionSource() }
    val metrics = seekbarMetrics(seekbarStyle)
    // Track geometry never changes while dragging: nothing shrinks or grows, so the bar
    // never jumps under the finger. Only the thumb reacts (below), plus the time bubble.

    // Thumb micro-interaction, M3 Expressive fast-spatial spring:
    //  - Standard widens subtly (5 -> 7dp) while scrubbing.
    //  - Expressive stretches slightly taller (30 -> 34dp) while scrubbing.
    //  - Thick stays put; its presence is the track itself.
    val thumbWidth by animateDpAsState(
        targetValue = if (isScrubbing && seekbarStyle == SeekbarStyle.Standard) metrics.thumbDragWidth else metrics.thumbWidth,
        animationSpec = spring(dampingRatio = SeekbarSprings.FastSpatialDamping, stiffness = SeekbarSprings.FastSpatialStiffness),
        label = "seekThumbWidth",
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (isScrubbing && seekbarStyle == SeekbarStyle.Expressive) metrics.thumbDragHeight else metrics.thumbHeight,
        animationSpec = spring(dampingRatio = SeekbarSprings.FastSpatialDamping, stiffness = SeekbarSprings.FastSpatialStiffness),
        label = "seekThumbHeight",
    )

    // Material3 1.5.0: a Slider with custom thumb/track takes a SliderState. The range is part of
    // the key so the state is rebuilt when the duration becomes known; the position is pushed in
    // on every recomposition (drags update the state themselves and report through onSeek).
    val seekRange = 0f..duration.coerceAtLeast(0.1f)
    val sliderState = remember(seekRange) { SliderState(value = position, trackRange = seekRange) }
    SideEffect { if (sliderState.value != position) sliderState.value = position }

    Slider(state = sliderState, onValueChange = onSeek, onValueChangeFinished = onSeekFinished, modifier = modifier.fillMaxWidth().height(48.dp), interactionSource = interactionSource,
        track = { sliderState ->
            Canvas(modifier = Modifier.fillMaxWidth().height(metrics.activeTrackHeight)) {
                val range = (sliderState.trackRange.endInclusive - sliderState.trackRange.start).takeIf { it > 0f } ?: 1f
                val fraction = ((sliderState.value - sliderState.trackRange.start) / range).coerceIn(0f, 1f)
                drawSeekTrack(
                    fraction = fraction,
                    chapterFractions = visibleChapterFractions(chapters, duration, size.width, 12.dp.toPx()),
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    activeTrackHeightPx = metrics.activeTrackHeight.toPx(),
                    inactiveTrackHeightPx = metrics.inactiveTrackHeight.toPx(),
                    innerCornerPx = metrics.innerCorner.toPx(),
                    chapterGapHalfPx = metrics.chapterGapHalf.toPx(),
                    thumbGapHalfPx = thumbWidth.toPx() / 2f + metrics.thumbClearance.toPx(),
                    stopDotRadiusPx = metrics.stopDotRadius.toPx(),
                )
                // A-B loop markers and range highlight. Guarded against unknown/zero
                // duration so fractions never divide by zero or go out of range.
                if (duration > 0f && (loopStart != null || loopEnd != null)) {
                    if (loopStart != null) { val startPx = (loopStart / duration).coerceIn(0f, 1f) * size.width; drawLine(color = loopColor, start = Offset(startPx, 0f), end = Offset(startPx, size.height), strokeWidth = 2.dp.toPx()) }
                    if (loopEnd != null) { val endPx = (loopEnd / duration).coerceIn(0f, 1f) * size.width; drawLine(color = loopColor, start = Offset(endPx, 0f), end = Offset(endPx, size.height), strokeWidth = 2.dp.toPx()) }
                    if (loopStart != null && loopEnd != null) { val minPx = (minOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width; val maxPx = (maxOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width; drawRect(color = loopColor.copy(alpha = 0.3f), topLeft = Offset(minPx, 0f), size = Size(maxPx - minPx, size.height)) }
                }
            }
        },
        thumb = {
            // Vertically rounded pill in the accent colour, one geometry per style.
            Box(
                modifier = Modifier
                    .width(thumbWidth)
                    .height(thumbHeight)
                    .background(activeColor, CircleShape),
            )
        }
    )
}

/**
 * Per-style seekbar geometry.
 *
 * Standard   - slim, minimal M3: 6dp track, 5x24dp pill thumb (widens to 7dp on drag).
 * Thick      - bold media-player track: 14dp track, 7x24dp pill thumb.
 * Expressive - M3 Expressive-inspired asymmetric timeline: the played side runs 18dp
 *              tall against an 8dp unplayed side, both centred on the same axis, with a
 *              7x30dp pill thumb. (A custom style inspired by M3 Expressive, not an
 *              official slider preset.)
 */
internal data class SeekbarMetrics(
    val activeTrackHeight: Dp,
    val inactiveTrackHeight: Dp,
    val thumbWidth: Dp,
    val thumbDragWidth: Dp,
    val thumbHeight: Dp,
    val thumbDragHeight: Dp,
    val thumbClearance: Dp,
    val chapterGapHalf: Dp,
    val innerCorner: Dp,
    val stopDotRadius: Dp,
)

internal fun seekbarMetrics(style: SeekbarStyle): SeekbarMetrics = when (style) {
    SeekbarStyle.Standard -> SeekbarMetrics(
        activeTrackHeight = 6.dp, inactiveTrackHeight = 6.dp,
        thumbWidth = 5.dp, thumbDragWidth = 7.dp, thumbHeight = 24.dp, thumbDragHeight = 24.dp,
        thumbClearance = 4.dp, chapterGapHalf = 1.dp, innerCorner = 2.dp, stopDotRadius = 2.dp,
    )
    SeekbarStyle.Thick -> SeekbarMetrics(
        activeTrackHeight = 14.dp, inactiveTrackHeight = 14.dp,
        thumbWidth = 7.dp, thumbDragWidth = 7.dp, thumbHeight = 24.dp, thumbDragHeight = 24.dp,
        thumbClearance = 4.dp, chapterGapHalf = 1.5.dp, innerCorner = 3.dp, stopDotRadius = 3.dp,
    )
    SeekbarStyle.Expressive -> SeekbarMetrics(
        activeTrackHeight = 18.dp, inactiveTrackHeight = 8.dp,
        thumbWidth = 7.dp, thumbDragWidth = 7.dp, thumbHeight = 30.dp, thumbDragHeight = 34.dp,
        thumbClearance = 5.dp, chapterGapHalf = 1.5.dp, innerCorner = 3.dp, stopDotRadius = 2.5.dp,
    )
}

/**
 * M3 Expressive spring values (same as MotionScheme.expressive()), kept here so the
 * seekbar doesn't need the experimental expressive API.
 */
internal object SeekbarSprings {
    /** Small, quick, bouncy moves: the handle and the bubble. */
    const val FastSpatialDamping = 0.6f
    const val FastSpatialStiffness = 800f

    /** Larger, calmer moves. */
    const val DefaultSpatialDamping = 0.8f
    const val DefaultSpatialStiffness = 380f
}

/** Unplayed-track colour: the accent at 30%, a lower-emphasis theme-derived tone. */
internal fun seekbarInactiveColor(activeColor: Color) = activeColor.copy(alpha = 0.3f)

/** Chapter starts as fractions of [duration], skipping a chapter at the very start. */
internal fun chapterFractions(chapters: List<Segment>, duration: Float): List<Float> {
    if (duration <= 0f || chapters.size < 2) return emptyList()
    return chapters.map { it.start / duration }.filter { it > 0.005f && it < 0.995f }.sorted()
}

/**
 * Visual-only chapter filtering shared by Standard, Thick and Expressive.
 * Does not change the chapters used for seeking, scrubbing, haptics or chapter menus.
 * Generic numbered scene markers are omitted; other boundaries are kept only when
 * there is enough horizontal room for them to remain individually legible.
 */
private val genericSceneChapter = Regex("^scene(?:[\\s_.:#-]*)(?:\\d+|[ivxlcdm]+)$", RegexOption.IGNORE_CASE)

internal fun visibleChapterFractions(
    chapters: List<Segment>,
    duration: Float,
    widthPx: Float,
    minimumSpacingPx: Float,
): List<Float> {
    if (!duration.isFinite() || duration <= 0f || chapters.size < 2 ||
        !widthPx.isFinite() || widthPx <= 0f
    ) return emptyList()

    val candidates = chapters.asSequence()
        .filterNot { genericSceneChapter.matches(it.name.trim()) }
        .map { it.start / duration }
        .filter { it.isFinite() && it > 0.005f && it < 0.995f }
        .distinct()
        .sorted()
        .toList()

    val minGap = minimumSpacingPx.coerceAtLeast(0f)
    val selected = ArrayList<Float>(candidates.size)
    for (fraction in candidates) {
        if (selected.isEmpty() || (fraction - selected.last()) * widthPx >= minGap) {
            selected.add(fraction)
        }
    }
    return selected
}

/**
 * Draws the seek track: played pieces at [activeTrackHeightPx] in [activeColor], unplayed
 * pieces at [inactiveTrackHeightPx] in [inactiveColor], both centred on the same axis, a
 * gap around the thumb, small gaps where chapters start, and a stop dot. Outer ends are
 * fully rounded; edges next to a gap get smaller inside corners so the pieces read as one
 * track with cuts in it. Equal heights give the Standard/Thick look; unequal heights give
 * the Expressive asymmetric timeline.
 */
internal fun DrawScope.drawSeekTrack(
    fraction: Float,
    chapterFractions: List<Float>,
    activeColor: Color,
    inactiveColor: Color,
    activeTrackHeightPx: Float,
    inactiveTrackHeightPx: Float,
    innerCornerPx: Float,
    chapterGapHalfPx: Float,
    thumbGapHalfPx: Float,
    stopDotRadiusPx: Float,
) {
    val width = size.width
    val canvasHeight = size.height
    val playedPx = width * fraction.coerceIn(0f, 1f)

    // Cuts: the thumb gap and one small gap per chapter start.
    val cuts = buildList {
        if (thumbGapHalfPx > 0f) add((playedPx - thumbGapHalfPx) to (playedPx + thumbGapHalfPx))
        chapterFractions.forEach { f -> add((width * f - chapterGapHalfPx) to (width * f + chapterGapHalfPx)) }
    }.sortedBy { it.first }

    // Pieces of the track between cuts, split at the played position for colouring.
    val pieces = mutableListOf<Pair<Float, Float>>()
    var cursor = 0f
    for ((cutStart, cutEnd) in cuts) {
        if (cutStart > cursor) pieces += cursor to cutStart.coerceAtMost(width)
        cursor = maxOf(cursor, cutEnd)
    }
    if (cursor < width) pieces += cursor to width

    fun drawPiece(startX: Float, endX: Float, color: Color, heightPx: Float, roundLeft: Boolean, roundRight: Boolean) {
        if (endX - startX < 0.5f) return
        val outerRadius = heightPx / 2f
        val inner = innerCornerPx.coerceAtMost(outerRadius)
        fun corner(x: Float): CornerRadius =
            if (x <= 0.5f || x >= width - 0.5f) CornerRadius(outerRadius) else CornerRadius(inner)
        val left = if (roundLeft) corner(startX) else CornerRadius.Zero
        val right = if (roundRight) corner(endX) else CornerRadius.Zero
        val top = (canvasHeight - heightPx) / 2f
        val path = Path()
        path.addRoundRect(
            RoundRect(
                left = startX, top = top, right = endX, bottom = top + heightPx,
                topLeftCornerRadius = left, bottomLeftCornerRadius = left,
                topRightCornerRadius = right, bottomRightCornerRadius = right,
            ),
        )
        drawPath(path, color)
    }

    for ((start, end) in pieces) {
        when {
            end <= playedPx -> drawPiece(start, end, activeColor, activeTrackHeightPx, roundLeft = true, roundRight = true)
            start >= playedPx -> drawPiece(start, end, inactiveColor, inactiveTrackHeightPx, roundLeft = true, roundRight = true)
            else -> {
                // Defensive: with a thumb gap a piece shouldn't span the play position,
                // but split it cleanly (each side at its own height) if it ever does.
                drawPiece(start, playedPx, activeColor, activeTrackHeightPx, roundLeft = true, roundRight = false)
                drawPiece(playedPx, end, inactiveColor, inactiveTrackHeightPx, roundLeft = false, roundRight = true)
            }
        }
    }

    // Stop indicator: a small dot at the end of the track while there is track left.
    if (stopDotRadiusPx > 0f) {
        val dotX = width - inactiveTrackHeightPx / 2f
        val dotRadius = stopDotRadiusPx.coerceAtMost((inactiveTrackHeightPx / 2f - 0.5f).coerceAtLeast(0f))
        if (dotRadius > 0f && playedPx + thumbGapHalfPx < dotX - dotRadius) {
            drawCircle(color = activeColor, radius = dotRadius, center = Offset(dotX, canvasHeight / 2f))
        }
    }
}

/**
 * Small drawing of a seekbar style for the settings picker: sample chapter gaps,
 * 40% played and the style's thumb, at roughly 70% of the real geometry so each
 * preview mirrors its style (including Expressive's asymmetric track).
 */
@Composable
fun SeekbarStylePreview(style: SeekbarStyle, modifier: Modifier = Modifier, showChapters: Boolean = true) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = seekbarInactiveColor(activeColor)
    Canvas(modifier = modifier.width(120.dp).height(28.dp)) {
        val fraction = 0.4f
        // Mini metrics, ~0.7x of the real styles so all three fit a 28dp strip.
        val (activeH, inactiveH, thumbH) = when (style) {
            SeekbarStyle.Standard -> Triple(5.dp.toPx(), 5.dp.toPx(), 17.dp.toPx())
            SeekbarStyle.Thick -> Triple(10.dp.toPx(), 10.dp.toPx(), 17.dp.toPx())
            SeekbarStyle.Expressive -> Triple(13.dp.toPx(), 6.dp.toPx(), 21.dp.toPx())
        }
        val thumbW = 5.dp.toPx()
        drawSeekTrack(
            fraction = fraction,
            chapterFractions = if (showChapters) listOf(0.18f, 0.72f) else emptyList(),
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            activeTrackHeightPx = activeH,
            inactiveTrackHeightPx = inactiveH,
            innerCornerPx = 2.dp.toPx(),
            chapterGapHalfPx = 1.dp.toPx(),
            thumbGapHalfPx = thumbW / 2f + 3.dp.toPx(),
            stopDotRadiusPx = 2.dp.toPx(),
        )
        // The style's pill thumb, standing taller than the track.
        val x = size.width * fraction
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(x - thumbW / 2f, (size.height - thumbH) / 2f),
            size = Size(thumbW, thumbH),
            cornerRadius = CornerRadius(thumbW / 2f),
        )
    }
}

/**
 * Bubble shown above the thumb while dragging: target time, chapter name (if any) and how
 * far the drag moved, e.g. "12:34 · Part B" and "+0:45". A fully rounded inverse-surface
 * capsule with tabular numerals that follows the thumb and stays inside the seekbar's width.
 */
@Composable
internal fun SeekBubble(
    position: Float,
    duration: Float,
    startPosition: Float,
    chapters: List<Segment>,
    thumbSlotWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    /** Applied to the bubble itself after it's positioned, e.g. a pop-in scale. */
    popModifier: Modifier = Modifier,
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
        }.then(popModifier),
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
