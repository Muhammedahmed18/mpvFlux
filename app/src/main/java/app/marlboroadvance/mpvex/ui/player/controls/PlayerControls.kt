package app.marlboroadvance.mpvex.ui.player.controls

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.preferences.preference.deleteAndGet
import app.marlboroadvance.mpvex.preferences.preference.plusAssign
import app.marlboroadvance.mpvex.preferences.preference.minusAssign
import app.marlboroadvance.mpvex.ui.player.Decoder.Companion.getDecoderFromValue
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerUpdates
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.controls.components.BrightnessSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.CompactSpeedIndicator
import app.marlboroadvance.mpvex.ui.player.controls.components.MultipleSpeedPlayerUpdate
import app.marlboroadvance.mpvex.ui.player.controls.components.SeekPlayerUpdate
import app.marlboroadvance.mpvex.ui.player.controls.components.SeekbarWithTimers
import app.marlboroadvance.mpvex.ui.player.controls.components.ThumbZoneUnlock
import app.marlboroadvance.mpvex.ui.player.controls.components.SpeedControlSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.TextPlayerUpdate
import app.marlboroadvance.mpvex.ui.player.controls.components.VolumeSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.toFixed
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButton
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButtonType
import app.marlboroadvance.mpvex.ui.theme.controlColor
import app.marlboroadvance.mpvex.ui.theme.playerRippleConfiguration
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.ui.theme.MpvexTheme
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> =
  tween(
    durationMillis = 300,
    easing = FastOutSlowInEasing,
  )

fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> =
  tween(
    durationMillis = 100,
    easing = LinearOutSlowInEasing,
  )

@OptIn(
  ExperimentalAnimationGraphicsApi::class,
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
  val seekBarShown by viewModel.seekBarShown.collectAsState()
  val pausedForCache by MPVLib.propBoolean["paused-for-cache"].collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val position by MPVLib.propInt["time-pos"].collectAsState()
  val precisePosition by viewModel.precisePosition.collectAsState()
  val preciseDuration by viewModel.preciseDuration.collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControlsTimestamp by remember { mutableStateOf(0L) }
  val seekText by viewModel.seekText.collectAsState()
  val currentChapter by MPVLib.propInt["chapter"].collectAsState()
  val mpvDecoder by MPVLib.propString["hwdec-current"].collectAsState()
  val decoder by remember { derivedStateOf { getDecoderFromValue(mpvDecoder ?: "auto") } }
  val isSpeedNonOne by remember(playbackSpeed) {
    derivedStateOf { abs((playbackSpeed ?: 1f) - 1f) > 0.001f }
  }
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val chapters by viewModel.chapters.collectAsState(persistentListOf())
  val playlistMode by playerPreferences.playlistMode.collectAsState()
    val haptic = LocalHapticFeedback.current
    
  val abLoopA by viewModel.abLoopA.collectAsState()
  val abLoopB by viewModel.abLoopB.collectAsState()
  val showNextUp by viewModel.showNextUp.collectAsState()
  val nextItemTitle by viewModel.nextItemTitle.collectAsState()

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

  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControlsTimestamp,
    areControlsLocked,
  ) {
    if (controlsShown && paused == false && !isSeeking) {
      // Use 2 second delay when controls are locked, otherwise use user preference
      val delayTime = if (areControlsLocked) 2000L else playerTimeToDisappear.toLong()
      delay(delayTime)
      viewModel.hideControls()
    }
  }

  val scrimAlpha by animateFloatAsState(
    targetValue = if ((controlsShown && !areControlsLocked) || showNextUp) 1f else 0f,
    animationSpec = if (controlsShown || showNextUp) playerControlsEnterAnimationSpec() else playerControlsExitAnimationSpec(),
    label = "scrim_alpha",
  )

  GestureHandler(
    viewModel = viewModel,
  )

  DoubleTapToSeekOvals(
    amount = doubleTapSeekAmount,
    showOvals = showDoubleTapOvals,
    showSeekIcon = showSeekTime,
    showSeekTime = showSeekTime,
    interactionSource = interactionSource,
  )

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

      ConstraintLayout(
        modifier =
          modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.55f),
                0.15f to Color.Transparent,
                0.85f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.55f),
              ),
              alpha = scrimAlpha,
            ),
      ) {
        val (topLeftControls, topRightControls) = createRefs()
        val (volumeSlider, brightnessSlider) = createRefs()
        val unlockControlsButton = createRef()
        val (bottomRightControls, bottomLeftControls) = createRefs()
        val playerPauseButton = createRef()
        val seekbar = createRef()
        val (playerUpdates) = createRefs()
        val nextUpPill = createRef()

        val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
        val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
        val brightness by viewModel.currentBrightness.collectAsState()
        val volume by viewModel.currentVolume.collectAsState()
        val mpvVolume by MPVLib.propInt["volume"].collectAsState()
        val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
        val reduceMotion by playerPreferences.reduceMotion.collectAsState()

        val aspect by viewModel.videoAspect.collectAsState()
        val currentZoom by viewModel.videoZoom.collectAsState()

        val rawMediaTitle by MPVLib.propString["media-title"].collectAsState()
        val mediaTitle by remember(rawMediaTitle, activity) {
          derivedStateOf {
            rawMediaTitle?.takeIf { it.isNotBlank() }
              ?: activity.getTitleForControls()
          }
        }

        // Slider display duration: 1000ms shown + 300ms exit animation = 1300ms total
        val sliderDisplayDuration = 1000L

        val volumeSliderTimestamp by viewModel.volumeSliderTimestamp.collectAsState()
        val brightnessSliderTimestamp by viewModel.brightnessSliderTimestamp.collectAsState()

        // Track timestamp to restart timer on every gesture event
        LaunchedEffect(volumeSliderTimestamp) {
          if (isVolumeSliderShown && volumeSliderTimestamp > 0) {
            delay(sliderDisplayDuration)
            viewModel.isVolumeSliderShown.update { false }
          }
        }

        LaunchedEffect(brightnessSliderTimestamp) {
          if (isBrightnessSliderShown && brightnessSliderTimestamp > 0) {
            delay(sliderDisplayDuration)
            viewModel.isBrightnessSliderShown.update { false }
          }
        }

        val areSlidersShown = isBrightnessSliderShown || isVolumeSliderShown

        AnimatedVisibility(
          isBrightnessSliderShown,
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
          modifier =
            Modifier.constrainAs(brightnessSlider) {
              if (swapVolumeAndBrightness) {
                start.linkTo(parent.start, if (isPortrait) spacing.large else spacing.extraLarge)
              } else {
                end.linkTo(parent.end, if (isPortrait) spacing.large else spacing.extraLarge)
              }
              top.linkTo(parent.top, spacing.larger)
              bottom.linkTo(parent.bottom, spacing.extraLarge)
            },
        ) { BrightnessSlider(brightness, 0f..1f) }

        AnimatedVisibility(
          isVolumeSliderShown,
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
              slideOutHorizontally(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
          modifier =
            Modifier.constrainAs(volumeSlider) {
              if (swapVolumeAndBrightness) {
                end.linkTo(parent.end, if (isPortrait) spacing.large else spacing.extraLarge)
              } else {
                start.linkTo(parent.start, if (isPortrait) spacing.large else spacing.extraLarge)
              }
              top.linkTo(parent.top, spacing.larger)
              bottom.linkTo(parent.bottom, spacing.extraLarge)
            },
        ) {
          val boostCap by audioPreferences.volumeBoostCap.collectAsState()
          val displayVolumeAsPercentage by playerPreferences.displayVolumeAsPercentage.collectAsState()
          
          // Show if boost is allowed (boostCap > 0) OR if we are currently boosted (> 100)
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

        val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
        val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
        val aspectRatio by viewModel.videoAspect.collectAsState()
        val currentAspectRatio by viewModel.currentAspectRatio.collectAsState()
        val videoZoom by viewModel.videoZoom.collectAsState()

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
          modifier =
            Modifier
              .then(
                if (showSystemStatusBar) {
                  Modifier.windowInsetsPadding(WindowInsets.statusBars)
                } else {
                  Modifier
                }
              )
              .constrainAs(playerUpdates) {
                linkTo(parent.start, parent.end)
                top.linkTo(parent.top, if (isPortrait) 104.dp else 64.dp)
              },
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
                if (isCollapsed) {
                  // Simple compact indicator
                  CompactSpeedIndicator(currentSpeed = currentSpeed)
                } else {
                  // Full speed control slider
                  SpeedControlSlider(currentSpeed = currentSpeed)
                }
              } else {
                // fallback, simple indicator
                CompactSpeedIndicator(currentSpeed = currentSpeed)
              }
            }
            is PlayerUpdates.AspectRatio -> {
              val customRatiosSet by playerPreferences.customAspectRatios.collectAsState()
              val displayText = if (currentAspectRatio > 0) {
                // Custom aspect ratio - try to find its label first
                val customLabel = customRatiosSet.firstNotNullOfOrNull { str ->
                  val parts = str.split("|")
                  if (parts.size == 2) {
                    val savedRatio = parts[1].toDoubleOrNull()
                    if (savedRatio != null && kotlin.math.abs(savedRatio - currentAspectRatio) < 0.01) {
                      parts[0] // Return the label
                    } else null
                  } else null
                }
                
                customLabel ?: run {
                  // No custom label found, use preset names or format as ratio
                  val ratio = currentAspectRatio
                  when {
                    kotlin.math.abs(ratio - 16.0/9.0) < 0.01 -> "16:9"
                    kotlin.math.abs(ratio - 4.0/3.0) < 0.01 -> "4:3"
                    kotlin.math.abs(ratio - 16.0/10.0) < 0.01 -> "16:10"
                    kotlin.math.abs(ratio - 21.0/9.0) < 0.01 -> "21:9"
                    kotlin.math.abs(ratio - 32.0/9.0) < 0.01 -> "32:9"
                    kotlin.math.abs(ratio - 1.0) < 0.01 -> "1:1"
                    kotlin.math.abs(ratio - 2.35) < 0.01 -> "2.35:1"
                    kotlin.math.abs(ratio - 2.39) < 0.01 -> "2.39:1"
                    else -> String.format("%.2f:1", ratio)
                  }
                }
              } else {
                // Standard mode (Fit/Crop/Stretch)
                stringResource(aspectRatio.titleRes)
              }
              TextPlayerUpdate(displayText)
            }
            is PlayerUpdates.ShowText ->
              TextPlayerUpdate(
                (currentPlayerUpdate as PlayerUpdates.ShowText).value,
                modifier = Modifier.widthIn(min = 120.dp),
              )

            is PlayerUpdates.VideoZoom -> {
              val zoomPercentage = (videoZoom * 100).toInt()
              TextPlayerUpdate(
                text = String.format("Zoom:%3d%%", zoomPercentage), 
                modifier = Modifier, // Let content size determine width
              )
            }

            is PlayerUpdates.HorizontalSeek -> {
              val seekUpdate = currentPlayerUpdate as PlayerUpdates.HorizontalSeek
              SeekPlayerUpdate(
                currentTime = seekUpdate.currentTime,
                seekDelta = "[${seekUpdate.seekDelta}]",
                modifier = Modifier, // Let content size determine width
              )
            }

            is PlayerUpdates.RepeatMode -> {
              val mode = (currentPlayerUpdate as PlayerUpdates.RepeatMode).mode
              val text = when (mode) {
                app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF -> "Repeat: Off"
                app.marlboroadvance.mpvex.ui.player.RepeatMode.ONE -> "Repeat: Current file"
                app.marlboroadvance.mpvex.ui.player.RepeatMode.ALL -> {
                  if (playlistMode && viewModel.hasPlaylistSupport()) {
                    "Repeat: All playlist"
                  } else {
                    "Repeat: Current file"
                  }
                }
              }
              TextPlayerUpdate(text)
            }

            is PlayerUpdates.Shuffle -> {
              val enabled = (currentPlayerUpdate as PlayerUpdates.Shuffle).enabled
              val text = if (enabled) {
                if (playlistMode && viewModel.hasPlaylistSupport()) {
                  "Shuffle: On"
                } else {
                  "Shuffle: Not available"
                }
              } else {
                "Shuffle: Off"
              }
              TextPlayerUpdate(text)
            }

            is PlayerUpdates.FrameInfo -> {
              val frameInfo = (currentPlayerUpdate as PlayerUpdates.FrameInfo)
              val text = if (frameInfo.totalFrames > 0) {
                "Frame: ${frameInfo.currentFrame}/${frameInfo.totalFrames}"
              } else {
                "Frame: ${frameInfo.currentFrame}"
              }
              TextPlayerUpdate(text)
            }

            else -> {}
          }
        }

        val areButtonsVisible = controlsShown && !areControlsLocked && !areSlidersShown

        AnimatedVisibility(
          visible = controlsShown && areControlsLocked,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier =
            Modifier
              .then(
                if (showSystemStatusBar) {
                  Modifier.windowInsetsPadding(WindowInsets.statusBars)
                } else {
                  Modifier
                }
              )
              .constrainAs(unlockControlsButton) {
                top.linkTo(parent.top, if (isPortrait) spacing.extraLarge else spacing.small)
                end.linkTo(parent.end, spacing.large)
              },
        ) {
          ThumbZoneUnlock(
            onUnlock = { viewModel.unlockControls() }
          )
        }

        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier =
            Modifier.constrainAs(playerPauseButton) {
              end.linkTo(parent.absoluteRight)
              start.linkTo(parent.absoluteLeft)
              if (isPortrait) {
                bottom.linkTo(bottomRightControls.top, spacing.large)
              } else {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
              }
            },
        ) {
          val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()
          val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
          val interaction = remember { MutableInteractionSource() }
          val isPressed by interaction.collectIsPressedAsState()
          
          val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "play_button_scale"
          )

          when {
            pausedForCache == true && showLoadingCircle -> {
              LoadingIndicator(
                modifier = Modifier.size(96.dp),
              )
            }

            else -> {
              val buttonShadow = Brush.radialGradient(
                  0.0f to Color.Black.copy(alpha = 0.35f),
                  0.85f to Color.Transparent,
              )

              Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (playlistMode && viewModel.hasPlaylistSupport()) {
                  // Previous Button
                  val prevEnabled = viewModel.hasPrevious()
                  ControlsButton(
                    icon = Icons.Outlined.SkipPrevious,
                    onClick = { viewModel.playPrevious() },
                    enabled = prevEnabled,
                    modifier = Modifier
                      .size(56.dp)
                      .then(if (hideBackground) Modifier.background(buttonShadow, CircleShape) else Modifier),
                    shape = CircleShape,
                    iconSize = 32.dp,
                    type = if (hideBackground) ControlsButtonType.Transparent else ControlsButtonType.Tonal,
                    color = controlColor
                  )
                }

                // Main Play/Pause Hero Button
                val playContentColor = MaterialTheme.colorScheme.onSurface
                val playContainerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                val heroBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))

                Surface(
                  modifier = Modifier
                    .size(92.dp)
                    .graphicsLayer {
                      scaleX = scale
                      scaleY = scale
                    }
                    .then(if (hideBackground) Modifier.background(buttonShadow, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .clickable(interaction, ripple(color = Color.White)) {
                      resetControlsTimestamp = System.currentTimeMillis()
                      viewModel.pauseUnpause()
                    },
                  shape = CircleShape,
                  color = playContainerColor,
                  contentColor = playContentColor,
                  border = if (hideBackground) null else heroBorder,
                  tonalElevation = 0.dp
                ) {
                  Image(
                    painter = rememberAnimatedVectorPainter(icon, paused == false),
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(28.dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(playContentColor),
                  )
                }

                if (playlistMode && viewModel.hasPlaylistSupport()) {
                  // Next Button
                  val nextEnabled = viewModel.hasNext()
                  ControlsButton(
                    icon = Icons.Outlined.SkipNext,
                    onClick = { viewModel.playNext() },
                    enabled = nextEnabled,
                    modifier = Modifier
                      .size(56.dp)
                      .then(if (hideBackground) Modifier.background(buttonShadow, CircleShape) else Modifier),
                    shape = CircleShape,
                    iconSize = 32.dp,
                    type = if (hideBackground) ControlsButtonType.Transparent else ControlsButtonType.Tonal,
                    color = controlColor
                  )
                }
              }
            }
          }
        }

        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked,
          enter =
            if (!reduceMotion) {
              slideInVertically(playerControlsEnterAnimationSpec()) { it } +
                fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
          exit =
            if (!reduceMotion) {
              slideOutVertically(playerControlsExitAnimationSpec()) { it } +
                fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
          modifier =
            Modifier
              .then(
                if (showSystemNavigationBar) {
                  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                  Modifier.padding(
                    start = navBarPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = navBarPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                  )
                } else {
                  Modifier
                }
              )
              .constrainAs(seekbar) {
                if (isPortrait) {
                  bottom.linkTo(playerPauseButton.top, spacing.medium)
                } else {
                  bottom.linkTo(parent.bottom, spacing.medium)
                }
                start.linkTo(parent.start, 24.dp)
                end.linkTo(parent.end, 24.dp)
                width = Dimension.fillToConstraints
              },
        ) {
          val invertDuration by playerPreferences.invertDuration.collectAsState()
          val seekbarStyle by appearancePreferences.seekbarStyle.collectAsState()
          var wasPlayerAlreadyPaused by remember { mutableStateOf(false) }

          SeekbarWithTimers(
            position = precisePosition,
            duration = if (preciseDuration > 0) preciseDuration else duration?.toFloat() ?: 0f,
            onValueChange = {
              if (!isSeeking) {
                // First drag frame - pause playback
                wasPlayerAlreadyPaused = paused ?: false
                if (!wasPlayerAlreadyPaused) {
                  viewModel.pause()
                }
              }
              isSeeking = true
              resetControlsTimestamp = System.currentTimeMillis()
              viewModel.seekTo(it.toInt())
            },
            onValueChangeFinished = {
              isSeeking = false
              resetControlsTimestamp = System.currentTimeMillis()
              // Unpause if it wasn't paused before seeking
              if (!wasPlayerAlreadyPaused) {
                viewModel.unpause()
              }
              viewModel.showControls()
            },
            timersInverted = Pair(false, invertDuration),
            durationTimerOnCLick = {
              resetControlsTimestamp = System.currentTimeMillis()
              playerPreferences.invertDuration.set(!invertDuration)
            },
            positionTimerOnClick = {},
            chapters = chapters.toImmutableList(),
            paused = paused ?: false,
            seekbarStyle = seekbarStyle,
            loopStart = abLoopA?.toFloat(),
            loopEnd = abLoopB?.toFloat(),
          )
        }

        AnimatedVisibility(
            visible = showNextUp,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.constrainAs(nextUpPill) {
                bottom.linkTo(parent.bottom, 100.dp)
                end.linkTo(parent.end, spacing.large)
            }
        ) {
            NextUpPill(
                title = nextItemTitle ?: "",
                onClick = { viewModel.playNext() },
                onDismiss = { viewModel.dismissNextUp() }
            )
        }

        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked,
          enter =
            if (!reduceMotion) {
              slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
                fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
          exit =
            if (!reduceMotion) {
              slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
                fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
          modifier =
            Modifier
              .then(
                if (showSystemStatusBar) {
                  Modifier.windowInsetsPadding(WindowInsets.statusBars)
                } else {
                  Modifier
                }
              )
              .then(
                if (showSystemNavigationBar) {
                  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                  Modifier.padding(
                    start = navBarPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = navBarPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                  )
                } else {
                  Modifier
                }
              )
              .constrainAs(topLeftControls) {
                top.linkTo(parent.top, if (isPortrait) spacing.extraLarge else spacing.small)
                start.linkTo(parent.start, spacing.large)
                if (isPortrait) {
                  width = Dimension.fillToConstraints
                  end.linkTo(parent.end, spacing.large)
                } else {
                  width = Dimension.fillToConstraints
                  end.linkTo(topRightControls.start, spacing.extraSmall)
                }
              },
        ) {
          if (isPortrait) {
            TopPlayerControlsPortrait(
              mediaTitle = mediaTitle,
              hideBackground = hideBackground,
              onBackPress = onBackPress,
              onOpenSheet = onOpenSheet,
              viewModel = viewModel,
              activity = activity,
            )
          } else {
            TopLeftPlayerControlsLandscape(
              mediaTitle = mediaTitle,
              hideBackground = hideBackground,
              onBackPress = onBackPress,
              onOpenSheet = onOpenSheet,
              viewModel = viewModel,
              activity = activity,
            )
          }
        }

        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked && !isPortrait,
          enter =
            if (!reduceMotion) {
              slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
                fadeIn(playerControlsEnterAnimationSpec())
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
          modifier =
            Modifier
              .then(
                if (showSystemStatusBar) {
                  Modifier.windowInsetsPadding(WindowInsets.statusBars)
                } else {
                  Modifier
                }
              )
              .then(
                if (showSystemNavigationBar) {
                  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                  Modifier.padding(
                    start = navBarPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = navBarPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                  )
                } else {
                  Modifier
                }
              )
              .constrainAs(topRightControls) {
                top.linkTo(parent.top, spacing.small)
                end.linkTo(parent.end, spacing.large)
              },
        ) {
          TopRightPlayerControlsLandscape(
            buttons = topRightButtons,
            chapters = chapters,
            currentChapter = currentChapter,
            isSpeedNonOne = isSpeedNonOne,
            currentZoom = currentZoom,
            aspect = aspect,
            mediaTitle = mediaTitle,
            hideBackground = hideBackground,
            decoder = decoder,
            playbackSpeed = playbackSpeed ?: 1f,
            onBackPress = onBackPress,
            onOpenSheet = onOpenSheet,
            onOpenPanel = onOpenPanel,
            viewModel = viewModel,
            activity = activity,
          )
        }

        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked && !areSlidersShown,
          enter =
            if (!reduceMotion) {
              slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
                fadeIn(playerControlsEnterAnimationSpec())
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
          modifier =
            Modifier
              .then(
                if (showSystemNavigationBar) {
                  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                  Modifier.padding(
                    start = navBarPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = navBarPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                  )
                } else {
                  Modifier
                }
              )
              .constrainAs(bottomRightControls) {
                if (isPortrait) {
                  bottom.linkTo(parent.bottom, spacing.extraLarge)
                  start.linkTo(parent.start, spacing.large)
                  end.linkTo(parent.end, spacing.large)
                  width = Dimension.fillToConstraints
                } else {
                  bottom.linkTo(seekbar.top, spacing.small)
                  end.linkTo(parent.end, spacing.large)
                }
              },
        ) {
          if (isPortrait) {
            BottomPlayerControlsPortrait(
              buttons = portraitBottomButtons,
              chapters = chapters,
              currentChapter = currentChapter,
              isSpeedNonOne = isSpeedNonOne,
              currentZoom = currentZoom,
              aspect = aspect,
              mediaTitle = mediaTitle,
              hideBackground = hideBackground,
              decoder = decoder,
              playbackSpeed = playbackSpeed ?: 1f,
              onBackPress = onBackPress,
              onOpenSheet = onOpenSheet,
              onOpenPanel = onOpenPanel,
              viewModel = viewModel,
              activity = activity,
            )
          } else {
            BottomRightPlayerControlsLandscape(
              buttons = bottomRightButtons,
              chapters = chapters,
              currentChapter = currentChapter,
              isSpeedNonOne = isSpeedNonOne,
              currentZoom = currentZoom,
              aspect = aspect,
              mediaTitle = mediaTitle,
              hideBackground = hideBackground,
              decoder = decoder,
              playbackSpeed = playbackSpeed ?: 1f,
              onBackPress = onBackPress,
              onOpenSheet = onOpenSheet,
              onOpenPanel = onOpenPanel,
              viewModel = viewModel,
              activity = activity,
            )
          }
        }

        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked && !isPortrait && !areSlidersShown,
          enter =
            if (!reduceMotion) {
              slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
                fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
          exit =
            if (!reduceMotion) {
              slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
                fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
          modifier =
            Modifier
              .then(
                if (showSystemNavigationBar) {
                  val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                  Modifier.padding(
                    start = navBarPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = navBarPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                  )
                } else {
                  Modifier
                }
              )
              .constrainAs(bottomLeftControls) {
                bottom.linkTo(seekbar.top, spacing.small)
                start.linkTo(parent.start, spacing.large)
                width = Dimension.fillToConstraints
                end.linkTo(bottomRightControls.start, spacing.small)
              },
        ) {
          BottomLeftPlayerControlsLandscape(
            buttons = bottomLeftButtons,
            chapters = chapters,
            currentChapter = currentChapter,
            isSpeedNonOne = isSpeedNonOne,
            currentZoom = currentZoom,
            aspect = aspect,
            mediaTitle = mediaTitle,
            hideBackground = hideBackground,
            decoder = decoder,
            playbackSpeed = playbackSpeed ?: 1f,
            onBackPress = onBackPress,
            onOpenSheet = onOpenSheet,
            onOpenPanel = onOpenPanel,
            viewModel = viewModel,
            activity = activity,
          )
        }

      }
    }

    val sheetShown by viewModel.sheetShown.collectAsState()
    val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
    val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
    val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
    val speedPresets by playerPreferences.speedPresets.collectAsState()

    // Smooth sheet enter/exit animation
    AnimatedVisibility(
      visible = sheetShown != Sheets.None,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut()
    ) {
      PlayerSheets(
        viewModel = viewModel,
        sheetShown = sheetShown,
        subtitles = subtitles.toImmutableList(),
        onAddSubtitle = viewModel::addSubtitle,
        onToggleSubtitle = { id ->
          if (viewModel.isSubtitleSelected(id)) {
            MPVLib.setPropertyString("sid", "no")
            MPVLib.setPropertyString("secondary-sid", "no")
          } else {
            MPVLib.setPropertyInt("sid", id)
            MPVLib.setPropertyString("secondary-sid", "no")
          }
        },
        isSubtitleSelected = viewModel::isSubtitleSelected,
        onRemoveSubtitle = viewModel::removeSubtitle,
        audioTracks = audioTracks.toImmutableList(),
        onAddAudio = viewModel::addAudio,
        onSelectAudio = {
          if (MPVLib.getPropertyInt("aid") == it.id) {
            MPVLib.setPropertyString("aid", "no")
          } else {
            MPVLib.setPropertyInt("aid", it.id)
          }
        },
        chapter = chapters.getOrNull(currentChapter ?: 0),
        chapters = chapters.toImmutableList(),
        onSeekToChapter = {
          MPVLib.setPropertyInt("chapter", it)
          viewModel.unpause()
        },
        decoder = decoder,
        onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) },
        speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
        onSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) },
        onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) },
        onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() },
        onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() },
        onResetSpeedPresets = playerPreferences.speedPresets::delete,
        speedPresets = speedPresets.map { it.toFloat() }.sorted(),
        onResetDefaultSpeed = {
          MPVLib.setPropertyFloat("speed", playerPreferences.defaultSpeed.deleteAndGet().toFixed(2))
        },
        sleepTimerTimeRemaining = sleepTimerTimeRemaining,
        onStartSleepTimer = viewModel::startTimer,
        onOpenPanel = onOpenPanel,
        onShowSheet = onOpenSheet,
        onDismissRequest = { onOpenSheet(Sheets.None) },
      )
    }

    val panel by viewModel.panelShown.collectAsState()
    PlayerPanels(
      panelShown = panel,
      onDismissRequest = { onOpenPanel(Panels.None) },
    )
  }
}

@Composable
fun NextUpPill(
    title: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val spacing = MaterialTheme.spacing
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "next_up_pill_scale"
    )
    
    // Trigger haptic when appearing
    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    Surface(
        modifier = modifier
            .padding(spacing.small)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX.value > 100f) {
                            scope.launch {
                                offsetX.animateTo(500f)
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f)
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow dragging to the right
                        if (dragAmount > 0 || offsetX.value > 0) {
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f))
                            }
                        }
                    }
                )
            }
            .height(64.dp)
            .widthIn(min = 180.dp, max = 300.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NEXT UP",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewNextUpPill() {
    MpvexTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gradient background to see the look better
            Box(
                modifier = Modifier
                    .size(400.dp, 200.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC6))
                        )
                    )
            )
            
            NextUpPill(
                title = "S01 E05 - The Final Stand",
                onClick = {},
                onDismiss = {}
            )
        }
    }
}
