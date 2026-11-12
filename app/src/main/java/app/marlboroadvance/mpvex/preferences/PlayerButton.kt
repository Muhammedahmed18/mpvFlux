package app.marlboroadvance.mpvex.preferences

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import app.marlboroadvance.mpvex.R

/**
 * Represents a customizable button in the player controls.
 *
 * [iconRes] is a Material Symbols Rounded drawable; the same icon is used in the player and
 * in Settings → Player layout so both always match.
 */
enum class PlayerButton(
  @DrawableRes val iconRes: Int,
) {
  BACK_ARROW(R.drawable.ic_sym_arrow_back),
  VIDEO_TITLE(R.drawable.ic_sym_title),
  BOOKMARKS_CHAPTERS(R.drawable.ic_sym_toc),
  PLAYBACK_SPEED(R.drawable.ic_sym_speed),
  DECODER(R.drawable.ic_sym_memory),
  SCREEN_ROTATION(R.drawable.ic_sym_screen_rotation),
  FRAME_NAVIGATION(R.drawable.ic_sym_filter_frames),
  VIDEO_ZOOM(R.drawable.ic_sym_zoom_in),
  PICTURE_IN_PICTURE(R.drawable.ic_sym_picture_in_picture_alt),
  ASPECT_RATIO(R.drawable.ic_sym_aspect_ratio),
  LOCK_CONTROLS(R.drawable.ic_sym_lock_open),
  AUDIO_TRACK(R.drawable.ic_sym_music_note),
  SUBTITLES(R.drawable.ic_sym_closed_caption),
  MORE_OPTIONS(R.drawable.ic_sym_more_vert),
  CURRENT_CHAPTER(R.drawable.ic_sym_bookmark),
  REPEAT_MODE(R.drawable.ic_sym_repeat),
  SHUFFLE(R.drawable.ic_sym_shuffle),
  MIRROR(R.drawable.ic_sym_flip),
  VERTICAL_FLIP(R.drawable.ic_sym_flip), // drawn rotated 90°
  AB_LOOP(R.drawable.ic_sym_laps),
  CUSTOM_SKIP(R.drawable.ic_sym_fast_forward),
  BACKGROUND_PLAYBACK(R.drawable.ic_sym_headphones),
  NONE(R.drawable.ic_sym_block),
}

/** The button's icon as an [ImageVector] (Material Symbols Rounded). */
val PlayerButton.icon: ImageVector
  @Composable get() = ImageVector.vectorResource(iconRes)

/**
 * A list of all buttons that the user can choose from in the customization menu.
 * Excludes NONE (placeholder) and constant buttons (BACK_ARROW, VIDEO_TITLE).
 */
val allPlayerButtons =
  PlayerButton.values().filter {
    it != PlayerButton.NONE &&
      it != PlayerButton.BACK_ARROW &&
      it != PlayerButton.VIDEO_TITLE
  }

/**
 * Gets the human-readable label for a player button.
 * TODO: You must add these string resources to your `strings.xml` file.
 */
@Composable
fun getPlayerButtonLabel(button: PlayerButton): String =
  when (button) {
    PlayerButton.BACK_ARROW -> "Back Arrow" // stringResource(R.string.btn_label_back)
    PlayerButton.VIDEO_TITLE -> "Video Title" // stringResource(R.string.btn_label_title)
    PlayerButton.BOOKMARKS_CHAPTERS -> "Chapters / Bookmarks" // stringResource(R.string.btn_label_bookmarks)
    PlayerButton.PLAYBACK_SPEED -> "Playback Speed" // stringResource(R.string.btn_label_speed)
    PlayerButton.DECODER -> "Decoder" // stringResource(R.string.btn_label_decoder)
    PlayerButton.SCREEN_ROTATION -> "Screen Rotation" // stringResource(R.string.btn_label_rotation)
    PlayerButton.FRAME_NAVIGATION -> "Frame Navigation" // stringResource(R.string.btn_label_frame_nav)
    PlayerButton.VIDEO_ZOOM -> "Video Zoom" // stringResource(R.string.btn_label_zoom)
    PlayerButton.PICTURE_IN_PICTURE -> "Picture-in-Picture" // stringResource(R.string.btn_label_pip)
    PlayerButton.ASPECT_RATIO -> "Aspect Ratio" // stringResource(R.string.btn_label_aspect)
    PlayerButton.LOCK_CONTROLS -> "Lock Controls" // stringResource(R.string.btn_label_lock)
    PlayerButton.AUDIO_TRACK -> "Audio Track" // stringResource(R.string.btn_label_audio)
    PlayerButton.SUBTITLES -> "Subtitles" // stringResource(R.string.btn_label_subtitles)
    PlayerButton.MORE_OPTIONS -> "More Options" // stringResource(R.string.btn_label_more)
    PlayerButton.CURRENT_CHAPTER -> "Current Chapter" // stringResource(R.string.btn_label_chapter)
    PlayerButton.REPEAT_MODE -> "Repeat Mode" // stringResource(R.string.btn_label_repeat_mode)
    PlayerButton.SHUFFLE -> "Shuffle" // stringResource(R.string.btn_label_shuffle)
    PlayerButton.MIRROR -> "Horizontal Flip"
    PlayerButton.VERTICAL_FLIP -> "Vertical Flip"
    PlayerButton.AB_LOOP -> "A-B Loop"
    PlayerButton.CUSTOM_SKIP -> "Custom Skip"
    PlayerButton.BACKGROUND_PLAYBACK -> "Background Playback"
    PlayerButton.NONE -> "None"
  }
