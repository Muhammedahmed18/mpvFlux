package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.VideoAspect
import app.marlboroadvance.mpvex.ui.player.controls.components.ControlsButton
import app.marlboroadvance.mpvex.ui.theme.controlColor
import app.marlboroadvance.mpvex.ui.theme.spacing
import dev.vivvvek.seeker.Segment

@Composable
fun TopPlayerControlsPortrait(
    mediaTitle: String?,
    hideBackground: Boolean,
    onBackPress: () -> Unit,
    onOpenSheet: (Sheets) -> Unit,
    viewModel: PlayerViewModel,
) {
    val playlistModeEnabled = viewModel.hasPlaylistSupport()
    val clickEvent = LocalPlayerButtonsClickEvent.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ControlsButton(
                icon = Icons.AutoMirrored.Default.ArrowBack,
                onClick = onBackPress,
                color = controlColor,
                modifier = Modifier.size(48.dp)
            )

            Box(
                modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = playlistModeEnabled,
                        onClick = {
                            clickEvent()
                            onOpenSheet(Sheets.Playlist)
                        },
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    val textShadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )

                    viewModel.getPlaylistInfo()?.let { playlistInfo ->
                        Text(
                            text = playlistInfo,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge.copy(
                                shadow = textShadow,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "|",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                            maxLines = 1,
                            color = controlColor.copy(alpha = 0.5f),
                            overflow = TextOverflow.Clip,
                        )
                    }
                    Text(
                        text = mediaTitle ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            shadow = textShadow,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = controlColor,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

@Composable
fun BottomPlayerControlsPortrait(
    buttons: List<PlayerButton>,
    chapters: List<Segment>,
    currentChapter: Int?,
    isSpeedNonOne: Boolean,
    currentZoom: Float,
    aspect: VideoAspect,
    mediaTitle: String?,
    hideBackground: Boolean,
    decoder: app.marlboroadvance.mpvex.ui.player.Decoder,
    playbackSpeed: Float,
    onBackPress: () -> Unit,
    onOpenSheet: (Sheets) -> Unit,
    onOpenPanel: (Panels) -> Unit,
    viewModel: PlayerViewModel,
    activity: PlayerActivity,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.large)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buttons.forEach { button ->
            RenderPlayerButton(
                button = button,
                chapters = chapters,
                currentChapter = currentChapter,
                isPortrait = true,
                isSpeedNonOne = isSpeedNonOne,
                currentZoom = currentZoom,
                aspect = aspect,
                mediaTitle = mediaTitle,
                hideBackground = hideBackground,
                onBackPress = onBackPress,
                onOpenSheet = onOpenSheet,
                onOpenPanel = onOpenPanel,
                viewModel = viewModel,
                activity = activity,
                decoder = decoder,
                playbackSpeed = playbackSpeed,
                buttonSize = 48.dp,
            )
        }
    }
}
