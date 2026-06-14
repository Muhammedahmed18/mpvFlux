package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AudioChannels
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.player.controls.glassIconButtonColors
import app.marlboroadvance.mpvex.ui.player.controls.playerControlsEnterAnimationSpec
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

sealed class AudioItem {
  data class Track(val node: TrackNode) : AudioItem()
  data class Header(val title: String) : AudioItem()
}

@Composable
fun AudioTracksSheet(
  tracks: ImmutableList<TrackNode>,
  onSelect: (TrackNode) -> Unit,
  onAddAudioTrack: () -> Unit,
  onOpenDelayPanel: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Orientation branching (side sheet vs bottom sheet) lives in the shared
  // TracksSheetHost. `dismiss` animates the sheet out before clearing state,
  // so e.g. picking an audio channel no longer pops the sheet instantly.
  TracksSheetHost(onDismissRequest = onDismissRequest) { dismiss ->
    AudioTracksSheetContent(
      tracks            = tracks,
      onSelect          = onSelect,
      onAddAudioTrack   = onAddAudioTrack,
      onOpenDelayPanel  = onOpenDelayPanel,
      onDismissRequest  = dismiss,
      modifier          = modifier,
    )
  }
}

/**
 * Inner content of the audio tracks sheet — the AnimatedContent swap between the
 * main view and the channel selection view. Extracted so it can be re-hosted inside
 * either a `ModalBottomSheet` (portrait) or a side-sheet wrapper (landscape).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioTracksSheetContent(
  tracks: ImmutableList<TrackNode>,
  onSelect: (TrackNode) -> Unit,
  onAddAudioTrack: () -> Unit,
  onOpenDelayPanel: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val audioPreferences = koinInject<AudioPreferences>()
  val audioChannels by audioPreferences.audioChannels.collectAsState()
  var isChannelSelectionMode by remember { mutableStateOf(false) }

  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  AnimatedContent(
    targetState = isChannelSelectionMode,
    transitionSpec = {
      // Spring physics shared with the player overlay — see PlayerControls.kt
      if (targetState) {
        slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
          fadeIn(playerControlsEnterAnimationSpec()) togetherWith
          slideOutHorizontally(playerControlsEnterAnimationSpec()) { -it } +
          fadeOut(playerControlsEnterAnimationSpec())
      } else {
        slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
          fadeIn(playerControlsEnterAnimationSpec()) togetherWith
          slideOutHorizontally(playerControlsEnterAnimationSpec()) { it } +
          fadeOut(playerControlsEnterAnimationSpec())
      }
    },
    label = "AudioSheetTransition"
  ) { selectionMode ->
    if (selectionMode) {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium))
      ) {
        // Back arrow + title — same typography as SheetHeader's title line for visual continuity.
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          FilledTonalIconButton(
            onClick  = { isChannelSelectionMode = false },
            shapes   = IconButtonDefaults.shapes(),
            colors   = glassIconButtonColors(hideBackground = false),
            modifier = Modifier.size(40.dp),
          ) {
            Icon(
              imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
            )
          }
          Text(
            text  = "Channels",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(AudioChannels.entries.toTypedArray()) { channel ->
            val isSelected = audioChannels == channel
            TrackSelectableBar(
              id = AudioChannels.entries.indexOf(channel) + 1,
              title = stringResource(channel.title),
              isSelected = isSelected,
              onClick = {
                audioPreferences.audioChannels.set(channel)
                if (channel == AudioChannels.ReverseStereo) {
                  MPVLib.setPropertyString(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
                } else {
                  MPVLib.setPropertyString(AudioChannels.ReverseStereo.property, "")
                }
                MPVLib.setPropertyString(channel.property, channel.value)
                onDismissRequest()
              }
            )
          }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
      }
    } else {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium))
      ) {
        Column {
          SheetHeader(
            title = "Audio",
            trailing = {
              AudioChannelBadge(
                channelName = stringResource(audioChannels.title),
                onClick     = { isChannelSelectionMode = true },
              )
            },
          )

          // Leading: add (intent = "get more audio"). Trailing: configure (timing).
          val leadingAudioActions = remember {
            listOf(
              TrackAction(label = "Add Track", icon = Icons.Default.Add, onClick = onAddAudioTrack),
            )
          }
          val trailingAudioActions = remember {
            listOf(
              TrackAction(label = "Sync Delay", icon = Icons.Default.MoreTime, onClick = onOpenDelayPanel),
            )
          }
          TrackActionsRow(
            leadingActions  = leadingAudioActions,
            trailingActions = trailingAudioActions,
          )
        }

        val audioItems = remember(tracks) {
          val list = mutableListOf<AudioItem>()
          val internal = tracks.filter { it.external != true }
          val external = tracks.filter { it.external == true }

          if (internal.isNotEmpty()) {
            list.add(AudioItem.Header("EMBEDDED"))
            list.addAll(internal.map { AudioItem.Track(it) })
          }

          if (external.isNotEmpty()) {
            list.add(AudioItem.Header("EXTERNAL"))
            list.addAll(external.map { AudioItem.Track(it) })
          }

          list.toImmutableList()
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.medium,
            vertical = MaterialTheme.spacing.small
          ),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(audioItems) { item ->
            when (item) {
              is AudioItem.Track -> {
                val node = item.node
                val externalLabel = stringResource(R.string.generic_external)
                val metadata = remember(node) {
                  mutableListOf<TrackMetadata>().apply {
                    if (!node.codec.isNullOrBlank()) {
                      add(TrackMetadata(node.codec, MetadataType.PRIMARY))
                    }
                    if (node.audioChannels != null) {
                      add(TrackMetadata(node.demuxChannels ?: "${node.audioChannels}CH"))
                    }
                    if (node.external == true) {
                      add(TrackMetadata(externalLabel, MetadataType.WARNING))
                    }
                    if (!node.lang.isNullOrBlank() && node.title?.contains(node.lang, ignoreCase = true) != true) {
                      add(TrackMetadata(node.lang))
                    }
                  }
                }

                TrackSelectableBar(
                  id = node.id,
                  title = getTrackTitle(node),
                  isSelected = node.isSelected,
                  onClick = { onSelect(node) },
                  metadata = metadata
                )
              }
              is AudioItem.Header -> {
                TrackHeaderPill(
                  title = item.title,
                  modifier = Modifier.padding(horizontal = 16.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Audio channel status pill: matches the player's value-chip language —
 * primaryContainer fill, 14dp rounded, leading 6dp dot, ExtraBold tabular-figures
 * label, trailing chevron. Spring-scales 0.97× on press (OxygenOS "soft squish").
 */
@Composable
private fun AudioChannelBadge(
  channelName: String,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue   = if (isPressed) 0.97f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness    = Spring.StiffnessLow,
    ),
    label = "audio_channel_badge_press_scale",
  )

  Surface(
    onClick           = onClick,
    interactionSource = interactionSource,
    shape             = RoundedCornerShape(14.dp),
    color             = MaterialTheme.colorScheme.primaryContainer,
    modifier          = Modifier.graphicsLayer {
      scaleX = pressScale
      scaleY = pressScale
    },
  ) {
    Row(
      modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .background(MaterialTheme.colorScheme.primary, CircleShape),
      )
      Text(
        text  = channelName.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight          = FontWeight.ExtraBold,
          fontFeatureSettings = "tnum",
        ),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Icon(
        imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier           = Modifier.size(14.dp),
        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}
