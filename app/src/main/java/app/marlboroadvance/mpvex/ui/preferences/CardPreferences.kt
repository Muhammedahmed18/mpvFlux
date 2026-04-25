package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Modern Material 3 Expressive Icon Container.
 * Uses tonal palettes instead of manual alpha for better accessibility.
 */
@Composable
fun PreferenceIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * A grouped container using the M3 'SurfaceContainer' role.
 */
@Composable
fun PreferenceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

/**
 * Native Material 3 ListItem wrapper for Preferences.
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
        headlineContent = title,
        supportingContent = summary,
        leadingContent = icon,
        trailingContent = trailingContent,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Preference(
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        summary = summary?.let { { Text(text = it, style = MaterialTheme.typography.bodySmall) } },
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun PreferenceDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp,
    )
}

@Composable
fun PreferenceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp),
    )
}
