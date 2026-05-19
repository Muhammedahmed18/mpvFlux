package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      // Use zero insets to prevent scaffold from reacting to bottom bar shifts
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
          // Bottom bar slot is now empty to keep layout stable.
          // The actual bar is drawn as a floating layer in the content area.
      }
    ) { _ ->
      Box(modifier = Modifier.fillMaxSize()) {
        val fabBottomPadding = 100.dp

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

        // Floating Navigation Bar Layer
        // Placing it here ensures the Pager content below doesn't re-layout when this toggles.
        if (!isPermissionDeniedState) {
          AnimatedVisibility(
            visible = !hideNavigationBarState,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
              animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
              initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = slideOutVertically(
              animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
              targetOffsetY = { fullHeight -> fullHeight }
            )
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
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
    }
  }
}

val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

private data class NavItem(
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val label: String,
  val contentDescription: String
)

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

  val primaryContainer = MaterialTheme.colorScheme.primaryContainer

  Surface(
    modifier = Modifier
      .height(80.dp)
      .fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    shadowElevation = 12.dp
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      navItems.forEachIndexed { index, item ->
        val isSelected = selectedTab == index
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val animatedScale by animateFloatAsState(
          targetValue = when {
            isPressed -> 0.92f
            isSelected -> 1.15f
            else -> 1f
          },
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium 
          ),
          label = "nav_item_scale"
        )

        val pillWidthDp by animateFloatAsState(
          targetValue = if (isSelected) 64f else 0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium 
          ),
          label = "pill_width"
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clip(CircleShape)
            .clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = { onTabSelected(index) }
            ),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .drawBehind {
                    if (pillWidthDp > 0f) {
                        val widthPx = pillWidthDp.dp.toPx()
                        val heightPx = size.height
                        val xOffset = (size.width - widthPx) / 2f
                        drawRoundRect(
                            color = primaryContainer,
                            topLeft = Offset(xOffset, 0f),
                            size = Size(widthPx, heightPx),
                            cornerRadius = CornerRadius(heightPx / 2f)
                        )
                    }
                }
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                modifier = Modifier.size(26.dp),
                tint = if (isSelected) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
              text = item.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
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
