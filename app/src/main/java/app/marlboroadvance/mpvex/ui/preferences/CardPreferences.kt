package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Flat group container: rows sit directly on the screen surface, no card background.
 */
@Composable
fun PreferenceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

/**
 * Flat preference row. Icon, text and trailing content share one vertical center line,
 * with equal top and bottom padding, whether the summary takes one line or several.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 80.dp)
            .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            // Icon is centered in a fixed slot so the text starts at the same x on every row.
            Box(modifier = Modifier.width(68.dp), contentAlignment = Alignment.Center) { icon() }
        }
        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) { title() }
            if (summary != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { summary() }
                }
            }
        }
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 8.dp).widthIn(min = 48.dp),
                contentAlignment = Alignment.Center,
            ) { trailingContent() }
        }
    }
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
        title = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMediumEmphasized 
            ) 
        },
        summary = summary?.let { 
            { 
                Text(
                    text = it, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                ) 
            } 
        },
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

/** Flat list style has no dividers; kept so existing call sites compile. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun PreferenceDivider(
    modifier: Modifier = Modifier,
) = Unit

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
