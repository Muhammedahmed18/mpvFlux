package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditConnectionSheet(
  connection: NetworkConnection,
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onSave: (NetworkConnection) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val state = remember(connection.id) { ConnectionFormState(connection) }

  val handleDismiss = {
    onDismiss()
  }

  val handleSave = {
    val updatedConnection =
      connection.copy(
        name = state.name,
        protocol = state.protocol,
        host = state.host,
        port = state.port.toIntOrNull() ?: state.protocol.defaultPort,
        username = if (state.isAnonymous) "" else state.username,
        password = if (state.isAnonymous) "" else state.password,
        path = state.path.ifBlank { "/" },
        isAnonymous = state.isAnonymous,
        useHttps = state.useHttps,
      )
    onSave(updatedConnection)
  }

  AlertDialog(
    onDismissRequest = handleDismiss,
    modifier = Modifier.widthIn(min = 400.dp, max = 600.dp),
    title = {
      Text(
        text = "Edit Connection",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Name and Protocol in one row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Connection Name
          OutlinedTextField(
            value = state.name,
            onValueChange = { state.name = it },
            label = { Text("Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.60f),
            singleLine = true,
          )

          // Protocol Dropdown
          ExposedDropdownMenuBox(
            expanded = state.protocolMenuExpanded,
            onExpandedChange = { state.protocolMenuExpanded = it },
            modifier = Modifier.weight(0.40f),
          ) {
            OutlinedTextField(
              value = state.protocol.displayName,
              onValueChange = { },
              readOnly = true,
              label = { Text("Protocol", maxLines = 1, overflow = TextOverflow.Ellipsis) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.protocolMenuExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
              expanded = state.protocolMenuExpanded,
              onDismissRequest = { state.protocolMenuExpanded = false },
            ) {
              NetworkProtocol.entries.forEach { proto ->
                DropdownMenuItem(
                  text = { Text(proto.displayName) },
                  onClick = {
                    state.protocol = proto
                    state.port = proto.defaultPort.toString()
                    state.protocolMenuExpanded = false
                  },
                )
              }
            }
          }
        }

        // Host
        OutlinedTextField(
          value = state.host,
          onValueChange = { state.host = it },
          label = { Text("Host/IP Address", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          placeholder = { Text("192.168.1.100", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )

        // Port and Path in one row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Port
          OutlinedTextField(
            value = state.port,
            onValueChange = { state.port = it },
            label = { Text("Port", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.3f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )

          // Path
          OutlinedTextField(
            value = state.path,
            onValueChange = { state.path = it },
            label = { Text("Path", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.7f),
            singleLine = true,
            placeholder = { Text("/", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
        }

        // Anonymous checkbox
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Checkbox(
            checked = state.isAnonymous,
            onCheckedChange = { state.isAnonymous = it },
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Anonymous/Guest Access")
        }

        // HTTPS checkbox (only for WebDAV)
        if (state.protocol == NetworkProtocol.WEBDAV) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Checkbox(
              checked = state.useHttps,
              onCheckedChange = {
                state.useHttps = it
                // Auto-update port when toggling HTTPS
                if (it && state.port == "80") {
                  state.port = "443"
                } else if (!it && state.port == "443") {
                  state.port = "80"
                }
              },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use HTTPS (Secure Connection)")
          }
        }

        // Username and Password in one row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Username
          OutlinedTextField(
            value = state.username,
            onValueChange = { state.username = it },
            label = { Text("Username", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.50f),
            singleLine = true,
            enabled = !state.isAnonymous,
          )

          // Password
          OutlinedTextField(
            value = state.password,
            onValueChange = { state.password = it },
            label = { Text("Password", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.weight(0.50f),
            singleLine = true,
            enabled = !state.isAnonymous,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = handleSave,
        enabled = state.host.isNotBlank() && (state.isAnonymous || state.username.isNotBlank()),
      ) {
        Text(
          text = "Save",
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = handleDismiss) {
        Text(
          text = "Cancel",
          fontWeight = FontWeight.Medium,
        )
      }
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}

/**
 * Holds all mutable form state for [EditConnectionSheet].
 * Each field is an individually observable [mutableStateOf] so Compose scopes
 * recompositions to only the composables that read each specific field.
 * Keyed on [NetworkConnection.id] so state resets when a different connection is opened.
 */
private class ConnectionFormState(connection: NetworkConnection) {
  var name by mutableStateOf(connection.name)
  var protocol by mutableStateOf(connection.protocol)
  var host by mutableStateOf(connection.host)
  var port by mutableStateOf(connection.port.toString())
  var username by mutableStateOf(connection.username)
  var password by mutableStateOf(connection.password)
  var path by mutableStateOf(connection.path)
  var isAnonymous by mutableStateOf(connection.isAnonymous)
  var useHttps by mutableStateOf(connection.useHttps)
  var passwordVisible by mutableStateOf(false)
  var protocolMenuExpanded by mutableStateOf(false)
}
