package app.marlboroadvance.mpvex.ui.browser

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.audio.ExtractedAudioScreen
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.marlboroadvance.mpvex.ui.utils.WindowWidthClass
import app.marlboroadvance.mpvex.ui.utils.currentWindowWidthClass
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object MainScreen : Screen {
  private var persistentSelectedTab: Int = 0

  @Composable
  override fun Content() {
    val browserPreferences = koinInject<BrowserPreferences>()
    // The Audio tab appears after the first extraction and can be hidden in Settings.
    val showAudioTab by browserPreferences.showAudioTab.collectAsState()
    val pageCount = if (showAudioTab) 4 else 3
    val pagerState = rememberPagerState(
      initialPage = persistentSelectedTab.coerceAtMost(pageCount - 1),
      pageCount = { pageCount }
    )
    val scope = rememberCoroutineScope()

    val hideNavigationBarState = NavigationBarState.shouldHideNavigationBar
    val isPermissionDeniedState = NavigationBarState.isPermissionDenied

    LaunchedEffect(pagerState.currentPage) {
      persistentSelectedTab = pagerState.currentPage
    }

    val navItems = remember(showAudioTab) {
      listOfNotNull(
        NavItem(Icons.Filled.Home, Icons.Outlined.Home, R.string.home),
        NavItem(Icons.Filled.History, Icons.Outlined.History, R.string.recents),
        NavItem(
          Icons.AutoMirrored.Filled.PlaylistPlay,
          Icons.AutoMirrored.Outlined.PlaylistPlay,
          R.string.playlists,
        ),
        if (showAudioTab) {
          NavItem(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, R.string.nav_audio)
        } else {
          null
        },
      )
    }

    // Material 3 adaptive navigation: the floating bar on phones in portrait, a navigation
    // rail on wider windows (landscape phones, tablets, foldables, split screen) so the
    // bar doesn't eat vertical space and content isn't stretched under it.
    val useRail = currentWindowWidthClass() != WindowWidthClass.Compact
    val showNavigation = !hideNavigationBarState && !isPermissionDeniedState

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        AnimatedVisibility(
          visible = !useRail && showNavigation,
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
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets.navigationBars)
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
              shadowElevation = 8.dp,
            ) {
              NavigationBar(
                modifier = Modifier.height(72.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
              ) {
                navItems.forEachIndexed { index, item ->
                  val selected = pagerState.currentPage == index
                  NavigationBarItem(
                    selected = selected,
                    onClick = {
                      scope.launch {
                        pagerState.animateScrollToPage(index)
                      }
                    },
                    icon = {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null
                      )
                    },
                    label = { Text(stringResource(item.labelRes)) },
                    // Material 3: navigation bar destinations always show their label.
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
        }
      }
    ) { paddingValues ->
      // Screens pad their lists/FABs by this. With the rail there is no bottom bar, so
      // only the system navigation bar needs to be cleared.
      val bottomInset = if (useRail) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      } else {
        paddingValues.calculateBottomPadding()
      }
      CompositionLocalProvider(
        LocalNavigationBarHeight provides bottomInset
      ) {
        Row(modifier = Modifier.fillMaxSize()) {
          AnimatedVisibility(
            visible = useRail && showNavigation,
            enter = slideInHorizontally(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
              ),
              initialOffsetX = { fullWidth -> -fullWidth }
            ),
            exit = slideOutHorizontally(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
              ),
              targetOffsetX = { fullWidth -> -fullWidth }
            )
          ) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
            ) {
              Spacer(Modifier.weight(1f))
              navItems.forEachIndexed { index, item ->
                val selected = pagerState.currentPage == index
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    scope.launch {
                      pagerState.animateScrollToPage(index)
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = null
                    )
                  },
                  label = { Text(stringResource(item.labelRes)) },
                  alwaysShowLabel = true,
                  colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                )
              }
              Spacer(Modifier.weight(1f))
            }
          }
          HorizontalPager(
            state = pagerState,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
            beyondViewportPageCount = 1
          ) { targetTab ->
            when (targetTab) {
              0 -> FolderListScreen.Content()
              1 -> RecentlyPlayedScreen.Content()
              2 -> PlaylistScreen.Content()
              3 -> ExtractedAudioScreen.Content()
            }
          }
        }
      }
    }
  }
}

val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

private data class NavItem(
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  @StringRes val labelRes: Int,
)
