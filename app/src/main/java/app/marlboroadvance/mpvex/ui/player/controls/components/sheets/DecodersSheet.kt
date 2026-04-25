package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.Decoder
import kotlinx.collections.immutable.toImmutableList

@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val decoders = remember { Decoder.entries.minusElement(Decoder.Auto).toImmutableList() }

  GenericTracksSheet(
    tracks = decoders,
    track = {
      TrackSelectableBar(
        id = decoders.indexOf(it) + 1,
        title = stringResource(R.string.player_sheets_decoder_formatted, it.title, it.value),
        isSelected = selectedDecoder == it,
        onClick = { onSelect(it) },
      )
    },
    onDismissRequest = onDismissRequest,
  )
}
