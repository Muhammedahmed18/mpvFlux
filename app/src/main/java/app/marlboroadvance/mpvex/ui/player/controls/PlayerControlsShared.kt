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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
// Shared press-animation spec — replaces the 8+ duplicated spring blocks
// ---------------------------------------------------------------------------
@Composable
private fun pressSpec() = spring<Float>(
  dampingRatio = Spring.DampingRatioMediumBouncy,
  stiffness    = Spring.StiffnessLow,
)

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
    // VIDEO TITLE — Capsule Header
    //   28dp pill · NOW PLAYING label + counter pill · ExtraBold title
    //   with fade-edge marquee · single rounded mini-bar progress
    //   soft bottom shadow · spring press-scale 0.97x
    // ------------------------------------------------------------------
    PlayerButton.VIDEO_TITLE -> {
      val playlistModeEnabled = viewModel.hasPlaylistSupport()
      val playlistInfo = viewModel.getPlaylistInfo()

      // "current/total" → Pair<Int, Int> for the counter pill / mini-bar; null when no playlist.
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
      // OxygenOS-style "soft squish": spring scale, MediumBouncy damping
      val pressScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness    = Spring.StiffnessLow,
        ),
        label = "video_title_press_scale",
      )

      Row(
        modifier              = modifier.wrapContentWidth(Alignment.Start),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .then(
              if (hideBackground) {
                Modifier
              } else {
                Modifier.shadow(elevation = 6.dp, shape = capsuleShape, clip = false)
              },
            )
            .clip(capsuleShape)
            .then(
              if (hideBackground) {
                Modifier
              } else {
                Modifier
                  .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
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
          // Header row: "NOW PLAYING" label (dim) + optional counter pill — sit snug
          // next to each other, no fillMaxWidth + weight Spacer stretching them apart.
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

          var isTitleOverflowing by remember { mutableStateOf(false) }

          Text(
            text  = mediaTitle ?: "",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            onTextLayout = { isTitleOverflowing = it.hasVisualOverflow },
            modifier = Modifier
              .widthIn(max = 280.dp)
              .then(
                if (isTitleOverflowing) {
                  Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                      drawContent()
                      val fadeWidth = 10.dp.toPx().coerceAtMost(size.width / 2f)
                      drawRect(
                        brush = Brush.horizontalGradient(
                          colors = listOf(Color.Transparent, Color.Black),
                          startX = 0f,
                          endX   = fadeWidth,
                        ),
                        blendMode = BlendMode.DstIn,
                      )
                      drawRect(
                        brush = Brush.horizontalGradient(
                          colors = listOf(Color.Black, Color.Transparent),
                          startX = size.width - fadeWidth,
                          endX   = size.width,
                        ),
                        blendMode = BlendMode.DstIn,
                      )
                    }
                } else {
                  Modifier
                },
              )
              .basicMarquee(
                iterations         = Int.MAX_VALUE,
                initialDelayMillis = 1500,
                repeatDelayMillis  = 1500,
                spacing            = MarqueeSpacing(48.dp),
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
