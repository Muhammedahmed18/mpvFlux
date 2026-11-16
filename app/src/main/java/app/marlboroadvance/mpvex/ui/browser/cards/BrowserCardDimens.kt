package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Row sizes shared by video and folder cards, chosen from the screen width and the
 * user's font size, so both kinds of row line up and grow together.
 *
 * Width tiers follow Material's window size classes (compact < 600dp, medium < 840dp,
 * expanded), with an extra tier for very small phones (< 360dp). Lists can call
 * [rememberBrowserCardDimens] once and pass the result to every row.
 */
@Immutable
data class BrowserCardDimens(
  val rowPadding: Dp,
  val thumbTextGap: Dp,
  // Video thumbnail: this share of the row, kept between the min and max widths.
  val thumbnailFraction: Float,
  val thumbnailMinWidth: Dp,
  val thumbnailMaxWidth: Dp,
  val detailsSpacing: Dp,
  val chipSpacing: Dp,
  val chipSize: UnifiedChipSize,
  // Very large fonts on a phone: video thumbnail on top, text underneath at full width.
  val stackThumbnail: Boolean,
  // Folder tile, its icon, and the selection badge on it.
  val folderTileSize: Dp,
  val folderIconSize: Dp,
  val folderBadgeSize: Dp,
  val folderPathMaxLines: Int,
) {
  companion object {
    fun forScreen(screenWidthDp: Int, fontScale: Float): BrowserCardDimens {
      val base = when {
        screenWidthDp < 360 -> BrowserCardDimens(
          rowPadding = 6.dp, thumbTextGap = 10.dp,
          thumbnailFraction = 0.45f, thumbnailMinWidth = 112.dp, thumbnailMaxWidth = 150.dp,
          detailsSpacing = 6.dp, chipSpacing = 4.dp, chipSize = UnifiedChipSize.Compact,
          stackThumbnail = false,
          folderTileSize = 48.dp, folderIconSize = 24.dp, folderBadgeSize = 20.dp, folderPathMaxLines = 2,
        )
        screenWidthDp < 600 -> BrowserCardDimens(
          rowPadding = 8.dp, thumbTextGap = 12.dp,
          thumbnailFraction = 0.45f, thumbnailMinWidth = 128.dp, thumbnailMaxWidth = 184.dp,
          detailsSpacing = 8.dp, chipSpacing = 6.dp, chipSize = UnifiedChipSize.Medium,
          stackThumbnail = false,
          folderTileSize = 56.dp, folderIconSize = 28.dp, folderBadgeSize = 22.dp, folderPathMaxLines = 2,
        )
        screenWidthDp < 840 -> BrowserCardDimens(
          rowPadding = 10.dp, thumbTextGap = 16.dp,
          thumbnailFraction = 0.36f, thumbnailMinWidth = 176.dp, thumbnailMaxWidth = 240.dp,
          detailsSpacing = 8.dp, chipSpacing = 6.dp, chipSize = UnifiedChipSize.Medium,
          stackThumbnail = false,
          folderTileSize = 64.dp, folderIconSize = 32.dp, folderBadgeSize = 26.dp, folderPathMaxLines = 2,
        )
        else -> BrowserCardDimens(
          rowPadding = 12.dp, thumbTextGap = 20.dp,
          thumbnailFraction = 0.30f, thumbnailMinWidth = 200.dp, thumbnailMaxWidth = 280.dp,
          detailsSpacing = 10.dp, chipSpacing = 8.dp, chipSize = UnifiedChipSize.Medium,
          stackThumbnail = false,
          folderTileSize = 72.dp, folderIconSize = 36.dp, folderBadgeSize = 28.dp, folderPathMaxLines = 2,
        )
      }
      return when {
        fontScale >= 1.7f && screenWidthDp < 600 -> base.copy(stackThumbnail = true, folderPathMaxLines = 1)
        // Bigger text needs more room beside the thumbnail, so the thumbnail gives some up,
        // and the folder path keeps to one line so the row doesn't get very tall.
        fontScale >= 1.3f -> base.copy(
          thumbnailFraction = base.thumbnailFraction * 0.85f,
          thumbnailMinWidth = base.thumbnailMinWidth - 16.dp,
          folderPathMaxLines = 1,
        )
        else -> base
      }
    }
  }
}

/** Card sizes for the current screen width and font size; updates on rotation or resize. */
@Composable
fun rememberBrowserCardDimens(): BrowserCardDimens {
  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val fontScale = LocalDensity.current.fontScale
  return remember(screenWidthDp, fontScale) { BrowserCardDimens.forScreen(screenWidthDp, fontScale) }
}

/** Old name, kept so earlier callers still compile. */
typealias VideoCardDimens = BrowserCardDimens

/** Old name, kept so earlier callers still compile. */
@Composable
fun rememberVideoCardDimens(): BrowserCardDimens = rememberBrowserCardDimens()
