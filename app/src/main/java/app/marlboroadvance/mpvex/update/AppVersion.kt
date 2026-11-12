package app.marlboroadvance.mpvex.update

/**
 * A release version such as "1.4.4", "v1.4.4" or "V1.5.0-beta1".
 *
 * Numbers are compared part by part (1.10.0 > 1.9.2). With equal numbers a normal release
 * is newer than a pre-release of it (1.5.0 > 1.5.0-beta1); pre-release labels compare as text.
 */
data class AppVersion(
  val numbers: List<Int>,
  val preRelease: String?,
) : Comparable<AppVersion> {

  override fun compareTo(other: AppVersion): Int {
    val size = maxOf(numbers.size, other.numbers.size)
    for (i in 0 until size) {
      val a = numbers.getOrElse(i) { 0 }
      val b = other.numbers.getOrElse(i) { 0 }
      if (a != b) return a.compareTo(b)
    }
    return when {
      preRelease == other.preRelease -> 0
      preRelease == null -> 1
      other.preRelease == null -> -1
      else -> preRelease.compareTo(other.preRelease, ignoreCase = true)
    }
  }

  override fun toString(): String =
    numbers.joinToString(".") + (preRelease?.let { "-$it" } ?: "")

  companion object {
    /** Parses a tag or versionName; returns null if it doesn't start with a number. */
    fun parse(raw: String): AppVersion? {
      val clean = raw.trim().removePrefix("v").removePrefix("V")
      val main = clean.substringBefore('-')
      val numbers = main.split('.').map { it.toIntOrNull() ?: return null }
      if (numbers.isEmpty()) return null
      val pre = clean.substringAfter('-', "").ifBlank { null }
      return AppVersion(numbers, pre)
    }
  }
}
