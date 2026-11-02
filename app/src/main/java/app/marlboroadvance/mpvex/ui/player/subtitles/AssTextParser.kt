package app.marlboroadvance.mpvex.ui.player.subtitles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Parses the current subtitle line as mpv reports it through `sub-text/ass`.
 *
 * mpv converts text subtitles (SRT, WebVTT, mov_text, ...) to ASS events internally, so
 * formatting such as SRT `<i>`, `<b>`, `<u>` and `<font color>` arrives as ASS override
 * tags (`{\i1}`, `{\b1}`, `{\u1}`, `{\c&HBBGGRR&}`) and line breaks as `\N`. Only the
 * tags that matter for plain text subtitles are interpreted; anything else (karaoke,
 * transforms, drawing commands) is ignored, because real ASS subtitles never come
 * through this path; they stay on libass.
 */
internal object AssTextParser {

  /** One run of text with the formatting active for it. */
  data class Segment(
    val text: String,
    val italic: Boolean,
    val bold: Boolean,
    val underline: Boolean,
    val strikeout: Boolean,
    val color: Color?,
  )

  /** A parsed subtitle line: its segments plus an optional `\anN` alignment (1-9). */
  data class Line(
    val segments: List<Segment>,
    val alignment: Int?,
  ) {
    val isBlank: Boolean get() = segments.all { it.text.isBlank() }
  }

  fun parse(raw: String?): Line {
    if (raw.isNullOrEmpty()) return Line(emptyList(), null)

    val segments = mutableListOf<Segment>()
    val text = StringBuilder()
    var italic = false
    var bold = false
    var underline = false
    var strikeout = false
    var color: Color? = null
    var alignment: Int? = null

    fun flush() {
      if (text.isNotEmpty()) {
        segments += Segment(text.toString(), italic, bold, underline, strikeout, color)
        text.clear()
      }
    }

    var i = 0
    while (i < raw.length) {
      val ch = raw[i]
      when {
        ch == '{' -> {
          val end = raw.indexOf('}', i + 1)
          if (end < 0) {
            // Unbalanced brace: treat the rest as text.
            text.append(raw, i, raw.length)
            i = raw.length
          } else {
            flush()
            val block = raw.substring(i + 1, end)
            for (tag in block.split('\\')) {
              when {
                tag.isEmpty() -> Unit
                tag == "i1" -> italic = true
                tag == "i0" -> italic = false
                tag == "u1" -> underline = true
                tag == "u0" -> underline = false
                tag == "s1" -> strikeout = true
                tag == "s0" -> strikeout = false
                tag.startsWith("b") && tag.drop(1).all(Char::isDigit) && tag.length > 1 -> {
                  val value = tag.drop(1).toIntOrNull() ?: 0
                  bold = value == 1 || value >= 600
                }
                tag.startsWith("an") -> alignment = tag.drop(2).toIntOrNull()?.takeIf { it in 1..9 }
                tag.startsWith("1c&H") || tag.startsWith("c&H") -> color = parseAssColor(tag) ?: color
                tag.startsWith("1c&") || tag.startsWith("c&") -> color = parseAssColor(tag) ?: color
                tag == "r" -> {
                  italic = false; bold = false; underline = false; strikeout = false; color = null
                }
              }
            }
            i = end + 1
          }
        }
        ch == '\\' && i + 1 < raw.length && (raw[i + 1] == 'N' || raw[i + 1] == 'n') -> {
          text.append('\n'); i += 2
        }
        ch == '\\' && i + 1 < raw.length && raw[i + 1] == 'h' -> {
          text.append(' '); i += 2
        }
        ch == '\r' -> i++
        else -> {
          text.append(ch); i++
        }
      }
    }
    flush()
    return Line(segments, alignment)
  }

  /** `c&HBBGGRR&` / `1c&HBBGGRR&` (ASS stores colors as blue-green-red). */
  private fun parseAssColor(tag: String): Color? {
    val hex = tag.substringAfter("&H", "").substringBefore('&').trim()
    if (hex.isEmpty()) return null
    val value = hex.toLongOrNull(16) ?: return null
    val b = ((value shr 16) and 0xFF).toInt()
    val g = ((value shr 8) and 0xFF).toInt()
    val r = (value and 0xFF).toInt()
    return Color(r, g, b)
  }
}

/**
 * Builds the styled text. [withColors] is false for the outline layer, which must be
 * drawn entirely in the outline color.
 */
internal fun AssTextParser.Line.toAnnotatedString(withColors: Boolean): AnnotatedString =
  buildAnnotatedString {
    for (segment in segments) {
      val decorations = buildList {
        if (segment.underline) add(TextDecoration.Underline)
        if (segment.strikeout) add(TextDecoration.LineThrough)
      }
      withStyle(
        SpanStyle(
          fontStyle = if (segment.italic) FontStyle.Italic else null,
          fontWeight = if (segment.bold) FontWeight.Bold else null,
          textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations),
          color = if (withColors && segment.color != null) segment.color else Color.Unspecified,
        ),
      ) {
        append(segment.text)
      }
    }
  }
