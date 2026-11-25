package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ChaptersSheet(
  chapters: ImmutableList<Segment>,
  currentChapter: Segment,
  onClick: (Segment) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val dismiss = LocalOverlayDismiss.current

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
        trailing = { ChapterCountPill(count = chapters.size) },
      )
    },
    track = {
      ChapterRow(
        it,
        index = chapters.indexOf(it),
        selected = currentChapter == it,
        // Seek first, then close the sheet with the host's animated dismiss.
        onClick = {
          onClick(it)
          dismiss()
        },
      )
    },
    modifier = modifier,
  )
}

/**
 * Count chip in the header — same look as [TrackCountPill], with chapter wording.
 */
@Composable
private fun ChapterCountPill(count: Int) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Text(
      text     = pluralStringResource(R.plurals.player_chapter_count, count, count),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      style    = MaterialTheme.typography.labelSmall.copy(
        letterSpacing       = 0.5.sp,
        fontFeatureSettings = "tnum",
      ),
      color    = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * One chapter: `01  Title  ✓`. The index is primary-colored; the current chapter sits
 * in a tonal container and carries the check. Start time reads as secondary text.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChapterRow(
  chapter: Segment,
  index: Int,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .semantics { this.selected = selected },
    shape = MaterialTheme.shapes.large,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = (index + 1).toString().padStart(2, '0'),
        style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = chapter.name,
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = Utils.prettyTime(chapter.start.toInt()),
          style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
          color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}
