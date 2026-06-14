package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.preferences.allPlayerButtons
import app.marlboroadvance.mpvex.preferences.preference.Preference
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.ui.preferences.components.PlayerButtonChip
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Serializable
data class ControlLayoutEditorScreen(
  val region: ControlRegion,
) : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AppearancePreferences>()

    val prefs: List<Preference<String>> = remember(region) {
        when (region) {
          ControlRegion.TOP_RIGHT ->
            listOf(
              preferences.topRightControls,
              preferences.topLeftControls,
              preferences.bottomRightControls,
              preferences.bottomLeftControls,
            )
          ControlRegion.BOTTOM_RIGHT ->
            listOf(preferences.bottomRightControls, preferences.topLeftControls, preferences.topRightControls, preferences.bottomLeftControls)
          ControlRegion.BOTTOM_LEFT ->
            listOf(preferences.bottomLeftControls, preferences.topLeftControls, preferences.topRightControls, preferences.bottomRightControls)
          ControlRegion.PORTRAIT_BOTTOM -> listOf(preferences.portraitBottomControls)
        }
      }

    val prefToEdit: Preference<String> = prefs[0]

    val disabledButtons by remember {
      mutableStateOf(
        if (region == ControlRegion.PORTRAIT_BOTTOM) {
          emptySet()
        } else {
          val otherPref1 = prefs[1]
          val otherPref2 = prefs[2]
          val otherPref3 = prefs[3]
          (otherPref1.get().split(',') + otherPref2.get().split(',') + otherPref3.get().split(','))
            .filter(String::isNotBlank)
            .mapNotNull {
              try { PlayerButton.valueOf(it) } catch (_: Exception) { null }
            }.toSet()
        },
      )
    }

    var selectedButtons by remember {
      mutableStateOf(
        prefToEdit.get().split(',').filter(String::isNotBlank).mapNotNull {
            try { PlayerButton.valueOf(it) } catch (_: Exception) { null }
          },
      )
    }

    DisposableEffect(Unit) {
      onDispose { prefToEdit.set(selectedButtons.joinToString(",")) }
    }

    val title = remember(region) {
        when (region) {
          ControlRegion.TOP_RIGHT -> "Edit Top Right"
          ControlRegion.BOTTOM_RIGHT -> "Edit Bottom Right"
          ControlRegion.BOTTOM_LEFT -> "Edit Bottom Left"
          ControlRegion.PORTRAIT_BOTTOM -> "Edit Portrait Bottom"
        }
      }

    val darkMode by preferences.darkMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDark = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDarkTheme
    }
    val backgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
      ConfirmDialog(
        title = "Reset to default?",
        subtitle = "This will reset the controls in this region to their default configuration.",
        onConfirm = {
          prefToEdit.delete()
          selectedButtons = prefToEdit.get().split(',').filter(String::isNotBlank).mapNotNull {
              try { PlayerButton.valueOf(it) } catch (_: Exception) { null }
            }
          showResetDialog = false
        },
        onCancel = { showResetDialog = false },
      )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Scaffold(
          modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
          containerColor = Color.Transparent,
          topBar = {
            PreferenceTopBar(
              title = { 
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                ) 
              },
              navigationIcon = {
                IconButton(onClick = backstack::removeLastOrNull) {
                  Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.secondary)
                }
              },
              actions = {
                IconButton(onClick = { showResetDialog = true }) {
                  Icon(Icons.Rounded.Restore, contentDescription = "Reset to default", tint = MaterialTheme.colorScheme.primary)
                }
              },
              scrollBehavior = scrollBehavior,
              containerColor = backgroundColor,
            )
          },
        ) { padding ->
          ProvidePreferenceLocals {
            val gridState = rememberLazyGridState()
            val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
                val fromKey = from.key as? PlayerButton
                val toKey = to.key as? PlayerButton
                val fromIndex = selectedButtons.indexOf(fromKey)
                val toIndex = selectedButtons.indexOf(toKey)
                if (fromIndex in selectedButtons.indices && toIndex in selectedButtons.indices && fromIndex != toIndex) {
                    selectedButtons = selectedButtons.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                  Text(
                          text = "Long press to reorder items. Tap the '-' icon to remove them.",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                          modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                      )
                }

                if (selectedButtons.isEmpty()) {
                     item(span = { GridItemSpan(maxLineSpan) }) {
                         Surface(
                             modifier = Modifier.fillMaxWidth().height(160.dp),
                             shape = MaterialTheme.shapes.extraLarge,
                             color = MaterialTheme.colorScheme.surfaceContainerLow,
                             border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                             tonalElevation = 1.dp
                         ) {
                             Column(
                                 modifier = Modifier.fillMaxSize(),
                                 horizontalAlignment = Alignment.CenterHorizontally,
                                 verticalArrangement = Arrangement.Center
                             ) {
                                 Icon(
                                     imageVector = Icons.Rounded.AddCircle,
                                     contentDescription = null,
                                     modifier = Modifier.size(48.dp).padding(bottom = 12.dp),
                                     tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                 )
                                 Text(
                                      text = "Region is empty",
                                      style = MaterialTheme.typography.titleMedium,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 )
                                 Text(
                                      text = "Add buttons from the palette below",
                                      style = MaterialTheme.typography.bodySmall,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                 )
                             }
                         }
                     }
                } else {
                    items(
                        count = selectedButtons.size,
                        key = { selectedButtons[it] },
                        span = { index ->
                            val button = selectedButtons[index]
                            if (button == PlayerButton.CURRENT_CHAPTER || button == PlayerButton.VIDEO_TITLE) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                        }
                    ) { index ->
                        val button = selectedButtons[index]
                        ReorderableItem(reorderableState, key = button) { isDragging ->
                           val elevation by animateFloatAsState(
                               targetValue = if (isDragging) 12f else 0f,
                               animationSpec = spring(stiffness = Spring.StiffnessLow),
                               label = "drag_elevation"
                           )
                           
                           Surface(
                               modifier = Modifier
                                   .draggableHandle()
                                   .then(if (button == PlayerButton.CURRENT_CHAPTER || button == PlayerButton.VIDEO_TITLE) Modifier.wrapContentWidth(Alignment.Start) else Modifier),
                               shape = MaterialTheme.shapes.extraLarge,
                               shadowElevation = elevation.dp,
                               color = Color.Transparent
                           ) {
                                PlayerButtonChip(
                                    button = button,
                                    enabled = true,
                                    onClick = { selectedButtons = selectedButtons - button },
                                    badgeIcon = Icons.Rounded.RemoveCircle,
                                    badgeColor = MaterialTheme.colorScheme.error,
                                )
                           }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(48.dp)) 
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    PreferenceSectionHeader(title = "Available Palette", modifier = Modifier.padding(start = 4.dp))
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                     Card(
                         modifier = Modifier.fillMaxWidth(),
                         shape = MaterialTheme.shapes.extraLarge,
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                         elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                     ) {
                         FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(16.dp), 
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val availableButtons = allPlayerButtons.filter { it !in selectedButtons }
                            availableButtons.forEach { button ->
                                val isEnabled = button !in disabledButtons
                                PlayerButtonChip(
                                    button = button,
                                    enabled = isEnabled,
                                    onClick = { selectedButtons = selectedButtons + button },
                                    badgeIcon = Icons.Rounded.AddCircle,
                                    badgeColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            }
                            
                            if (availableButtons.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "All available buttons are in use.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                     }
                }
                
                item(span = { GridItemSpan(maxLineSpan) }) {
                    IconsLegend()
                    Spacer(Modifier.height(32.dp))
                }
            }
          }
        }
    }
  }
}

@Composable
private fun IconsLegend() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Icons Legend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                allPlayerButtons.forEach { button ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        val iconModifier = if (button == PlayerButton.VERTICAL_FLIP) Modifier.rotate(90f) else Modifier
                        
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = button.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(6.dp).then(iconModifier)
                            )
                        }
                        
                        Text(
                            text = app.marlboroadvance.mpvex.preferences.getPlayerButtonLabel(button),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
