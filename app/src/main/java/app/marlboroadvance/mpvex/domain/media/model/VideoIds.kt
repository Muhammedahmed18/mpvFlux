package app.marlboroadvance.mpvex.domain.media.model

/**
 * IDs for videos that don't come from MediaStore (filesystem scans, recently played
 * files, URLs).
 *
 * These used to be `path.hashCode().toLong()`, a 32-bit hash: two different paths could
 * share an id, and because the id is the LazyColumn key, a collision in one list crashed
 * it ("Key ... was already used"). This uses 64-bit FNV-1a, so collisions are practically
 * impossible, and forces the sign bit so the result is always negative and can never
 * clash with a MediaStore _ID (always positive) shown in the same list.
 */
object VideoIds {
  private const val FNV_OFFSET_BASIS = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
  private const val FNV_PRIME = 0x100000001b3L

  fun fromPath(path: String): Long {
    var hash = FNV_OFFSET_BASIS
    for (ch in path) {
      hash = hash xor ch.code.toLong()
      hash *= FNV_PRIME
    }
    return hash or Long.MIN_VALUE
  }
}
