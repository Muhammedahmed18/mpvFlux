package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

/**
 * Plain preference icon (no container). [containerColor] and [iconColor] are kept so existing
 * call sites compile; the icon always uses the neutral on-surface-variant tint.
 */
@Composable
fun PreferenceIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Unspecified,
    iconColor: Color = Color.Unspecified,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = if (iconColor.isSpecified) iconColor else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(24.dp),
    )
}

/** Gap between the rows of a [PreferenceCard]; each row is its own tonal segment. */
private val PreferenceCardGap = 2.dp

/** Corner radius of the edges where two segments meet. */
private val PreferenceCardInnerCorner = 4.dp

private fun segmentShape(outer: CornerBasedShape, inner: CornerSize, index: Int, count: Int): Shape =
    when {
        count <= 1 -> outer
        index == 0 -> outer.copy(bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> outer.copy(topStart = inner, topEnd = inner)
        else -> outer.copy(all = inner)
    }

/**
 * Grouped tonal container in the M3 segmented list style: every row is a `surfaceContainer`
 * segment separated by a thin gap. The first and last segments get the large outer corners and
 * the ones in between get small corners. Each row is clipped to its own segment, so ripples
 * follow the shape. Position is known only at layout time, so the shapes are applied there.
 */
@Composable
fun PreferenceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val outer = MaterialTheme.shapes.large
    val inner = CornerSize(PreferenceCardInnerCorner)
    var bounds by remember { mutableStateOf<List<IntRect>>(emptyList()) }

    Layout(
        content = content,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                bounds.forEachIndexed { index, rect ->
                    val shape = segmentShape(outer, inner, index, bounds.size)
                    translate(rect.left.toFloat(), rect.top.toFloat()) {
                        drawOutline(
                            outline = shape.createOutline(
                                Size(rect.width.toFloat(), rect.height.toFloat()),
                                layoutDirection,
                                this,
                            ),
                            color = containerColor,
                        )
                    }
                }
            },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val childConstraints = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0,
            maxHeight = constraints.maxHeight,
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        val gap = PreferenceCardGap.roundToPx()
        val tops = IntArray(placeables.size)
        val rects = ArrayList<IntRect>(placeables.size)
        var y = 0
        placeables.forEachIndexed { index, placeable ->
            tops[index] = y
            rects.add(IntRect(0, y, width, y + placeable.height))
            y += placeable.height + gap
        }
        val totalHeight = if (placeables.isEmpty()) 0 else y - gap
        layout(width, totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            placeables.forEachIndexed { index, placeable ->
                val segment = segmentShape(outer, inner, index, placeables.size)
                placeable.placeWithLayer(0, tops[index]) {
                    shape = segment
                    clip = true
                }
            }
            if (bounds != rects) bounds = rects
        }
    }
}

/**
 * Title slot style shared by every preference row, so call sites can pass a plain `Text`.
 * An explicit `style` on the Text still wins.
 */
@Composable
internal fun PreferenceTitle(content: @Composable () -> Unit) {
    ProvideTextStyle(MaterialTheme.typography.titleMediumEmphasized, content)
}

/**
 * Flat preference row built on the same M3 [ListItem] as the switch and dropdown rows, so every
 * row shares one geometry. The summary uses the ListItem default (bodyMedium, onSurfaceVariant).
 */
@Composable
fun Preference(
    title: @Composable () -> Unit,
    summary: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { PreferenceTitle(title) },
        supportingContent = summary,
        leadingContent = icon,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

/**
 * Compatibility wrapper for the redesigned UI.
 */
@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
) {
    Preference(
        title = { Text(text = title) },
        summary = summary?.let { { Text(text = it) } },
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        trailingContent = if (showChevron) {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun PreferenceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

/**
 * Background color shared by preference screens: the theme surface, same as the library.
 * Pure black comes from the AMOLED option (which sets surface to black), so Settings no
 * longer jumps to black when the user picked the tinted dark theme.
 */
@Composable
fun rememberPreferenceBackgroundColor(): Color = MaterialTheme.colorScheme.surface

/**
 * Shared Material 3 top app bar for preference screens.
 *
 * The bar is opaque at rest ([containerColor] defaults to the screen background) so the
 * scrolling list cannot ghost through it during the scrolled-color crossfade — the root
 * cause of the top-bar flicker. [scrolledContainerColor] keeps the elevation cue on scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    containerColor: Color = rememberPreferenceBackgroundColor(),
    scrolledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    actions: @Composable RowScope.() -> Unit = {},
    large: Boolean = false,
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = scrolledContainerColor,
        titleContentColor = MaterialTheme.colorScheme.primary,
    )
    if (large) {
        LargeTopAppBar(
            title = { ProvideLargeTitleStyle(scrollBehavior) { title() } },
            expandedHeight = LargeTitleBarExpandedHeight,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = colors,
        )
    } else {
        TopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = colors,
        )
    }
}

/** Expanded height of the large settings bar; shorter than the M3 default to keep the title close to the back button. */
val LargeTitleBarExpandedHeight = 112.dp

private val LargeTitleExpandedSize = 36.sp
private val LargeTitleCollapsedSize = 22.sp

/**
 * Scales the large-bar title between its expanded and collapsed size with the scroll, so every
 * large-title screen (settings and sub-screens) uses the same title size. The scroll fraction is
 * read here, inside the title, so only the title recomposes while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvideLargeTitleStyle(
    scrollBehavior: TopAppBarScrollBehavior?,
    content: @Composable () -> Unit,
) {
    val fraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val size = lerp(LargeTitleExpandedSize, LargeTitleCollapsedSize, fraction)
    ProvideTextStyle(
        value = LocalTextStyle.current.copy(fontSize = size, lineHeight = size * 1.2f),
        content = content,
    )
}
