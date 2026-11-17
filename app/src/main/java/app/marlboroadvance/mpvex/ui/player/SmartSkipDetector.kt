package app.marlboroadvance.mpvex.ui.player

import androidx.annotation.StringRes
import app.marlboroadvance.mpvex.R
import dev.vivvvek.seeker.Segment
import kotlin.math.ceil

internal enum class SmartSkipKind(@StringRes val labelRes: Int) {
  INTRO(R.string.smart_skip_intro), OUTRO(R.string.smart_skip_outro)
}

internal data class SmartSkipRange(val kind: SmartSkipKind, val start: Float, val end: Float)

/**
 * Detects intro/outro chapters from their titles.
 *
 * A title is split into parts at separators ("/", "|", ":", " - ", brackets, ...).
 * A part counts when every word in it is either an intro/outro keyword or a
 * harmless filler word ("episode", "start", "theme", "part", numbers, ...).
 * One matching part is enough, so all of these are detected:
 *   "Episode Start/Opening Intro", "OP - Gurenge", "NCOP2", "Opening (Creditless)",
 *   "Ending Credits", "ED + Preview", "Title Sequence", "オープニング".
 * These are deliberately rejected, because they have other real words in them:
 *   "Intro to the case", "Pre-Credits Scene", "Prologue", "Episode Start".
 * If a title names both an intro and an outro ("Opening & Ending") it is ambiguous
 * and ignored. Generic numbered scenes ("Chapter 3") never trigger skipping.
 */
internal object SmartSkipDetector {
  /** Ranges shorter than this are almost certainly marker chapters, not real intros/outros. */
  private const val MIN_RANGE_SECONDS = 1f

  // Compiled once instead of on every chapter lookup.
  private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")

  // Where a title splits into independent parts. A bare hyphen is NOT a separator
  // ("Pre-Credits" must stay one part), but a spaced dash (" - ") or en/em dash is.
  private val partSeparators = Regex("""[/|\\:;,+&~()\[\]{}<>«»「」『』【】]|\s-\s|[–—]""")

  // Strong keywords, with optional "nc" (creditless) prefix and index: "op", "op2", "ncop1", "ending3".
  private val introWord = Regex("^(?:nc)?(?:op|opening|intro|introduction)\\d*$")
  private val outroWord = Regex("^(?:nc)?(?:ed|ending|outro|closing)\\d*$")
  private val introWordsOther = setOf("オープニング", "片头", "片頭", "오프닝")
  private val outroWordsOther = setOf("エンディング", "片尾", "엔딩")

  /** Means outro only when no strong keyword is present ("End Credits" vs "Opening Credits"). */
  private val weakOutroWords = setOf("credits", "credit")

  /** Multi-word intro names rewritten to a single keyword before matching. */
  private val phrases = listOf(
    "title sequence" to "opening",
    "main titles" to "opening",
    "main title" to "opening",
  )

  /** Words allowed next to a keyword without blocking the match. */
  private val fillerWords = setOf(
    "episode", "ep", "start", "begin", "beginning", "end", "part", "pt", "the", "a", "of", "and",
    "main", "theme", "song", "songs", "music", "sequence", "title", "titles", "full", "clean",
    "creditless", "nc", "version", "ver", "v", "tv", "size", "anime", "animation", "series",
    "show", "staff", "roll",
  )

  private fun classifyPart(part: String): SmartSkipKind? {
    var text = " " + part.lowercase().replace(nonAlphanumeric, " ").trim() + " "
    if (text.isBlank()) return null
    for ((from, to) in phrases) text = text.replace(" $from ", " $to ")

    var intro = false
    var outro = false
    var weakOutro = false
    for (word in text.split(' ')) {
      when {
        word.isEmpty() -> Unit
        introWord.matches(word) || word in introWordsOther -> intro = true
        outroWord.matches(word) || word in outroWordsOther -> outro = true
        word in weakOutroWords -> weakOutro = true
        word in fillerWords || word.all(Char::isDigit) -> Unit
        else -> return null // any other real word means this part is content, not an intro/outro
      }
    }
    return when {
      intro && outro -> null
      intro -> SmartSkipKind.INTRO
      outro || weakOutro -> SmartSkipKind.OUTRO
      else -> null
    }
  }

  internal fun kind(title: String): SmartSkipKind? =
    // Exactly one kind across all parts; a title naming both is ambiguous.
    title.split(partSeparators).mapNotNull(::classifyPart).toSet().singleOrNull()

  fun ranges(chapters: List<Segment>, duration: Int): List<SmartSkipRange> {
    if (duration <= 0 || chapters.isEmpty()) return emptyList()
    val limit = duration.toFloat()
    // Drop invalid markers first so they can never act as a "next chapter boundary".
    val ordered = chapters
      .filter { it.start.isFinite() && it.start >= 0f }
      .sortedBy { it.start }

    val result = mutableListOf<SmartSkipRange>()
    for (index in ordered.indices) {
      val chapter = ordered[index]
      val type = kind(chapter.name) ?: continue
      val start = chapter.start
      if (start >= limit) continue

      // A named segment ends at the next *distinct* chapter boundary (duplicate start
      // times would otherwise produce an empty range). Unrelated chapters are never merged.
      var nextStart: Float? = null
      for (i in index + 1 until ordered.size) {
        if (ordered[i].start > start) {
          nextStart = ordered[i].start
          break
        }
      }
      val end = (nextStart ?: limit).coerceAtMost(limit)
      if (end - start < MIN_RANGE_SECONDS) continue
      result += SmartSkipRange(type, start, end)
    }
    return result
  }

  fun active(ranges: List<SmartSkipRange>, position: Int): SmartSkipRange? {
    val p = position.toFloat()
    return ranges.firstOrNull { p >= it.start && p < it.end }
  }

  fun seekTarget(range: SmartSkipRange, duration: Int): Int =
    // coerceIn throws if max < min, so guard against a transient/invalid duration.
    ceil(range.end.toDouble()).toInt().coerceIn(0, duration.coerceAtLeast(0))
}
