package app.marlboroadvance.mpvex.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Small Markdown renderer for GitHub release notes: headings, bullet and numbered lists,
 * block quotes, horizontal rules, tables, and inline **bold**, *italic*, `code` and links.
 * The checksum table that the release workflow appends is left out.
 *
 * Built in rather than a library so the app stays small; release notes only use this subset.
 */
@Composable
fun ReleaseNotes(
  markdown: String,
  modifier: Modifier = Modifier,
  maxBlocks: Int = Int.MAX_VALUE,
) {
  val blocks = remember(markdown) { parseBlocks(stripChecksums(markdown)) }
  val linkColor = MaterialTheme.colorScheme.primary
  val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    blocks.take(maxBlocks).forEach { block ->
      when (block) {
        is Block.Heading -> Text(
          text = inline(block.text, linkColor, codeBackground),
          style = when (block.level) {
            1 -> MaterialTheme.typography.titleLargeEmphasized
            2 -> MaterialTheme.typography.titleMediumEmphasized
            else -> MaterialTheme.typography.titleSmallEmphasized
          },
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = if (block.level <= 2) 8.dp else 4.dp),
        )

        is Block.Paragraph -> Text(
          text = inline(block.text, linkColor, codeBackground),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is Block.ListItem -> Row(modifier = Modifier.padding(start = (block.indent * 16).dp)) {
          Text(
            text = block.marker,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp),
          )
          Text(
            text = inline(block.text, linkColor, codeBackground),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        is Block.Quote -> Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
          Box(
            modifier = Modifier
              .width(4.dp)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.primary),
          )
          Text(
            text = inline(block.text, linkColor, codeBackground),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }

        Block.Rule -> HorizontalDivider(
          modifier = Modifier.padding(vertical = 4.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )

        is Block.Table -> Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
          block.rows.forEachIndexed { index, cells ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              cells.forEach { cell ->
                Text(
                  text = inline(cell, linkColor, codeBackground),
                  style = if (index == 0) {
                    MaterialTheme.typography.labelLarge
                  } else {
                    MaterialTheme.typography.bodySmall
                  },
                  color = if (index == 0) {
                    MaterialTheme.colorScheme.onSurface
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Parsing
// ---------------------------------------------------------------------------

private sealed interface Block {
  data class Heading(val level: Int, val text: String) : Block
  data class Paragraph(val text: String) : Block
  data class ListItem(val marker: String, val text: String, val indent: Int) : Block
  data class Quote(val text: String) : Block
  data object Rule : Block
  data class Table(val rows: List<List<String>>) : Block
}

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val NUMBERED = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val RULE = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
private val TABLE_SEPARATOR = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

/** Drops the "### Checksums" section the release workflow adds at the end. */
private fun stripChecksums(markdown: String): String {
  val index = markdown.indexOf("### Checksums")
  if (index < 0) return markdown
  return markdown.substring(0, index).trimEnd().removeSuffix("---").trimEnd()
}

private fun parseBlocks(markdown: String): List<Block> {
  val blocks = mutableListOf<Block>()
  val paragraph = StringBuilder()
  val table = mutableListOf<List<String>>()

  fun flushParagraph() {
    if (paragraph.isNotBlank()) blocks += Block.Paragraph(paragraph.toString().trim())
    paragraph.clear()
  }
  fun flushTable() {
    if (table.isNotEmpty()) blocks += Block.Table(table.toList())
    table.clear()
  }

  for (raw in markdown.replace("\r\n", "\n").lines()) {
    val line = raw.trimEnd()
    val trimmed = line.trim()

    if (trimmed.startsWith("|")) {
      flushParagraph()
      if (!TABLE_SEPARATOR.matches(trimmed)) {
        table += trimmed.trim('|').split('|').map { it.trim() }
      }
      continue
    } else {
      flushTable()
    }

    when {
      trimmed.isEmpty() -> flushParagraph()
      RULE.matches(trimmed) -> {
        flushParagraph()
        blocks += Block.Rule
      }
      HEADING.matches(trimmed) -> {
        flushParagraph()
        val m = HEADING.matchEntire(trimmed)!!
        blocks += Block.Heading(m.groupValues[1].length, m.groupValues[2].trim())
      }
      trimmed.startsWith(">") -> {
        flushParagraph()
        val text = trimmed.removePrefix(">").trim()
        val last = blocks.lastOrNull()
        if (last is Block.Quote) {
          blocks[blocks.lastIndex] = Block.Quote(last.text + " " + text)
        } else {
          blocks += Block.Quote(text)
        }
      }
      BULLET.matches(line) -> {
        flushParagraph()
        val m = BULLET.matchEntire(line)!!
        blocks += Block.ListItem("•", m.groupValues[2], m.groupValues[1].length / 2)
      }
      NUMBERED.matches(line) -> {
        flushParagraph()
        val m = NUMBERED.matchEntire(line)!!
        blocks += Block.ListItem("${m.groupValues[2]}.", m.groupValues[3], m.groupValues[1].length / 2)
      }
      else -> {
        if (paragraph.isNotEmpty()) paragraph.append(' ')
        paragraph.append(trimmed)
      }
    }
  }
  flushParagraph()
  flushTable()
  return blocks
}

// ---------------------------------------------------------------------------
// Inline styles
// ---------------------------------------------------------------------------

private val INLINE = Regex(
  "\\*\\*(.+?)\\*\\*" + // 1: bold
    "|`([^`]+)`" + // 2: code
    "|\\[([^\\]]+)]\\(([^)\\s]+)\\)" + // 3,4: link
    "|(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])", // 5: italic
)

private fun inline(text: String, linkColor: Color, codeBackground: Color): AnnotatedString =
  buildAnnotatedString {
    var cursor = 0
    for (match in INLINE.findAll(text)) {
      append(text.substring(cursor, match.range.first))
      val g = match.groupValues
      when {
        g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
          append(inline(g[1], linkColor, codeBackground))
        }
        g[2].isNotEmpty() -> withStyle(
          SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
        ) { append(g[2]) }
        g[3].isNotEmpty() -> withLink(
          LinkAnnotation.Url(
            url = g[4],
            styles = TextLinkStyles(
              style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
            ),
          ),
        ) { append(g[3]) }
        g[5].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
          append(inline(g[5], linkColor, codeBackground))
        }
      }
      cursor = match.range.last + 1
    }
    append(text.substring(cursor))
  }
