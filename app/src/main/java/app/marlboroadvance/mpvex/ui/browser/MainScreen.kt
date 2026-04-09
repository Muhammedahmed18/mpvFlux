package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
  // Keep only the persistent tab state
  private var persistentSelectedTab: Int = 0

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    val pagerState = rememberPagerState(
      initialPage = persistentSelectedTab,
      pageCount = { 4 }
    )
    val scope = rememberCoroutineScope()

    // Use NavigationBarState for reactive updates (no polling needed)
    val hideNavigationBarState = NavigationBarState.shouldHideNavigationBar
    val isPermissionDeniedState = NavigationBarState.isPermissionDenied
    
    // Update persistent state whenever tab changes
    LaunchedEffect(pagerState.currentPage) {
      android.util.Log.d("MainScreen", "selectedTab changed to: ${pagerState.currentPage} (was ${persistentSelectedTab})")
      persistentSelectedTab = pagerState.currentPage
    }

    // Scaffold with floating island bottom navigation
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        // Hide bottom navigation when permission is denied
        if (!isPermissionDeniedState) {
          // Floating island navigation bar with spring animations
          AnimatedVisibility(
            visible = !hideNavigationBarState,
            enter = slideInVertically(
              animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
              initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = slideOutVertically(
              animationSpec = tween(durationMillis = 300),
              targetOffsetY = { fullHeight -> fullHeight }
            )
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
              contentAlignment = Alignment.Center
            ) {
              FloatingIslandNavigationBar(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                  scope.launch {
                    pagerState.animateScrollToPage(index)
                  }
                }
              )
            }
          }
        }
      }
    ) { _ ->
      Box(modifier = Modifier.fillMaxSize()) {
        // Padding to account for the floating island navigation bar
        val fabBottomPadding = 88.dp

        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize(),
          beyondViewportPageCount = 1
        ) { targetTab ->
          CompositionLocalProvider(
            LocalNavigationBarHeight provides fabBottomPadding
          ) {
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
}

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

/**
 * Data class representing a navigation item in the floating island bar
 */
private data class NavItem(
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val label: String,
  val contentDescription: String
)

/**
 * Floating island-style navigation bar
 * Features active/inactive icon scaling
 */
@Composable
private fun FloatingIslandNavigationBar(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit
) {
  val navItems = remember {
    listOf(
      NavItem(Icons.Filled.Home, "Home", "Home"),
      NavItem(Icons.Filled.History, "Recents", "Recents"),
      NavItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists", "Playlists"),
      NavItem(Icons.Filled.Language, "Network", "Network")
    )
  }

  Surface(
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .height(72.dp)
      .fillMaxWidth(),
    shape = RoundedCornerShape(36.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    tonalElevation = 12.dp,
    shadowElevation = 8.dp
  ) {
    // Navigation items row
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      navItems.forEachIndexed { index, item ->
        val isSelected = selectedTab == index

        // Scale animation for icon
        val iconScale by animateFloatAsState(
          targetValue = if (isSelected) 1.1f else 1f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
          ),
          label = "icon_scale_$index"
        )

        // Pill background alpha animation
        val pillAlpha by animateFloatAsState(
          targetValue = if (isSelected) 1f else 0f,
          animationSpec = tween(durationMillis = 300),
          label = "pill_alpha_$index"
        )

        // Dot indicator alpha animation
        val dotAlpha by animateFloatAsState(
          targetValue = if (isSelected) 1f else 0f,
          animationSpec = tween(durationMillis = 200),
          label = "dot_alpha_$index"
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = { onTabSelected(index) }
            ),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .height(32.dp)
                .width(64.dp)
            ) {
              // Pill background - only this animates alpha
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer { alpha = pillAlpha }
                  .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                  )
              )

              Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                modifier = Modifier
                  .size(24.dp)
                  .scale(iconScale),
                tint = if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Dot Indicator
            Box(
              modifier = Modifier
                .size(4.dp)
                .graphicsLayer { alpha = dotAlpha }
                .background(
                  color = MaterialTheme.colorScheme.primary,
                  shape = CircleShape
                )
            )

            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
              text = item.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }
      }
    }
  }
}
