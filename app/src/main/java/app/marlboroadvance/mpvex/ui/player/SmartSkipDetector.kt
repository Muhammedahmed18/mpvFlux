package app.marlboroadvance.mpvex.ui.player

import androidx.annotation.StringRes
import app.marlboroadvance.mpvex.R
import dev.vivvvek.seeker.Segment
import kotlin.math.ceil

internal enum class SmartSkipKind(@StringRes val labelRes: Int) {
  INTRO(R.string.smart_skip_intro), OUTRO(R.string.smart_skip_outro)
}

internal data class SmartSkipRange(val kind: SmartSkipKind, val start: Float, val end: Float)

/** Only explicit chapter labels are eligible; generic numbered scenes never trigger skipping. */
internal object SmartSkipDetector {
  /** Ranges shorter than this are almost certainly marker chapters, not real intros/outros. */
  private const val MIN_RANGE_SECONDS = 1f

  // Compiled once instead of on every chapter lookup.
  private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")

  // Optional trailing index, e.g. "Intro 2", "Opening Part 1", "OP1".
  private const val INDEX = "(?: ?(?:part )?\\d+)?"
  private val intro = Regex(
    "^(?:intro|introduction|opening|opening credits|opening theme|title sequence|op)$INDEX$",
  )
  private val outro = Regex(
    "^(?:outro|ending|ending credits|ending theme|end credits|closing|closing credits|closing theme|ed)$INDEX$",
  )

  private fun kind(title: String): SmartSkipKind? {
    // Any punctuation ("_", "-", brackets, colons, ...) becomes a single space.
    val normalized = title.lowercase().replace(nonAlphanumeric, " ").trim()
    return when {
      intro.matches(normalized) -> SmartSkipKind.INTRO
      outro.matches(normalized) -> SmartSkipKind.OUTRO
      else -> null
    }
  }

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
