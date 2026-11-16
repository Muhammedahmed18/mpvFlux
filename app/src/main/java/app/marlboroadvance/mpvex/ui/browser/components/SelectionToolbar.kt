package app.marlboroadvance.mpvex.ui.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import app.marlboroadvance.mpvex.ui.utils.WindowWidthClass
import app.marlboroadvance.mpvex.ui.utils.currentWindowWidthClass
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

/**
 * One action in the selection toolbar.
 *
 * Actions that don't apply to the current selection should be left out of the list rather
 * than disabled, so the toolbar never shows dead buttons.
 */
@Immutable
class SelectionAction(
  val icon: ImageVector,
  val label: String,
  val onClick: () -> Unit,
  val destructive: Boolean = false,
  val enabled: Boolean = true,
)

object SelectionToolbarDefaults {
  /** Most icon buttons shown in the bar itself; extra actions go into the overflow menu. */
  const val MaxVisibleActions = 4

  /** Space a list needs at the bottom so its last item can scroll above the toolbar. */
  val ContentClearance: Dp = 64.dp + FloatingToolbarDefaults.ScreenOffset + 16.dp

  /** [ContentClearance] plus the system navigation bar inset. */
  @Composable
  fun contentBottomPadding(): Dp =
    ContentClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * M3 Expressive floating toolbar shown while items are selected.
 *
 * Layout: up to [SelectionToolbarDefaults.MaxVisibleActions] icon buttons, a "More" menu for
 * the rest, and an optional FAB for the [primaryAction] (e.g. Delete). When [expanded] is false
 * (the list is scrolling down) the toolbar collapses so only the FAB stays on screen.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SelectionToolbar(
  visible: Boolean,
  actions: List<SelectionAction>,
  modifier: Modifier = Modifier,
  primaryAction: SelectionAction? = null,
  overflowActions: List<SelectionAction> = emptyList(),
  expanded: Boolean = true,
) {
  AnimatedVisibility(
    visible = visible,
    modifier = modifier,
    enter =
      slideInVertically(
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        initialOffsetY = { it },
      ) + scaleIn(initialScale = 0.9f) + fadeIn(),
    exit =
      slideOutVertically(animationSpec = tween(180), targetOffsetY = { it }) + fadeOut(tween(150)),
  ) {
    // Only the bottom system inset matters here; the old bar padded for the status bar too.
    val paneTitle = stringResource(R.string.selection_toolbar_pane)
    // Tablets, unfolded foldables and landscape windows have room for text labels.
    val showLabels = currentWindowWidthClass() == WindowWidthClass.Expanded
    Box(
      modifier =
        Modifier
          .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
          .padding(bottom = FloatingToolbarDefaults.ScreenOffset)
          // TalkBack announces "Selection actions" when the toolbar appears and reads its
          // buttons as one group, left to right, ending on the main action (FAB).
          .semantics {
            this.paneTitle = paneTitle
            isTraversalGroup = true
          },
      contentAlignment = Alignment.Center,
    ) {
      val visibleActions = actions.take(SelectionToolbarDefaults.MaxVisibleActions)
      val moreActions = actions.drop(visibleActions.size) + overflowActions

      if (primaryAction != null) {
        HorizontalFloatingToolbar(
          expanded = expanded,
          colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
          floatingActionButton = {
            ToolbarTooltip(primaryAction.label) {
              if (primaryAction.destructive) {
                // A destructive main action (Delete) gets the error container so it can't be
                // mistaken for a harmless primary action.
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                  onClick = primaryAction.onClick,
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                  Icon(primaryAction.icon, contentDescription = primaryAction.label)
                }
              } else {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = primaryAction.onClick) {
                  Icon(primaryAction.icon, contentDescription = primaryAction.label)
                }
              }
            }
          },
        ) {
          ToolbarActions(visibleActions, moreActions, showLabels)
        }
      } else {
        HorizontalFloatingToolbar(
          expanded = expanded,
          colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        ) {
          ToolbarActions(visibleActions, moreActions, showLabels)
        }
      }
    }
  }
}

@Composable
private fun ToolbarActions(
  visibleActions: List<SelectionAction>,
  moreActions: List<SelectionAction>,
  showLabels: Boolean,
) {
  // Each button is keyed by its label, so when the selection goes from one item to many
  // only the button that actually changes (e.g. Rename -> Copy or move) animates in. The
  // others keep their identity and stay perfectly still; the row itself never re-fades.
  val initialLabels = remember { visibleActions.map { it.label }.toSet() }
  Row(
    modifier = Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    visibleActions.forEach { action ->
      key(action.label) {
        ActionEnterAnimation(animate = action.label !in initialLabels) {
          if (showLabels) ToolbarLabeledButton(action) else ToolbarIconButton(action)
        }
      }
    }
    if (moreActions.isNotEmpty()) OverflowButton(moreActions)
  }
}

/**
 * Pops a newly added button in (fade + scale). Buttons that were there when the toolbar
 * appeared skip it, since the whole toolbar is already animating in at that point.
 */
@Composable
private fun ActionEnterAnimation(
  animate: Boolean,
  content: @Composable () -> Unit,
) {
  val progress = remember { Animatable(if (animate) 0f else 1f) }
  LaunchedEffect(Unit) {
    if (progress.value < 1f) {
      progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
  }
  Box(
    modifier =
      Modifier.graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        scaleX = 0.6f + 0.4f * progress.value
        scaleY = 0.6f + 0.4f * progress.value
      },
  ) {
    content()
  }
}

@Composable
private fun ToolbarIconButton(action: SelectionAction) {
  ToolbarTooltip(action.label) {
    IconButton(
      onClick = action.onClick,
      enabled = action.enabled,
      colors = toolbarIconButtonColors(action.destructive),
    ) {
      Icon(action.icon, contentDescription = action.label)
    }
  }
}

/**
 * Destructive actions get an errorContainer / onErrorContainer pill instead of plain red
 * text: the toolbar sits on primaryContainer, and red-on-primaryContainer can fall below
 * WCAG contrast in some themes. The container pair is guaranteed to contrast.
 */
@Composable
private fun toolbarIconButtonColors(destructive: Boolean) =
  if (destructive) {
    IconButtonDefaults.iconButtonColors(
      containerColor = MaterialTheme.colorScheme.errorContainer,
      contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
  } else {
    IconButtonDefaults.iconButtonColors()
  }

/** Icon + text button used on wide windows (see [WindowWidthClass.Expanded]). */
@Composable
private fun ToolbarLabeledButton(action: SelectionAction) {
  val colors =
    if (action.destructive) {
      ButtonDefaults.textButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
      )
    } else {
      ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)
    }
  TextButton(
    onClick = action.onClick,
    enabled = action.enabled,
    colors = colors,
    modifier = Modifier.padding(horizontal = 2.dp),
  ) {
    // The text already names the action, so the icon is decorative here.
    Icon(action.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(action.label, maxLines = 1)
  }
}

@Composable
private fun OverflowButton(actions: List<SelectionAction>) {
  var showMenu by remember { mutableStateOf(false) }
  val moreLabel = stringResource(R.string.cd_more_options)
  Box {
    ToolbarTooltip(moreLabel) {
      IconButton(onClick = { showMenu = true }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = moreLabel)
      }
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
      actions.forEach { action ->
        val tint = if (action.destructive) MaterialTheme.colorScheme.error else LocalContentColor.current
        DropdownMenuItem(
          text = { Text(action.label, color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
          leadingIcon = { Icon(action.icon, contentDescription = null, tint = tint) },
          enabled = action.enabled,
          onClick = {
            showMenu = false
            action.onClick()
          },
        )
      }
    }
  }
}

/** Long-press tooltip so icon-only buttons are still discoverable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarTooltip(
  label: String,
  content: @Composable () -> Unit,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(label) } },
    state = rememberTooltipState(),
  ) {
    content()
  }
}

/**
 * Collapses the selection toolbar while the content under it scrolls down and expands it
 * again on scroll up. Put it on a parent of the scrolling list.
 */
@Composable
fun Modifier.selectionToolbarScroll(
  onExpand: () -> Unit,
  onCollapse: () -> Unit,
  threshold: Dp = 24.dp,
): Modifier {
  val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
  val currentOnExpand by rememberUpdatedState(onExpand)
  val currentOnCollapse by rememberUpdatedState(onCollapse)
  val connection =
    remember(thresholdPx) {
      object : NestedScrollConnection {
        private var accumulated = 0f

        override fun onPostScroll(
          consumed: Offset,
          available: Offset,
          source: NestedScrollSource,
        ): Offset {
          val dy = consumed.y
          if (dy == 0f) return Offset.Zero
          // Reset when the direction flips so small reversals don't toggle the toolbar.
          if ((accumulated < 0f && dy > 0f) || (accumulated > 0f && dy < 0f)) accumulated = 0f
          accumulated += dy
          if (accumulated <= -thresholdPx) {
            currentOnCollapse()
            accumulated = 0f
          } else if (accumulated >= thresholdPx) {
            currentOnExpand()
            accumulated = 0f
          }
          return Offset.Zero
        }
      }
    }
  return this.nestedScroll(connection)
}
