package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.utils.media.VideoMark
import app.marlboroadvance.mpvex.utils.media.VideoMarkOps
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * "Mark as" sheet for the selected videos: New, Last played, Finished or Unwatched.
 * Same header as the Delete sheet; options use the M3 Expressive segmented list style
 * (separate rounded tiles with a small gap, larger corners at the ends).
 *
 * [allFinished] greys out "Finished" when every selected video is already finished.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkAsSheet(
  selectedCount: Int,
  allFinished: Boolean,
  onDismiss: () -> Unit,
  onMark: (VideoMark) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 32.dp),
    ) {
      Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
        Text(
          text = stringResource(R.string.mark_as_title),
          style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
          text = pluralStringResource(R.plurals.mark_as_selected_count, selectedCount, selectedCount),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      val options = listOf(
        MarkOption(VideoMark.New, markIcon(VideoMark.New), R.string.mark_as_new, R.string.mark_as_new_summary),
        MarkOption(VideoMark.LastPlayed, markIcon(VideoMark.LastPlayed), R.string.mark_as_last_played, R.string.mark_as_last_played_summary),
        MarkOption(
          VideoMark.Finished,
          markIcon(VideoMark.Finished),
          R.string.mark_as_finished,
          if (allFinished) R.string.mark_as_finished_already else R.string.mark_as_finished_summary,
          enabled = !allFinished,
        ),
        MarkOption(VideoMark.Unwatched, markIcon(VideoMark.Unwatched), R.string.mark_as_unwatched, R.string.mark_as_unwatched_summary),
      )

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { index, option ->
          MarkOptionRow(
            option = option,
            shape = segmentShape(index, options.size),
            onClick = { onMark(option.mark) },
          )
        }
      }
    }
  }
}

/**
 * Shows [MarkAsSheet] while [open] is true and runs the chosen mark on [selectedVideos].
 * Afterwards [onApplied] runs (to leave selection mode) and a floating pill offers Undo.
 *
 * The pill is drawn by this host itself (in a Popup), so no Scaffold/SnackbarHost changes
 * are needed. [snackbarHostState] is kept only so existing call sites still compile; it is
 * just used to clear any stock snackbar that happens to be showing.
 *
 * Note: this composable has to stay in the composition after [onApplied] runs, otherwise
 * the pill disappears with it. Don't wrap the call in `if (selectionMode) { ... }`.
 */
@Composable
fun MarkAsHost(
  open: MutableState<Boolean>,
  selectedVideos: () -> List<Video>,
  allFinished: Boolean,
  snackbarHostState: SnackbarHostState,
  onApplied: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var toast by remember { mutableStateOf<MarkToast?>(null) }

  toast?.let { current ->
    // key() gives every toast fresh animation state, so a new mark replaces the old pill.
    key(current.id) {
      MarkToastPopup(
        toast = current,
        onDismissed = { if (toast?.id == current.id) toast = null },
      )
    }
  }

  if (!open.value) return
  val videos = selectedVideos()
  MarkAsSheet(
    selectedCount = videos.size,
    allFinished = allFinished,
    onDismiss = { open.value = false },
    onMark = { mark ->
      open.value = false
      scope.launch {
        val snapshot = VideoMarkOps.apply(videos, mark)
        onApplied()
        // Only the resource id is kept here; the pill resolves the text in composition.
        val plural = if (snapshot == null) {
          null
        } else {
          when (mark) {
            VideoMark.New -> R.plurals.mark_as_done_new
            VideoMark.LastPlayed -> R.plurals.mark_as_done_last_played
            VideoMark.Finished -> R.plurals.mark_as_done_finished
            VideoMark.Unwatched -> R.plurals.mark_as_done_unwatched
          }
        }
        val undo: (() -> Unit)? = snapshot?.let { taken ->
          {
            scope.launch { VideoMarkOps.undo(taken) }
            Unit
          }
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        toast = MarkToast(
          id = System.nanoTime(),
          plural = plural,
          count = videos.size,
          icon = markIcon(mark),
          onUndo = undo,
        )
      }
    },
  )
}

private fun markIcon(mark: VideoMark): Int = when (mark) {
  VideoMark.New -> R.drawable.ic_sym_new_releases
  VideoMark.LastPlayed -> R.drawable.ic_sym_history
  VideoMark.Finished -> R.drawable.ic_sym_check_circle_fill
  VideoMark.Unwatched -> R.drawable.ic_sym_remove_done
}

// ---------------------------------------------------------------------------------------
// Floating pill ("toast") shown after a mark is applied
// ---------------------------------------------------------------------------------------

/** How long the pill stays up (and how long the countdown bar runs). */
private const val TOAST_DURATION_MS = 4000

/** Gap between the pill and the navigation bar. Raise it if you have a bottom bar. */
private val TOAST_BOTTOM_GAP: Dp = 16.dp

private class MarkToast(
  val id: Long,
  /** Plural resource for the "done" message, or null when the mark failed. */
  val plural: Int?,
  val count: Int,
  val icon: Int,
  val onUndo: (() -> Unit)?,
)

/** Pins the popup to the bottom centre of the window, wherever the host is composed. */
private object BottomCenterOfWindow : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset = IntOffset(
    x = (windowSize.width - popupContentSize.width) / 2,
    y = windowSize.height - popupContentSize.height,
  )
}

@Composable
private fun MarkToastPopup(
  toast: MarkToast,
  onDismissed: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val appear = remember { Animatable(0f) } // 0 = hidden, 1 = resting; the spring overshoots
  val countdown = remember { Animatable(1f) } // 1 = full bar, 0 = time is up
  // Swipe offset in px. Plain state (not an Animatable) so drag updates are applied
  // synchronously and can never interrupt the fling-out animation.
  var dragX by remember { mutableFloatStateOf(0f) }
  var touching by remember { mutableStateOf(false) } // finger down: countdown is paused
  var flinging by remember { mutableStateOf(false) } // flying off: ignore further drags
  var settleJob by remember { mutableStateOf<Job?>(null) } // spring-back after a short drag
  var pillWidth by remember { mutableIntStateOf(0) }
  val message = if (toast.plural != null) {
    pluralStringResource(toast.plural, toast.count, toast.count)
  } else {
    stringResource(R.string.mark_as_failed)
  }

  // Read outside the Popup: insets are not reliable inside a popup window.
  val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  LaunchedEffect(Unit) {
    appear.animateTo(
      targetValue = 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
    )
  }

  // Restarts whenever the finger goes down or up, so the countdown pauses while touching
  // and resumes with the time that was left.
  LaunchedEffect(touching) {
    if (touching) return@LaunchedEffect
    val remainingMs = (TOAST_DURATION_MS * countdown.value).toInt()
    countdown.animateTo(0f, tween(remainingMs, easing = LinearEasing))
    appear.animateTo(0f, tween(180))
    onDismissed()
  }

  fun dismiss() {
    scope.launch {
      appear.animateTo(0f, tween(160))
      onDismissed()
    }
  }

  Popup(
    popupPositionProvider = BottomCenterOfWindow,
    properties = PopupProperties(focusable = false),
  ) {
    Box(
      modifier = Modifier
        // The padding also leaves room for the shadow and the spring overshoot.
        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = navBarInset + TOAST_BOTTOM_GAP)
        // Gesture modifiers sit before graphicsLayer on purpose: inside the moving layer
        // the finger position would be measured relative to the pill it is moving.
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            touching = true
            try {
              do {
                val event = awaitPointerEvent()
              } while (event.changes.any { it.pressed })
            } finally {
              touching = false
            }
          }
        }
        .draggable(
          orientation = Orientation.Horizontal,
          state = rememberDraggableState { delta -> if (!flinging) dragX += delta },
          onDragStarted = { settleJob?.cancel() },
          onDragStopped = { velocity ->
            if (!flinging) {
              val far = abs(dragX) > pillWidth * 0.2f
              val fast = abs(velocity) > 600f
              if (pillWidth > 0 && (far || fast)) {
                flinging = true
                val direction = if (fast) sign(velocity) else sign(dragX)
                // Launched in the popup's own scope so nothing in the gesture can cancel it.
                scope.launch {
                  animate(dragX, direction * pillWidth * 1.2f, animationSpec = tween(160)) { value, _ ->
                    dragX = value
                  }
                  onDismissed()
                }
              } else {
                settleJob = scope.launch {
                  animate(
                    initialValue = dragX,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                  ) { value, _ -> dragX = value }
                }
              }
            }
          },
        )
        .graphicsLayer {
          val a = appear.value
          val swiped = if (pillWidth > 0) (abs(dragX) / pillWidth).coerceIn(0f, 1f) else 0f
          translationX = dragX
          translationY = (1f - a) * 56.dp.toPx()
          scaleX = 0.85f + 0.15f * a
          scaleY = 0.85f + 0.15f * a
          alpha = a.coerceIn(0f, 1f) * (1f - swiped)
        },
    ) {
      val barColor = MaterialTheme.colorScheme.primary
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 6.dp,
        modifier = Modifier
          .widthIn(max = 480.dp)
          .onSizeChanged { pillWidth = it.width }
          .semantics { liveRegion = LiveRegionMode.Polite },
      ) {
        Row(
          modifier = Modifier
            .defaultMinSize(minHeight = 56.dp)
            .drawWithContent {
              drawContent()
              // Countdown bar along the bottom edge; the pill shape clips its ends.
              val barHeight = 3.dp.toPx()
              drawRect(
                color = barColor,
                topLeft = Offset(0f, size.height - barHeight),
                size = Size(size.width * countdown.value, barHeight),
              )
            }
            .padding(
              start = 8.dp,
              end = if (toast.onUndo != null) 10.dp else 22.dp,
              top = 8.dp,
              bottom = 8.dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = ImageVector.vectorResource(toast.icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (toast.onUndo != null) {
            Button(
              onClick = {
                toast.onUndo.invoke()
                dismiss()
              },
              contentPadding = PaddingValues(horizontal = 16.dp),
              modifier = Modifier.height(36.dp),
            ) {
              Text(
                text = stringResource(R.string.mark_as_undo),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------------------
// Sheet rows
// ---------------------------------------------------------------------------------------

private class MarkOption(
  val mark: VideoMark,
  val icon: Int,
  val title: Int,
  val summary: Int,
  val enabled: Boolean = true,
)

/** Larger outer corners on the first and last tile, small corners between tiles. */
private fun segmentShape(index: Int, count: Int): RoundedCornerShape {
  val outer = 20.dp
  val inner = 4.dp
  return RoundedCornerShape(
    topStart = if (index == 0) outer else inner,
    topEnd = if (index == 0) outer else inner,
    bottomStart = if (index == count - 1) outer else inner,
    bottomEnd = if (index == count - 1) outer else inner,
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MarkOptionRow(
  option: MarkOption,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = option.enabled,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .alpha(if (option.enabled) 1f else 0.38f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = ImageVector.vectorResource(option.icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(option.title),
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(option.summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
