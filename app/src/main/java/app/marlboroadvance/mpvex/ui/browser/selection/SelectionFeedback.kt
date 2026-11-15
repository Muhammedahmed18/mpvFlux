package app.marlboroadvance.mpvex.ui.browser.selection

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.utils.media.MediaUtils

/**
 * A light haptic tick each time the selection count changes while selection mode stays on.
 * Entering selection mode isn't ticked here: the long-press that starts it already gives
 * haptic feedback.
 */
@Composable
fun SelectionHaptics(selectedCount: Int) {
  val haptics = LocalHapticFeedback.current
  val previous = remember { mutableIntStateOf(selectedCount) }
  LaunchedEffect(selectedCount) {
    val before = previous.intValue
    previous.intValue = selectedCount
    if (before > 0 && selectedCount > 0 && before != selectedCount) {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }
}

/** Plays [videos] in order: one video plays normally, several play as a temporary queue. */
fun playVideosAsQueue(
  context: Context,
  videos: List<Video>,
) {
  if (videos.isEmpty()) return
  if (videos.size == 1) {
    MediaUtils.playFile(videos.first(), context)
    return
  }
  val first = videos.first()
  val intent =
    Intent(Intent.ACTION_VIEW, first.uri).apply {
      setClass(context, PlayerActivity::class.java)
      putExtra("internal_launch", true)
      putParcelableArrayListExtra("playlist", ArrayList(videos.map { it.uri }))
      putExtra("playlist_index", 0)
      putExtra("launch_source", "playlist")
      putExtra("title", first.displayName)
      putExtra("absolute_path", first.path)
      putExtra("video_id", first.id)
      putExtra("date_modified", first.dateModified)
      putExtra("size", first.size)
    }
  context.startActivity(intent)
}
