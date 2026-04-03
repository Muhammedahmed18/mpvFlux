package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Deprecated("Use SortBottomSheet instead")
@Composable
fun SortDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  title: String,
  sortType: String,
  onSortTypeChange: (String) -> Unit,
  sortOrderAsc: Boolean,
  onSortOrderChange: (Boolean) -> Unit,
  types: List<String>,
  icons: List<ImageVector>,
  getLabelForType: (String, Boolean) -> Pair<String, String>,
  modifier: Modifier = Modifier,
  visibilityToggles: List<VisibilityToggle> = emptyList(),
  showSortOptions: Boolean = true,
) {
  // Original implementation removed as it's no longer used
}

data class VisibilityToggle(
  val label: String,
  val checked: Boolean,
  val onCheckedChange: (Boolean) -> Unit,
)
