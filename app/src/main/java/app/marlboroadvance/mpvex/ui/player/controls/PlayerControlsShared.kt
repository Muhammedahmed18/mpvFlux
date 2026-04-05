package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.VideoAspect
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButton
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButtonType
import app.marlboroadvance.mpvex.ui.player.controls.components.CurrentChapter
import app.marlboroadvance.mpvex.ui.theme.controlColor
import app.marlboroadvance.mpvex.ui.theme.spacing
import dev.vivvvek.seeker.Segment

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
  buttonSize: Dp = 40.dp,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val buttonShape = MaterialTheme.shapes.medium
  
  when (button) {
    PlayerButton.BACK_ARROW -> {
      ControlsButton(
        icon = Icons.AutoMirrored.Default.ArrowBack,
        onClick = onBackPress,
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
        type = ControlsButtonType.Transparent
      )
    }

    PlayerButton.VIDEO_TITLE -> {
      val playlistModeEnabled = viewModel.hasPlaylistSupport()

      Surface(
        modifier =
          Modifier
            .height(buttonSize)
            .widthIn(max = 220.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(
              enabled = playlistModeEnabled,
              onClick = {
                clickEvent()
                onOpenSheet(Sheets.Playlist)
              },
            ),
        shape = MaterialTheme.shapes.large,
        color =
          if (hideBackground) {
            Color.Transparent
          } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(
              alpha = 0.55f,
            )
          },
        contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
          if (hideBackground) {
            null
          } else {
            BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
          },
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
          modifier =
            Modifier.padding(
              horizontal = MaterialTheme.spacing.medium,
              vertical = MaterialTheme.spacing.small,
            ),
        ) {
          viewModel.getPlaylistInfo()?.let { playlistInfo ->
            Text(
              text = playlistInfo,
              textAlign = TextAlign.Center,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Visible,
                            color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = Typography.bullet.toString(),
              textAlign = TextAlign.Center,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
              overflow = TextOverflow.Clip,
            )
          }
          Text(
            text = mediaTitle ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
                        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
      }
    }

    PlayerButton.BOOKMARKS_CHAPTERS -> {
      if (chapters.isNotEmpty()) {
        ControlsButton(
          Icons.Default.Bookmarks,
          onClick = { onOpenSheet(Sheets.Chapters) },
          color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.PLAYBACK_SPEED -> {
      if (isSpeedNonOne) {
        Surface(
          shape = buttonShape,
          color = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          contentColor = if (hideBackground) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
          border = if (hideBackground) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
          ),
          modifier = Modifier
            .height(buttonSize)
            .clip(buttonShape)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true),
              onClick = {
                clickEvent()
                onOpenSheet(Sheets.PlaybackSpeed)
              },
            ),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            modifier = Modifier.padding(
              horizontal = MaterialTheme.spacing.small,
              vertical = MaterialTheme.spacing.small,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = "Playback Speed",
              tint = if (hideBackground) controlColor else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = String.format("%.2fx", playbackSpeed),
              maxLines = 1,
              style = MaterialTheme.typography.labelLarge,
                          )
          }
        }
      } else {
        ControlsButton(
          icon = Icons.Default.Speed,
          onClick = { onOpenSheet(Sheets.PlaybackSpeed) },
          color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.DECODER -> {
      Surface(
        shape = buttonShape,
        color =
          if (hideBackground) {
            Color.Transparent
          } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(
              alpha = 0.55f,
            )
          },
        contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
          if (hideBackground) {
            null
          } else {
            BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
          },
        modifier = Modifier
          .height(buttonSize)
          .clip(buttonShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = {
              clickEvent()
              onOpenSheet(Sheets.Decoders)
            },
          ),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
              ),
        ) {
          Text(
            text = decoder.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
          )
        }
      }
    }

    PlayerButton.SCREEN_ROTATION -> {
      ControlsButton(
        icon = Icons.Default.ScreenRotation,
        onClick = viewModel::cycleScreenRotations,
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.FRAME_NAVIGATION -> {
      val isExpanded by viewModel.isFrameNavigationExpanded.collectAsState()
      val isSnapshotLoading by viewModel.isSnapshotLoading.collectAsState()
      val context = LocalContext.current

      AnimatedContent(
        targetState = isExpanded,
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
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
            border = if (hideBackground) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.height(buttonSize),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp),
            ) {
              // Previous frame button
              Surface(
                shape = buttonShape,
                color = Color.Transparent,
                modifier = Modifier
                  .size(buttonSize - 4.dp)
                  .clip(buttonShape)
                  .clickable(onClick = {
                    viewModel.frameStepBackward()
                    viewModel.resetFrameNavigationTimer()
                  }),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "Previous Frame",
                    tint = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }

              // Camera / Loading button
              if (isSnapshotLoading) {
                Surface(
                  shape = buttonShape,
                  color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                  modifier = Modifier.size(buttonSize - 4.dp),
                ) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp,
                      color = if (hideBackground) controlColor else MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              } else {
                @OptIn(ExperimentalFoundationApi::class)
                Surface(
                  shape = buttonShape,
                  color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                  modifier = Modifier
                    .size(buttonSize - 4.dp)
                    .clip(buttonShape)
                    .combinedClickable(
                      onClick = {
                        viewModel.takeSnapshot(context)
                        viewModel.resetFrameNavigationTimer()
                      },
                      onLongClick = { onOpenSheet(Sheets.FrameNavigation) },
                    ),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.CameraAlt,
                      contentDescription = "Take Screenshot",
                      tint = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }

              // Next frame button
              Surface(
                shape = buttonShape,
                color = Color.Transparent,
                modifier = Modifier
                  .size(buttonSize - 4.dp)
                  .clip(buttonShape)
                  .clickable(onClick = {
                    viewModel.frameStepForward()
                    viewModel.resetFrameNavigationTimer()
                  }),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Next Frame",
                    tint = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        } else {
          // Collapsed: Show camera icon button
          ControlsButton(
            icon = Icons.Default.Camera,
            onClick = viewModel::toggleFrameNavigationExpanded,
            onLongClick = { onOpenSheet(Sheets.FrameNavigation) },
            color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(buttonSize),
            shape = buttonShape,
          )
        }
      }
    }

    PlayerButton.VIDEO_ZOOM -> {
      if (kotlin.math.abs(currentZoom) >= 0.005f) {
        @OptIn(ExperimentalFoundationApi::class)
        Surface(
          shape = buttonShape,
          color = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          contentColor = if (hideBackground) controlColor else MaterialTheme.colorScheme.onPrimaryContainer,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
          border = if (hideBackground) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
          ),
          modifier = Modifier
            .height(buttonSize)
            .clip(buttonShape)
            .combinedClickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true),
              onClick = {
                clickEvent()
                onOpenSheet(Sheets.VideoZoom)
              },
              onLongClick = {
                clickEvent()
                viewModel.resetVideoZoom()
              },
            ),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            modifier = Modifier.padding(
              horizontal = MaterialTheme.spacing.small,
              vertical = MaterialTheme.spacing.small,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.ZoomIn,
              contentDescription = "Video Zoom",
              tint = if (hideBackground) controlColor else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = String.format("%.0f%%", currentZoom * 100),
              maxLines = 1,
              style = MaterialTheme.typography.labelLarge,
                          )
          }
        }
      } else {
        ControlsButton(
          Icons.Default.ZoomIn,
          onClick = {
            clickEvent()
            onOpenSheet(Sheets.VideoZoom)
          },
          onLongClick = { viewModel.resetVideoZoom() },
          color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.PICTURE_IN_PICTURE -> {
      ControlsButton(
        Icons.Default.PictureInPictureAlt,
        onClick = { activity.enterPipModeHidingOverlay() },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.ASPECT_RATIO -> {
      ControlsButton(
        icon =
          when (aspect) {
            VideoAspect.Fit -> Icons.Default.AspectRatio
            VideoAspect.Stretch -> Icons.Default.ZoomOutMap
            VideoAspect.Crop -> Icons.Default.FitScreen
          },
        onClick = {
          when (aspect) {
            VideoAspect.Fit -> viewModel.changeVideoAspect(VideoAspect.Stretch)
            VideoAspect.Stretch -> viewModel.changeVideoAspect(VideoAspect.Crop)
            VideoAspect.Crop -> viewModel.changeVideoAspect(VideoAspect.Fit)
          }
        },
        onLongClick = { onOpenSheet(Sheets.AspectRatios) },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.LOCK_CONTROLS -> {
      ControlsButton(
        Icons.Default.LockOpen,
        onClick = viewModel::lockControls,
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.AUDIO_TRACK -> {
      ControlsButton(
        Icons.Default.Audiotrack,
        onClick = { onOpenSheet(Sheets.AudioTracks) },
        onLongClick = { onOpenPanel(Panels.AudioDelay) },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.SUBTITLES -> {
      ControlsButton(
        Icons.Default.Subtitles,
        onClick = { onOpenSheet(Sheets.SubtitleTracks) },
        onLongClick = { onOpenPanel(Panels.SubtitleDelay) },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.MORE_OPTIONS -> {
      ControlsButton(
        Icons.Default.MoreVert,
        onClick = { onOpenSheet(Sheets.More) },
        onLongClick = { onOpenPanel(Panels.VideoFilters) },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.CURRENT_CHAPTER -> {
      if (isPortrait) {
      } else {
        AnimatedVisibility(
          chapters.getOrNull(currentChapter ?: 0) != null,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          chapters.getOrNull(currentChapter ?: 0)?.let { chapter ->
            CurrentChapter(
              chapter = chapter,
              onClick = { onOpenSheet(Sheets.Chapters) },
            )
          }
        }
      }
    }

    PlayerButton.REPEAT_MODE -> {
      val repeatMode by viewModel.repeatMode.collectAsState()
      val icon = when (repeatMode) {
        app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF -> Icons.Default.Repeat
        app.marlboroadvance.mpvex.ui.player.RepeatMode.ONE -> Icons.Default.RepeatOne
        app.marlboroadvance.mpvex.ui.player.RepeatMode.ALL -> Icons.Default.RepeatOn
      }
      ControlsButton(
        icon = icon,
        onClick = viewModel::cycleRepeatMode,
        type = if (repeatMode == app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF) ControlsButtonType.Tonal else ControlsButtonType.Filled,
        color = if (hideBackground) {
          when (repeatMode) {
            app.marlboroadvance.mpvex.ui.player.RepeatMode.OFF -> controlColor
            else -> MaterialTheme.colorScheme.primary
          }
        } else {
          null // Handled by type
        },
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.CUSTOM_SKIP -> {
      val playerPreferences = org.koin.compose.koinInject<app.marlboroadvance.mpvex.preferences.PlayerPreferences>()
      ControlsButton(
        icon = Icons.Default.FastForward,
        onClick = { viewModel.seekBy(playerPreferences.customSkipDuration.get()) },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.SHUFFLE -> {
      // Only show shuffle button if there's a playlist (more than one video)
      if (viewModel.hasPlaylistSupport()) {
        val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
        ControlsButton(
          icon = if (shuffleEnabled) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
          onClick = viewModel::toggleShuffle,
          type = if (shuffleEnabled) ControlsButtonType.Filled else ControlsButtonType.Tonal,
          color = if (hideBackground) {
            if (shuffleEnabled) MaterialTheme.colorScheme.primary else controlColor
          } else {
            null // Handled by type
          },
          modifier = Modifier.size(buttonSize),
          shape = buttonShape,
        )
      }
    }

    PlayerButton.MIRROR -> {
      val isMirrored by viewModel.isMirrored.collectAsState()
      ControlsButton(
        icon = Icons.Default.Flip,
        onClick = viewModel::toggleMirroring,
        type = if (isMirrored) ControlsButtonType.Filled else ControlsButtonType.Tonal,
        color = if (hideBackground) {
          if (isMirrored) MaterialTheme.colorScheme.primary else controlColor
        } else {
          null // Handled by type
        },
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.VERTICAL_FLIP -> {
      val isVerticalFlipped by viewModel.isVerticalFlipped.collectAsState()
      val vFlipColor = if (hideBackground) {
        if (isVerticalFlipped) MaterialTheme.colorScheme.primary else controlColor
      } else {
        if (isVerticalFlipped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
      }
      Surface(
        shape = buttonShape,
        color = if (hideBackground) {
            Color.Transparent 
        } else {
            if (isVerticalFlipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
        },
        contentColor = vFlipColor,
        border = if (hideBackground || isVerticalFlipped) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
          .size(buttonSize)
          .clip(buttonShape)
          .clickable(onClick = viewModel::toggleVerticalFlip),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Flip,
            contentDescription = "Vertical Flip",
            tint = vFlipColor,
            modifier = Modifier
              .padding(MaterialTheme.spacing.small)
              .size(20.dp)
              .rotate(90f),
          )
        }
      }
    }

    PlayerButton.AB_LOOP -> {
      val isExpanded by viewModel.isABLoopExpanded.collectAsState()
      val loopA by viewModel.abLoopA.collectAsState()
      val loopB by viewModel.abLoopB.collectAsState()

      AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
          (fadeIn(animationSpec = tween(200)) + expandHorizontally(animationSpec = tween(250)))
            .togetherWith(fadeOut(animationSpec = tween(200)) + shrinkHorizontally(animationSpec = tween(250)))
            .using(SizeTransform(clip = false))
        },
        label = "ABLoopExpandCollapse",
      ) { expanded ->
        if (expanded) {
          Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
            border = if (hideBackground) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.height(buttonSize),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp),
            ) {
              // Point A Button - always transparent background
              Surface(
                shape = buttonShape,
                color = if (loopA != null) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                modifier = Modifier
                  .height(buttonSize - 4.dp)
                  .widthIn(min = buttonSize - 4.dp)
                  .clip(buttonShape)
                  .clickable(onClick = { viewModel.setLoopA() }),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = if (loopA != null) viewModel.formatTimestamp(loopA!!) else "A",
                    style = MaterialTheme.typography.labelLarge,
                                        color = if (loopA != null) {
                      MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                      if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = if (loopA != null) 8.dp else 0.dp),
                  )
                }
              }

              // Clear/Close Button - always has background
              Surface(
                shape = buttonShape,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                  .size(buttonSize - 4.dp)
                  .clip(buttonShape)
                  .clickable(onClick = {
                    viewModel.clearABLoop()
                    viewModel.toggleABLoopExpanded()
                  }),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Loop",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }

              // Point B Button - always transparent background
              Surface(
                shape = buttonShape,
                color = if (loopB != null) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                modifier = Modifier
                  .height(buttonSize - 4.dp)
                  .widthIn(min = buttonSize - 4.dp)
                  .clip(buttonShape)
                  .clickable(onClick = { viewModel.setLoopB() }),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = if (loopB != null) viewModel.formatTimestamp(loopB!!) else "B",
                    style = MaterialTheme.typography.labelLarge,
                                        color = if (loopB != null) {
                      MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                      if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = if (loopB != null) 8.dp else 0.dp),
                  )
                }
              }
            }
          }
        } else {
          // Collapsed: Show Autorenew icon
          Surface(
            shape = buttonShape,
            color = if (hideBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
            border = if (hideBackground) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .size(buttonSize)
              .clip(buttonShape)
              .clickable(onClick = viewModel::toggleABLoopExpanded),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Autorenew,
                contentDescription = "AB Loop",
                tint = if (loopA != null && loopB != null) {
                  MaterialTheme.colorScheme.primary
                } else {
                  if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(24.dp),
              )
            }
          }
        }
      }
    }

    PlayerButton.BACKGROUND_PLAYBACK -> {
      ControlsButton(
        icon = Icons.Default.Headset,
        onClick = { activity.triggerBackgroundPlayback() },
        color = if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(buttonSize),
        shape = buttonShape,
      )
    }

    PlayerButton.NONE -> { /* Do nothing */
    }
  }
}
