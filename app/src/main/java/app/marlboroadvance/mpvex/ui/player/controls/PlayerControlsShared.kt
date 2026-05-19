package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOn
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.ShuffleOn
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.ZoomIn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.RepeatMode
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButton
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButtonType
import app.marlboroadvance.mpvex.ui.player.controls.components.CurrentChapter
import app.marlboroadvance.mpvex.ui.theme.controlColor
import dev.vivvvek.seeker.Segment

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
  val buttonShape = CircleShape
  val glassBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
  val context = LocalContext.current
  
  // Use Configuration from LocalConfiguration to ensure observability and fix lint warning
  val configuration = LocalConfiguration.current
  val locale = remember(configuration) {
    ConfigurationCompat.getLocales(configuration)[0] ?: java.util.Locale.getDefault()
  }
  
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
      val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      
      val scale by animateFloatAsState(
          targetValue = if (isPressed) 0.92f else 1f,
          animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessLow
          ),
          label = "title_scale"
      )

      val titleShape = RoundedCornerShape(24.dp)

      Surface(
        modifier =
          modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .widthIn(max = 320.dp)
            .clip(titleShape)
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
        shape = titleShape,
        tonalElevation = 2.dp,
        border = if (hideBackground) null else glassBorder
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Text(
              text = mediaTitle ?: "",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Start
            )

            viewModel.getPlaylistInfo()?.let { playlistInfo ->
              val parts = playlistInfo.split("/")
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.PlaylistPlay,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp).padding(end = 4.dp),
                  tint = MaterialTheme.colorScheme.primary
                )

                val annotatedString = if (parts.size == 2) {
                  buildAnnotatedString {
                    withStyle(style = SpanStyle(
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Black
                    )) {
                      append(parts[0].trim())
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))) {
                      append(" of ${parts[1].trim()}")
                    }
                  }
                } else {
                  AnnotatedString(playlistInfo)
                }

                Text(
                  text = annotatedString,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Start,
                )
              }
            }
          }

          if (playlistModeEnabled) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
              contentDescription = null,
              modifier = Modifier.size(20.dp).padding(start = 8.dp),
              tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

    PlayerButton.CURRENT_CHAPTER -> {
      chapters.getOrNull(currentChapter ?: 0)?.let { activeChapter ->
        CurrentChapter(
          chapter = activeChapter,
          modifier = modifier,
          onClick = { 
            clickEvent()
            onOpenSheet(Sheets.Chapters) 
          }
        )
      }
    }

    PlayerButton.PLAYBACK_SPEED -> {
      val pillShape = CircleShape
      if (isSpeedNonOne) {
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.primary
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "speed_scale"
        )
        
        Surface(
          shape = pillShape,
          color = containerColor,
          contentColor = contentColor,
          tonalElevation = 4.dp,
          border = if (hideBackground) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
            modifier = Modifier.padding(horizontal = 16.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Speed,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = contentColor
            )
            val speedText = remember(playbackSpeed, locale) {
                String.format(locale, "%.2fx", playbackSpeed)
            }
            Text(
                text = speedText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
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

    PlayerButton.SUBTITLES -> {
      ControlsButton(
        Icons.Outlined.Subtitles,
        onClick = { onOpenSheet(Sheets.SubtitleTracks) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.AUDIO_TRACK -> {
      ControlsButton(
        Icons.Outlined.Audiotrack,
        onClick = { onOpenSheet(Sheets.AudioTracks) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.ASPECT_RATIO -> {
      ControlsButton(
        Icons.Outlined.AspectRatio,
        onClick = { onOpenSheet(Sheets.AspectRatios) },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.DECODER -> {
        val pillShape = CircleShape
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "decoder_scale"
        )

        Surface(
            shape = pillShape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 2.dp,
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                val decoderName = remember(decoder, locale) {
                   decoder.title
                }
                Text(
                    text = decoderName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = contentColor
                )
            }
        }
    }

    PlayerButton.PICTURE_IN_PICTURE -> {
      ControlsButton(
        Icons.Outlined.PictureInPictureAlt,
        onClick = { activity.enterPipModeHidingOverlay() },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.SCREEN_ROTATION -> {
      ControlsButton(
        icon = Icons.Outlined.ScreenRotation,
        onClick = { viewModel.cycleScreenRotations() },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.LOCK_CONTROLS -> {
      ControlsButton(
        Icons.Outlined.LockOpen,
        onClick = { viewModel.lockControls() },
        color = controlColor,
        modifier = modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.FRAME_NAVIGATION -> {
        ControlsButton(
            Icons.Outlined.CameraAlt,
            onClick = { viewModel.takeSnapshot(context) },
            color = controlColor,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    PlayerButton.VIDEO_ZOOM -> {
        val pillShape = CircleShape
        val containerColor = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
        val contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "zoom_scale"
        )

        Surface(
            shape = pillShape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 2.dp,
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
                        onOpenSheet(Sheets.VideoZoom)
                    },
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ZoomIn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                val zoomText = remember(currentZoom, locale) {
                    String.format(locale, "%.0f%%", currentZoom * 100)
                }
                Text(
                    text = zoomText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = contentColor
                )
            }
        }
    }
    
    PlayerButton.SHUFFLE -> {
        val isShuffle = viewModel.shuffleEnabled.collectAsState().value
        ControlsButton(
            icon = if (isShuffle) Icons.Outlined.ShuffleOn else Icons.Outlined.Shuffle,
            onClick = { viewModel.toggleShuffle() },
            color = if (isShuffle) MaterialTheme.colorScheme.primary else controlColor,
            type = if (isShuffle) ControlsButtonType.Filled else ControlsButtonType.Tonal,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }
    
    PlayerButton.REPEAT_MODE -> {
        val repeatMode = viewModel.repeatMode.collectAsState().value
        val icon = when (repeatMode) {
            RepeatMode.OFF -> Icons.Outlined.Repeat
            RepeatMode.ONE -> Icons.Outlined.RepeatOne
            RepeatMode.ALL -> Icons.Outlined.RepeatOn
        }
        val isEnabled = repeatMode != RepeatMode.OFF
        
        ControlsButton(
            icon = icon,
            onClick = { viewModel.cycleRepeatMode() },
            color = if (isEnabled) MaterialTheme.colorScheme.primary else controlColor,
            type = if (isEnabled) ControlsButtonType.Filled else ControlsButtonType.Tonal,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }
    
    PlayerButton.MIRROR -> {
        val isMirrored = viewModel.isMirrored.collectAsState().value
        ControlsButton(
            icon = Icons.Outlined.Flip,
            onClick = { viewModel.toggleMirroring() },
            color = if (isMirrored) MaterialTheme.colorScheme.primary else controlColor,
            type = if (isMirrored) ControlsButtonType.Filled else ControlsButtonType.Tonal,
            modifier = modifier.size(buttonSize),
            shape = buttonShape,
        )
    }

    else -> {}
  }
}
