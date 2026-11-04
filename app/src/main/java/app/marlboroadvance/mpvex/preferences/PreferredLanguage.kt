package app.marlboroadvance.mpvex.preferences

import androidx.annotation.StringRes
import app.marlboroadvance.mpvex.R

/**
 * Languages offered by the "Preferred language" pickers (audio and subtitles).
 *
 * Video files tag tracks with different codes for the same language: two-letter ("en"),
 * three-letter ("eng"), the older bibliographic form ("ger" vs "deu") or with a region
 * ("en-US", "pt-BR", "es-419"). [aliases] lists every form so a pick always matches.
 *
 * The preference stores [code] ("" = video default). Older free-text values such as
 * "eng,jpn" or "English" are read through [fromStored], so nothing needs migrating.
 */
enum class PreferredLanguage(
  val code: String,
  @StringRes val title: Int,
  /** Name in the language itself, shown next to the English name. Null for English. */
  val nativeName: String?,
  val aliases: Set<String>,
) {
  English("en", R.string.lang_english, null, setOf("en", "eng")),
  Japanese("ja", R.string.lang_japanese, "日本語", setOf("ja", "jpn", "jp")),
  Urdu("ur", R.string.lang_urdu, "اردو", setOf("ur", "urd")),
  Hindi("hi", R.string.lang_hindi, "हिन्दी", setOf("hi", "hin")),
  Arabic("ar", R.string.lang_arabic, "العربية", setOf("ar", "ara")),
  Spanish("es", R.string.lang_spanish, "Español", setOf("es", "spa")),
  Portuguese("pt", R.string.lang_portuguese, "Português", setOf("pt", "por")),
  French("fr", R.string.lang_french, "Français", setOf("fr", "fra", "fre")),
  German("de", R.string.lang_german, "Deutsch", setOf("de", "deu", "ger")),
  Indonesian("id", R.string.lang_indonesian, "Bahasa Indonesia", setOf("id", "ind", "in")),
  Chinese("zh", R.string.lang_chinese, "中文", setOf("zh", "zho", "chi", "chs", "cht")),
  Korean("ko", R.string.lang_korean, "한국어", setOf("ko", "kor")),
  ;

  /** English names used to recognise untagged tracks by their title, e.g. "English Subs". */
  private val englishName: String get() = name.lowercase()

  /**
   * True if a track's language tag is this language. Tags are compared on their main part
   * only, so "en-US", "pt_BR" and "es-419" match English, Portuguese and Spanish.
   */
  fun matchesTag(tag: String): Boolean {
    val main = tag.trim().lowercase().substringBefore('-').substringBefore('_')
    return main.isNotEmpty() && main in aliases
  }

  /** True if an untagged track's title names this language ("English", "[Eng] Dialogue"). */
  fun matchesTitle(title: String): Boolean {
    val t = title.lowercase()
    if (englishName in t) return true
    return aliases.any { alias -> alias.length >= 3 && Regex("\\b$alias\\b").containsMatchIn(t) }
  }

  companion object {
    /**
     * Reads the stored preference. Blank means "video default" (null). Old free-text values
     * use their first entry: "eng,jpn" → English, "Japanese" → Japanese. Anything that
     * can't be recognised also falls back to video default.
     */
    fun fromStored(value: String): PreferredLanguage? {
      val first = value.split(',').map { it.trim().lowercase() }.firstOrNull { it.isNotEmpty() }
        ?: return null
      return entries.firstOrNull { it.code == first }
        ?: entries.firstOrNull { it.matchesTag(first) }
        ?: entries.firstOrNull { it.englishName == first }
    }
  }
}
