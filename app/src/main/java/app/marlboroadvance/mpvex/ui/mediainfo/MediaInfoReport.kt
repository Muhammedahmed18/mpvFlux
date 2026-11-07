@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package app.marlboroadvance.mpvex.ui.mediainfo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

@Composable
internal fun MediaInfoReport(sections: List<ReportSection>, copyActions: CopyActions) {
  var query by rememberSaveable { mutableStateOf("") }
  // Section names that are open. "General" starts open; the rest start collapsed.
  val expanded =
    rememberSaveable(sections, saver = expandedSaver()) {
      sections.take(1).map { it.name }.toMutableStateList()
    }

  val trimmedQuery = query.trim()
  val filtered =
    remember(sections, trimmedQuery) {
      if (trimmedQuery.isEmpty()) {
        sections
      } else {
        sections.mapNotNull { section ->
          val matches =
            section.properties.filter { field ->
              field.label.contains(trimmedQuery, ignoreCase = true) ||
                field.value.contains(trimmedQuery, ignoreCase = true)
            }
          when {
            section.name.contains(trimmedQuery, ignoreCase = true) -> section
            matches.isNotEmpty() -> section.copy(properties = matches)
            else -> null
          }
        }
      }
    }
  val searching = trimmedQuery.isNotEmpty()
  val allExpanded = sections.isNotEmpty() && sections.all { it.name in expanded }
  val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(top = 12.dp, bottom = bottomPadding),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "search") {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text(stringResource(R.string.media_info_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.media_info_clear_search))
            }
          }
        },
        singleLine = true,
        shape = CircleShape,
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ),
      )
    }

    if (!searching && sections.isNotEmpty()) {
      item(key = "expand_all") {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterEnd) {
          TextButton(
            onClick = {
              expanded.clear()
              if (!allExpanded) expanded.addAll(sections.map { it.name })
            },
          ) {
            Icon(
              imageVector = if (allExpanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
              contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
              stringResource(if (allExpanded) R.string.media_info_collapse_all else R.string.media_info_expand_all),
            )
          }
        }
      }
    }

    if (searching && filtered.isEmpty()) {
      item(key = "no_matches") {
        Text(
          text = stringResource(R.string.media_info_no_matches, trimmedQuery),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth().padding(32.dp),
        )
      }
    }

    // MediaInfo can repeat a section name, so the index is part of the key.
    itemsIndexed(filtered, key = { index, section -> "section_${index}_${section.name}" }) { _, section ->
      ReportSectionCard(
        section = section,
        // While searching, every section with a match is shown open.
        isExpanded = searching || section.name in expanded,
        onToggle = {
          if (!searching) {
            if (section.name in expanded) expanded.remove(section.name) else expanded.add(section.name)
          }
        },
        copyActions = copyActions,
      )
    }
  }
}

@Composable
private fun ReportSectionCard(
  section: ReportSection,
  isExpanded: Boolean,
  onToggle: () -> Unit,
  copyActions: CopyActions,
) {
  val chevronRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    label = "chevron",
  )
  val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
  val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = section.name,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = section.properties.size.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.width(8.dp))
      Icon(
        Icons.Filled.ExpandMore,
        contentDescription = null,
        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(spatial) + fadeIn(effects),
      exit = shrinkVertically(spatial) + fadeOut(effects),
    ) {
      Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
        section.properties.forEachIndexed { index, field ->
          if (index > 0) {
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 8.dp),
              thickness = 0.5.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
          }
          ReportRow(field, copyActions)
        }
      }
    }
  }
}

@Composable
private fun ReportRow(field: InfoField, copyActions: CopyActions) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .combinedClickable(
          onClick = {},
          onLongClick = { copyActions.copyValue(field.label, field.value) },
        )
        .padding(horizontal = 8.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = field.label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.42f),
    )
    Text(
      text = field.value,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(0.58f),
    )
  }
}

/** Saves the open section names across rotation and process death. */
private fun expandedSaver() =
  androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
  )
