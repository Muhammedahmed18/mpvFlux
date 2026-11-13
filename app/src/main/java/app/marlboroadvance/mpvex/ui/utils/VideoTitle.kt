package app.marlboroadvance.mpvex.ui.utils

// Shared by the video cards and the player playlist.

private val LEADING_TAGS = Regex("^\\s*(\\[[^\\]]*]\\s*)+")
private val TRAILING_TAGS = Regex("(\\s*[\\[(][^\\])]*[\\])])+\\s*$")
private val FILE_EXTENSION = Regex("\\.[A-Za-z0-9]{2,4}$")
private val SPACES = Regex("\\s{2,}")

/**
 * The video's file name as stored on disk. With [showExtension] off, only a trailing
 * extension such as ".mkv" or ".mp4" is dropped; the rest of the name is left untouched.
 */
fun videoFileTitle(fileName: String, showExtension: Boolean): String =
  if (showExtension) fileName else fileName.replace(FILE_EXTENSION, "").ifBlank { fileName }

/**
 * Turns a release file name into a readable title:
 * "[SubsPlease] Frieren - 05 (1080p) [A1B2C3D4].mkv" → "Frieren - 05".
 * Falls back to the original name if nothing is left.
 */
fun cleanVideoTitle(raw: String): String {
  var t = raw.replace(FILE_EXTENSION, "")
  t = t.replace(LEADING_TAGS, "")
  t = t.replace(TRAILING_TAGS, "")
  // Names like "Frieren.S01E05.1080p" or "Frieren_-_05" use dots/underscores as spaces.
  if (!t.contains(' ')) t = t.replace('_', ' ').replace('.', ' ')
  t = t.replace(SPACES, " ").trim()
  return t.ifBlank { raw }
}
