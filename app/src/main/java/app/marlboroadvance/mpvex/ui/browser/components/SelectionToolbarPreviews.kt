package app.marlboroadvance.mpvex.ui.browser.components

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Previews use a plain MaterialTheme: MpvexTheme reads preferences through Koin, which isn't
// available in the preview renderer.

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    Surface { content() }
  }
}

@Composable
private fun ToolbarFrame(content: @Composable () -> Unit) {
  Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.BottomCenter) { content() }
}

private val play = SelectionAction(Icons.Rounded.PlayArrow, "Play", {})
private val share = SelectionAction(Icons.Rounded.Share, "Share", {})
private val playlist = SelectionAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to Playlist", {})
private val rename = SelectionAction(Icons.Rounded.DriveFileRenameOutline, "Rename", {})
private val send = SelectionAction(Icons.AutoMirrored.Rounded.DriveFileMove, "Copy or move", {})
private val delete = SelectionAction(Icons.Rounded.Delete, "Delete", {}, destructive = true)
private val info = SelectionAction(Icons.Rounded.Info, "Info", {})
private val markAs = SelectionAction(Icons.Rounded.DoneAll, "Mark as", {})
private val hide = SelectionAction(Icons.Rounded.VisibilityOff, "Hide from library", {})

@Composable
private fun VideoToolbar(single: Boolean, expanded: Boolean = true) {
  SelectionToolbar(
    visible = true,
    expanded = expanded,
    primaryAction = play,
    actions = listOf(share, playlist, if (single) rename else send, delete),
    overflowActions = if (single) listOf(info, markAs, send) else listOf(markAs),
  )
}

@Preview(name = "Videos · one selected", showBackground = true)
@Composable
private fun VideoSingleSelectionPreview() = PreviewTheme { ToolbarFrame { VideoToolbar(single = true) } }

@Preview(name = "Videos · several selected · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VideoMultiSelectionDarkPreview() = PreviewTheme { ToolbarFrame { VideoToolbar(single = false) } }

@Preview(name = "Videos · collapsed while scrolling", showBackground = true)
@Composable
private fun VideoCollapsedPreview() = PreviewTheme { ToolbarFrame { VideoToolbar(single = false, expanded = false) } }

@Preview(name = "Folders · one selected", showBackground = true)
@Composable
private fun FolderSelectionPreview() =
  PreviewTheme {
    ToolbarFrame {
      SelectionToolbar(
        visible = true,
        primaryAction = play,
        actions = listOf(markAs, playlist, rename, delete),
        overflowActions = listOf(hide),
      )
    }
  }

@Preview(name = "Videos · RTL", showBackground = true, locale = "ar")
@Composable
private fun VideoRtlPreview() =
  PreviewTheme {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      ToolbarFrame { VideoToolbar(single = true) }
    }
  }

@Preview(name = "Videos · tablet (labels)", showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun VideoTabletPreview() = PreviewTheme { ToolbarFrame { VideoToolbar(single = false) } }

@Preview(name = "Top bar · selection", showBackground = true)
@Composable
private fun SelectionTopBarPreview() =
  PreviewTheme {
    Column {
      BrowserTopBar(
        title = "Movies",
        isInSelectionMode = true,
        selectedCount = 3,
        totalCount = 48,
        onCancelSelection = {},
        selectionSummary = "3.2 GB · 4h 12m",
        onSelectAll = {},
        onDeselectAll = {},
        onInvertSelection = {},
      )
    }
  }
