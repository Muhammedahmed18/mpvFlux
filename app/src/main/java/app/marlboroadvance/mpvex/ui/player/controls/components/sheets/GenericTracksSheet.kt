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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
 * Single source of truth for every size, alpha and motion value used by the player sheets
 * (Chapters, Audio, Subtitles, Decoders, ...). Tweak a value here and every sheet follows.
 */
internal object SheetDimens {
  // --- Overlay host -------------------------------------------------------
  const val SheetMaxHeightFraction = 0.75f
  const val SheetContainerAlpha = 0.85f

  // --- Glass edge ---------------------------------------------------------
  const val GlassEdgeTopAlpha = 0.25f
  const val GlassEdgeBottomAlpha = 0.05f

  // --- List ---------------------------------------------------------------
  val ListItemSpacing = 6.dp

  // --- Header -------------------------------------------------------------
  val HeaderLabelGap = 2.dp
  val HeaderBackGap = 10.dp
  const val HeaderLabelAlpha = 0.6f
  const val HeaderLabelLetterSpacing = 1f // sp

  // --- Section divider ("EMBEDDED" / "EXTERNAL") --------------------------
  val SectionPaddingTop = 12.dp
  val SectionPaddingBottom = 2.dp
  val SectionPaddingHorizontal = 12.dp
  val SectionRuleWidth = 24.dp
  val SectionRuleThickness = 1.dp
  val SectionRuleGap = 10.dp
  const val SectionRuleAlpha = 0.3f
  const val SectionLabelAlpha = 0.6f

  // --- Track row ----------------------------------------------------------
  val RowPaddingHorizontal = 12.dp
  val RowPaddingVertical = 9.dp
  val RowMinHeight = 48.dp
  val RowContentGap = 10.dp
  val RowCornerRadius = 16.dp
  val RowCornerRadiusPressed = 22.dp
  val RowBorderWidth = 1.dp
  val RowBorderWidthSelected = 1.5.dp
  const val RowPressScale = 0.98f
  const val RowDisabledAlpha = 0.5f
  const val RowSelectedContainerAlpha = 0.35f
  const val RowIdleContainerAlpha = 0.4f
  const val RowSelectedBorderAlpha = 0.3f
  const val RowSpringDamping = 0.7f
  const val IndicatorSpringDamping = 0.6f
  val RowTitleMetadataGap = 4.dp
  val RowMetadataGap = 4.dp

  // --- Round badges / selection indicator ---------------------------------
  val BadgeSize = 20.dp
  val BadgeIconSize = 12.dp
  val CheckIconSize = 14.dp
  val RingStroke = 1.5.dp
  const val BadgeIdleAlpha = 0.12f
  const val RingAlpha = 0.4f

  // --- Metadata tag -------------------------------------------------------
  val TagPaddingHorizontal = 6.dp
  val TagPaddingVertical = 1.dp
  const val TagFontSize = 9f // sp
  const val TagLetterSpacing = 0.5f // sp

  // --- Count pill ---------------------------------------------------------
  val CountPillPaddingHorizontal = 6.dp
  val CountPillPaddingVertical = 2.dp
  const val CountPillLetterSpacing = 0.5f // sp

  // --- Status badge (chapter count, audio channels) -----------------------
  val StatusBadgeCorner = 10.dp
  val StatusBadgePaddingHorizontal = 8.dp
  val StatusBadgePaddingVertical = 4.dp
  val StatusBadgeGap = 4.dp
  val StatusBadgeDotSize = 5.dp
  val StatusBadgeChevronSize = 12.dp
  const val StatusBadgePressScale = 0.97f

  // --- Action buttons -----------------------------------------------------
  val ActionButtonSize = 36.dp
  val ActionIconSize = 18.dp
  val AddButtonPaddingVertical = 10.dp
  val AddButtonIconSize = 18.dp

  // --- Trailing action inside a row (e.g. remove subtitle) ----------------
  val TrailingActionSize = 28.dp
  val TrailingIconSize = 18.dp
  const val TrailingIconAlpha = 0.8f
}

/**
 * Single source of truth for the soft top-light gradient that traces every glass
 * surface in the sheets — the sheet itself, track rows in idle state, etc.
 * Centralised so a tweak to the highlight feel propagates everywhere.
 */
internal val SheetGlassEdgeBrush: Brush = Brush.verticalGradient(
  listOf(
    Color.White.copy(alpha = SheetDimens.GlassEdgeTopAlpha),
    Color.White.copy(alpha = SheetDimens.GlassEdgeBottomAlpha),
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
 * Animated dismiss of the surrounding [PlayerOverlayHost]. Content calls it for
 * programmatic dismissal (e.g. after picking an option) so the overlay slides out
 * before the state is cleared, instead of popping instantly.
 */
internal val LocalOverlayDismiss = staticCompositionLocalOf<() -> Unit> { {} }

/** Plain holder (not snapshot state) recording the orientation the overlay opened in. */
private class OrientationTracker(val opened: Boolean) {
  var rotated: Boolean = false
}

/**
 * Single orientation-aware host for every sheet and panel the player shows in a
 * sheet-style overlay — one source of truth for anchoring, shape, scrim, insets,
 * motion and dismissal.
 *
 *  - Landscape → right-anchored [PlayerSideSheet] so the video stays watchable.
 *  - Portrait  → canonical M3 [ModalBottomSheet] with built-in drag handle.
 *
 * The host owns its entrance/exit animation. The content slot receives a `dismiss`
 * lambda (also exposed as [LocalOverlayDismiss]) that animates the overlay out
 * **before** [onDismissRequest] clears the state; the raw `onDismissRequest` pops
 * instantly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerOverlayHost(
  onDismissRequest: () -> Unit,
  content: @Composable (dismiss: () -> Unit) -> Unit,
) {
  val useSideSheet =
    LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

  // Remembered at host level, so it survives the portrait <-> landscape branch switch.
  // Once the orientation differs from the one the overlay opened in, the overlay was
  // already on screen: the side sheet then starts open instead of replaying its entrance.
  val orientationTracker = remember { OrientationTracker(useSideSheet) }
  if (orientationTracker.opened != useSideSheet) orientationTracker.rotated = true

  if (useSideSheet) {
    PlayerSideSheet(
      onDismissRequest = onDismissRequest,
      sheetWidth       = rememberSideSheetWidth(),
      surfaceColor     = MaterialTheme.colorScheme.surfaceContainerLow,
      animateEntrance  = !orientationTracker.rotated,
    ) { dismiss ->
      CompositionLocalProvider(LocalOverlayDismiss provides dismiss) { content(dismiss) }
    }
  } else {
    // Partial expansion kept (no skipPartiallyExpanded): the sheet opens at
    // half height so the video stays visible while picking a track; drag up
    // reveals the rest of the list at the capped full height.
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    // Even fully expanded the sheet stops short of the screen height, so it never
    // reads as a full-screen takeover of the player.
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * SheetDimens.SheetMaxHeightFraction
    // One-shot: a second tap/drag while hide() is running must not start another hide
    // or call onDismissRequest twice.
    val dismissing = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val dismiss: () -> Unit = {
      if (dismissing.compareAndSet(false, true)) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
      }
    }
    ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      sheetState       = sheetState,
      // Translucent "glass" container — MPV's SurfaceView can't be backdrop-blurred,
      // so glass is approximated by letting the video glow through a high-alpha
      // surface tint, matching the translucent track rows it hosts.
      containerColor   = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SheetDimens.SheetContainerAlpha),
      dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
      Box(modifier = Modifier.heightIn(max = maxSheetHeight)) {
        CompositionLocalProvider(LocalOverlayDismiss provides dismiss) { content(dismiss) }
      }
    }
  }
}

@Composable
fun <T> GenericTracksSheet(
  tracks: ImmutableList<T>,
  modifier: Modifier = Modifier,
  lazyListState: LazyListState? = null,
  header: @Composable () -> Unit = {},
  track: @Composable (T) -> Unit = {},
  footer: @Composable () -> Unit = {},
) {
  GenericTracksSheetContent(
    tracks        = tracks,
    modifier      = modifier,
    lazyListState = lazyListState,
    header        = header,
    track         = track,
    footer        = footer,
  )
}

/**
 * Inner content of the tracks sheet — header + scrollable track list + footer.
 * Hosted by [PlayerOverlayHost] (`ModalBottomSheet` in portrait, side sheet in landscape).
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
      verticalArrangement = Arrangement.spacedBy(SheetDimens.ListItemSpacing)
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

/** Sheet title line. Shared by [SheetHeader] and sub-views that need the same look. */
@Composable
internal fun SheetTitleText(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text     = text,
    modifier = modifier,
    style    = MaterialTheme.typography.titleLargeEmphasized,
    color    = MaterialTheme.colorScheme.onSurface,
  )
}

/**
 * Sheet header. Renders the title plus an optional dim eyebrow line and an optional
 * trailing slot.
 *
 *   label  → small `labelSmall` eyebrow line above the title. Omit (or pass `null`)
 *            when the eyebrow would just repeat the title.
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
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = SheetDimens.HeaderLabelLetterSpacing.sp),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = SheetDimens.HeaderLabelAlpha),
        )
        Spacer(modifier = Modifier.height(SheetDimens.HeaderLabelGap))
      }
      SheetTitleText(text = title)
    }
    trailing?.invoke()
  }
}

/**
 * Compact count chip — "4 TRACKS" — sits in `SheetHeader`'s trailing slot.
 * Same rounded mini-tag visual language as `TrackMetadataTag`, so the
 * counter reads as a piece of metadata about the list rather than another control.
 */
@Composable
fun TrackCountPill(count: Int) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Text(
      text     = pluralStringResource(R.plurals.player_track_count, count, count),
      modifier = Modifier.padding(
        horizontal = SheetDimens.CountPillPaddingHorizontal,
        vertical   = SheetDimens.CountPillPaddingVertical,
      ),
      style    = MaterialTheme.typography.labelSmall.copy(
        fontWeight          = FontWeight.Medium,
        letterSpacing       = SheetDimens.CountPillLetterSpacing.sp,
        fontFeatureSettings = "tnum",
      ),
      color    = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * Status badge used in sheet headers: primaryContainer fill, leading dot, ExtraBold
 * tabular-figures label. Pass [onClick] (and optionally [trailingIcon]) to make it
 * tappable — it then spring-scales on press; without [onClick] it is a static pill.
 *
 * Used by the Chapters sheet (chapter count) and the Audio sheet (channel layout).
 */
@Composable
fun SheetStatusBadge(
  text: String,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  trailingIcon: ImageVector? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue   = if (isPressed && onClick != null) SheetDimens.StatusBadgePressScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness    = Spring.StiffnessLow,
    ),
    label = "sheet_status_badge_press_scale",
  )

  val shape = RoundedCornerShape(SheetDimens.StatusBadgeCorner)
  val color = MaterialTheme.colorScheme.primaryContainer
  val scaledModifier = modifier.graphicsLayer {
    scaleX = pressScale
    scaleY = pressScale
  }
  val badgeContent: @Composable () -> Unit = {
    Row(
      modifier = Modifier.padding(
        horizontal = SheetDimens.StatusBadgePaddingHorizontal,
        vertical   = SheetDimens.StatusBadgePaddingVertical,
      ),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(SheetDimens.StatusBadgeGap),
    ) {
      Box(
        modifier = Modifier
          .size(SheetDimens.StatusBadgeDotSize)
          .background(MaterialTheme.colorScheme.primary, CircleShape),
      )
      Text(
        text  = text,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight          = FontWeight.ExtraBold,
          fontFeatureSettings = "tnum",
        ),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      if (trailingIcon != null) {
        Icon(
          imageVector        = trailingIcon,
          contentDescription = null,
          modifier           = Modifier.size(SheetDimens.StatusBadgeChevronSize),
          tint               = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }

  if (onClick != null) {
    Surface(
      onClick           = onClick,
      interactionSource = interactionSource,
      shape             = shape,
      color             = color,
      modifier          = scaledModifier,
      content           = badgeContent,
    )
  } else {
    Surface(
      shape    = shape,
      color    = color,
      modifier = scaledModifier,
      content  = badgeContent,
    )
  }
}

/**
 * Section header divider — One UI–style label flanked by two short rules.
 * Replaces the previous Surface pill so groups read as clean breaks rather
 * than another tappable-looking container.
 */
@Composable
fun TrackHeaderPill(
  title: String,
  modifier: Modifier = Modifier,
) {
  val ruleColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SheetDimens.SectionRuleAlpha)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        top        = SheetDimens.SectionPaddingTop,
        bottom     = SheetDimens.SectionPaddingBottom,
        start      = SheetDimens.SectionPaddingHorizontal,
        end        = SheetDimens.SectionPaddingHorizontal,
      ),
    verticalAlignment     = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    SectionRule(ruleColor)
    Spacer(modifier = Modifier.width(SheetDimens.SectionRuleGap))
    Text(
      text  = title.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = SheetDimens.HeaderLabelLetterSpacing.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SheetDimens.SectionLabelAlpha),
    )
    Spacer(modifier = Modifier.width(SheetDimens.SectionRuleGap))
    SectionRule(ruleColor)
  }
}

@Composable
private fun SectionRule(color: Color) {
  Box(
    modifier = Modifier
      .width(SheetDimens.SectionRuleWidth)
      .height(SheetDimens.SectionRuleThickness)
      .background(color),
  )
}

/**
 * Circular badge slot used at the start of a track row. Filled with the primary colour
 * when selected, a faint tint otherwise. Callers supply the content (an id number, an icon…)
 * and can read the matching content colour from [trackBadgeContentColor].
 */
@Composable
fun TrackBadge(
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .size(SheetDimens.BadgeSize)
      .clip(CircleShape)
      .background(
        if (isSelected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SheetDimens.BadgeIdleAlpha)
        },
      ),
    contentAlignment = Alignment.Center,
    content          = content,
  )
}

@Composable
fun trackBadgeContentColor(isSelected: Boolean): Color =
  if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

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
  enabled: Boolean = true,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  // Shape morph on press + subtle scale — OxygenOS "settle"
  val cornerRadius by animateDpAsState(
    targetValue   = if (isPressed) SheetDimens.RowCornerRadiusPressed else SheetDimens.RowCornerRadius,
    animationSpec = spring(dampingRatio = SheetDimens.RowSpringDamping, stiffness = Spring.StiffnessMediumLow),
    label         = "track_row_corner_radius",
  )
  val pressScale by animateFloatAsState(
    targetValue   = if (isPressed) SheetDimens.RowPressScale else 1f,
    animationSpec = spring(dampingRatio = SheetDimens.RowSpringDamping, stiffness = Spring.StiffnessMediumLow),
    label         = "track_row_press_scale",
  )

  val shape = RoundedCornerShape(cornerRadius)

  // Glass idle / primaryContainer selected
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = SheetDimens.RowSelectedContainerAlpha)
  } else {
    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = SheetDimens.RowIdleContainerAlpha)
  }
  val borderWidth = if (isSelected) SheetDimens.RowBorderWidthSelected else SheetDimens.RowBorderWidth
  val borderBrush = if (isSelected) {
    SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = SheetDimens.RowSelectedBorderAlpha))
  } else {
    SheetGlassEdgeBrush
  }

  Surface(
    onClick           = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    interactionSource = interactionSource,
    // Unavailable options (e.g. a hardware decoder this phone doesn't have) stay visible
    // but dimmed and can't be selected.
    enabled           = enabled,
    modifier          = modifier
      .fillMaxWidth()
      .semantics { selected = isSelected }
      .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
        alpha = if (enabled) 1f else SheetDimens.RowDisabledAlpha
      },
    shape             = shape,
    color             = containerColor,
    // Border drawn by the Surface itself so it always shares the fill's bounds and shape.
    border            = BorderStroke(borderWidth, borderBrush),
  ) {
    Row(
      modifier              = Modifier
        .fillMaxWidth()
        // Fill the 48dp touch-target height so the container covers the whole area inside the border.
        .heightIn(min = SheetDimens.RowMinHeight)
        .padding(
          horizontal = SheetDimens.RowPaddingHorizontal,
          vertical   = SheetDimens.RowPaddingVertical,
        ),
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(SheetDimens.RowContentGap),
    ) {
      // Badge slot — overridden by `badge` lambda when callers need a custom icon
      // (e.g. SubtitlesOff). Default is a circular ID chip with tabular figures.
      if (badge != null) {
        badge()
      } else {
        TrackBadge(isSelected = isSelected) {
          Text(
            text  = id.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight          = FontWeight.Bold,
              fontFeatureSettings = "tnum",
            ),
            color = trackBadgeContentColor(isSelected),
          )
        }
      }

      // Title + metadata column
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text       = title,
          style      = MaterialTheme.typography.titleSmall.copy(
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
          Spacer(modifier = Modifier.height(SheetDimens.RowTitleMetadataGap))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SheetDimens.RowMetadataGap),
            verticalArrangement   = Arrangement.spacedBy(SheetDimens.RowMetadataGap),
          ) {
            metadata.forEach { data ->
              TrackMetadataTag(data, isSelected)
            }
          }
        }
      }

      // Selection indicator — AnimatedContent morph between ring and filled-check disc
      if (isSelected || trailingContent == null) {
        AnimatedContent(
          targetState = isSelected,
          transitionSpec = {
            (scaleIn(spring(dampingRatio = SheetDimens.IndicatorSpringDamping, stiffness = Spring.StiffnessMedium)) + fadeIn()) togetherWith
              (scaleOut(spring(dampingRatio = SheetDimens.IndicatorSpringDamping, stiffness = Spring.StiffnessMedium)) + fadeOut())
          },
          label = "track_row_selection_indicator",
        ) { selected ->
          if (selected) {
            Box(
              modifier = Modifier
                .size(SheetDimens.BadgeSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onPrimary,
                modifier           = Modifier.size(SheetDimens.CheckIconSize),
              )
            }
          } else {
            Box(
              modifier = Modifier
                .size(SheetDimens.BadgeSize)
                .border(
                  SheetDimens.RingStroke,
                  MaterialTheme.colorScheme.outlineVariant.copy(alpha = SheetDimens.RingAlpha),
                  CircleShape,
                ),
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
  // Map to the player's container/onContainer token pairs
  val (backgroundColor, contentColor) = when (metadata.type) {
    MetadataType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    MetadataType.WARNING -> MaterialTheme.colorScheme.errorContainer  to MaterialTheme.colorScheme.onErrorContainer
    MetadataType.DEFAULT -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
  }

  // Mini-tag: rounded, no border, compact padding, Medium weight
  Surface(
    color = backgroundColor,
    shape = MaterialTheme.shapes.small,
  ) {
    Text(
      text       = metadata.text.uppercase(),
      modifier   = Modifier.padding(
        horizontal = SheetDimens.TagPaddingHorizontal,
        vertical   = SheetDimens.TagPaddingVertical,
      ),
      style      = MaterialTheme.typography.labelSmall.copy(
        fontSize      = SheetDimens.TagFontSize.sp,
        letterSpacing = SheetDimens.TagLetterSpacing.sp,
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
 * pinned to the end, separated by a flexible spacer. Each button is an icon-only
 * `FilledTonalIconButton` carrying its **own** tonal background; there is no
 * unifying glass wrapper. The button morphs from a rounded square at rest
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
    shape        = MaterialTheme.shapes.medium,
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
      modifier = Modifier.size(SheetDimens.ActionButtonSize),
      shapes   = chipShapes,
    ) {
      Icon(
        imageVector        = action.icon,
        contentDescription = action.label,
        modifier           = Modifier.size(SheetDimens.ActionIconSize),
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
      contentPadding = PaddingValues(
        horizontal = MaterialTheme.spacing.medium,
        vertical = SheetDimens.AddButtonPaddingVertical,
      )
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(SheetDimens.AddButtonIconSize),
      )
      Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLargeEmphasized
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
