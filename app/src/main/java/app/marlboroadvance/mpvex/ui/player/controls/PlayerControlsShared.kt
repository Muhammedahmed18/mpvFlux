package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.preferences.getPlayerButtonLabel
import app.marlboroadvance.mpvex.ui.player.VideoAspect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.icon
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.RepeatMode
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.controls.components.CurrentChapter
import dev.vivvvek.seeker.Segment

// ---------------------------------------------------------------------------
// Shared text style for value-display chips (Playback Speed, Decoder, Video Zoom)
// — centralizes the `labelLarge.copy(fontWeight = ExtraBold)` triplet
// ---------------------------------------------------------------------------
@Composable
private fun chipValueLabelStyle() =
  MaterialTheme.typography.labelLargeEmphasized

// ---------------------------------------------------------------------------
// One UI–style translucent "glass" for controls layered over video. MPV draws
// to a SurfaceView, which can't be sampled for a real backdrop blur, so frosted
// glass is approximated with a translucent tonal fill + a soft top highlight edge.
// ---------------------------------------------------------------------------
@Composable
internal fun Modifier.glassPanel(shape: Shape, hideBackground: Boolean): Modifier =
  if (hideBackground) {
    this.clip(shape)
  } else {
    this
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.05f),
          ),
        ),
        shape = shape,
      )
  }

/**
 * Text and icon color for player chrome. On the glass fill it is the theme's onSurface.
 * With "hide button background" on there is no fill, so content sits straight on the
 * (dark-scrimmed) video and must stay light in every theme; in light theme onSurface is
 * near-black and disappears.
 */
@Composable
internal fun playerChromeContentColor(hideBackground: Boolean): Color =
  if (hideBackground) Color.White else MaterialTheme.colorScheme.onSurface

@Composable
internal fun glassIconButtonColors(hideBackground: Boolean) =
  IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = if (hideBackground) {
      Color.Transparent
    } else {
      MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    },
    contentColor = playerChromeContentColor(hideBackground),
  )

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun RenderPlayerButton(
  button: PlayerButton,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 48.dp,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val context    = LocalContext.current

  val configuration = LocalConfiguration.current
  val locale = remember(configuration) {
    ConfigurationCompat.getLocales(configuration)[0] ?: java.util.Locale.getDefault()
  }

  // M3 Expressive shape-morphing: round at rest → squircle-ish on press
  val expressiveShapes = IconButtonDefaults.shapes()

  // Spoken name for TalkBack; same label the layout editor shows for this button.
  val buttonLabel = getPlayerButtonLabel(button)

  when (button) {

    // ------------------------------------------------------------------
    // BACK ARROW — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.BACK_ARROW -> {
      FilledTonalIconButton(
        onClick  = onBackPress,
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = PlayerButton.BACK_ARROW.icon,
          contentDescription = stringResource(R.string.back),
        )
      }
    }

    // ------------------------------------------------------------------
    // VIDEO TITLE — Stable "Now Playing" capsule
    //   28dp pill · neutral frosted glass (scrim + tint + rim) · NOW PLAYING label
    //   (left) + accent counter pill (right) · ExtraBold single-line title (ellipsis
    //   by default, marquee only when it actually overflows) · layer-free press
    //   feedback · NO elevation shadow — hardware shadows show through translucent
    //   fills as a dark rectangular core over the SurfaceView
    //
    //   No always-on graphicsLayer / basicMarquee: MPV draws to a SurfaceView,
    //   and any persistent hardware/offscreen layer composited over it leaks
    //   its rectangular bounds as a faint "box" seam. In the common case this
    //   block is now pure layout + inline draws — nothing for the Surface to
    //   seam against.
    // ------------------------------------------------------------------
    PlayerButton.VIDEO_TITLE -> {
      val playlistModeEnabled = viewModel.hasPlaylistSupport()
      val playlistInfo = viewModel.getPlaylistInfo()

      // "current/total" → Pair<Int, Int> for the counter pill; null when no playlist.
      val playlistCounter = remember(playlistInfo) {
        playlistInfo
          ?.split("/")
          ?.takeIf { it.size == 2 }
          ?.let {
            val cur = it[0].trim().toIntOrNull()
            val tot = it[1].trim().toIntOrNull()
            if (cur != null && tot != null && tot > 0) cur to tot else null
          }
      }

      val capsuleShape = RoundedCornerShape(28.dp)
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      // Frosted-glass press feedback: animate only the translucent neutral tint's
      // alpha (rest → brighter on press) via inline background() draws — no
      // graphicsLayer scale/alpha trick. Nothing allocates a hardware/offscreen
      // layer, so there's no rectangular seam over the SurfaceView. The dark scrim
      // base underneath keeps the small title text legible over any video frame.
      val tintAlpha by animateFloatAsState(
        targetValue   = if (isPressed) 0.60f else 0.45f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness    = Spring.StiffnessLow,
        ),
        label = "video_title_glass_tint",
      )

      Row(
        modifier              = modifier.wrapContentWidth(Alignment.Start),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .clip(capsuleShape)
            .then(
              if (hideBackground) {
                Modifier
              } else {
                // Neutral frosted glass, same recipe as the rest of the chrome: a
                // dark scrim base (legibility) + a translucent surface tint gradient
                // (the glass, brightening on press via tintAlpha) + a white rim-light
                // edge. The accent stays reserved for the counter pill and the play
                // hero, so the capsule reads as supporting glass, not a second focal.
                Modifier
                  .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.40f))
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = tintAlpha),
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                          alpha = (tintAlpha - 0.13f).coerceAtLeast(0f),
                        ),
                      ),
                    ),
                  )
                  .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                      listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f),
                      ),
                    ),
                    shape = capsuleShape,
                  )
              },
            )
            .then(
              if (playlistModeEnabled) {
                Modifier.clickable(
                  interactionSource = interactionSource,
                  indication        = ripple(),
                ) {
                  clickEvent()
                  onOpenSheet(Sheets.Playlist)
                }
              } else {
                Modifier
              },
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.Center,
        ) {
          // Header row: "NOW PLAYING" label + optional counter pill, snug and
          // left-grouped (no fillMaxWidth) so the capsule's width is driven by
          // the title, not stretched to the full available width.
          Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text  = stringResource(R.string.now_playing),
              style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
              color = playerChromeContentColor(hideBackground).copy(alpha = 0.6f),
              maxLines = 1,
            )
            playlistCounter?.let { (current, total) ->
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text  = "$current / $total",
                  style = MaterialTheme.typography.labelSmallEmphasized,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          // Latch overflow once per title: ellipsis first, then switch to
          // marquee. Latch-only (never reset to false here) so the two layout
          // passes can't oscillate; remember is keyed on the title so a new
          // (possibly shorter) title re-evaluates from scratch.
          var isTitleOverflowing by remember(mediaTitle) { mutableStateOf(false) }

          Text(
            text  = mediaTitle ?: "",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = playerChromeContentColor(hideBackground),
            maxLines = 1,
            overflow = if (isTitleOverflowing) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { if (it.hasVisualOverflow) isTitleOverflowing = true },
            modifier = Modifier
              .widthIn(max = 280.dp)
              .then(
                if (isTitleOverflowing) {
                  Modifier.basicMarquee(
                    iterations         = Int.MAX_VALUE,
                    initialDelayMillis = 1500,
                    repeatDelayMillis  = 1500,
                    spacing            = MarqueeSpacing(48.dp),
                  )
                } else {
                  Modifier
                },
              ),
          )
        }
      }
    }

    // ------------------------------------------------------------------
    // All icon buttons below share PlayerIconButton (round tonal button, glass colours,
    // primary container when "on"); value buttons share PlayerValueChip (same height,
    // pill shape). Icons are Material Symbols Rounded, see PlayerButton.iconRes.
    // ------------------------------------------------------------------

    PlayerButton.BOOKMARKS_CHAPTERS -> {
      if (chapters.isNotEmpty()) {
        PlayerIconButton(
          icon           = button.icon,
          onClick        = { onOpenSheet(Sheets.Chapters) },
          hideBackground = hideBackground,
          contentDescription = buttonLabel,
          buttonSize     = buttonSize,
          modifier       = modifier,
        )
      }
    }

    // ------------------------------------------------------------------
    // CURRENT CHAPTER — unchanged (uses its own CurrentChapter component)
    // ------------------------------------------------------------------
    PlayerButton.CURRENT_CHAPTER -> {
      chapters.getOrNull(currentChapter ?: 0)?.let { activeChapter ->
        CurrentChapter(
          chapter  = activeChapter,
          modifier = modifier,
          onClick  = {
            clickEvent()
            onOpenSheet(Sheets.Chapters)
          },
        )
      }
    }

    PlayerButton.PLAYBACK_SPEED -> {
      if (isSpeedNonOne) {
        val speedText = remember(playbackSpeed, locale) {
          String.format(locale, "%.2fx", playbackSpeed)
        }
        PlayerValueChip(
          icon           = button.icon,
          text           = speedText,
          onClick        = { onOpenSheet(Sheets.PlaybackSpeed) },
          hideBackground = hideBackground,
          buttonSize     = buttonSize,
          modifier       = modifier,
          highlighted    = true,
        )
      } else {
        PlayerIconButton(
          icon           = button.icon,
          onClick        = { onOpenSheet(Sheets.PlaybackSpeed) },
          hideBackground = hideBackground,
          contentDescription = buttonLabel,
          buttonSize     = buttonSize,
          modifier       = modifier,
        )
      }
    }

    PlayerButton.SUBTITLES -> {
      // Filled CC icon while a subtitle track is showing.
      val sid by MPVLib.propInt["sid"].collectAsState()
      val subtitlesOn = (sid ?: 0) > 0
      PlayerIconButton(
        icon           = if (subtitlesOn) {
          ImageVector.vectorResource(R.drawable.ic_sym_closed_caption_fill)
        } else {
          button.icon
        },
        onClick        = { onOpenSheet(Sheets.SubtitleTracks) },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.AUDIO_TRACK -> {
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { onOpenSheet(Sheets.AudioTracks) },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    // Tap cycles Fit → Stretch → Crop (as in mpvEx); long-press opens the ratio sheet.
    PlayerButton.ASPECT_RATIO -> {
      val aspect by viewModel.videoAspect.collectAsState()
      val customRatio by viewModel.currentAspectRatio.collectAsState()
      PlayerIconButton(
        icon           = ImageVector.vectorResource(
          when {
            customRatio > 0 -> R.drawable.ic_sym_aspect_ratio
            aspect == VideoAspect.Stretch -> R.drawable.ic_sym_open_in_full
            aspect == VideoAspect.Crop -> R.drawable.ic_sym_fit_screen
            else -> R.drawable.ic_sym_aspect_ratio
          },
        ),
        onClick        = {
          val next = when {
            customRatio > 0 -> VideoAspect.Fit // leave a custom ratio (e.g. 21:9) back to Fit
            aspect == VideoAspect.Fit -> VideoAspect.Stretch
            aspect == VideoAspect.Stretch -> VideoAspect.Crop
            else -> VideoAspect.Fit
          }
          viewModel.changeVideoAspect(next)
        },
        onLongClick    = { onOpenSheet(Sheets.AspectRatios) },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.DECODER -> {
      PlayerValueChip(
        icon           = button.icon,
        text           = remember(decoder, locale) { decoder.title },
        onClick        = { onOpenSheet(Sheets.Decoders) },
        hideBackground = hideBackground,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.PICTURE_IN_PICTURE -> {
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { activity.enterPipModeHidingOverlay() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.SCREEN_ROTATION -> {
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { viewModel.cycleScreenRotations() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.LOCK_CONTROLS -> {
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { viewModel.lockControls() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    // Tap expands to  ‹ previous frame · screenshot · next frame ›  (collapses on its own
    // after a few seconds); long-press opens the frame navigation sheet.
    PlayerButton.FRAME_NAVIGATION -> {
      val isExpanded by viewModel.isFrameNavigationExpanded.collectAsState()
      val isSnapshotLoading by viewModel.isSnapshotLoading.collectAsState()
      if (isExpanded) {
        PlayerButtonGroup(hideBackground = hideBackground, buttonSize = buttonSize, modifier = modifier) {
          PlayerGroupIconButton(
            icon       = ImageVector.vectorResource(R.drawable.ic_sym_chevron_left),
            contentDescription = stringResource(R.string.cd_frame_back),
            onClick    = {
              viewModel.frameStepBackward()
              viewModel.resetFrameNavigationTimer()
            },
            buttonSize = buttonSize,
          )
          if (isSnapshotLoading) {
            Box(modifier = Modifier.size(buttonSize - 8.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
          } else {
            PlayerGroupIconButton(
              icon       = ImageVector.vectorResource(R.drawable.ic_sym_photo_camera),
              contentDescription = stringResource(R.string.cd_take_snapshot),
              onClick    = {
                viewModel.takeSnapshot(context)
                viewModel.resetFrameNavigationTimer()
              },
              buttonSize = buttonSize,
            )
          }
          PlayerGroupIconButton(
            icon       = ImageVector.vectorResource(R.drawable.ic_sym_chevron_right),
            contentDescription = stringResource(R.string.cd_frame_forward),
            onClick    = {
              viewModel.frameStepForward()
              viewModel.resetFrameNavigationTimer()
            },
            buttonSize = buttonSize,
          )
        }
      } else {
        PlayerIconButton(
          icon           = button.icon,
          onClick        = { viewModel.toggleFrameNavigationExpanded() },
          onLongClick    = { onOpenSheet(Sheets.FrameNavigation) },
          hideBackground = hideBackground,
          contentDescription = buttonLabel,
          buttonSize     = buttonSize,
          modifier       = modifier,
        )
      }
    }

    PlayerButton.VIDEO_ZOOM -> {
      PlayerValueChip(
        icon           = button.icon,
        text           = remember(currentZoom, locale) { String.format(locale, "%.0f%%", currentZoom * 100) },
        onClick        = { onOpenSheet(Sheets.VideoZoom) },
        hideBackground = hideBackground,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.SHUFFLE -> {
      val isShuffle by viewModel.shuffleEnabled.collectAsState()
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { viewModel.toggleShuffle() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
        checked        = isShuffle,
      )
    }

    // Cycles OFF → ONE → ALL; highlighted while repeating.
    PlayerButton.REPEAT_MODE -> {
      val repeatMode by viewModel.repeatMode.collectAsState()
      PlayerIconButton(
        icon           = if (repeatMode == RepeatMode.ONE) {
          ImageVector.vectorResource(R.drawable.ic_sym_repeat_one)
        } else {
          button.icon
        },
        onClick        = { viewModel.cycleRepeatMode() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
        checked        = repeatMode != RepeatMode.OFF,
      )
    }

    PlayerButton.MIRROR -> {
      val isMirrored by viewModel.isMirrored.collectAsState()
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { viewModel.toggleMirroring() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
        checked        = isMirrored,
      )
    }

    PlayerButton.VERTICAL_FLIP -> {
      val isVerticalFlipped by viewModel.isVerticalFlipped.collectAsState()
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { viewModel.toggleVerticalFlip() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
        checked        = isVerticalFlipped,
        iconModifier   = Modifier.rotate(90f),
      )
    }

    // Tap expands to  A · ✕ · B ; set A and B to loop that part, ✕ clears and closes.
    PlayerButton.AB_LOOP -> {
      val isExpanded by viewModel.isABLoopExpanded.collectAsState()
      val loopA by viewModel.abLoopA.collectAsState()
      val loopB by viewModel.abLoopB.collectAsState()
      if (isExpanded) {
        PlayerButtonGroup(hideBackground = hideBackground, buttonSize = buttonSize, modifier = modifier) {
          PlayerGroupTextButton(
            text       = loopA?.let { viewModel.formatTimestamp(it) } ?: "A",
            active     = loopA != null,
            onClick    = { viewModel.setLoopA() },
            buttonSize = buttonSize,
          )
          PlayerGroupIconButton(
            icon       = ImageVector.vectorResource(R.drawable.ic_sym_close),
            contentDescription = stringResource(R.string.cd_clear_ab_loop),
            onClick    = {
              viewModel.clearABLoop()
              viewModel.toggleABLoopExpanded()
            },
            buttonSize = buttonSize,
          )
          PlayerGroupTextButton(
            text       = loopB?.let { viewModel.formatTimestamp(it) } ?: "B",
            active     = loopB != null,
            onClick    = { viewModel.setLoopB() },
            buttonSize = buttonSize,
          )
        }
      } else {
        PlayerIconButton(
          icon           = button.icon,
          onClick        = { viewModel.toggleABLoopExpanded() },
          hideBackground = hideBackground,
          contentDescription = buttonLabel,
          buttonSize     = buttonSize,
          modifier       = modifier,
          checked        = loopA != null && loopB != null,
        )
      }
    }

    // Skips ahead by the custom duration from Settings → Player (default 90 s, e.g. an OP).
    PlayerButton.CUSTOM_SKIP -> {
      val playerPreferences = koinInject<PlayerPreferences>()
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { viewModel.seekBy(playerPreferences.customSkipDuration.get()) },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.BACKGROUND_PLAYBACK -> {
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { activity.triggerBackgroundPlayback() },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.MORE_OPTIONS -> {
      PlayerIconButton(
        icon           = button.icon,
        onClick        = { onOpenSheet(Sheets.More) },
        hideBackground = hideBackground,
        contentDescription = buttonLabel,
        buttonSize     = buttonSize,
        modifier       = modifier,
      )
    }

    PlayerButton.BACK_ARROW, PlayerButton.VIDEO_TITLE, PlayerButton.NONE -> {}
  }
}

// ---------------------------------------------------------------------------
// Shared player button styles
// ---------------------------------------------------------------------------

/**
 * Round tonal icon button used by every icon-only player button. [checked] shows the
 * "on" state (primary container). [onLongClick] adds a long-press: M3 icon buttons have
 * no long-click of their own, so presses are watched and a press held past the system
 * long-press time runs [onLongClick] instead of [onClick].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerIconButton(
  icon: ImageVector,
  onClick: () -> Unit,
  hideBackground: Boolean,
  buttonSize: Dp,
  modifier: Modifier = Modifier,
  checked: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  contentDescription: String? = null,
  iconModifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val haptic = LocalHapticFeedback.current
  val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis
  val interactionSource = remember { MutableInteractionSource() }
  var longPressHandled by remember { mutableStateOf(false) }

  if (onLongClick != null) {
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    LaunchedEffect(interactionSource) {
      interactionSource.interactions.collectLatest { interaction ->
        if (interaction is PressInteraction.Press) {
          longPressHandled = false
          delay(longPressTimeout)
          longPressHandled = true
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          clickEvent()
          currentOnLongClick()
        }
      }
    }
  }

  FilledTonalIconButton(
    onClick           = {
      if (longPressHandled) {
        longPressHandled = false
      } else {
        clickEvent()
        onClick()
      }
    },
    modifier          = modifier.size(buttonSize),
    shapes            = IconButtonDefaults.shapes(),
    colors            = if (checked) {
      IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    } else {
      glassIconButtonColors(hideBackground)
    },
    interactionSource = interactionSource,
  ) {
    Icon(
      imageVector        = icon,
      contentDescription = contentDescription,
      modifier           = iconModifier,
    )
  }
}

/**
 * Pill button showing a value next to its icon (speed, decoder, zoom). Same height and
 * glass colours as [PlayerIconButton]; [highlighted] uses the primary container.
 */
@Composable
internal fun PlayerValueChip(
  icon: ImageVector,
  text: String,
  onClick: () -> Unit,
  hideBackground: Boolean,
  buttonSize: Dp,
  modifier: Modifier = Modifier,
  highlighted: Boolean = false,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  Surface(
    onClick      = {
      clickEvent()
      onClick()
    },
    modifier     = modifier.height(buttonSize),
    shape        = CircleShape,
    color        = when {
      highlighted -> MaterialTheme.colorScheme.primaryContainer
      hideBackground -> Color.Transparent
      else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    },
    contentColor = if (highlighted) {
      MaterialTheme.colorScheme.onPrimaryContainer
    } else {
      playerChromeContentColor(hideBackground)
    },
  ) {
    Row(
      modifier              = Modifier.padding(start = 12.dp, end = 16.dp),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
      Text(text = text, style = chipValueLabelStyle(), maxLines = 1)
    }
  }
}

/** Pill container for expanded buttons (frame navigation, A-B loop). */
@Composable
private fun PlayerButtonGroup(
  hideBackground: Boolean,
  buttonSize: Dp,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier     = modifier.height(buttonSize),
    shape        = CircleShape,
    color        = if (hideBackground) {
      Color.Transparent
    } else {
      MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    },
    contentColor = playerChromeContentColor(hideBackground),
  ) {
    Row(
      modifier              = Modifier.padding(horizontal = 4.dp),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      content()
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerGroupIconButton(
  icon: ImageVector,
  contentDescription: String?,
  onClick: () -> Unit,
  buttonSize: Dp,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  IconButton(
    onClick  = {
      clickEvent()
      onClick()
    },
    modifier = Modifier.size(buttonSize - 8.dp),
    shapes   = IconButtonDefaults.shapes(),
  ) {
    Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
  }
}

/** "A" / "B" (or the set time) inside the A-B loop group; tertiary container once set. */
@Composable
private fun PlayerGroupTextButton(
  text: String,
  active: Boolean,
  onClick: () -> Unit,
  buttonSize: Dp,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  Surface(
    onClick      = {
      clickEvent()
      onClick()
    },
    modifier     = Modifier
      .height(buttonSize - 8.dp)
      .widthIn(min = buttonSize - 8.dp),
    shape        = CircleShape,
    color        = if (active) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
    contentColor = if (active) {
      MaterialTheme.colorScheme.onTertiaryContainer
    } else {
      LocalContentColor.current // set by PlayerButtonGroup for the current background mode
    },
  ) {
    Box(
      modifier         = Modifier.padding(horizontal = if (active) 10.dp else 0.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = text, style = chipValueLabelStyle())
    }
  }
}
