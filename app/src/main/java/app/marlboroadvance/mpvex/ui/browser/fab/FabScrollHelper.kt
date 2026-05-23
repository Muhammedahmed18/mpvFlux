package app.marlboroadvance.mpvex.ui.browser.fab

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop

object FabScrollHelper {
    @Composable
    fun trackScrollForFabVisibility(
        listState: LazyListState,
        gridState: LazyGridState?,
        isFabVisible: MutableState<Boolean>,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
    ) {
        val currentExpanded by rememberUpdatedState<Boolean>(expanded)
        val currentOnExpandedChange by rememberUpdatedState<(Boolean) -> Unit>(onExpandedChange)

        // Show FAB on tab switch and track scroll direction to update visibility.
        // Previous positions are plain coroutine-local vars — they are implementation
        // details and must not be Compose state (which would itself trigger recompositions).
        LaunchedEffect(listState, gridState) {
            isFabVisible.value = true

            var prevListIndex = listState.firstVisibleItemIndex
            var prevListOffset = listState.firstVisibleItemScrollOffset
            var prevGridIndex = gridState?.firstVisibleItemIndex ?: 0
            var prevGridOffset = gridState?.firstVisibleItemScrollOffset ?: 0

            snapshotFlow {
                Pair(
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset,
                    gridState?.let { it.firstVisibleItemIndex to it.firstVisibleItemScrollOffset },
                )
            }
            .drop(1) // Skip the initial emission — prev values already match current state
            .collect { (list, grid) ->
                val (listIndex, listOffset) = list
                updateFabVisibility(isFabVisible, listIndex, listOffset, prevListIndex, prevListOffset)
                prevListIndex = listIndex
                prevListOffset = listOffset

                if (grid != null) {
                    val (gridIndex, gridOffset) = grid
                    updateFabVisibility(isFabVisible, gridIndex, gridOffset, prevGridIndex, prevGridOffset)
                    prevGridIndex = gridIndex
                    prevGridOffset = gridOffset
                }
            }
        }

        // Collapse the FAB menu whenever a scroll begins.
        // Uses rememberUpdatedState so the latest `expanded` and `onExpandedChange`
        // are always read without restarting the effect on every recomposition.
        LaunchedEffect(listState, gridState) {
            snapshotFlow {
                listState.isScrollInProgress || (gridState?.isScrollInProgress ?: false)
            }.collect { isScrolling ->
                if (isScrolling && currentExpanded) {
                    currentOnExpandedChange(false)
                }
            }
        }
    }

    private fun updateFabVisibility(
        isFabVisible: MutableState<Boolean>,
        currentIndex: Int,
        currentScrollOffset: Int,
        previousIndex: Int,
        previousScrollOffset: Int,
    ) {
        if (currentIndex == 0 && currentScrollOffset == 0) {
            isFabVisible.value = true
        } else {
            val isScrollingDown = if (currentIndex != previousIndex) {
                currentIndex > previousIndex
            } else {
                currentScrollOffset > previousScrollOffset
            }
            isFabVisible.value = !isScrollingDown
        }
    }
}
