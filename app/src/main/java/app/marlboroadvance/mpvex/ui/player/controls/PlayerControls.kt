package app.marlboroadvance.mpvex.ui.player.controls

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.preferences.preference.deleteAndGet
import app.marlboroadvance.mpvex.preferences.preference.minusAssign
import app.marlboroadvance.mpvex.preferences.preference.plusAssign
import app.marlboroadvance.mpvex.ui.player.Decoder.Companion.getDecoderFromValue
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerUpdates
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.controls.components.BrightnessSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.CompactSpeedIndicator
import app.marlboroadvance.mpvex.ui.player.controls.components.MultipleSpeedPlayerUpdate
import app.marlboroadvance.mpvex.ui.player.controls.components.SpeedControlSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.TextPlayerUpdate
import app.marlboroadvance.mpvex.ui.player.controls.components.ThumbZoneUnlock
import app.marlboroadvance.mpvex.ui.player.controls.components.VolumeSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.SeekbarWithTimers
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.toFixed
import app.marlboroadvance.mpvex.ui.theme.MpvexTheme
import app.marlboroadvance.mpvex.ui.theme.playerRippleConfiguration
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import org.koin.compose.koinInject
import java.util.Locale
import kotlin.math.abs

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

// Exit: 250ms FastOutLinearIn — elements leaving accelerate out. Snappier than the
// previous 350ms ease-in-out so the controls disappear without lingering.
fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> =
  tween(
    durationMillis = 250,
    easing = FastOutLinearInEasing,
  )

// Enter: OxygenOS-style fluid spring driving translation + fade as one physics curve.
// dampingRatio 0.8 gives a barely-perceptible overshoot; StiffnessMediumLow lands the
// animation in ~220ms perceived without the abrupt stop a tween produces.
fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> =
  spring(
    dampingRatio = 0.8f,
    stiffness    = Spring.StiffnessMediumLow,
  )

// Long-press detector that does NOT swallow normal taps. The underlying button's
// clickable still fires on quick taps; once the long-press timeout elapses we
// consume the up event so the click handler doesn't double-fire.
private fun Modifier.onLongPressNoConsume(onLongPress: () -> Unit): Modifier =
  this.pointerInput(Unit) {
    awaitEachGesture {
      awaitFirstDown(requireUnconsumed = false)
      try {
        withTimeout(viewConfiguration.longPressTimeoutMillis) {
          waitForUpOrCancellation()
        }
      } catch (_: PointerEventTimeoutCancellationException) {
        onLongPress()
        waitForUpOrCancellation()?.consume()
      }
    }
  }

// Heavy "smoked glass" fill for a circular transport button. MPV draws to a
// SurfaceView that can't be sampled for a real backdrop blur, so the glass is
// approximated: a deep near-opaque dark `scrim` disc (one contrast strategy —
// dark disc against any video frame, vivid accent glyph against the dark disc)
// + a white vertical rim-light border, which on dark glass is the main specular
// "glass" signal. No accent tint in the disc itself: the accent lives solely in
// the glyph. `strong` is the heavier Play/Pause hero; Prev/Next sit lighter.
// `rimBoost` (0..1) momentarily brightens the rim — light catching the glass
// edge — and is pulsed by the hero on each play/pause toggle.
private fun Modifier.transportGlass(
  scrim: Color,
  strong: Boolean,
  rimBoost: Float = 0f,
): Modifier =
  this
    .background(scrim.copy(alpha = if (strong) 0.58f else 0.48f))
    .border(
      width = 1.dp,
      brush = Brush.verticalGradient(
        listOf(
          Color.White.copy(alpha = (if (strong) 0.30f else 0.22f) + 0.25f * rimBoost),
          Color.White.copy(alpha = (if (strong) 0.05f else 0.04f) + 0.10f * rimBoost),
        ),
      ),
      shape = CircleShape,
    )

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalFoundationApi::class,
)
@Composable
@Suppress("CyclomaticComplexMethod", "ViewModelForwarding", "LongMethod")
fun PlayerControls(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = MaterialTheme.spacing
  val appearancePreferences = koinInject<AppearancePreferences>()
  val hideBackground by appearancePreferences.hidePlayerButtonsBackground.collectAsState()
  val playerPreferences = koinInject<PlayerPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val showSystemStatusBar by playerPreferences.showSystemStatusBar.collectAsState()
  val showSystemNavigationBar by playerPreferences.showSystemNavigationBar.collectAsState()
  val interactionSource = remember { MutableInteractionSource() }
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()

  // OPTIMIZATION: position/duration/decoder are NOT collected at this scope.
  // Position and duration flow directly to SeekbarWithTimers.
  // pausedForCache and decoder are read inside their consuming AnimatedVisibility
  // so they only recompose the content that actually needs them.

  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControlsTimestamp by remember { mutableLongStateOf(0L) }
  val seekText by viewModel.seekText.collectAsState()
  val currentChapter by MPVLib.propInt["chapter"].collectAsState()
  val isSpeedNonOne by remember(playbackSpeed) {
    derivedStateOf { abs((playbackSpeed ?: 1f) - 1f) > 0.001f }
  }
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val chapters by viewModel.chapters.collectAsState(persistentListOf())
  // chapters flows as a plain List, so toImmutableList() copies. Hoist one copy and
  // share it across the seekbar block and PlayerSheets instead of re-copying on every
  // controls-root recomposition (which fires on each scrub delta via resetControlsTimestamp).
  val chaptersImm = remember(chapters) { chapters.toImmutableList() }
  val playlistMode by playerPreferences.playlistMode.collectAsState()

  val abLoopA by viewModel.abLoopA.collectAsState()
  val abLoopB by viewModel.abLoopB.collectAsState()

  val activity = LocalActivity.current as PlayerActivity

  val onOpenSheet: (Sheets) -> Unit = {
    viewModel.setSheetShown(it)
    if (it == Sheets.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.panelShown.update { Panels.None }
    }
  }

  val onOpenPanel: (Panels) -> Unit = {
    viewModel.panelShown.update { _ -> it }
    if (it == Panels.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.sheetShown.update { Sheets.None }
    }
  }

  val topRightControlsPref by appearancePreferences.topRightControls.collectAsState()
  val bottomRightControlsPref by appearancePreferences.bottomRightControls.collectAsState()
  val bottomLeftControlsPref by appearancePreferences.bottomLeftControls.collectAsState()
  val portraitBottomControlsPref by appearancePreferences.portraitBottomControls.collectAsState()

  val (topRightButtons, bottomRightButtons, bottomLeftButtons) =
    remember(
      topRightControlsPref,
      bottomRightControlsPref,
      bottomLeftControlsPref,
    ) {
      val usedButtons = mutableSetOf<app.marlboroadvance.mpvex.preferences.PlayerButton>()
      val topR = appearancePreferences.parseButtons(topRightControlsPref, usedButtons)
      val bottomR = appearancePreferences.parseButtons(bottomRightControlsPref, usedButtons)
      val bottomL = appearancePreferences.parseButtons(bottomLeftControlsPref, usedButtons)
      listOf(topR, bottomR, bottomL)
    }

  val portraitBottomButtons = remember(portraitBottomControlsPref) {
    appearancePreferences.parseButtons(portraitBottomControlsPref, mutableSetOf())
  }

  // Show chapter markers in the seekbar only when the user has opted in by adding
  // the BOOKMARKS_CHAPTERS button to any of their layout slots.
  val showChapterMarkers = remember(topRightButtons, bottomRightButtons, bottomLeftButtons, portraitBottomButtons) {
    app.marlboroadvance.mpvex.preferences.PlayerButton.BOOKMARKS_CHAPTERS in
      (topRightButtons + bottomRightButtons + bottomLeftButtons + portraitBottomButtons)
  }

  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControlsTimestamp,
    areControlsLocked,
  ) {
    // Read the timestamp to ensure it's considered "used" and correctly triggers re-execution
    if (resetControlsTimestamp >= 0 && controlsShown && paused == false && !isSeeking) {
      val delayTime = if (areControlsLocked) 2000L else playerTimeToDisappear.toLong()
      delay(delayTime)
      viewModel.hideControls()
    }
  }

  val scrimAlpha by animateFloatAsState(
    targetValue = if (controlsShown && !areControlsLocked) 1f else 0f,
    animationSpec = if (controlsShown) playerControlsEnterAnimationSpec() else playerControlsExitAnimationSpec(),
    label = "scrim_alpha",
  )

  GestureHandler(
    viewModel = viewModel,
  )

  DoubleTapToSeekOvals(
    amount = doubleTapSeekAmount,
    text = seekText,
    showOvals = showDoubleTapOvals,
    showSeekIcon = showSeekTime,
    showSeekTime = showSeekTime,
    interactionSource = interactionSource,
  )

  // decoder is defined here (not inside ConstraintLayout) so it is accessible both
  // to the ConstraintLayout control blocks and to PlayerSheets, which is called
  // outside the ConstraintLayout scope. Decoder changes only on explicit user action.
  val mpvDecoder by MPVLib.propString["hwdec-current"].collectAsState()
  val decoder by remember { derivedStateOf { getDecoderFromValue(mpvDecoder ?: "auto") } }

  CompositionLocalProvider(
    LocalRippleConfiguration provides playerRippleConfiguration,
    LocalPlayerButtonsClickEvent provides { resetControlsTimestamp = System.currentTimeMillis() },
    LocalContentColor provides Color.White,
  ) {
    CompositionLocalProvider(
      LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
      val configuration = LocalConfiguration.current
      val isPortrait by remember(configuration) {
        derivedStateOf { configuration.orientation == ORIENTATION_PORTRAIT }
      }

      Box(modifier = modifier.fillMaxSize()) {
        // OPTIMIZATION: Scrim is isolated in a dedicated layer to avoid layout-wide redraws.
        // M3 scrim token replaces hardcoded Color.Black — adapts to any dynamic color scheme.
        // Two-pass scrim: a vertical top/bottom darkening for control legibility, plus a
        // soft radial "focus light" centered behind the play button (One UI focal emphasis).
        val scrimColor = MaterialTheme.colorScheme.scrim
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrimAlpha },
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  0.0f to scrimColor.copy(alpha = 0.55f),
                  0.15f to Color.Transparent,
                  0.85f to Color.Transparent,
                  1.0f to scrimColor.copy(alpha = 0.55f),
                )
              )
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors = listOf(
                    scrimColor.copy(alpha = 0.20f),
                    Color.Transparent,
                  ),
                )
              )
          )
        }

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
          val (topLeftControls, topRightControls) = createRefs()
          val (volumeSlider, brightnessSlider) = createRefs()
          val unlockControlsButton = createRef()
          val (bottomRightControls, bottomLeftControls) = createRefs()
          val playerPauseButton = createRef()
          val seekbar = createRef()
          val (playerUpdates) = createRefs()

          val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
          val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
          val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
          val reduceMotion by playerPreferences.reduceMotion.collectAsState()

          val videoZoom by viewModel.videoZoom.collectAsState()

          val rawMediaTitle by MPVLib.propString["media-title"].collectAsState()
          val mediaTitle by remember(rawMediaTitle, activity) {
            derivedStateOf {
              rawMediaTitle?.takeIf { it.isNotBlank() }
                ?: activity.getTitleForControls()
            }
          }

          val areSlidersShown = isBrightnessSliderShown || isVolumeSliderShown

          BrightnessSliderSection(
            viewModel = viewModel,
            isVisible = isBrightnessSliderShown,
            reduceMotion = reduceMotion,
            swapVolumeAndBrightness = swapVolumeAndBrightness,
            modifier = Modifier.constrainAs(brightnessSlider) {
              if (swapVolumeAndBrightness) {
                start.linkTo(parent.start, if (isPortrait) spacing.large else spacing.extraLarge)
              } else {
                end.linkTo(parent.end, if (isPortrait) spacing.large else spacing.extraLarge)
              }
              top.linkTo(parent.top, spacing.larger)
              bottom.linkTo(parent.bottom, spacing.extraLarge)
            },
          )

          VolumeSliderSection(
            viewModel = viewModel,
            audioPreferences = audioPreferences,
            playerPreferences = playerPreferences,
            isVisible = isVolumeSliderShown,
            reduceMotion = reduceMotion,
            swapVolumeAndBrightness = swapVolumeAndBrightness,
            modifier = Modifier.constrainAs(volumeSlider) {
              if (swapVolumeAndBrightness) {
                end.linkTo(parent.end, if (isPortrait) spacing.large else spacing.extraLarge)
              } else {
                start.linkTo(parent.start, if (isPortrait) spacing.large else spacing.extraLarge)
              }
              top.linkTo(parent.top, spacing.larger)
              bottom.linkTo(parent.bottom, spacing.extraLarge)
            },
          )

          PlayerUpdatesSection(
            viewModel = viewModel,
            playerPreferences = playerPreferences,
            videoZoom = videoZoom,
            playlistMode = playlistMode,
            modifier = Modifier.constrainAs(playerUpdates) {
              linkTo(parent.start, parent.end)
              linkTo(parent.top, parent.bottom, bias = 0.25f)
            },
          )

          AnimatedVisibility(
            visible = controlsShown && areControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.then(if (showSystemStatusBar) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier).constrainAs(unlockControlsButton) {
              top.linkTo(parent.top, if (isPortrait) spacing.extraLarge else spacing.small)
              end.linkTo(parent.end, spacing.large)
            },
          ) { ThumbZoneUnlock(onUnlock = { viewModel.unlockControls() }) }

          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = fadeIn(playerControlsEnterAnimationSpec()),
            exit = fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.constrainAs(playerPauseButton) {
              end.linkTo(parent.absoluteRight)
              start.linkTo(parent.absoluteLeft)
              if (isPortrait) bottom.linkTo(bottomRightControls.top, spacing.large)
              else { top.linkTo(parent.top); bottom.linkTo(parent.bottom) }
            },
          ) {
            // pausedForCache is read here (not at PlayerControls root) so buffering
            // events only recompose this small content block, not the entire tree.
            val pausedForCache by MPVLib.propBoolean["paused-for-cache"].collectAsState()
            val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()

            val isBuffering = pausedForCache == true && showLoadingCircle
            val showSkip = playlistMode && viewModel.hasPlaylistSupport()

            val prevInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }

            // One UI motion: a single soft, slightly slow spring drives the liquid
            // play/pause path morph AND every press scale, so the whole transport
            // cluster reacts as one calm, fluid material. reduceMotion snaps instead.
            val oneUiSpring = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

            val playPressed by playInteraction.collectIsPressedAsState()
            val prevPressed by prevInteraction.collectIsPressedAsState()
            val nextPressed by nextInteraction.collectIsPressedAsState()

            val isPlaying = paused == false
            val heroScale by animateFloatAsState(
              targetValue   = if (playPressed && !reduceMotion) 0.92f else 1f,
              animationSpec = oneUiSpring,
              label         = "hero_scale",
            )
            val prevScale by animateFloatAsState(
              targetValue   = if (prevPressed && !reduceMotion) 0.90f else 1f,
              animationSpec = oneUiSpring,
              label         = "prev_scale",
            )
            val nextScale by animateFloatAsState(
              targetValue   = if (nextPressed && !reduceMotion) 0.90f else 1f,
              animationSpec = oneUiSpring,
              label         = "next_scale",
            )

            // Toggle "beat": a one-shot 0→1→0 envelope fired on each play/pause
            // change, driving the hero's disc pulse (scale up to +6%) and rim-light
            // flare together so the whole button reacts as one material on the same
            // beat — fast spring up, the shared soft spring back down. lastPlaying
            // is captured on (re)composition so controls reappearing doesn't pulse;
            // reduceMotion skips the beat entirely.
            val toggleBeat = remember { Animatable(0f) }
            var lastPlaying by remember { mutableStateOf(isPlaying) }
            LaunchedEffect(isPlaying) {
              if (isPlaying == lastPlaying) return@LaunchedEffect
              lastPlaying = isPlaying
              if (!reduceMotion) {
                toggleBeat.snapTo(0f)
                toggleBeat.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
                toggleBeat.animateTo(0f, oneUiSpring)
              }
            }

            val haptic = LocalHapticFeedback.current

            val primaryColor   = MaterialTheme.colorScheme.primary
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val scrimColor     = MaterialTheme.colorScheme.scrim

            // M3 Expressive transport: the hero is a bold SOLID `primary` button whose
            // CONTAINER morphs shape on toggle — a full circle when paused, a rounded
            // square (squircle) when playing — driven by an animated corner radius on
            // the shared soft spring. The glyph rides in `onPrimary`. Prev/Next stay as
            // recessed dark smoked-glass circles with white glyphs, so the filled hero
            // is the unmistakable focal and the secondaries fall back. Hierarchy by
            // size + fill weight; unified by one shared spring. hideBackground collapses
            // every disc to a bare glyph over the video.
            Row(
              horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
              verticalAlignment     = Alignment.CenterVertically,
            ) {
              // ── Skip Previous ──────────────────────────────────────────
              if (showSkip) {
                val prevEnabled = viewModel.hasPrevious()
                Box(
                  modifier = Modifier
                    .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
                    .size(54.dp)
                    .clip(CircleShape)
                    .then(if (!hideBackground) Modifier.transportGlass(scrimColor, strong = false) else Modifier)
                    .alpha(if (isBuffering || !prevEnabled) 0.5f else 1f)
                    .clickable(
                      interactionSource = prevInteraction,
                      indication        = ripple(bounded = true),
                      enabled           = prevEnabled,
                      onClick           = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        viewModel.playPrevious()
                      },
                    )
                    .onLongPressNoConsume {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      val newPos = (viewModel.precisePosition.value - 10f)
                        .coerceAtLeast(0f)
                        .toInt()
                      resetControlsTimestamp = System.currentTimeMillis()
                      viewModel.seekTo(newPos)
                    },
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector        = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint               = if (hideBackground) onSurfaceColor else Color.White,
                    modifier           = Modifier.size(26.dp),
                  )
                }
              }

              // ── Play / Pause — M3 Expressive shape-morph hero ───────────
              // The container shape itself is the toggle animation: a full circle
              // (corner = half the 88dp box) when paused relaxes into a squircle
              // (~30% corner) when playing, on the shared soft spring. A press
              // softens the corner further (expressive squeeze) and `heroScale`
              // shrinks the whole button. reduceMotion holds the press squeeze flat
              // but the shape still settles via the spring.
              val baseCorner = if (isPlaying) 26.dp else 44.dp
              val targetCorner = if (playPressed && !reduceMotion) baseCorner - 8.dp else baseCorner
              val heroCorner by animateDpAsState(
                targetValue   = targetCorner,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                label         = "hero_corner",
              )
              val heroShape = RoundedCornerShape(heroCorner)

              if (isBuffering) {
                // Buffering reuses the hero's solid `primary` disc (same 88dp, same
                // fill) with an `onPrimary` LoadingIndicator at its center, so it reads
                // as the same button thinking — not a different component swapping in.
                Box(
                  modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .then(if (!hideBackground) Modifier.background(primaryColor) else Modifier),
                  contentAlignment = Alignment.Center,
                ) {
                  LoadingIndicator(
                    modifier = Modifier.size(52.dp),
                    color    = if (hideBackground) primaryColor else onPrimaryColor,
                  )
                }
              } else {
                // Bold solid-`primary` morphing button — the unmistakable focal. No
                // glass, no rim, no shadow: the saturated fill carries the depth and
                // the shape change carries the motion. `toggleBeat` adds a subtle +4%
                // scale pop on each toggle so the morph lands with a beat. hideBackground
                // drops the fill and keeps the bare `primary` glyph.
                Box(
                  modifier = Modifier
                    .graphicsLayer {
                      val beatScale = heroScale * (1f + 0.04f * toggleBeat.value)
                      scaleX = beatScale
                      scaleY = beatScale
                    }
                    .size(88.dp)
                    .clip(heroShape)
                    .then(if (!hideBackground) Modifier.background(primaryColor) else Modifier)
                    .clickable(
                      interactionSource = playInteraction,
                      indication        = ripple(bounded = true),
                      onClick           = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        resetControlsTimestamp = System.currentTimeMillis()
                        viewModel.pauseUnpause()
                      },
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  // Stock M3 glyphs (Rounded Play/Pause) crossfaded with a small scale
                  // via AnimatedContent. reduceMotion snaps with no transition. Glyph
                  // rides in `onPrimary` over the filled disc (or `primary` when bare).
                  AnimatedContent(
                    targetState   = isPlaying,
                    transitionSpec = {
                      if (reduceMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                      } else {
                        (fadeIn(oneUiSpring) + scaleIn(oneUiSpring, initialScale = 0.7f)) togetherWith
                          (fadeOut(oneUiSpring) + scaleOut(oneUiSpring, targetScale = 0.7f))
                      }
                    },
                    label = "play_pause_icon",
                  ) { playing ->
                    Icon(
                      imageVector        = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                      contentDescription = null,
                      tint               = if (hideBackground) primaryColor else onPrimaryColor,
                      modifier           = Modifier.size(44.dp),
                    )
                  }
                }
              }

              // ── Skip Next ───────────────────────────────────────────────
              if (showSkip) {
                val nextEnabled = viewModel.hasNext()
                Box(
                  modifier = Modifier
                    .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                    .size(54.dp)
                    .clip(CircleShape)
                    .then(if (!hideBackground) Modifier.transportGlass(scrimColor, strong = false) else Modifier)
                    .alpha(if (isBuffering || !nextEnabled) 0.5f else 1f)
                    .clickable(
                      interactionSource = nextInteraction,
                      indication        = ripple(bounded = true),
                      enabled           = nextEnabled,
                      onClick           = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        viewModel.playNext()
                      },
                    )
                    .onLongPressNoConsume {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      val maxPos = viewModel.effectiveDuration.value
                      val newPos = (viewModel.precisePosition.value + 10f)
                        .coerceAtMost(maxPos)
                        .toInt()
                      resetControlsTimestamp = System.currentTimeMillis()
                      viewModel.seekTo(newPos)
                    },
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector        = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint               = if (hideBackground) onSurfaceColor else Color.White,
                    modifier           = Modifier.size(26.dp),
                  )
                }
              }
            }
            }

          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = if (!reduceMotion) slideInVertically(playerControlsEnterAnimationSpec()) { it } + fadeIn(playerControlsEnterAnimationSpec()) else fadeIn(playerControlsEnterAnimationSpec()),
            exit = if (!reduceMotion) slideOutVertically(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec()) else fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.then(if (showSystemNavigationBar) { val navBarPadding = WindowInsets.navigationBars.asPaddingValues(); Modifier.padding(start = navBarPadding.calculateLeftPadding(LayoutDirection.Ltr), end = navBarPadding.calculateRightPadding(LayoutDirection.Ltr)) } else Modifier).constrainAs(seekbar) {
              if (isPortrait) bottom.linkTo(playerPauseButton.top, spacing.medium) else bottom.linkTo(parent.bottom, spacing.medium)
              start.linkTo(parent.start, 24.dp); end.linkTo(parent.end, 24.dp); width = Dimension.fillToConstraints
            },
          ) {
            val invertDuration by playerPreferences.invertDuration.collectAsState()
            val seekbarStyle by appearancePreferences.seekbarStyle.collectAsState()
            var wasPlayerAlreadyPaused by remember { mutableStateOf(false) }

            SeekbarWithTimers(
              positionFlow = viewModel.precisePosition,
              durationFlow = viewModel.effectiveDuration,
              onValueChange = {
                if (!isSeeking) {
                  wasPlayerAlreadyPaused = paused ?: false
                  if (!wasPlayerAlreadyPaused) viewModel.pause()
                }
                isSeeking = true
                resetControlsTimestamp = System.currentTimeMillis()
                viewModel.seekTo(it.toInt())
              },
              onValueChangeFinished = {
                isSeeking = false
                resetControlsTimestamp = System.currentTimeMillis()
                if (!wasPlayerAlreadyPaused) viewModel.unpause()
                viewModel.showControls()
              },
              timersInverted = Pair(false, invertDuration),
              durationTimerOnCLick = {
                resetControlsTimestamp = System.currentTimeMillis()
                playerPreferences.invertDuration.set(!invertDuration)
              },
              positionTimerOnClick = {},
              chapters = if (showChapterMarkers) chaptersImm else persistentListOf(),
              paused = paused ?: false,
              seekbarStyle = seekbarStyle,
              loopStart = abLoopA?.toFloat(),
              loopEnd = abLoopB?.toFloat(),
            )
          }

          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = if (!reduceMotion) slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } + fadeIn(playerControlsEnterAnimationSpec()) else fadeIn(playerControlsEnterAnimationSpec()),
            exit = if (!reduceMotion) slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } + fadeOut(playerControlsExitAnimationSpec()) else fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.then(if (showSystemStatusBar) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier).then(if (showSystemNavigationBar) { val navBarPadding = WindowInsets.navigationBars.asPaddingValues(); Modifier.padding(start = navBarPadding.calculateLeftPadding(LayoutDirection.Ltr), end = navBarPadding.calculateRightPadding(LayoutDirection.Ltr)) } else Modifier).constrainAs(topLeftControls) {
              top.linkTo(parent.top, if (isPortrait) spacing.extraLarge else spacing.small)
              start.linkTo(parent.start, spacing.large)
              if (isPortrait) { width = Dimension.fillToConstraints; end.linkTo(parent.end, spacing.large) } else { width = Dimension.fillToConstraints; end.linkTo(topRightControls.start, spacing.extraSmall) }
            },
          ) {
            if (isPortrait) TopPlayerControlsPortrait(mediaTitle = mediaTitle, hideBackground = hideBackground, onBackPress = onBackPress, onOpenSheet = onOpenSheet, viewModel = viewModel, activity = activity)
            else TopLeftPlayerControlsLandscape(mediaTitle = mediaTitle, hideBackground = hideBackground, onBackPress = onBackPress, onOpenSheet = onOpenSheet, viewModel = viewModel, activity = activity)
          }

          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked && !isPortrait,
            enter = if (!reduceMotion) slideInHorizontally(playerControlsEnterAnimationSpec()) { it } + fadeIn(playerControlsEnterAnimationSpec()) else fadeIn(playerControlsEnterAnimationSpec()),
            exit = if (!reduceMotion) slideOutHorizontally(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec()) else fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.then(if (showSystemStatusBar) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier).then(if (showSystemNavigationBar) { val navBarPadding = WindowInsets.navigationBars.asPaddingValues(); Modifier.padding(start = navBarPadding.calculateLeftPadding(LayoutDirection.Ltr), end = navBarPadding.calculateRightPadding(LayoutDirection.Ltr)) } else Modifier).constrainAs(topRightControls) { top.linkTo(parent.top, spacing.small); end.linkTo(parent.end, spacing.large) },
          ) { TopRightPlayerControlsLandscape(buttons = topRightButtons, chapters = chapters, currentChapter = currentChapter, isSpeedNonOne = isSpeedNonOne, currentZoom = videoZoom, mediaTitle = mediaTitle, hideBackground = hideBackground, decoder = decoder, playbackSpeed = playbackSpeed ?: 1f, onBackPress = onBackPress, onOpenSheet = onOpenSheet, viewModel = viewModel, activity = activity) }

          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked && (isPortrait || !areSlidersShown),
            enter = if (!reduceMotion) slideInHorizontally(playerControlsEnterAnimationSpec()) { it } + fadeIn(playerControlsEnterAnimationSpec()) else fadeIn(playerControlsEnterAnimationSpec()),
            exit = if (!reduceMotion) slideOutHorizontally(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec()) else fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.then(if (showSystemNavigationBar) { val navBarPadding = WindowInsets.navigationBars.asPaddingValues(); Modifier.padding(start = navBarPadding.calculateLeftPadding(LayoutDirection.Ltr), end = navBarPadding.calculateRightPadding(LayoutDirection.Ltr)) } else Modifier).constrainAs(bottomRightControls) { if (isPortrait) { bottom.linkTo(parent.bottom, spacing.extraLarge); start.linkTo(parent.start, spacing.large); end.linkTo(parent.end, spacing.large); width = Dimension.fillToConstraints } else { bottom.linkTo(seekbar.top, spacing.small); end.linkTo(parent.end, spacing.large) } },
          ) {
            if (isPortrait) BottomPlayerControlsPortrait(buttons = portraitBottomButtons, chapters = chapters, currentChapter = currentChapter, isSpeedNonOne = isSpeedNonOne, currentZoom = videoZoom, mediaTitle = mediaTitle, hideBackground = hideBackground, decoder = decoder, playbackSpeed = playbackSpeed ?: 1f, onBackPress = onBackPress, onOpenSheet = onOpenSheet, viewModel = viewModel, activity = activity)
            else BottomRightPlayerControlsLandscape(buttons = bottomRightButtons, chapters = chapters, currentChapter = currentChapter, isSpeedNonOne = isSpeedNonOne, currentZoom = videoZoom, mediaTitle = mediaTitle, hideBackground = hideBackground, decoder = decoder, playbackSpeed = playbackSpeed ?: 1f, onBackPress = onBackPress, onOpenSheet = onOpenSheet, viewModel = viewModel, activity = activity)
          }

          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked && !isPortrait && !areSlidersShown,
            enter = if (!reduceMotion) slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } + fadeIn(playerControlsEnterAnimationSpec()) else fadeIn(playerControlsEnterAnimationSpec()),
            exit = if (!reduceMotion) slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } + fadeOut(playerControlsExitAnimationSpec()) else fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.then(if (showSystemNavigationBar) { val navBarPadding = WindowInsets.navigationBars.asPaddingValues(); Modifier.padding(start = navBarPadding.calculateLeftPadding(LayoutDirection.Ltr), end = navBarPadding.calculateRightPadding(LayoutDirection.Ltr)) } else Modifier).constrainAs(bottomLeftControls) { bottom.linkTo(seekbar.top, spacing.small); start.linkTo(parent.start, spacing.large); width = Dimension.fillToConstraints; end.linkTo(bottomRightControls.start, spacing.small) },
          ) { BottomLeftPlayerControlsLandscape(buttons = bottomLeftButtons, chapters = chapters, currentChapter = currentChapter, isSpeedNonOne = isSpeedNonOne, currentZoom = videoZoom, mediaTitle = mediaTitle, hideBackground = hideBackground, decoder = decoder, playbackSpeed = playbackSpeed ?: 1f, onBackPress = onBackPress, onOpenSheet = onOpenSheet, viewModel = viewModel, activity = activity) }
        }
      }
    }

    val sheetShown by viewModel.sheetShown.collectAsState()
    val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
    val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
    val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
    val speedPresets by playerPreferences.speedPresets.collectAsState()

    // These flows emit plain Lists/Sets; hoist the immutable conversions and the
    // preset sort so they aren't rebuilt on every controls-root recomposition (the
    // sheets themselves are usually None and skip on their own).
    val subtitlesImm = remember(subtitles) { subtitles.toImmutableList() }
    val audioTracksImm = remember(audioTracks) { audioTracks.toImmutableList() }
    val sortedSpeedPresets = remember(speedPresets) { speedPresets.map { it.toFloat() }.sorted() }

    // No outer AnimatedVisibility here: each sheet host owns its entrance/exit
    // (portrait ModalBottomSheet animates inside its own dialog window; landscape
    // PlayerSideSheet slides in from the right). Wrapping them in a vertical slide
    // composited both motions into a diagonal "rises from the bottom" mush.
    PlayerSheets(viewModel = viewModel, sheetShown = sheetShown, subtitles = subtitlesImm, onAddSubtitle = viewModel::addSubtitle, onToggleSubtitle = { id -> if (viewModel.isSubtitleSelected(id)) { MPVLib.setPropertyString("sid", "no"); MPVLib.setPropertyString("secondary-sid", "no") } else { MPVLib.setPropertyInt("sid", id); MPVLib.setPropertyString("secondary-sid", "no") } }, isSubtitleSelected = viewModel::isSubtitleSelected, onRemoveSubtitle = viewModel::removeSubtitle, audioTracks = audioTracksImm, onAddAudio = viewModel::addAudio, onSelectAudio = { if (MPVLib.getPropertyInt("aid") == it.id) MPVLib.setPropertyString("aid", "no") else MPVLib.setPropertyInt("aid", it.id) }, chapter = chaptersImm.getOrNull(currentChapter ?: 0), chapters = chaptersImm, onSeekToChapter = { MPVLib.setPropertyInt("chapter", it); viewModel.unpause() }, decoder = decoder, onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) }, speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(), onSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) }, onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) }, onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() }, onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() }, onResetSpeedPresets = playerPreferences.speedPresets::delete, speedPresets = sortedSpeedPresets, onResetDefaultSpeed = { MPVLib.setPropertyFloat("speed", playerPreferences.defaultSpeed.deleteAndGet().toFixed(2)) }, sleepTimerTimeRemaining = sleepTimerTimeRemaining, onStartSleepTimer = viewModel::startTimer, onOpenPanel = onOpenPanel, onShowSheet = onOpenSheet, onDismissRequest = { onOpenSheet(Sheets.None) })

    val panel by viewModel.panelShown.collectAsState()
    PlayerPanels(panelShown = panel, onDismissRequest = { onOpenPanel(Panels.None) }, viewModel = viewModel)
  }
}

// ---------------------------------------------------------------------------
// Isolated recomposition scopes — each composable below only recomposes when
// its own state changes, not when other gesture-driven state in the outer
// ConstraintLayout changes.
// ---------------------------------------------------------------------------

@Composable
private fun BrightnessSliderSection(
  viewModel: PlayerViewModel,
  isVisible: Boolean,
  reduceMotion: Boolean,
  swapVolumeAndBrightness: Boolean,
  modifier: Modifier = Modifier,
) {
  val brightness by viewModel.currentBrightness.collectAsState()
  val brightnessSliderTimestamp by viewModel.brightnessSliderTimestamp.collectAsState()
  LaunchedEffect(brightnessSliderTimestamp) {
    if (isVisible && brightnessSliderTimestamp > 0) {
      delay(1000L)
      viewModel.isBrightnessSliderShown.update { false }
    }
  }
  AnimatedVisibility(
    isVisible,
    enter =
      if (!reduceMotion) {
        slideInHorizontally(playerControlsEnterAnimationSpec()) {
          if (swapVolumeAndBrightness) -it else it
        } + fadeIn(playerControlsEnterAnimationSpec())
      } else {
        fadeIn(playerControlsEnterAnimationSpec())
      },
    exit =
      if (!reduceMotion) {
        slideOutHorizontally(playerControlsExitAnimationSpec()) {
          if (swapVolumeAndBrightness) -it else it
        } + fadeOut(playerControlsExitAnimationSpec())
      } else {
        fadeOut(playerControlsExitAnimationSpec())
      },
    modifier = modifier,
  ) { BrightnessSlider(brightness, 0f..1f) }
}

@Composable
private fun VolumeSliderSection(
  viewModel: PlayerViewModel,
  audioPreferences: AudioPreferences,
  playerPreferences: PlayerPreferences,
  isVisible: Boolean,
  reduceMotion: Boolean,
  swapVolumeAndBrightness: Boolean,
  modifier: Modifier = Modifier,
) {
  val volume by viewModel.currentVolume.collectAsState()
  val mpvVolume by MPVLib.propInt["volume"].collectAsState()
  val volumeSliderTimestamp by viewModel.volumeSliderTimestamp.collectAsState()
  val boostCap by audioPreferences.volumeBoostCap.collectAsState()
  val displayVolumeAsPercentage by playerPreferences.displayVolumeAsPercentage.collectAsState()
  LaunchedEffect(volumeSliderTimestamp) {
    if (isVisible && volumeSliderTimestamp > 0) {
      delay(1000L)
      viewModel.isVolumeSliderShown.update { false }
    }
  }
  AnimatedVisibility(
    isVisible,
    enter =
      if (!reduceMotion) {
        slideInHorizontally(playerControlsEnterAnimationSpec()) {
          if (swapVolumeAndBrightness) it else -it
        } + fadeIn(playerControlsEnterAnimationSpec())
      } else {
        fadeIn(playerControlsEnterAnimationSpec())
      },
    exit =
      if (!reduceMotion) {
        slideOutHorizontally(playerControlsExitAnimationSpec()) { it } +
          fadeOut(playerControlsExitAnimationSpec())
      } else {
        fadeOut(playerControlsExitAnimationSpec())
      },
    modifier = modifier,
  ) {
    val currentBoost = (mpvVolume ?: 100) - 100
    val showBoost = boostCap > 0 || currentBoost > 0
    val effBoostCap = maxOf(boostCap, currentBoost)
    VolumeSlider(
      volume,
      mpvVolume = mpvVolume ?: 100,
      range = 0..viewModel.maxVolume,
      boostRange = if (showBoost) 0..effBoostCap else null,
      displayAsPercentage = displayVolumeAsPercentage,
    )
  }
}

@Composable
private fun PlayerUpdatesSection(
  viewModel: PlayerViewModel,
  playerPreferences: PlayerPreferences,
  videoZoom: Float,
  playlistMode: Boolean,
  modifier: Modifier = Modifier,
) {
  val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
  val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
  val aspectRatio by viewModel.videoAspect.collectAsState()
  val currentAspectRatio by viewModel.currentAspectRatio.collectAsState()

  LaunchedEffect(currentPlayerUpdate, aspectRatio, videoZoom) {
    if (currentPlayerUpdate is PlayerUpdates.MultipleSpeed ||
      currentPlayerUpdate is PlayerUpdates.DynamicSpeedControl ||
      currentPlayerUpdate is PlayerUpdates.None
    ) {
      return@LaunchedEffect
    }
    delay(2000)
    viewModel.playerUpdate.update { PlayerUpdates.None }
  }

  AnimatedVisibility(
    currentPlayerUpdate !is PlayerUpdates.None,
    enter = fadeIn(playerControlsEnterAnimationSpec()),
    exit = fadeOut(playerControlsExitAnimationSpec()),
    modifier = modifier,
  ) {
    when (currentPlayerUpdate) {
      is PlayerUpdates.MultipleSpeed -> MultipleSpeedPlayerUpdate(currentSpeed = holdForMultipleSpeed)
      is PlayerUpdates.DynamicSpeedControl -> {
        val speedUpdate = currentPlayerUpdate as PlayerUpdates.DynamicSpeedControl
        val currentSpeed = speedUpdate.speed
        val showDynamicSpeedOverlay by playerPreferences.showDynamicSpeedOverlay.collectAsState()
        val shouldShowFull = speedUpdate.showFullOverlay
        var isCollapsed by remember { mutableStateOf(false) }
        LaunchedEffect(currentSpeed, shouldShowFull) {
          if (shouldShowFull) {
            isCollapsed = false
            delay(1500)
            isCollapsed = true
          } else {
            isCollapsed = true
          }
        }
        if (showDynamicSpeedOverlay) {
          if (isCollapsed) CompactSpeedIndicator(currentSpeed = currentSpeed)
          else SpeedControlSlider(currentSpeed = currentSpeed)
        } else {
          CompactSpeedIndicator(currentSpeed = currentSpeed)
        }
      }
      is PlayerUpdates.AspectRatio -> {
        val customRatiosSet by playerPreferences.customAspectRatios.collectAsState()
        val displayText = if (currentAspectRatio > 0) {
          val customLabel = customRatiosSet.firstNotNullOfOrNull { str ->
            val parts = str.split("|")
            if (parts.size == 2) {
              val savedRatio = parts[1].toDoubleOrNull()
              if (savedRatio != null && abs(savedRatio - currentAspectRatio) < 0.01) parts[0] else null
            } else null
          }
          customLabel ?: run {
            val ratio = currentAspectRatio
            when {
              abs(ratio - 16.0 / 9.0) < 0.01 -> "16:9"
              abs(ratio - 4.0 / 3.0) < 0.01 -> "4:3"
              abs(ratio - 16.0 / 10.0) < 0.01 -> "16:10"
              abs(ratio - 21.0 / 9.0) < 0.01 -> "21:9"
              abs(ratio - 32.0 / 9.0) < 0.01 -> "32:9"
              abs(ratio - 1.0) < 0.01 -> "1:1"
              abs(ratio - 2.35) < 0.01 -> "2.35:1"
              abs(ratio - 2.39) < 0.01 -> "2.39:1"
              else -> String.format(Locale.US, "%.2f:1", ratio)
            }
          }
        } else {
          stringResource(aspectRatio.titleRes)
        }
        TextPlayerUpdate(displayText)
      }
      is PlayerUpdates.ShowText -> TextPlayerUpdate((currentPlayerUpdate as PlayerUpdates.ShowText).value)
      is PlayerUpdates.VideoZoom -> TextPlayerUpdate("Zoom: ${(videoZoom * 100).toInt()}%")
      is PlayerUpdates.HorizontalSeek -> {
        val seekUpdate = currentPlayerUpdate as PlayerUpdates.HorizontalSeek
        TextPlayerUpdate("${seekUpdate.currentTime} [ ${seekUpdate.seekDelta} ]")
      }
      is PlayerUpdates.RepeatMode -> {
        val mode = (currentPlayerUpdate as PlayerUpdates.RepeatMode).mode
        val text = when (mode) {
          app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF -> "Repeat: Off"
          app.marlboroadvance.mpvex.ui.player.RepeatMode.ONE -> "Repeat: Current file"
          app.marlboroadvance.mpvex.ui.player.RepeatMode.ALL ->
            if (playlistMode && viewModel.hasPlaylistSupport()) "Repeat: All playlist"
            else "Repeat: Current file"
        }
        TextPlayerUpdate(text)
      }
      is PlayerUpdates.Shuffle -> {
        val enabled = (currentPlayerUpdate as PlayerUpdates.Shuffle).enabled
        val text =
          if (enabled) {
            if (playlistMode && viewModel.hasPlaylistSupport()) "Shuffle: On"
            else "Shuffle: Not available"
          } else {
            "Shuffle: Off"
          }
        TextPlayerUpdate(text)
      }
      is PlayerUpdates.FrameInfo -> {
        val frameInfo = currentPlayerUpdate as PlayerUpdates.FrameInfo
        val text =
          if (frameInfo.totalFrames > 0) "Frame: ${frameInfo.currentFrame}/${frameInfo.totalFrames}"
          else "Frame: ${frameInfo.currentFrame}"
        TextPlayerUpdate(text)
      }
      else -> {}
    }
  }
}

