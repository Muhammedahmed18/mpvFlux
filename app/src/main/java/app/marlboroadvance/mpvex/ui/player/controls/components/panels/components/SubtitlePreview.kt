package app.marlboroadvance.mpvex.ui.player.controls.components.panels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.SubtitlesBorderStyle
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.utils.media.loadCustomFontEntries
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject

// Video-pixel sizes from mpv are much larger than on-screen UI sizes; these keep the
// preview proportional to the real subtitle without overflowing the panel.
private const val FONT_SIZE_FACTOR = 0.4f
private const val BORDER_SIZE_FACTOR = 0.5f
private const val SHADOW_OFFSET_FACTOR = 0.3f

/**
 * Live approximation of the subtitle style (font, size, colors, border, shadow).
 * Reads the same mpv properties and preferences the settings controls write to, so
 * it updates as the user adjusts them.
 */
@Composable
fun SubtitlePreview(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val preferences = koinInject<SubtitlesPreferences>()

  val fontSize by MPVLib.propInt["sub-font-size"].collectAsState()
  val isBold by MPVLib.propBoolean["sub-bold"].collectAsState()
  val isItalic by MPVLib.propBoolean["sub-italic"].collectAsState()
  val borderSize by MPVLib.propInt["sub-outline-size"].collectAsState()
  val shadowOffset by MPVLib.propInt["sub-shadow-offset"].collectAsState()
  val mpvBorderStyle by MPVLib.propString["sub-border-style"].collectAsState()
  val fontName by preferences.font.collectAsState()
  val textColor by preferences.textColor.collectAsState()
  val borderColor by preferences.borderColor.collectAsState()
  val backgroundColor by preferences.backgroundColor.collectAsState()

  var fontFamilies by remember { mutableStateOf<Map<String, FontFamily>>(emptyMap()) }
  LaunchedEffect(Unit) {
    val map = mutableMapOf<String, FontFamily>()
    for (entry in loadCustomFontEntries(context)) {
      runCatching { map[entry.familyName] = FontFamily(android.graphics.Typeface.createFromFile(entry.file)) }
    }
    fontFamilies = map
  }

  val isOpaqueBox =
    (mpvBorderStyle ?: preferences.borderStyle.get().value) == SubtitlesBorderStyle.OpaqueBox.value
  val outlinePx = with(density) { ((borderSize ?: preferences.borderSize.get()) * BORDER_SIZE_FACTOR).dp.toPx() }
  val shadowPx = with(density) { ((shadowOffset ?: preferences.shadowOffset.get()) * SHADOW_OFFSET_FACTOR).dp.toPx() }
  val baseStyle =
    TextStyle(
      fontSize = ((fontSize ?: preferences.fontSize.get()) * FONT_SIZE_FACTOR).coerceIn(12f, 40f).sp,
      fontWeight = if (isBold == true) FontWeight.Bold else FontWeight.Normal,
      fontStyle = if (isItalic == true) FontStyle.Italic else FontStyle.Normal,
      fontFamily = fontFamilies[fontName],
      textAlign = TextAlign.Center,
    )
  val sample = stringResource(R.string.font_preview_sample)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.small)
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
        .heightIn(min = 88.dp)
        .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
    contentAlignment = Alignment.Center,
  ) {
    if (isOpaqueBox) {
      Text(
        text = sample,
        style = baseStyle.copy(color = Color(textColor)),
        modifier = Modifier.background(Color(backgroundColor)).padding(horizontal = 6.dp, vertical = 2.dp),
      )
    } else {
      if (outlinePx > 0f) {
        // Outline layer: a stroked copy of the text sits behind the fill.
        Text(
          text = sample,
          style =
            baseStyle.copy(
              color = Color(borderColor),
              drawStyle = Stroke(width = outlinePx * 2f, join = StrokeJoin.Round),
            ),
        )
      }
      Text(
        text = sample,
        style =
          baseStyle.copy(
            color = Color(textColor),
            shadow = Shadow(color = Color(backgroundColor), offset = Offset(shadowPx, shadowPx), blurRadius = 0f),
          ),
      )
    }
  }
}
