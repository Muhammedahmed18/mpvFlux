package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOn
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.ShuffleOn
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
  MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)

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

@Composable
internal fun glassIconButtonColors(hideBackground: Boolean) =
  IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = if (hideBackground) {
      Color.Transparent
    } else {
      MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    },
    contentColor = MaterialTheme.colorScheme.onSurface,
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
          imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = null,
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
              text  = "NOW PLAYING",
              style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
              maxLines = 1,
            )
            playlistCounter?.let { (current, total) ->
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text  = "$current / $total",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
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
    // BOOKMARKS / CHAPTERS — FilledTonalIconButton (hidden when no chapters)
    // ------------------------------------------------------------------
    PlayerButton.BOOKMARKS_CHAPTERS -> {
      if (chapters.isNotEmpty()) {
        FilledTonalIconButton(
          onClick  = {
            clickEvent()
            onOpenSheet(Sheets.Chapters)
          },
          modifier = modifier.size(buttonSize),
          shapes   = expressiveShapes,
        ) {
          Icon(
            imageVector        = Icons.Outlined.Bookmarks,
            contentDescription = null,
          )
        }
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

    // ------------------------------------------------------------------
    // PLAYBACK SPEED
    //   inactive → SuggestionChip (icon only, neutral)
    //   active   → SuggestionChip with primary tint + speed value
    // ------------------------------------------------------------------
    PlayerButton.PLAYBACK_SPEED -> {
      val speedText = remember(playbackSpeed, locale) {
        String.format(locale, "%.2fx", playbackSpeed)
      }

      if (isSpeedNonOne) {
        SuggestionChip(
          onClick = {
            clickEvent()
            onOpenSheet(Sheets.PlaybackSpeed)
          },
          label   = {
            Text(
              text  = speedText,
              style = chipValueLabelStyle(),
            )
          },
          modifier    = modifier,
          icon        = {
            Icon(
              imageVector        = Icons.Outlined.Speed,
              contentDescription = null,
              modifier           = Modifier.size(18.dp),
            )
          },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor    = MaterialTheme.colorScheme.primaryContainer,
            labelColor        = MaterialTheme.colorScheme.onPrimaryContainer,
            iconContentColor  = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          elevation = SuggestionChipDefaults.suggestionChipElevation(elevation = 2.dp),
        )
      } else {
        // Neutral chip when speed == 1x
        FilledTonalIconButton(
          onClick  = {
            clickEvent()
            onOpenSheet(Sheets.PlaybackSpeed)
          },
          modifier = modifier.size(buttonSize),
          shapes   = expressiveShapes,
        ) {
          Icon(
            imageVector        = Icons.Outlined.Speed,
            contentDescription = null,
          )
        }
      }
    }

    // ------------------------------------------------------------------
    // SUBTITLES — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.SUBTITLES -> {
      FilledTonalIconButton(
        onClick  = {
          clickEvent()
          onOpenSheet(Sheets.SubtitleTracks)
        },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.Subtitles,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // AUDIO TRACK — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.AUDIO_TRACK -> {
      FilledTonalIconButton(
        onClick  = {
          clickEvent()
          onOpenSheet(Sheets.AudioTracks)
        },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.Audiotrack,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // ASPECT RATIO — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.ASPECT_RATIO -> {
      FilledTonalIconButton(
        onClick  = {
          clickEvent()
          onOpenSheet(Sheets.AspectRatios)
        },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.AspectRatio,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // DECODER — SuggestionChip with text label
    // ------------------------------------------------------------------
    PlayerButton.DECODER -> {
      val decoderName = remember(decoder, locale) { decoder.title }

      SuggestionChip(
        onClick = {
          clickEvent()
          onOpenSheet(Sheets.Decoders)
        },
        label   = {
          Text(
            text  = decoderName,
            style = chipValueLabelStyle(),
          )
        },
        modifier = modifier,
        icon     = {
          Icon(
            imageVector        = Icons.Outlined.Memory,
            contentDescription = null,
            modifier           = Modifier.size(18.dp),
          )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor   = if (hideBackground)
            Color.Transparent
          else
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
          labelColor       = MaterialTheme.colorScheme.onSurface,
          iconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = SuggestionChipDefaults.suggestionChipElevation(
          elevation = if (hideBackground) 0.dp else 2.dp,
        ),
      )
    }

    // ------------------------------------------------------------------
    // PICTURE IN PICTURE — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.PICTURE_IN_PICTURE -> {
      FilledTonalIconButton(
        onClick  = { activity.enterPipModeHidingOverlay() },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.PictureInPictureAlt,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // SCREEN ROTATION — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.SCREEN_ROTATION -> {
      FilledTonalIconButton(
        onClick  = { viewModel.cycleScreenRotations() },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.ScreenRotation,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // LOCK CONTROLS — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.LOCK_CONTROLS -> {
      FilledTonalIconButton(
        onClick  = { viewModel.lockControls() },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.LockOpen,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // FRAME NAVIGATION / SNAPSHOT — FilledTonalIconButton
    // ------------------------------------------------------------------
    PlayerButton.FRAME_NAVIGATION -> {
      FilledTonalIconButton(
        onClick  = { viewModel.takeSnapshot(context) },
        modifier = modifier.size(buttonSize),
        shapes   = expressiveShapes,
        colors   = glassIconButtonColors(hideBackground),
      ) {
        Icon(
          imageVector        = Icons.Outlined.CameraAlt,
          contentDescription = null,
        )
      }
    }

    // ------------------------------------------------------------------
    // VIDEO ZOOM — SuggestionChip with zoom % value
    // ------------------------------------------------------------------
    PlayerButton.VIDEO_ZOOM -> {
      val zoomText = remember(currentZoom, locale) {
        String.format(locale, "%.0f%%", currentZoom * 100)
      }

      SuggestionChip(
        onClick = {
          clickEvent()
          onOpenSheet(Sheets.VideoZoom)
        },
        label   = {
          Text(
            text  = zoomText,
            style = chipValueLabelStyle(),
          )
        },
        modifier = modifier,
        icon     = {
          Icon(
            imageVector        = Icons.Outlined.ZoomIn,
            contentDescription = null,
            modifier           = Modifier.size(18.dp),
          )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor   = if (hideBackground)
            Color.Transparent
          else
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
          labelColor       = MaterialTheme.colorScheme.onSurface,
          iconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = SuggestionChipDefaults.suggestionChipElevation(
          elevation = if (hideBackground) 0.dp else 2.dp,
        ),
      )
    }

    // ------------------------------------------------------------------
    // SHUFFLE — ElevatedFilterChip (selected state = M3 checked visual)
    // ------------------------------------------------------------------
    PlayerButton.SHUFFLE -> {
      val isShuffle by viewModel.shuffleEnabled.collectAsState()

      ElevatedFilterChip(
        selected = isShuffle,
        onClick  = {
          clickEvent()
          viewModel.toggleShuffle()
        },
        label    = {
          Icon(
            imageVector        = if (isShuffle) Icons.Outlined.ShuffleOn else Icons.Outlined.Shuffle,
            contentDescription = null,
            modifier           = Modifier.size(20.dp),
          )
        },
        modifier = modifier.size(buttonSize),
        colors   = FilterChipDefaults.elevatedFilterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 2.dp),
      )
    }

    // ------------------------------------------------------------------
    // REPEAT MODE — ElevatedFilterChip cycling OFF → ONE → ALL
    // ------------------------------------------------------------------
    PlayerButton.REPEAT_MODE -> {
      val repeatMode by viewModel.repeatMode.collectAsState()
      val isEnabled  = repeatMode != RepeatMode.OFF
      val icon = when (repeatMode) {
        RepeatMode.OFF -> Icons.Outlined.Repeat
        RepeatMode.ONE -> Icons.Outlined.RepeatOne
        RepeatMode.ALL -> Icons.Outlined.RepeatOn
      }

      ElevatedFilterChip(
        selected = isEnabled,
        onClick  = {
          clickEvent()
          viewModel.cycleRepeatMode()
        },
        label    = {
          Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(20.dp),
          )
        },
        modifier = modifier.size(buttonSize),
        colors   = FilterChipDefaults.elevatedFilterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 2.dp),
      )
    }

    // ------------------------------------------------------------------
    // MIRROR — ElevatedFilterChip
    // ------------------------------------------------------------------
    PlayerButton.MIRROR -> {
      val isMirrored by viewModel.isMirrored.collectAsState()

      ElevatedFilterChip(
        selected = isMirrored,
        onClick  = {
          clickEvent()
          viewModel.toggleMirroring()
        },
        label    = {
          Icon(
            imageVector        = Icons.Outlined.Flip,
            contentDescription = null,
            modifier           = Modifier.size(20.dp),
          )
        },
        modifier = modifier.size(buttonSize),
        colors   = FilterChipDefaults.elevatedFilterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 2.dp),
      )
    }

    else -> {}
  }
}
