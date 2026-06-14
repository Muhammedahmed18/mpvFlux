package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.components.PlayerSideSheet
import app.marlboroadvance.mpvex.presentation.components.rememberSideSheetWidth
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/**
 * Single source of truth for the soft top-light gradient that traces every glass
 * surface in the sheets — the sheet itself, track rows in idle state, etc.
 * Centralised so a tweak to the highlight feel propagates everywhere.
 */
internal val SheetGlassEdgeBrush: Brush = Brush.verticalGradient(
  listOf(
    Color.White.copy(alpha = 0.25f),
    Color.White.copy(alpha = 0.05f),
  ),
)

enum class MetadataType {
  DEFAULT, PRIMARY, WARNING
}

data class TrackMetadata(
  val text: String,
  val type: MetadataType = MetadataType.DEFAULT
)

@Composable
fun getTrackTitle(track: TrackNode): String {
  val title = if (track.external == true) {
    track.title?.substringBeforeLast(".")
  } else {
    track.title
  }
  
  return if (!title.isNullOrBlank()) {
    title
  } else {
    if (track.type == "audio") {
      stringResource(R.string.player_sheets_chapter_title_substitute_audio, track.id)
    } else {
      stringResource(R.string.player_sheets_chapter_title_substitute_subtitle, track.id)
    }
  }
}

/**
 * Shared orientation-aware host for every track-style sheet in the player —
 * single source of truth, used by both the subtitles and audio sheets (the
 * branching below was previously copy-pasted in each).
 *
 *  - Landscape → right-anchored [PlayerSideSheet] so the video stays watchable.
 *  - Portrait  → canonical M3 [ModalBottomSheet] with built-in drag handle.
 *
 * Each host fully owns its entrance/exit animation. The content slot receives a
 * `dismiss` lambda that animates the sheet out **before** clearing the sheet
 * state — content should call it for programmatic dismissal (e.g. after the user
 * picks an option) instead of the raw `onDismissRequest`, which pops instantly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TracksSheetHost(
  onDismissRequest: () -> Unit,
  content: @Composable (dismiss: () -> Unit) -> Unit,
) {
  val isLandscape =
    LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

  if (isLandscape) {
    PlayerSideSheet(
      onDismissRequest = onDismissRequest,
      sheetWidth       = rememberSideSheetWidth(),
      surfaceColor     = MaterialTheme.colorScheme.surfaceContainerLow,
      content          = content,
    )
  } else {
    // Partial expansion kept (no skipPartiallyExpanded): the sheet opens at
    // half height so the video stays visible while picking a track; drag up
    // reveals the rest of the list at the capped full height.
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    // Even fully expanded the sheet stops at ~75% of the screen, so it never
    // reads as a full-screen takeover of the player.
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.75f
    ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      sheetState       = sheetState,
      // Translucent "glass" container — MPV's SurfaceView can't be backdrop-blurred,
      // so glass is approximated by letting the video glow through a high-alpha
      // surface tint, matching the translucent track rows it hosts.
      containerColor   = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
      dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
      Box(modifier = Modifier.heightIn(max = maxSheetHeight)) {
        content {
          scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        }
      }
    }
  }
}

@Composable
fun <T> GenericTracksSheet(
  tracks: ImmutableList<T>,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  lazyListState: LazyListState? = null,
  header: @Composable () -> Unit = {},
  track: @Composable (T) -> Unit = {},
  footer: @Composable () -> Unit = {},
) {
  TracksSheetHost(onDismissRequest = onDismissRequest) { _ ->
    GenericTracksSheetContent(
      tracks        = tracks,
      modifier      = modifier,
      lazyListState = lazyListState,
      header        = header,
      track         = track,
      footer        = footer,
    )
  }
}

/**
 * Inner content of the tracks sheet — header + scrollable track list + footer.
 * Extracted so it can be re-hosted inside either a `ModalBottomSheet` (portrait)
 * or a side-sheet wrapper (landscape).
 */
@Composable
private fun <T> GenericTracksSheetContent(
  tracks: ImmutableList<T>,
  modifier: Modifier = Modifier,
  lazyListState: LazyListState? = null,
  header: @Composable () -> Unit = {},
  track: @Composable (T) -> Unit = {},
  footer: @Composable () -> Unit = {},
) {
  val listState = lazyListState ?: rememberLazyListState()
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium))
  ) {
    header()
    LazyColumn(
      // fill = true so the list takes all remaining vertical space inside the
      // sheet, giving room to scroll all tracks regardless of sheet height.
      state = listState,
      modifier = Modifier.weight(1f),
      contentPadding = PaddingValues(
        horizontal = MaterialTheme.spacing.medium,
        vertical = MaterialTheme.spacing.small
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(tracks) {
        track(it)
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.medium)
    ) {
      footer()
    }
  }
}

/**
 * Sheet header. Renders the title (`headlineSmall ExtraBold`) plus an optional
 * dim eyebrow line and an optional trailing slot.
 *
 *   label  → small `labelSmall` `1.sp`-tracking eyebrow line above the title.
 *            Omit (or pass `null`) when the eyebrow would just repeat the title.
 *   trailing → composable slot at the end of the title row (e.g. the Audio
 *              sheet's channel-status badge).
 */
@Composable
fun SheetHeader(
  title: String,
  modifier: Modifier = Modifier,
  label: String? = null,
  trailing: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        top   = MaterialTheme.spacing.medium,
        start = MaterialTheme.spacing.medium,
        end   = MaterialTheme.spacing.medium,
      ),
    verticalAlignment     = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f, fill = false)) {
      if (label != null) {
        Text(
          text  = label,
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(2.dp))
      }
      Text(
        text  = title,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    trailing?.invoke()
  }
}

/**
 * Compact count chip — "4 TRACKS" — sits in `SheetHeader`'s trailing slot.
 * Same 8dp rounded mini-tag visual language as `TrackMetadataTag`, so the
 * counter reads as a piece of metadata about the list rather than another control.
 */
@Composable
fun TrackCountPill(count: Int) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Text(
      text     = "$count TRACK${if (count != 1) "S" else ""}",
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      style    = MaterialTheme.typography.labelSmall.copy(
        fontWeight          = FontWeight.Medium,
        letterSpacing       = 0.5.sp,
        fontFeatureSettings = "tnum",
      ),
      color    = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * Section header divider — One UI–style label flanked by two short rules.
 * Replaces the previous Surface pill so groups read as clean breaks rather
 * than another tappable-looking container.
 *
 * `[─── 24dp ───]  LABEL  [─── 24dp ───]`
 */
@Composable
fun TrackHeaderPill(
    title: String,
    modifier: Modifier = Modifier,
) {
    val ruleColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(1.dp)
                .background(ruleColor),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(1.dp)
                .background(ruleColor),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackSelectableBar(
  id: Int,
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  metadata: List<TrackMetadata> = emptyList(),
  trailingContent: @Composable (() -> Unit)? = null,
  badge: (@Composable () -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  // E1: shape morph 20dp → 28dp on press, scale 1.0 → 0.98 on press — OxygenOS "settle"
  val cornerRadius by animateDpAsState(
    targetValue   = if (isPressed) 28.dp else 20.dp,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
    label         = "track_row_corner_radius",
  )
  val pressScale by animateFloatAsState(
    targetValue   = if (isPressed) 0.98f else 1f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
    label         = "track_row_press_scale",
  )

  val shape = RoundedCornerShape(cornerRadius)

  // E1: glass idle / primaryContainer selected
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
  }
  val borderWidth = if (isSelected) 1.5.dp else 1.dp
  val borderBrush = if (isSelected) {
    SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
  } else {
    SheetGlassEdgeBrush
  }

  Surface(
    onClick           = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    interactionSource = interactionSource,
    modifier          = modifier
      .fillMaxWidth()
      .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
      .border(borderWidth, borderBrush, shape),
    shape             = shape,
    color             = containerColor,
  ) {
    Row(
      modifier              = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // E2: Circular badge slot — overridden by `badge` lambda when callers need a
      // custom icon (e.g. SubtitlesOff / channel mode). Default is a 24dp circular
      // ID chip with tabular figures.
      if (badge != null) {
        badge()
      } else {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
              if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
              },
            ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text  = id.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight          = FontWeight.Bold,
              fontFeatureSettings = "tnum",
            ),
            color = if (isSelected) {
              MaterialTheme.colorScheme.onPrimary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }
      }

      // E3: Title + metadata column
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text       = title,
          style      = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
          ),
          color      = if (isSelected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines   = 2,
          overflow   = TextOverflow.Ellipsis,
        )

        if (metadata.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp),
          ) {
            metadata.forEach { data ->
              TrackMetadataTag(data, isSelected)
            }
          }
        }
      }

      // E4: Selection indicator — AnimatedContent morph between ring and filled-check disc
      if (isSelected || trailingContent == null) {
        AnimatedContent(
          targetState = isSelected,
          transitionSpec = {
            (scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) + fadeIn()) togetherWith
              (scaleOut(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) + fadeOut())
          },
          label = "track_row_selection_indicator",
        ) { selected ->
          if (selected) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onPrimary,
                modifier           = Modifier.size(16.dp),
              )
            }
          } else {
            Box(
              modifier = Modifier
                .size(24.dp)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
            )
          }
        }
      }

      if (trailingContent != null) {
        trailingContent()
      }
    }
  }
}

@Composable
fun TrackMetadataTag(
  metadata: TrackMetadata,
  isSelected: Boolean,
) {
  // Map to the player's container/onContainer token pairs — drops opacity literals
  val (backgroundColor, contentColor) = when (metadata.type) {
    MetadataType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    MetadataType.WARNING -> MaterialTheme.colorScheme.errorContainer  to MaterialTheme.colorScheme.onErrorContainer
    MetadataType.DEFAULT -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
  }

  // Mini-tag: 8dp rounded, no border, compact padding, Medium weight (drops the old Black)
  Surface(
    color = backgroundColor,
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text       = metadata.text.uppercase(),
      modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
      style      = MaterialTheme.typography.labelSmall.copy(
        fontSize      = 10.sp,
        letterSpacing = 0.5.sp,
        fontWeight    = FontWeight.Medium,
      ),
      color      = if (isSelected && metadata.type == MetadataType.DEFAULT) {
        MaterialTheme.colorScheme.primary
      } else {
        contentColor
      },
    )
  }
}

data class TrackAction(
  val label: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
)

/**
 * Action toolbar with two slots — leading actions pinned to the start, trailing
 * pinned to the end, separated by a flexible spacer. Each button is a 40dp icon-only
 * `FilledTonalIconButton` carrying its **own** tonal background; there is no
 * unifying glass wrapper. The button morphs from a 12dp-rounded square at rest
 * to a full circle on press. Labels are revealed via `TooltipBox` on long-press.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrackActionsRow(
  modifier: Modifier = Modifier,
  leadingActions: List<TrackAction> = emptyList(),
  trailingActions: List<TrackAction> = emptyList(),
) {
  if (leadingActions.isEmpty() && trailingActions.isEmpty()) return

  Row(
    modifier              = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
    verticalAlignment     = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
  ) {
    leadingActions.forEach { ActionChip(it) }
    Spacer(modifier = Modifier.weight(1f))
    trailingActions.forEach { ActionChip(it) }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionChip(action: TrackAction) {
  val haptic = LocalHapticFeedback.current
  val chipShapes = IconButtonDefaults.shapes(
    shape        = RoundedCornerShape(12.dp),
    pressedShape = CircleShape,
  )
  val tooltipState = rememberTooltipState()
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip          = { PlainTooltip { Text(action.label) } },
    state            = tooltipState,
  ) {
    FilledTonalIconButton(
      onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        action.onClick()
      },
      modifier = Modifier.size(40.dp),
      shapes   = chipShapes,
    ) {
      Icon(
        imageVector        = action.icon,
        contentDescription = action.label,
        modifier           = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun AddTrackRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
  ) {
    FilledTonalButton(
      onClick = onClick,
      modifier = Modifier.weight(1f),
      shape = MaterialTheme.shapes.extraLarge,
      contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium, vertical = 12.dp)
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      actions()
    }
  }
}
