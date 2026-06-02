package app.marlboroadvance.mpvex.ui.player.controls

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// Exit: 350ms with FastOutSlowIn — M3 motion standard for elements leaving the screen.
// Slightly longer than before (300ms → 350ms) so the fade feels smooth rather than abrupt.
fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> =
  tween(
    durationMillis = 350,
    easing = FastOutSlowInEasing,
  )

// Enter: 200ms with LinearOutSlowIn — M3 motion standard for elements entering the screen.
// Extended from 100ms → 200ms so the controls arrive deliberately rather than snapping in.
fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> =
  tween(
    durationMillis = 200,
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
  val playlistMode by playerPreferences.playlistMode.collectAsState()

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
        val scrimColor = MaterialTheme.colorScheme.scrim
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrimAlpha }
            .background(
              Brush.verticalGradient(
                0.0f to scrimColor.copy(alpha = 0.55f),
                0.15f to Color.Transparent,
                0.85f to Color.Transparent,
                1.0f to scrimColor.copy(alpha = 0.55f),
              )
            )
        )

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
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

            // M3 Expressive shape-morphing for skip buttons
            val skipButtonShapes = IconButtonDefaults.shapes()
            val prevInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }

            // Connected ButtonGroup: pressing a skip button expands it and compresses
            // its neighbours. Play/pause is a circular ghost button whose M3 icon
            // morphs between play and pause; buffering swaps it for a LoadingIndicator
            // in the same footprint so the row never shifts.
            ButtonGroup(
              overflowIndicator = {},
              horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // ── Skip Previous ──────────────────────────────────────────
              if (showSkip) {
                customItem(
                  buttonGroupContent = {
                    FilledTonalIconButton(
                      onClick           = { viewModel.playPrevious() },
                      enabled           = viewModel.hasPrevious(),
                      shapes            = skipButtonShapes,
                      interactionSource = prevInteraction,
                      colors            = glassIconButtonColors(hideBackground),
                      modifier          = Modifier
                        .size(56.dp)
                        .alpha(if (isBuffering) 0.5f else 1f)
                        .animateWidth(prevInteraction),
                    ) {
                      Icon(
                        imageVector        = Icons.Outlined.SkipPrevious,
                        contentDescription = null,
                        modifier           = Modifier.size(32.dp),
                      )
                    }
                  },
                  menuContent = {},
                )
              }

              // ── Play / Pause — ToggleFloatingActionButton / buffering ────
              customItem(
                buttonGroupContent = {
                  if (isBuffering) {
                    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                      LoadingIndicator(
                        modifier = Modifier.size(72.dp),
                        color    = MaterialTheme.colorScheme.primary,
                      )
                    }
                  } else {
                    Box(
                      modifier = Modifier
                        .size(92.dp)
                        .glassPanel(CircleShape, hideBackground)
                        .clickable(
                          interactionSource = remember { MutableInteractionSource() },
                          indication        = ripple(bounded = true, radius = 46.dp),
                        ) {
                          resetControlsTimestamp = System.currentTimeMillis()
                          viewModel.pauseUnpause()
                        },
                      contentAlignment = Alignment.Center,
                    ) {
                      AnimatedContent(
                        targetState    = paused == false,
                        transitionSpec = {
                          (scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.6f) + fadeIn()) togetherWith
                            (scaleOut(tween(120), targetScale = 0.6f) + fadeOut(tween(120)))
                        },
                        label = "play_pause_morph",
                      ) { isPlaying ->
                        Icon(
                          imageVector        = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                          contentDescription = null,
                          tint               = MaterialTheme.colorScheme.primary,
                          modifier           = Modifier.size(52.dp),
                        )
                      }
                    }
                  }
                },
                menuContent = {},
              )

              // ── Skip Next ───────────────────────────────────────────────
              if (showSkip) {
                customItem(
                  buttonGroupContent = {
                    FilledTonalIconButton(
                      onClick           = { viewModel.playNext() },
                      enabled           = viewModel.hasNext(),
                      shapes            = skipButtonShapes,
                      interactionSource = nextInteraction,
                      colors            = glassIconButtonColors(hideBackground),
                      modifier          = Modifier
                        .size(56.dp)
                        .alpha(if (isBuffering) 0.5f else 1f)
                        .animateWidth(nextInteraction),
                    ) {
                      Icon(
                        imageVector        = Icons.Outlined.SkipNext,
                        contentDescription = null,
                        modifier           = Modifier.size(32.dp),
                      )
                    }
                  },
                  menuContent = {},
                )
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
              chapters = chapters.toImmutableList(),
              paused = paused ?: false,
              seekbarStyle = seekbarStyle,
              loopStart = abLoopA?.toFloat(),
              loopEnd = abLoopB?.toFloat(),
            )
          }

          AnimatedVisibility(visible = showNextUp, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(), modifier = Modifier.constrainAs(nextUpPill) { bottom.linkTo(parent.bottom, 100.dp); end.linkTo(parent.end, spacing.large) }) {
            NextUpPill(title = nextItemTitle ?: "", onClick = { viewModel.playNext() }, onDismiss = { viewModel.dismissNextUp() })
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

    AnimatedVisibility(visible = sheetShown != Sheets.None, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
      PlayerSheets(viewModel = viewModel, sheetShown = sheetShown, subtitles = subtitles.toImmutableList(), onAddSubtitle = viewModel::addSubtitle, onToggleSubtitle = { id -> if (viewModel.isSubtitleSelected(id)) { MPVLib.setPropertyString("sid", "no"); MPVLib.setPropertyString("secondary-sid", "no") } else { MPVLib.setPropertyInt("sid", id); MPVLib.setPropertyString("secondary-sid", "no") } }, isSubtitleSelected = viewModel::isSubtitleSelected, onRemoveSubtitle = viewModel::removeSubtitle, audioTracks = audioTracks.toImmutableList(), onAddAudio = viewModel::addAudio, onSelectAudio = { if (MPVLib.getPropertyInt("aid") == it.id) MPVLib.setPropertyString("aid", "no") else MPVLib.setPropertyInt("aid", it.id) }, chapter = chapters.getOrNull(currentChapter ?: 0), chapters = chapters.toImmutableList(), onSeekToChapter = { MPVLib.setPropertyInt("chapter", it); viewModel.unpause() }, decoder = decoder, onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) }, speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(), onSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) }, onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) }, onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() }, onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() }, onResetSpeedPresets = playerPreferences.speedPresets::delete, speedPresets = speedPresets.map { it.toFloat() }.sorted(), onResetDefaultSpeed = { MPVLib.setPropertyFloat("speed", playerPreferences.defaultSpeed.deleteAndGet().toFixed(2)) }, sleepTimerTimeRemaining = sleepTimerTimeRemaining, onStartSleepTimer = viewModel::startTimer, onOpenPanel = onOpenPanel, onShowSheet = onOpenSheet, onDismissRequest = { onOpenSheet(Sheets.None) })
    }

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

@Composable
fun NextUpPill(
  title: String,
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic   = LocalHapticFeedback.current
  val spacing  = MaterialTheme.spacing

  // Fire haptic once when the pill appears
  LaunchedEffect(Unit) {
    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
  }

  // SwipeToDismissBox replaces the manual Animatable + detectHorizontalDragGestures block.
  // EndToStart = swipe left-to-right to dismiss (matches the original right-swipe logic).
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      if (value == SwipeToDismissBoxValue.StartToEnd) {
        onDismiss()
        true
      } else {
        false
      }
    },
    positionalThreshold = { totalDistance -> totalDistance * 0.35f },
  )

  SwipeToDismissBox(
    state            = dismissState,
    // No background layer needed — the pill slides away cleanly
    backgroundContent = {},
    modifier          = modifier.padding(spacing.small),
    enableDismissFromStartToEnd = true,
    enableDismissFromEndToStart = false,
  ) {
    Surface(
      modifier = Modifier
        .height(64.dp)
        .widthIn(min = 180.dp, max = 300.dp),
      shape          = CircleShape,
      color          = MaterialTheme.colorScheme.secondaryContainer,
      tonalElevation = 3.dp,
      shadowElevation = 4.dp,
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication        = ripple(),
            onClick           = onClick,
          )
          .padding(horizontal = 20.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
      ) {
        Icon(
          imageVector        = Icons.Outlined.SkipNext,
          contentDescription = null,
          tint               = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier           = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
          modifier              = Modifier.weight(1f),
          verticalArrangement   = Arrangement.Center,
        ) {
          // "NEXT UP" label aligned to M3 labelSmall role —
          // removes the manual ExtraBold + 1.2sp tracking overrides
          Text(
            text  = "NEXT UP",
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
          )
          Text(
            text     = title,
            color    = MaterialTheme.colorScheme.onSecondaryContainer,
            style    = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewNextUpPill() {
  MpvexTheme {
    Box(
      modifier        = Modifier.fillMaxSize().padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(400.dp, 200.dp)
          .background(Brush.linearGradient(colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC6)))),
      )
      NextUpPill(title = "S01 E05 - The Final Stand", onClick = {}, onDismiss = {})
    }
  }
}
