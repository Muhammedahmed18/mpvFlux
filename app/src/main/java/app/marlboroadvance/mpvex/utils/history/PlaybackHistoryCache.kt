package app.marlboroadvance.mpvex.utils.history

import app.marlboroadvance.mpvex.database.MpvExDatabase
import app.marlboroadvance.mpvex.database.dao.PlaybackBadgeRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.java.KoinJavaComponent.inject

/**
 * One shared, in-memory view of the playback history the watched/progress/NEW badges need.
 *
 * Saved progress is read from the database in a single query and kept as a map keyed by file
 * name (the same key the player and "Mark as" use). Screens look videos up in the map instead
 * of querying the database once per video. Room re-emits when the tables change, so the
 * badges stay live. The flows are `null` until the first database read has finished, so a
 * consumer can wait for real data instead of briefly showing every video as unplayed.
 */
object PlaybackHistoryCache {
  private val database: MpvExDatabase by inject(MpvExDatabase::class.java)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  /** Saved progress by file name. */
  val playback: StateFlow<Map<String, PlaybackBadgeRow>?> by lazy {
    database
      .videoDataDao()
      .observePlaybackBadgeRows()
      .map<List<PlaybackBadgeRow>, Map<String, PlaybackBadgeRow>?> { rows -> rows.associateBy { it.mediaTitle } }
      .distinctUntilChanged()
      .flowOn(Dispatchers.IO)
      .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
  }

  /** File names marked as New from the "Mark as" sheet. */
  val markedNew: StateFlow<Set<String>?> by lazy {
    database
      .videoMarkDao()
      .observeMarkedTitles()
      .map<List<String>, Set<String>?> { it.toSet() }
      .distinctUntilChanged()
      .flowOn(Dispatchers.IO)
      .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
  }
}
