package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.PreferredLanguage
import me.zhanghai.compose.preference.Preference

/**
 * "Preferred language" setting: shows the current choice and opens an M3 dialog with one
 * radio button per language plus "Video default". Picking an option saves it and closes
 * the dialog. [value] is the stored preference string ("" = video default).
 * [showOffOption] adds "Off" at the top (subtitles only), stored as [PreferredLanguage.OFF].
 */
@Composable
fun LanguagePickerPreference(
  title: String,
  value: String,
  onValueChange: (String) -> Unit,
  showOffOption: Boolean = false,
) {
  var showDialog by remember { mutableStateOf(false) }
  val isOff = showOffOption && PreferredLanguage.isOff(value)
  val selected = remember(value) { PreferredLanguage.fromStored(value) }

  Preference(
    title = {
      Text(text = title, style = MaterialTheme.typography.titleMediumEmphasized)
    },
    summary = {
      Text(
        text = when {
          isOff -> stringResource(R.string.lang_off)
          selected != null -> languageLabel(selected)
          else -> stringResource(R.string.lang_video_default)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    onClick = { showDialog = true },
  )

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text(title) },
      text = {
        LazyColumn(
          modifier = Modifier
            .heightIn(max = 480.dp)
            .selectableGroup(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          if (showOffOption) {
            item {
              LanguageOption(
                label = stringResource(R.string.lang_off),
                supporting = stringResource(R.string.lang_off_summary),
                selected = isOff,
                onClick = {
                  onValueChange(PreferredLanguage.OFF)
                  showDialog = false
                },
              )
            }
          }
          item {
            LanguageOption(
              label = stringResource(R.string.lang_video_default),
              supporting = stringResource(R.string.lang_video_default_summary),
              selected = selected == null && !isOff,
              onClick = {
                onValueChange("")
                showDialog = false
              },
            )
          }
          items(PreferredLanguage.entries, key = { it.code }) { language ->
            LanguageOption(
              label = stringResource(language.title),
              supporting = language.nativeName,
              selected = selected == language,
              onClick = {
                onValueChange(language.code)
                showDialog = false
              },
            )
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showDialog = false }) {
          Text(stringResource(android.R.string.cancel))
        }
      },
    )
  }
}

@Composable
private fun LanguageOption(
  label: String,
  supporting: String?,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(CircleShape)
      .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
      .padding(horizontal = 4.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Column(modifier = Modifier.padding(start = 12.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      )
      if (supporting != null) {
        Text(
          text = supporting,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun languageLabel(language: PreferredLanguage): String {
  val name = stringResource(language.title)
  return language.nativeName?.let { "$name · $it" } ?: name
}
