package app.marlboroadvance.mpvex.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.networkstreaming.NetworkStreamingScreen
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object MainScreen : Screen {
  private var persistentSelectedTab: Int = 0

  @Composable
  override fun Content() {
    val pagerState = rememberPagerState(
      initialPage = persistentSelectedTab,
      pageCount = { 4 }
    )
    val scope = rememberCoroutineScope()

    val hideNavigationBarState = NavigationBarState.shouldHideNavigationBar
    val isPermissionDeniedState = NavigationBarState.isPermissionDenied

    LaunchedEffect(pagerState.currentPage) {
      persistentSelectedTab = pagerState.currentPage
    }

    val navItems = remember {
      listOf(
        NavItem(Icons.Filled.Home, "Home", "Home"),
        NavItem(Icons.Filled.History, "Recents", "Recents"),
        NavItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists", "Playlists"),
        NavItem(Icons.Filled.Language, "Network", "Network")
      )
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        AnimatedVisibility(
          visible = !hideNavigationBarState && !isPermissionDeniedState,
          enter = slideInVertically(
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioNoBouncy,
              stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { fullHeight -> fullHeight }
          ),
          exit = slideOutVertically(
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioNoBouncy,
              stiffness = Spring.StiffnessMedium
            ),
            targetOffsetY = { fullHeight -> fullHeight }
          )
        ) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
          ) {
            navItems.forEachIndexed { index, item ->
              NavigationBarItem(
                selected = pagerState.currentPage == index,
                onClick = {
                  scope.launch {
                    pagerState.animateScrollToPage(index)
                  }
                },
                icon = {
                  Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription
                  )
                },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }
        }
      }
    ) { paddingValues ->
      CompositionLocalProvider(
        LocalNavigationBarHeight provides paddingValues.calculateBottomPadding()
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize(),
          beyondViewportPageCount = 1
        ) { targetTab ->
          when (targetTab) {
            0 -> FolderListScreen.Content()
            1 -> RecentlyPlayedScreen.Content()
            2 -> PlaylistScreen.Content()
            3 -> NetworkStreamingScreen.Content()
          }
        }
      }
    }
  }
}

val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

private data class NavItem(
  val icon: ImageVector,
  val label: String,
  val contentDescription: String
)
