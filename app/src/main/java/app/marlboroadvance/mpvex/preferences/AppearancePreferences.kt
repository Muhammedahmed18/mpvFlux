package app.marlboroadvance.mpvex.preferences

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore
import app.marlboroadvance.mpvex.preferences.preference.getEnum
import app.marlboroadvance.mpvex.ui.theme.AppTheme
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

/**
 * Which frame of a video is used for its thumbnail. [fraction] is the position as a share of
 * the duration; null means Smart mode, which samples several candidates and picks the best.
 */
enum class ThumbnailPosition(
  val titleRes: Int,
  val fraction: Double?,
) {
  Smart(R.string.thumbnail_position_smart, null),
  P10(R.string.thumbnail_position_10, 0.10),
  P25(R.string.thumbnail_position_25, 0.25),
  P50(R.string.thumbnail_position_50, 0.50),
  P75(R.string.thumbnail_position_75, 0.75),
}

/**
 * Where the player button rows sit relative to the seekbar. Stored by name, so new entries
 * must be appended rather than renamed to keep existing saved preferences valid.
 */
enum class ControlsPosition {
  Above,
  Below,
}

class AppearancePreferences(
  preferenceStore: PreferenceStore,
) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val appTheme = preferenceStore.getEnum("app_theme", AppTheme.Dynamic)
  val amoledMode = preferenceStore.getBoolean("amoled_mode", false)

  /** On: follow the device system font. Off: use the default sans-serif (Roboto on stock Android). */
  val useSystemFont = preferenceStore.getBoolean("use_system_font", true)

  val unlimitedNameLines = preferenceStore.getBoolean("unlimited_name_lines", false)
  val hidePlayerButtonsBackground = preferenceStore.getBoolean("hide_player_buttons_background", false)
  val showUnplayedOldVideoLabel = preferenceStore.getBoolean("show_unplayed_old_video_label", true)
  val unplayedOldVideoDays = preferenceStore.getInt("unplayed_old_video_days", 7)
  val showNetworkThumbnails = preferenceStore.getBoolean("show_network_thumbnails", false)
  val thumbnailPosition = preferenceStore.getEnum("thumbnail_position", ThumbnailPosition.Smart)
  val seekbarStyle = preferenceStore.getEnum("seekbar_style", SeekbarStyle.Thick)

  /** Split the seekbar at chapter starts (OP / Part A / ED…) when the file has chapters. */
  val showSeekbarChapters = preferenceStore.getBoolean("seekbar_show_chapters", true)

  /** Whether the player button rows sit above (default) or below the seekbar. */
  val controlsPosition = preferenceStore.getEnum("controls_position", ControlsPosition.Above)

  val topLeftControls =
    preferenceStore.getString(
      "top_left_controls",
      "BACK_ARROW,VIDEO_TITLE",
    )

  val topRightControls =
    preferenceStore.getString(
      "top_right_controls",
      "CURRENT_CHAPTER,DECODER,AUDIO_TRACK,SUBTITLES,MORE_OPTIONS",
    )

  val bottomRightControls =
    preferenceStore.getString(
      "bottom_right_controls",
      "FRAME_NAVIGATION,VIDEO_ZOOM,PICTURE_IN_PICTURE,ASPECT_RATIO",
    )

  val bottomLeftControls =
    preferenceStore.getString(
      "bottom_left_controls",
      "BACKGROUND_PLAYBACK,LOCK_CONTROLS,SCREEN_ROTATION,PLAYBACK_SPEED,REPEAT_MODE,SHUFFLE,AB_LOOP",
    )

  val portraitBottomControls =
    preferenceStore.getString(
      "portrait_bottom_controls",
      "SCREEN_ROTATION,DECODER,AUDIO_TRACK,SUBTITLES,BOOKMARKS_CHAPTERS,PLAYBACK_SPEED,BACKGROUND_PLAYBACK,REPEAT_MODE,SHUFFLE,VIDEO_ZOOM,FRAME_NAVIGATION,ASPECT_RATIO,PICTURE_IN_PICTURE,LOCK_CONTROLS,MORE_OPTIONS",
    )

  fun parseButtons(
    csv: String,
    usedButtons: MutableSet<PlayerButton>,
  ): List<PlayerButton> =
    csv
      .splitToSequence(',')
      .map { it.trim().uppercase() }
      .mapNotNull { name ->
        try {
          PlayerButton.valueOf(name)
        } catch (_: IllegalArgumentException) {
          null
        }
      }.filter { it != PlayerButton.NONE }
      .filter { usedButtons.add(it) }
      .toList()
}

@Composable
fun MultiChoiceSegmentedButton(
  choices: ImmutableList<String>,
  selectedIndices: ImmutableList<Int>,
  onClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  MultiChoiceSegmentedButtonRow(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
  ) {
    choices.forEachIndexed { index, choice ->
      SegmentedButton(
        checked = selectedIndices.contains(index),
        onCheckedChange = { onClick(index) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
      ) {
        Text(text = choice)
      }
    }
  }
}
