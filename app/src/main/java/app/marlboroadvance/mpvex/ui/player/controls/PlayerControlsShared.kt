package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.VideoAspect
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButton
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButtonType
import app.marlboroadvance.mpvex.ui.theme.controlColor
import dev.vivvvek.seeker.Segment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

@Composable
fun RenderPlayerButton(
  button: PlayerButton,
  chapters: List<Segment>,
  currentChapter: Int?,
  isPortrait: Boolean,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  mediaTitle: String?,
  hideBackground: Boolean,
  decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
  playbackSpeed: Float,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
  buttonSize: Dp = 48.dp,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val buttonShape = RoundedCornerShape(14.dp)
  val glassBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
  
  when (button) {
    PlayerButton.BACK_ARROW -> {
      ControlsButton(
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        onClick = onBackPress,
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
        type = ControlsButtonType.Tonal
      )
    }

    PlayerButton.VIDEO_TITLE -> {
      val playlistModeEnabled = viewModel.hasPlaylistSupport()
      val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      val scale by animateFloatAsState(
          targetValue = if (isPressed) 0.98f else 1f,
          animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
          label = "title_scale"
      )

      Surface(
        modifier =
          modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
              enabled = playlistModeEnabled,
              interactionSource = interactionSource,
              indication = ripple(color = Color.White),
              onClick = {
                clickEvent()
                onOpenSheet(Sheets.Playlist)
              },
            ),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = if (hideBackground) null else glassBorder
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(0.dp),
          modifier =
            Modifier.padding(
              horizontal = 16.dp,
              vertical = 8.dp,
            ),
        ) {
          Text(
            text = mediaTitle ?: "",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Medium,
              letterSpacing = 0.2.sp,
              lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )
          
          viewModel.getPlaylistInfo()?.let { playlistInfo ->
            val parts = playlistInfo.split("/")
            val annotatedString = if (parts.size == 2) {
              buildAnnotatedString {
                withStyle(style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )) {
                  append(parts[0].trim())
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) {
                  append(" / ${parts[1].trim()}")
                }
              }
            } else {
              AnnotatedString(playlistInfo)
            }

            Text(
              text = annotatedString,
              textAlign = TextAlign.Start,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp
              ),
              modifier = Modifier.padding(top = 2.dp),
              maxLines = 1,
              overflow = TextOverflow.Visible,
            )
          }
        }
      }
    }

    PlayerButton.BOOKMARKS_CHAPTERS -> {
      if (chapters.isNotEmpty()) {
        ControlsButton(
          Icons.Outlined.Bookmarks,
          onClick = { onOpenSheet(Sheets.Chapters) },
          color = controlColor,
          modifier = modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.PLAYBACK_SPEED -> {
      val pillShape = RoundedCornerShape(50)
      if (isSpeedNonOne) {
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurfaceVariant
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
            label = "speed_scale"
        )
        
        Surface(
          shape = pillShape,
          color = containerColor,
          contentColor = contentColor,
          border = if (hideBackground) null else glassBorder,
          modifier = modifier
            .height(buttonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pillShape)
            .clickable(
              interactionSource = interactionSource,
              indication = ripple(bounded = true, color = Color.White),
              onClick = {
                clickEvent()
                onOpenSheet(Sheets.PlaybackSpeed)
              },
            ),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Speed,
              contentDescription = "Playback Speed",
              tint = contentColor,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = String.format(Locale.US, "%.2fx", playbackSpeed),
              maxLines = 1,
              style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
              ),
              color = contentColor
            )
          }
        }
      } else {
        ControlsButton(
          icon = Icons.Outlined.Speed,
          onClick = { onOpenSheet(Sheets.PlaybackSpeed) },
          color = controlColor,
          modifier = modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.DECODER -> {
        val pillShape = RoundedCornerShape(50)
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurfaceVariant
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
            label = "decoder_scale"
        )

      Surface(
        shape = pillShape,
        color = containerColor,
        contentColor = contentColor,
        border = if (hideBackground) null else glassBorder,
        modifier = modifier
          .height(buttonSize)
          .graphicsLayer {
              scaleX = scale
              scaleY = scale
          }
          .clip(pillShape)
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = Color.White),
            onClick = {
              clickEvent()
              onOpenSheet(Sheets.Decoders)
            },
          ),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp),
        ) {
          Text(
            text = decoder.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = contentColor
          )
        }
      }
    }

    PlayerButton.SCREEN_ROTATION -> {
      ControlsButton(
        icon = Icons.Outlined.ScreenRotation,
        onClick = viewModel::cycleScreenRotations,
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.FRAME_NAVIGATION -> {
      val isFrameExpanded by viewModel.isFrameNavigationExpanded.collectAsState()
      val isSnapshotLoading by viewModel.isSnapshotLoading.collectAsState()

      AnimatedContent(
        targetState = isFrameExpanded,
        transitionSpec = {
          (fadeIn(animationSpec = tween(200)) + expandHorizontally(animationSpec = tween(250)))
            .togetherWith(fadeOut(animationSpec = tween(200)) + shrinkHorizontally(animationSpec = tween(250)))
            .using(SizeTransform(clip = false))
        },
        label = "FrameNavExpandCollapse",
      ) { expanded ->
        if (expanded) {
          Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            modifier = modifier.height(buttonSize),
            border = glassBorder
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(horizontal = 8.dp)
            ) {
              ControlsButton(
                icon = Icons.Outlined.FastRewind,
                onClick = { viewModel.frameStepBackward() },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp)
              )
              ControlsButton(
                icon = Icons.Outlined.CameraAlt,
                onClick = { viewModel.takeSnapshot(activity) },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp),
                enabled = !isSnapshotLoading
              )
              ControlsButton(
                icon = Icons.Outlined.FastForward,
                onClick = { viewModel.frameStepForward() },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp)
              )
              ControlsButton(
                icon = Icons.Outlined.Close,
                onClick = { (viewModel.isFrameNavigationExpanded as? MutableStateFlow<Boolean>)?.update { false } },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp)
              )
            }
          }
        } else {
          ControlsButton(
            icon = Icons.Outlined.Camera,
            onClick = { (viewModel.isFrameNavigationExpanded as? MutableStateFlow<Boolean>)?.update { true } },
            color = controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape
          )
        }
      }
    }

    PlayerButton.ASPECT_RATIO -> {
      ControlsButton(
        icon = Icons.Outlined.AspectRatio,
        onClick = { onOpenSheet(Sheets.AspectRatios) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.AUDIO_TRACK -> {
      ControlsButton(
        icon = Icons.Outlined.Audiotrack,
        onClick = { onOpenSheet(Sheets.AudioTracks) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.SUBTITLES -> {
      ControlsButton(
        icon = Icons.Outlined.Subtitles,
        onClick = { onOpenSheet(Sheets.SubtitleTracks) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.VIDEO_ZOOM -> {
      ControlsButton(
        icon = if (currentZoom > 1f) Icons.Outlined.ZoomOutMap else Icons.Outlined.ZoomIn,
        onClick = { onOpenSheet(Sheets.VideoZoom) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.PICTURE_IN_PICTURE -> {
      ControlsButton(
        icon = Icons.Outlined.PictureInPictureAlt,
        onClick = { activity.enterPipModeHidingOverlay() },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.MORE_OPTIONS -> {
      ControlsButton(
        icon = Icons.Outlined.MoreVert,
        onClick = { onOpenSheet(Sheets.More) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }
    
    PlayerButton.LOCK_CONTROLS -> {
        ControlsButton(
            icon = Icons.Outlined.LockOpen,
            onClick = { viewModel.lockControls() },
            color = controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.REPEAT_MODE -> {
        val repeatModeState by viewModel.repeatMode.collectAsState()
        val icon = when (repeatModeState) {
            app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF -> Icons.Outlined.Repeat
            app.marlboroadvance.mpvex.ui.player.RepeatMode.ONE -> Icons.Outlined.RepeatOne
            app.marlboroadvance.mpvex.ui.player.RepeatMode.ALL -> Icons.Outlined.RepeatOn
        }
        ControlsButton(
            icon = icon,
            onClick = { viewModel.cycleRepeatMode() },
            color = if (repeatModeState == app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF) controlColor else MaterialTheme.colorScheme.tertiary,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.SHUFFLE -> {
        val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
        ControlsButton(
            icon = if (shuffleEnabled) Icons.Outlined.ShuffleOn else Icons.Outlined.Shuffle,
            onClick = { viewModel.toggleShuffle() },
            color = if (shuffleEnabled) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }
    
    PlayerButton.MIRROR -> {
        val isMirrored by viewModel.isMirrored.collectAsState()
        ControlsButton(
            icon = Icons.Outlined.Flip,
            onClick = { viewModel.toggleMirroring() },
            color = if (isMirrored) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.VERTICAL_FLIP -> {
        val isVerticalFlipped by viewModel.isVerticalFlipped.collectAsState()
        ControlsButton(
            icon = Icons.Outlined.Flip, 
            onClick = { viewModel.toggleVerticalFlip() },
            color = if (isVerticalFlipped) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.AB_LOOP -> {
        val abLoopA by viewModel.abLoopA.collectAsState()
        val abLoopB by viewModel.abLoopB.collectAsState()
        val isLooping = abLoopA != null && abLoopB != null
        
        ControlsButton(
            icon = Icons.Outlined.FitScreen,
            onClick = { 
                when {
                    abLoopA == null -> viewModel.setLoopA()
                    abLoopB == null -> viewModel.setLoopB()
                    else -> viewModel.clearABLoop()
                }
            },
            color = if (isLooping) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    else -> {}
  }
}
