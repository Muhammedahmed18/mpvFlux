package app.marlboroadvance.mpvex.ui.preferences.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.theme.AppTheme

/**
 * Horizontal carousel of theme swatches. Every card is painted with the theme's own colors
 * (light or dark set, following [isDarkMode]). The Dynamic theme is only offered on Android 12+,
 * where it previews the device's wallpaper colors.
 */
@Composable
fun ThemePicker(
  currentTheme: AppTheme,
  isDarkMode: Boolean,
  onThemeSelected: (AppTheme) -> Unit,
  modifier: Modifier = Modifier,
) {
  val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val themes = remember(dynamicSupported) {
    AppTheme.entries.filter { dynamicSupported || !it.isDynamic }
  }
  val listState = rememberLazyListState()

  // Runs once: jump to the saved theme (no animation, so tapping a swatch never moves the row).
  // A saved Dynamic choice on a device that cannot show it is left untouched in storage.
  LaunchedEffect(Unit) {
    val index = themes.indexOf(currentTheme)
    if (index > 1) listState.scrollToItem(index - 1)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = stringResource(R.string.pref_appearance_theme_picker_label),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )

    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      state = listState,
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(themes, key = { it.name }) { theme ->
        ThemeSwatchCard(
          theme = theme,
          isSelected = theme == currentTheme,
          isDarkMode = isDarkMode,
          onClick = { onThemeSelected(theme) },
        )
      }
    }
  }
}

private class SwatchColors(
  val background: Color,
  val primary: Color,
  val secondary: Color,
  val tertiary: Color,
)

@Composable
private fun rememberSwatchColors(theme: AppTheme, isDarkMode: Boolean): SwatchColors {
  val context = LocalContext.current
  return remember(theme, isDarkMode) {
    if (theme.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val scheme = if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      SwatchColors(scheme.surface, scheme.primary, scheme.secondary, scheme.tertiary)
    } else if (isDarkMode) {
      SwatchColors(theme.backgroundDark, theme.primaryDark, theme.secondaryDark, theme.tertiaryDark)
    } else {
      SwatchColors(theme.backgroundLight, theme.primaryLight, theme.secondaryLight, theme.tertiaryLight)
    }
  }
}

/** Readable foreground for text or icons drawn on [this] color. */
private fun Color.readableOn(): Color =
  if (luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White

@Composable
private fun ThemeSwatchCard(
  theme: AppTheme,
  isSelected: Boolean,
  isDarkMode: Boolean,
  onClick: () -> Unit,
) {
  val colors = rememberSwatchColors(theme, isDarkMode)
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    label = "swatchBorderColor",
  )
  val borderWidth by animateDpAsState(
    targetValue = if (isSelected) 3.dp else 1.dp,
    label = "swatchBorderWidth",
  )
  val checkProgress by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    label = "swatchCheck",
  )

  Surface(
    onClick = onClick,
    modifier = Modifier
      .width(104.dp)
      .semantics {
        role = Role.RadioButton
        selected = isSelected
      },
    shape = RoundedCornerShape(24.dp),
    color = colors.background,
    border = BorderStroke(borderWidth, borderColor),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp)
          .background(colors.primary, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        // Scale and fade driven by an animated value (no scoped AnimatedVisibility overloads).
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = colors.primary.readableOn(),
          modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
              val scale = 0.6f + 0.4f * checkProgress
              scaleX = scale
              scaleY = scale
              alpha = checkProgress
            },
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(24.dp).background(colors.secondary, CircleShape))
        Box(Modifier.size(24.dp).background(colors.tertiary, CircleShape))
      }
      Text(
        text = stringResource(theme.titleRes),
        style = MaterialTheme.typography.labelMedium,
        color = colors.background.readableOn(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
