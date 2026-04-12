package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
  currentName: String,
  itemType: String,
  extension: String? = null,
) {
  if (!isOpen) return

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val ime = WindowInsets.ime

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { targetValue ->
      if (targetValue == SheetValue.Hidden) {
        val isKeyboardVisible = ime.getBottom(density) > 0
        if (isKeyboardVisible) {
          focusManager.clearFocus()
          keyboardController?.hide()
          false // Veto the dismiss gesture
        } else {
          true // Allow dismiss
        }
      } else {
        true
      }
    }
  )
  
  var baseName by remember(currentName) { mutableStateOf(currentName) }
  var isError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  fun validateAndConfirm() {
    when {
      baseName.isBlank() -> {
        isError = true
        errorMessage = "Name cannot be empty"
      }
      baseName.contains("/") || baseName.contains("\\") -> {
        isError = true
        errorMessage = "Name cannot contain / or \\"
      }
      else -> {
        onConfirm(baseName + (extension ?: ""))
        onDismiss()
      }
    }
  }

  ModalBottomSheet(
    onDismissRequest = {
      val isKeyboardVisible = ime.getBottom(density) > 0
      if (isKeyboardVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
      } else {
        onDismiss()
      }
    },
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header with Title and Confirm Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Rename $itemType",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )

        IconButton(
          onClick = { validateAndConfirm() },
          enabled = baseName.isNotBlank(),
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
          ),
          modifier = Modifier.size(56.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Confirm Rename",
            modifier = Modifier.size(32.dp)
          )
        }
      }

      // Input field
      OutlinedTextField(
        value = baseName,
        onValueChange = {
          baseName = it
          isError = false
          errorMessage = ""
        },
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester),
        label = { Text("New name", fontWeight = FontWeight.Medium) },
        singleLine = false,
        maxLines = 5,
        isError = isError,
        supportingText = if (isError) {
          { Text(errorMessage) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardOptions = KeyboardOptions(
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
          onDone = { validateAndConfirm() },
        ),
        shape = MaterialTheme.shapes.extraLarge,
      )
    }
  }
}
