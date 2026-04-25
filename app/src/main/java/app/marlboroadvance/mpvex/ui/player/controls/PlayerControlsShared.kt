package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val buttonShape = CircleShape
  
  when (button) {
    PlayerButton.BACK_ARROW -> {
      ControlsButton(
        icon = Icons.AutoMirrored.Default.ArrowBack,
        onClick = onBackPress,
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
        type = ControlsButtonType.Transparent
      )
    }

    PlayerButton.VIDEO_TITLE -> {
      val playlistModeEnabled = viewModel.hasPlaylistSupport()
      val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f)

      Surface(
        modifier =
          Modifier
            .widthIn(max = 320.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
              enabled = playlistModeEnabled,
              onClick = {
                clickEvent()
                onOpenSheet(Sheets.Playlist)
              },
            ),
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(2.dp),
          modifier =
            Modifier.padding(
              horizontal = 12.dp,
              vertical = 6.dp,
            ),
        ) {
          viewModel.getPlaylistInfo()?.let { playlistInfo ->
            Text(
              text = playlistInfo.uppercase(Locale.US),
              textAlign = TextAlign.Start,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              maxLines = 1,
              overflow = TextOverflow.Visible,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text = mediaTitle ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    PlayerButton.BOOKMARKS_CHAPTERS -> {
      if (chapters.isNotEmpty()) {
        ControlsButton(
          Icons.Default.Bookmarks,
          onClick = { onOpenSheet(Sheets.Chapters) },
          color = controlColor,
          modifier = Modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.PLAYBACK_SPEED -> {
      if (isSpeedNonOne) {
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurfaceVariant
        
        Surface(
          shape = buttonShape,
          color = containerColor,
          contentColor = contentColor,
          modifier = Modifier
            .height(buttonSize)
            .clip(buttonShape)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
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
            modifier = Modifier.padding(horizontal = 12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = "Playback Speed",
              tint = contentColor,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = String.format(Locale.US, "%.2fx", playbackSpeed),
              maxLines = 1,
              style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              ),
              color = contentColor
            )
          }
        }
      } else {
        ControlsButton(
          icon = Icons.Default.Speed,
          onClick = { onOpenSheet(Sheets.PlaybackSpeed) },
          color = controlColor,
          modifier = Modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.DECODER -> {
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurfaceVariant

      Surface(
        shape = buttonShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
          .height(buttonSize)
          .clip(buttonShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = Color.White),
            onClick = {
              clickEvent()
              onOpenSheet(Sheets.Decoders)
            },
          ),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp),
        ) {
          Text(
            text = decoder.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor
          )
        }
      }
    }

    PlayerButton.SCREEN_ROTATION -> {
      ControlsButton(
        icon = Icons.Default.ScreenRotation,
        onClick = viewModel::cycleScreenRotations,
        color = controlColor,
        modifier = Modifier.size(buttonSize),
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
            modifier = Modifier.height(buttonSize)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(horizontal = 8.dp)
            ) {
              ControlsButton(
                icon = Icons.Default.FastRewind,
                onClick = { viewModel.frameStepBackward() },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp)
              )
              ControlsButton(
                icon = Icons.Default.CameraAlt,
                onClick = { viewModel.takeSnapshot(activity) },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp),
                enabled = !isSnapshotLoading
              )
              ControlsButton(
                icon = Icons.Default.FastForward,
                onClick = { viewModel.frameStepForward() },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp)
              )
              ControlsButton(
                icon = Icons.Default.Close,
                onClick = { (viewModel.isFrameNavigationExpanded as? MutableStateFlow<Boolean>)?.update { false } },
                color = controlColor,
                type = ControlsButtonType.Transparent,
                modifier = Modifier.size(36.dp)
              )
            }
          }
        } else {
          ControlsButton(
            icon = Icons.Default.Camera,
            onClick = { (viewModel.isFrameNavigationExpanded as? MutableStateFlow<Boolean>)?.update { true } },
            color = controlColor,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape
          )
        }
      }
    }

    PlayerButton.ASPECT_RATIO -> {
      ControlsButton(
        icon = Icons.Default.AspectRatio,
        onClick = { onOpenSheet(Sheets.AspectRatios) },
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.AUDIO_TRACK -> {
      ControlsButton(
        icon = Icons.Default.Audiotrack,
        onClick = { onOpenSheet(Sheets.AudioTracks) },
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.SUBTITLES -> {
      ControlsButton(
        icon = Icons.Default.Subtitles,
        onClick = { onOpenSheet(Sheets.SubtitleTracks) },
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.VIDEO_ZOOM -> {
      ControlsButton(
        icon = if (currentZoom > 1f) Icons.Default.ZoomOutMap else Icons.Default.ZoomIn,
        onClick = { onOpenSheet(Sheets.VideoZoom) },
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.PICTURE_IN_PICTURE -> {
      ControlsButton(
        icon = Icons.Default.PictureInPictureAlt,
        onClick = { activity.enterPipModeHidingOverlay() },
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.MORE_OPTIONS -> {
      ControlsButton(
        icon = Icons.Default.MoreVert,
        onClick = { onOpenSheet(Sheets.More) },
        color = controlColor,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }
    
    PlayerButton.LOCK_CONTROLS -> {
        ControlsButton(
            icon = Icons.Default.LockOpen,
            onClick = { viewModel.lockControls() },
            color = controlColor,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.REPEAT_MODE -> {
        val repeatModeState by viewModel.repeatMode.collectAsState()
        val icon = when (repeatModeState) {
            app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF -> Icons.Default.Repeat
            app.marlboroadvance.mpvex.ui.player.RepeatMode.ONE -> Icons.Default.RepeatOne
            app.marlboroadvance.mpvex.ui.player.RepeatMode.ALL -> Icons.Default.RepeatOn
        }
        ControlsButton(
            icon = icon,
            onClick = { viewModel.cycleRepeatMode() },
            color = if (repeatModeState == app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF) controlColor else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.SHUFFLE -> {
        val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
        ControlsButton(
            icon = if (shuffleEnabled) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
            onClick = { viewModel.toggleShuffle() },
            color = if (shuffleEnabled) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
        )
    }
    
    PlayerButton.MIRROR -> {
        val isMirrored by viewModel.isMirrored.collectAsState()
        ControlsButton(
            icon = Icons.Default.Flip,
            onClick = { viewModel.toggleMirroring() },
            color = if (isMirrored) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.VERTICAL_FLIP -> {
        val isVerticalFlipped by viewModel.isVerticalFlipped.collectAsState()
        ControlsButton(
            icon = Icons.Default.Flip, 
            onClick = { viewModel.toggleVerticalFlip() },
            color = if (isVerticalFlipped) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.AB_LOOP -> {
        val abLoopA by viewModel.abLoopA.collectAsState()
        val abLoopB by viewModel.abLoopB.collectAsState()
        val isLooping = abLoopA != null && abLoopB != null
        
        ControlsButton(
            icon = Icons.Default.FitScreen,
            onClick = { 
                when {
                    abLoopA == null -> viewModel.setLoopA()
                    abLoopB == null -> viewModel.setLoopB()
                    else -> viewModel.clearABLoop()
                }
            },
            color = if (isLooping) MaterialTheme.colorScheme.tertiary else controlColor,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    else -> {}
  }
}
