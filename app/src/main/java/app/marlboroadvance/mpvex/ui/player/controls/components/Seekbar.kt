package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import app.marlboroadvance.mpvex.ui.player.controls.LocalPlayerButtonsClickEvent
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
    positionTimerOnClick: () -> Unit,
    durationTimerOnCLick: () -> Unit,
    chapters: ImmutableList<Segment>,
    paused: Boolean,
    seekbarStyle: SeekbarStyle = SeekbarStyle.Thick,
    loopStart: Float? = null,
    loopEnd: Float? = null,
    modifier: Modifier = Modifier,
) {
    val clickEvent = LocalPlayerButtonsClickEvent.current
    var isUserInteracting by remember { mutableStateOf(false) }
    var userPosition by remember { mutableFloatStateOf(0f) }

    val position by positionFlow.collectAsState()
    val duration by durationFlow.collectAsState()

    val scope = rememberCoroutineScope()
    val animatedPosition = remember { Animatable(positionFlow.value) }

    LaunchedEffect(position, isUserInteracting) {
        if (!isUserInteracting && position != animatedPosition.value) {
            animatedPosition.animateTo(
                targetValue = position,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing),
            )
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
            onClick = {
                clickEvent()
                positionTimerOnClick()
            },
            modifier = Modifier.width(72.dp),
        )

        Box(
            modifier = Modifier.weight(1f).height(48.dp).padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp)
                    .pointerInput(duration) {
                        detectTapGestures(onTap = { offset ->
                            val newPosition = (offset.x / size.width) * duration
                            if (!isUserInteracting) isUserInteracting = true
                            userPosition = newPosition.coerceIn(0f, duration)
                            onValueChange(userPosition)
                            scope.launch {
                                animatedPosition.snapTo(userPosition)
                                isUserInteracting = false
                                onValueChangeFinished()
                            }
                        })
                    }
                    .pointerInput(duration) {
                        detectDragGestures(onDragStart = { isUserInteracting = true }, onDragEnd = {
                            scope.launch {
                                delay(50)
                                animatedPosition.snapTo(userPosition)
                                isUserInteracting = false
                                onValueChangeFinished()
                            }
                        }, onDragCancel = {
                            scope.launch {
                                delay(50)
                                animatedPosition.snapTo(userPosition)
                                isUserInteracting = false
                                onValueChangeFinished()
                            }
                        }) { change, _ ->
                            change.consume()
                            val newPosition = (change.position.x / size.width) * duration
                            userPosition = newPosition.coerceIn(0f, duration)
                            onValueChange(userPosition)
                        }
                    }
            )

            val currentPos = if (isUserInteracting) userPosition else animatedPosition.value
            
            when (seekbarStyle) {
                SeekbarStyle.Standard -> StandardSeekbar(position = currentPos, duration = duration, chapters = chapters, isPaused = paused, isScrubbing = isUserInteracting, seekbarStyle = SeekbarStyle.Standard, onSeek = { newPos -> if (!isUserInteracting) isUserInteracting = true; userPosition = newPos; onValueChange(newPos) }, onSeekFinished = { scope.launch { animatedPosition.snapTo(userPosition) }; isUserInteracting = false; onValueChangeFinished() }, loopStart = loopStart, loopEnd = loopEnd)
                SeekbarStyle.Thick -> StandardSeekbar(position = currentPos, duration = duration, chapters = chapters, isPaused = paused, isScrubbing = isUserInteracting, seekbarStyle = SeekbarStyle.Thick, onSeek = { newPos -> if (!isUserInteracting) isUserInteracting = true; userPosition = newPos; onValueChange(newPos) }, onSeekFinished = { scope.launch { animatedPosition.snapTo(userPosition) }; isUserInteracting = false; onValueChangeFinished() }, loopStart = loopStart, loopEnd = loopEnd)
            }
        }

        VideoTimer(
            value = if (timersInverted.second) position - duration else duration,
            isInverted = timersInverted.second,
            onClick = {
                clickEvent()
                durationTimerOnCLick()
            },
            modifier = Modifier.width(72.dp),
        )
    }
}

@Composable
fun VideoTimer(
    value: Float,
    isInverted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), shape)
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Utils.prettyTime(value.toInt(), isInverted),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StandardSeekbar(position: Float, duration: Float, chapters: ImmutableList<Segment>, isPaused: Boolean = false, isScrubbing: Boolean = false, seekbarStyle: SeekbarStyle = SeekbarStyle.Standard, onSeek: (Float) -> Unit, onSeekFinished: () -> Unit, loopStart: Float? = null, loopEnd: Float? = null, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    var heightFraction by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(isPaused, isScrubbing) {
        val shouldFlatten = isPaused || isScrubbing
        val targetHeight = if (shouldFlatten) 0.7f else 1f
        delay(if (shouldFlatten) 0L else 60L)
        val animator = Animatable(heightFraction)
        animator.animateTo(targetValue = targetHeight, animationSpec = tween(durationMillis = if (shouldFlatten) 550 else 800, easing = LinearEasing)) { heightFraction = value }
    }
    val isThick = seekbarStyle == SeekbarStyle.Thick
    val trackHeightDp = (if (isThick) 16.dp else 8.dp) * heightFraction
    Slider(value = position, onValueChange = onSeek, onValueChangeFinished = onSeekFinished, valueRange = 0f..duration.coerceAtLeast(0.1f), modifier = Modifier.fillMaxWidth(), interactionSource = interactionSource,
        track = { sliderState ->
            Canvas(modifier = Modifier.fillMaxWidth().height(trackHeightDp)) {
                val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).takeIf { it > 0f } ?: 1f
                val playedPx = size.width * ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                val trackHeight = size.height
                val outerRadius = trackHeight / 2f
                val innerRadius = if (isThick) outerRadius else 2.dp.toPx()
                val gapHalf = 7.dp.toPx()
                val thumbGapStart = (playedPx - gapHalf).coerceIn(0f, size.width)
                val thumbGapEnd = (playedPx + gapHalf).coerceIn(0f, size.width)
                fun drawSegment(startX: Float, endX: Float, color: Color) {
                    if (endX - startX < 0.5f) return
                    val path = Path()
                    val cornerRadiusLeft = when { startX <= 0.5f -> androidx.compose.ui.geometry.CornerRadius(outerRadius); kotlin.math.abs(startX - thumbGapEnd) < 0.5f -> androidx.compose.ui.geometry.CornerRadius(innerRadius); else -> androidx.compose.ui.geometry.CornerRadius.Zero }
                    val cornerRadiusRight = when { endX >= size.width - 0.5f -> androidx.compose.ui.geometry.CornerRadius(outerRadius); kotlin.math.abs(endX - thumbGapStart) < 0.5f -> androidx.compose.ui.geometry.CornerRadius(innerRadius); else -> androidx.compose.ui.geometry.CornerRadius.Zero }
                    path.addRoundRect(androidx.compose.ui.geometry.RoundRect(left = startX, top = 0f, right = endX, bottom = trackHeight, topLeftCornerRadius = cornerRadiusLeft, bottomLeftCornerRadius = cornerRadiusLeft, topRightCornerRadius = cornerRadiusRight, bottomRightCornerRadius = cornerRadiusRight))
                    drawPath(path, color)
                }
                drawSegment(thumbGapEnd, size.width, primaryColor.copy(alpha = 0.3f))
                if (thumbGapStart > 0) drawSegment(0f, thumbGapStart, primaryColor)
                if (loopStart != null || loopEnd != null) {
                    val loopColor = Color(0xFFFFB300)
                    if (loopStart != null) { val startPx = (loopStart / duration).coerceIn(0f, 1f) * size.width; drawLine(color = loopColor, start = Offset(startPx, 0f), end = Offset(startPx, size.height), strokeWidth = 2.dp.toPx()) }
                    if (loopEnd != null) { val endPx = (loopEnd / duration).coerceIn(0f, 1f) * size.width; drawLine(color = loopColor, start = Offset(endPx, 0f), end = Offset(endPx, size.height), strokeWidth = 2.dp.toPx()) }
                    if (loopStart != null && loopEnd != null) { val minPx = (minOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width; val maxPx = (maxOf(loopStart, loopEnd) / duration).coerceIn(0f, 1f) * size.width; drawRect(color = loopColor.copy(alpha = 0.3f), topLeft = Offset(minPx, 0f), size = Size(maxPx - minPx, size.height)) }
                }
            }
        },
        thumb = { Box(modifier = Modifier.width(6.dp).height(if (isThick) 16.dp else 24.dp).background(primaryColor, if (isThick) RoundedCornerShape(3.dp) else CircleShape)) }
    )
}

