package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Short pause between the tap and the animated dismiss, so the selection change is visible. */
private const val CHAPTER_DISMISS_DELAY_MS = 220L

@Composable
fun ChaptersSheet(
  chapters: ImmutableList<Segment>,
  currentChapter: Segment,
  onClick: (Segment) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val dismiss = LocalOverlayDismiss.current
  val scope = rememberCoroutineScope()

  // The tapped chapter is highlighted immediately (before mpv reports the new chapter),
  // then the sheet is dismissed through the host's animated dismiss.
  var pendingChapter by remember { mutableStateOf<Segment?>(null) }

  LaunchedEffect(currentChapter, chapters) {
    val index = chapters.indexOf(currentChapter)
    if (index >= 0) {
      listState.scrollToItem(index)
    }
  }

  GenericTracksSheet(
    chapters,
    lazyListState = listState,
    header = {
      SheetHeader(
        title = stringResource(R.string.player_sheets_chapters_title),
        // Same look as the audio sheet's channel badge, but not tappable.
        trailing = {
          SheetStatusBadge(
            text = pluralStringResource(R.plurals.player_chapter_count, chapters.size, chapters.size).uppercase(),
          )
        },
      )
    },
    track = { chapter ->
      val startTime = remember(chapter) { Utils.prettyTime(chapter.start.toInt()) }
      val metadata = remember(chapter) { listOf(TrackMetadata(startTime)) }

      TrackSelectableBar(
        id = chapters.indexOf(chapter) + 1,
        title = chapter.name,
        isSelected = (pendingChapter ?: currentChapter) == chapter,
        onClick = {
          if (pendingChapter == null) {
            pendingChapter = chapter
            // Seek first, then let the selection settle before the animated dismiss.
            onClick(chapter)
            scope.launch {
              delay(CHAPTER_DISMISS_DELAY_MS)
              dismiss()
            }
          }
        },
        metadata = metadata,
      )
    },
    modifier = modifier,
  )
}
