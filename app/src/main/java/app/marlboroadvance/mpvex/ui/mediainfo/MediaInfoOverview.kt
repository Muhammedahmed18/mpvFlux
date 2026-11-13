@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.foundation.ExperimentalFoundationApi::class,
  androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package app.marlboroadvance.mpvex.ui.mediainfo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

private val CardShape = RoundedCornerShape(28.dp)
private const val COLLAPSED_TRACK_COUNT = 5

@Composable
internal fun MediaInfoOverview(
  details: MediaInfoDetails,
  copyActions: CopyActions,
) {
  val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

  BoxWithConstraints(Modifier.fillMaxSize()) {
    if (maxWidth >= 720.dp) {
      // Landscape / tablet: badges and the phone card on the left, details on the right.
      Row(Modifier.fillMaxSize()) {
        LazyColumn(
          modifier = Modifier.weight(0.42f).fillMaxHeight(),
          contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
        ) {
          headerItems(details)
        }
        LazyColumn(
          modifier = Modifier.weight(0.58f).fillMaxHeight(),
          contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
        ) {
          sectionItems(details, copyActions)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
      ) {
        headerItems(details)
        sectionItems(details, copyActions)
      }
    }
  }
}

private fun LazyListScope.headerItems(details: MediaInfoDetails) {
  if (details.badges.isNotEmpty()) {
    item(key = "badges") { BadgeRow(details.badges) }
  }
  details.decoder?.let { verdict ->
    item(key = "decoder") { DecoderCard(verdict) }
  }
}

private fun LazyListScope.sectionItems(details: MediaInfoDetails, copyActions: CopyActions) {
  details.videoBlocks.forEachIndexed { index, block ->
    item(key = "video_$index") { VideoSection(block, copyActions) }
  }
  if (details.audioTracks.isNotEmpty()) {
    item(key = "audio") {
      TrackSection(
        title = stringResource(R.string.media_info_section_audio),
        icon = Icons.Filled.MusicNote,
        tracks = details.audioTracks,
        accent = MaterialTheme.colorScheme.tertiaryContainer,
        onAccent = MaterialTheme.colorScheme.onTertiaryContainer,
        shapeFor = { MaterialShapes.Cookie7Sided.toShape() },
        copyActions = copyActions,
      )
    }
  }
  if (details.subtitleTracks.isNotEmpty()) {
    item(key = "subtitles") {
      TrackSection(
        title = stringResource(R.string.media_info_section_subtitles),
        icon = Icons.Filled.Subtitles,
        tracks = details.subtitleTracks,
        accent = MaterialTheme.colorScheme.primaryContainer,
        onAccent = MaterialTheme.colorScheme.onPrimaryContainer,
        shapeFor = { MaterialShapes.Cookie4Sided.toShape() },
        copyActions = copyActions,
      )
    }
  }
  if (details.fileFields.isNotEmpty()) {
    item(key = "file") { FileSection(details.fileFields, copyActions) }
  }
}

// ── Badges and phone card ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BadgeRow(badges: List<InfoBadge>) {
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    badges.forEach { badge ->
      Surface(
        shape = MaterialTheme.shapes.medium,
        color =
          if (badge.highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor =
          if (badge.highlighted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
      ) {
        Text(
          text = badge.label,
          style = MaterialTheme.typography.labelLargeEmphasized,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DecoderCard(verdict: DecoderVerdict) {
  val hardware = verdict is DecoderVerdict.Hardware
  val accent = if (hardware) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
  val onAccent =
    if (hardware) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    shape = CardShape,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier.size(52.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(accent),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (hardware) Icons.Filled.Memory else Icons.Filled.BatteryAlert,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = onAccent,
        )
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.media_info_phone_title),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text =
            stringResource(if (hardware) R.string.media_info_hw_title else R.string.media_info_sw_title),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text =
            when (verdict) {
              DecoderVerdict.Hardware -> stringResource(R.string.media_info_hw_body)
              is DecoderVerdict.Software -> stringResource(R.string.media_info_sw_body, verdict.reason)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// ── Sections ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionCard(
  title: String,
  icon: ImageVector,
  shape: Shape,
  accent: Color,
  onAccent: Color,
  trailing: String? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    shape = CardShape,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(40.dp).clip(shape).background(accent),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = onAccent)
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          modifier = Modifier.weight(1f),
        )
        trailing?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(Modifier.height(14.dp))
      content()
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoSection(block: VideoBlock, copyActions: CopyActions) {
  SectionCard(
    title = block.title,
    icon = Icons.Filled.Movie,
    shape = MaterialShapes.Sunny.toShape(),
    accent = MaterialTheme.colorScheme.secondaryContainer,
    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
  ) {
    // Two-column grid of value tiles.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      block.fields.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          row.forEach { field ->
            ValueTile(field, copyActions, Modifier.weight(1f))
          }
          if (row.size == 1) Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ValueTile(field: InfoField, copyActions: CopyActions, modifier: Modifier = Modifier) {
  Surface(
    modifier =
      modifier
        .clip(MaterialTheme.shapes.large)
        .combinedClickable(
          onClick = {},
          onLongClick = { copyActions.copyValue(field.label, field.value) },
        ),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      Text(
        text = field.label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = field.value,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun TrackSection(
  title: String,
  icon: ImageVector,
  tracks: List<TrackRow>,
  accent: Color,
  onAccent: Color,
  shapeFor: @Composable () -> Shape,
  copyActions: CopyActions,
) {
  var expanded by rememberSaveable(title) { mutableStateOf(false) }
  val alwaysShown = tracks.take(COLLAPSED_TRACK_COUNT)
  val extra = tracks.drop(COLLAPSED_TRACK_COUNT)
  val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
  val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

  SectionCard(
    title = title,
    icon = icon,
    shape = shapeFor(),
    accent = accent,
    onAccent = onAccent,
    trailing = pluralStringResource(R.plurals.media_info_tracks, tracks.size, tracks.size),
  ) {
    alwaysShown.forEachIndexed { index, track ->
      if (index > 0) TrackDivider()
      TrackRowItem(track, copyActions)
    }
    AnimatedVisibility(
      visible = expanded,
      enter = expandVertically(spatial) + fadeIn(effects),
      exit = shrinkVertically(spatial) + fadeOut(effects),
    ) {
      Column {
        extra.forEach { track ->
          TrackDivider()
          TrackRowItem(track, copyActions)
        }
      }
    }
    if (extra.isNotEmpty()) {
      TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(
          if (expanded) {
            stringResource(R.string.media_info_show_less)
          } else {
            stringResource(R.string.media_info_show_all, tracks.size)
          },
        )
      }
    }
  }
}

@Composable
private fun TrackDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = 2.dp),
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
  )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrackRowItem(track: TrackRow, copyActions: CopyActions) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .combinedClickable(
          onClick = {},
          onLongClick = { copyActions.copyValue(track.headline, track.copyText) },
        )
        .padding(horizontal = 4.dp, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = track.headline,
        style = MaterialTheme.typography.titleSmallEmphasized,
        modifier = Modifier.weight(1f, fill = false),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      track.tags.forEach { tag ->
        Spacer(Modifier.width(6.dp))
        TrackTagChip(tag)
      }
    }
    track.title?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (track.supporting.isNotBlank()) {
      Text(
        text = track.supporting,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun TrackTagChip(tag: TrackTag) {
  val (label, container, content) =
    when (tag) {
      TrackTag.Default ->
        Triple(
          stringResource(R.string.media_info_tag_default),
          MaterialTheme.colorScheme.secondaryContainer,
          MaterialTheme.colorScheme.onSecondaryContainer,
        )
      TrackTag.Forced ->
        Triple(
          stringResource(R.string.media_info_tag_forced),
          MaterialTheme.colorScheme.tertiaryContainer,
          MaterialTheme.colorScheme.onTertiaryContainer,
        )
      TrackTag.ImageBased ->
        Triple(
          stringResource(R.string.media_info_tag_image),
          MaterialTheme.colorScheme.surfaceContainerHighest,
          MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
  Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileSection(fields: List<InfoField>, copyActions: CopyActions) {
  SectionCard(
    title = stringResource(R.string.media_info_section_file),
    icon = Icons.Filled.Description,
    shape = MaterialShapes.Pentagon.toShape(),
    accent = MaterialTheme.colorScheme.surfaceContainerHighest,
    onAccent = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    fields.forEachIndexed { index, field ->
      if (index > 0) TrackDivider()
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
              // Values here (file name, location) are long: a single tap copies them.
              onClick = { copyActions.copyValue(field.label, field.value) },
              onLongClick = { copyActions.copyValue(field.label, field.value) },
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
      ) {
        Text(
          text = field.label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = field.value,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
