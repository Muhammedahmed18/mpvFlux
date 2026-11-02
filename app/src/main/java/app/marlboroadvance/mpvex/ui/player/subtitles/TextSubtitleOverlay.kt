package app.marlboroadvance.mpvex.ui.player.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.SubtitleJustification
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.SubtitlesBorderStyle
import app.marlboroadvance.mpvex.utils.media.loadCustomFontEntries
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject
import kotlin.math.max
import kotlin.math.min
import androidx.compose.runtime.collectAsState as collectStateFlowAsState

/**
 * Draws text subtitles (SRT, WebVTT, mov_text, ...) with Android's own text engine instead
 * of libass, so they use the phone's current default font, including fonts applied from
 * a theme store (MIUI/HyperOS, Samsung, ...), which libass cannot see.
 *
 * How it works:
 * - When the selected primary/secondary track is a plain-text subtitle and the setting is
 *   on, mpv's own drawing for that slot is hidden (`sub-visibility` /
 *   `secondary-sub-visibility` = no).
 * - mpv still does all timing (including subtitle delay/speed) and reports the current
 *   line through `sub-text/ass` / `secondary-sub-text/ass`, which only change when the line
 *   changes, so this costs nothing per frame.
 * - ASS/SSA and image subtitles (PGS, VobSub) are never affected: they stay on libass so
 *   typesetting, signs and karaoke render exactly as authored.
 *
 * Styling follows the existing subtitle settings (size, scale, colors, outline, shadow,
 * box, bold/italic, justification, position, scale-by-window). A custom font chosen in the
 * subtitle settings is used if its file is available; otherwise the phone font is used.
 *
 * This view sits between the video and the player controls in player_layout.xml so it
 * stays visible in picture-in-picture, where the controls layer is hidden.
 */
@Composable
fun TextSubtitleLayer(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<SubtitlesPreferences>()
  val enabled by preferences.usePhoneFontForTextSubs.collectAsState()
  val tracks by viewModel.subtitleTracks.collectStateFlowAsState()
  val sid by MPVLib.propInt["sid"].collectStateFlowAsState()
  val secondarySid by MPVLib.propInt["secondary-sid"].collectStateFlowAsState()

  val primaryOverlay = enabled && isPlainTextSubtitle(tracks, sid)
  val secondaryOverlay = enabled && isPlainTextSubtitle(tracks, secondarySid)

  // Hand drawing over from libass to this layer (and back) per subtitle slot.
  LaunchedEffect(primaryOverlay) {
    runCatching { MPVLib.setPropertyBoolean("sub-visibility", !primaryOverlay) }
  }
  LaunchedEffect(secondaryOverlay) {
    runCatching { MPVLib.setPropertyBoolean("secondary-sub-visibility", !secondaryOverlay) }
  }

  if (!primaryOverlay && !secondaryOverlay) return

  val style = rememberTextSubtitleStyle(preferences)

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val heightPx = constraints.maxHeight.toFloat()
    val widthPx = constraints.maxWidth.toFloat()
    // mpv/libass sizes are defined against a 720-pixel-tall frame.
    val unit = if (style.scaleByWindow) heightPx / ASS_REFERENCE_HEIGHT else min(heightPx, widthPx) / ASS_REFERENCE_HEIGHT
    val metrics = remember(unit, style, density) {
      with(density) {
        SubtitleMetrics(
          fontSize = (style.fontSize * style.scale * unit * LIBASS_SIZE_TO_EM).toSp(),
          outlinePx = style.borderSize * unit,
          shadowPx = style.shadowOffset * unit,
          marginX = (MPV_MARGIN_X * unit).toDp(),
          marginY = (MPV_MARGIN_Y * unit).toDp(),
        )
      }
    }

    if (primaryOverlay) {
      val raw by MPVLib.propString["sub-text/ass"].collectStateFlowAsState()
      // Plain-text fallback in case the ASS-formatted variant is unavailable.
      val plain by MPVLib.propString["sub-text"].collectStateFlowAsState()
      val line = remember(raw, plain) { AssTextParser.parse(raw ?: plain?.replace("\n", "\\N")) }
      if (!line.isBlank) {
        // sub-pos: 100 = bottom of the screen, 0 = top. The line's bottom edge sits there.
        val bottomFromPos = with(density) { max(0f, heightPx * (1f - style.subPos / 100f)).toDp() }
        PositionedLine(
          line = line,
          style = style,
          metrics = metrics,
          defaultTopAligned = false,
          bottomOffset = bottomFromPos,
        )
      }
    }

    if (secondaryOverlay) {
      val raw by MPVLib.propString["secondary-sub-text/ass"].collectStateFlowAsState()
      // Plain-text fallback in case the ASS-formatted variant is unavailable.
      val plain by MPVLib.propString["secondary-sub-text"].collectStateFlowAsState()
      val line = remember(raw, plain) { AssTextParser.parse(raw ?: plain?.replace("\n", "\\N")) }
      if (!line.isBlank) {
        // The app places secondary subtitles at the top so they don't overlap the primary.
        PositionedLine(
          line = line,
          style = style,
          metrics = metrics,
          defaultTopAligned = true,
          bottomOffset = 0.dp,
        )
      }
    }
  }
}

@Composable
private fun PositionedLine(
  line: AssTextParser.Line,
  style: TextSubtitleStyle,
  metrics: SubtitleMetrics,
  defaultTopAligned: Boolean,
  bottomOffset: Dp,
) {
  // Honor SRT/ASS positioning tags ({\an8} = top center, etc.).
  val an = line.alignment
  val vertical = when (an) {
    in 7..9 -> Alignment.Top
    in 4..6 -> Alignment.CenterVertically
    in 1..3 -> Alignment.Bottom
    else -> if (defaultTopAligned) Alignment.Top else Alignment.Bottom
  }
  val horizontal = when (an) {
    1, 4, 7 -> Alignment.Start
    3, 6, 9 -> Alignment.End
    else -> Alignment.CenterHorizontally
  }
  val textAlign = when (style.justification) {
    SubtitleJustification.Left -> TextAlign.Start
    SubtitleJustification.Right -> TextAlign.End
    SubtitleJustification.Center -> TextAlign.Center
    SubtitleJustification.Auto -> when (horizontal) {
      Alignment.Start -> TextAlign.Start
      Alignment.End -> TextAlign.End
      else -> TextAlign.Center
    }
  }

  val boxAlignment = when (vertical) {
    Alignment.Top -> when (horizontal) {
      Alignment.Start -> Alignment.TopStart
      Alignment.End -> Alignment.TopEnd
      else -> Alignment.TopCenter
    }
    Alignment.CenterVertically -> when (horizontal) {
      Alignment.Start -> Alignment.CenterStart
      Alignment.End -> Alignment.CenterEnd
      else -> Alignment.Center
    }
    else -> when (horizontal) {
      Alignment.Start -> Alignment.BottomStart
      Alignment.End -> Alignment.BottomEnd
      else -> Alignment.BottomCenter
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        start = metrics.marginX,
        end = metrics.marginX,
        top = metrics.marginY,
        bottom = metrics.marginY + if (vertical == Alignment.Bottom) bottomOffset else 0.dp,
      ),
    contentAlignment = boxAlignment,
  ) {
    SubtitleText(line = line, style = style, metrics = metrics, textAlign = textAlign)
  }
}

@Composable
private fun SubtitleText(
  line: AssTextParser.Line,
  style: TextSubtitleStyle,
  metrics: SubtitleMetrics,
  textAlign: TextAlign,
) {
  val fillText = remember(line) { line.toAnnotatedString(withColors = true) }
  val outlineText = remember(line) { line.toAnnotatedString(withColors = false) }

  val baseStyle = TextStyle(
    fontFamily = style.fontFamily,
    fontSize = metrics.fontSize,
    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
    textAlign = textAlign,
  )

  val opaqueBox = style.borderStyle == SubtitlesBorderStyle.OpaqueBox
  val shadowColor = if (style.backgroundColor.alpha > 0f) style.backgroundColor else Color.Black.copy(alpha = 0.6f)
  val shadow = if (metrics.shadowPx > 0f) {
    Shadow(color = shadowColor, offset = Offset(metrics.shadowPx, metrics.shadowPx), blurRadius = 0f)
  } else {
    null
  }

  val boxModifier = when {
    // libass "opaque box" fills the box with the outline color.
    opaqueBox -> Modifier
      .background(style.borderColor, RoundedCornerShape(4.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp)
    style.backgroundColor.alpha > 0f && metrics.shadowPx <= 0f -> Modifier
      .background(style.backgroundColor, RoundedCornerShape(4.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp)
    else -> Modifier
  }

  Box(modifier = boxModifier) {
    val drawOutline = !opaqueBox && metrics.outlinePx > 0f
    if (drawOutline) {
      // Outline layer: the same text stroked in the outline color (libass draws the
      // border on both sides of the glyph edge, hence the doubled stroke width).
      Text(
        text = outlineText,
        style = baseStyle.copy(
          color = style.borderColor,
          drawStyle = Stroke(width = metrics.outlinePx * 2f, join = StrokeJoin.Round),
          shadow = if (opaqueBox) null else shadow,
        ),
      )
    }
    Text(
      text = fillText,
      style = baseStyle.copy(
        color = style.textColor,
        shadow = if (drawOutline || opaqueBox) null else shadow,
      ),
    )
  }
}

@Immutable
private data class TextSubtitleStyle(
  val fontFamily: FontFamily,
  val fontSize: Int,
  val scale: Float,
  val borderSize: Int,
  val shadowOffset: Int,
  val bold: Boolean,
  val italic: Boolean,
  val textColor: Color,
  val borderColor: Color,
  val backgroundColor: Color,
  val borderStyle: SubtitlesBorderStyle,
  val justification: SubtitleJustification,
  val subPos: Int,
  val scaleByWindow: Boolean,
)

@Immutable
private data class SubtitleMetrics(
  val fontSize: TextUnit,
  val outlinePx: Float,
  val shadowPx: Float,
  val marginX: Dp,
  val marginY: Dp,
)

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberTextSubtitleStyle(preferences: SubtitlesPreferences): TextSubtitleStyle {
  val context = LocalContext.current
  val fontName by preferences.font.collectAsState()
  val fontSize by preferences.fontSize.collectAsState()
  val scale by preferences.subScale.collectAsState()
  val borderSize by preferences.borderSize.collectAsState()
  val shadowOffset by preferences.shadowOffset.collectAsState()
  val bold by preferences.bold.collectAsState()
  val italic by preferences.italic.collectAsState()
  val textColor by preferences.textColor.collectAsState()
  val borderColor by preferences.borderColor.collectAsState()
  val backgroundColor by preferences.backgroundColor.collectAsState()
  val borderStyle by preferences.borderStyle.collectAsState()
  val justification by preferences.justification.collectAsState()
  val subPos by preferences.subPos.collectAsState()
  val scaleByWindow by preferences.scaleByWindow.collectAsState()

  // A custom font picked in the subtitle settings still applies; otherwise use the
  // phone's default font (FontFamily.Default follows the system / theme-store font).
  val customFamily by produceState<FontFamily?>(initialValue = null, fontName) {
    value = if (fontName.isBlank()) {
      null
    } else {
      runCatching {
        loadCustomFontEntries(context)
          .firstOrNull { it.familyName.equals(fontName, ignoreCase = true) }
          ?.let { FontFamily(Font(it.file)) }
      }.getOrNull()
    }
  }

  return TextSubtitleStyle(
    fontFamily = customFamily ?: FontFamily.Default,
    fontSize = fontSize,
    scale = scale,
    borderSize = borderSize,
    shadowOffset = shadowOffset,
    bold = bold,
    italic = italic,
    textColor = Color(textColor),
    borderColor = Color(borderColor),
    backgroundColor = Color(backgroundColor),
    borderStyle = borderStyle,
    justification = justification,
    subPos = subPos,
    scaleByWindow = scaleByWindow,
  )
}

/** Codecs that are images, not text; these always stay on mpv's renderer. */
private val BITMAP_SUBTITLE_CODECS = setOf(
  "hdmv_pgs_subtitle",
  "pgs",
  "dvd_subtitle",
  "dvdsub",
  "dvb_subtitle",
  "dvbsub",
  "xsub",
)

/** ASS/SSA keep libass so typesetting, signs and karaoke stay exactly as authored. */
private val ASS_SUBTITLE_CODECS = setOf("ass", "ssa")

private fun isPlainTextSubtitle(tracks: List<TrackNode>, id: Int?): Boolean {
  if (id == null || id <= 0) return false
  val track = tracks.firstOrNull { it.id == id } ?: return false
  if (track.image == true) return false
  val codec = track.codec?.lowercase() ?: return false
  return codec !in ASS_SUBTITLE_CODECS && codec !in BITMAP_SUBTITLE_CODECS
}

private const val ASS_REFERENCE_HEIGHT = 720f

// libass treats the font size as the full line height (ascent + descent), which is about
// 1.17x the em size Android uses; this keeps the overlay the same size as mpv's text.
private const val LIBASS_SIZE_TO_EM = 0.85f

// mpv defaults: sub-margin-x = 25, sub-margin-y = 22 (in 720p units).
private const val MPV_MARGIN_X = 25f
private const val MPV_MARGIN_Y = 22f
