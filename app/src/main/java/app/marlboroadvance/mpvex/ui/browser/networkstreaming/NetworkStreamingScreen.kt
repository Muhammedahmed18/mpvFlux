package app.marlboroadvance.mpvex.ui.browser.networkstreaming

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.cards.NetworkConnectionCard
import app.marlboroadvance.mpvex.ui.browser.dialogs.AddConnectionSheet
import app.marlboroadvance.mpvex.ui.browser.dialogs.EditConnectionSheet
import app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object NetworkStreamingScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val viewModel: NetworkStreamingViewModel =
      viewModel(factory = NetworkStreamingViewModel.factory(context.applicationContext as android.app.Application))

    val connections by viewModel.connections.collectAsState()
    val connectionStatuses by viewModel.connectionStatuses.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<NetworkConnection?>(null) }
    val navigationBarHeight = LocalNavigationBarHeight.current

    val listState = remember { LazyListState() }
    
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItemScrollOffset by remember { mutableIntStateOf(0) }
    
    val isFabVisible by remember {
      derivedStateOf {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        if (currentIndex == 0 && currentOffset == 0) true
        else {
          val isScrollingUp = currentIndex < previousFirstVisibleItemIndex ||
            (currentIndex == previousFirstVisibleItemIndex && currentOffset < previousFirstVisibleItemScrollOffset)
          previousFirstVisibleItemIndex = currentIndex
          previousFirstVisibleItemScrollOffset = currentOffset
          isScrollingUp
        }
      }
    }

    Scaffold(
        topBar = {
          BrowserTopBar(
            title = "Network",
            isInSelectionMode = false,
            selectedCount = 0,
            totalCount = 0,
            onBackClick = null,
            onCancelSelection = { },
            onSettingsClick = { backstack.add(PreferencesScreen) },
          )
      },
      floatingActionButton = {
        if (isFabVisible) {
          val fabScale by animateFloatAsState(
              targetValue = 1f,
              animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
              label = "fab_scale"
          )
          ExtendedFloatingActionButton(
            onClick = { showAddSheet = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
            text = { Text("Add Connection", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(bottom = navigationBarHeight).scale(fabScale)
          )
        }
      },
    ) { padding ->
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = navigationBarHeight + 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
          item {
            StreamLinkSection(
              onPlayLink = { url -> MediaUtils.playFile(url, context, "network_stream") },
            )
          }

          item {
            Text(
              text = "Saved Connections",
              style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.3).sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
          }

          if (connections.isEmpty()) {
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SignalWifiStatusbarConnectedNoInternet4,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  Text(
                    text = "No connections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "Add SMB, FTP, or WebDAV to browse files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          } else {
            items(connections, key = { it.id }) { connection ->
              val status = connectionStatuses[connection.id]
              NetworkConnectionCard(
                connection = connection,
                onConnect = { viewModel.connect(it) },
                onDisconnect = { viewModel.disconnect(it) },
                onEdit = { editingConnection = it },
                onDelete = { viewModel.deleteConnection(it) },
                onBrowse = { conn ->
                  if (status?.isConnected == true) {
                    backstack.add(NetworkBrowserScreen(connectionId = conn.id, connectionName = conn.name, currentPath = "/"))
                  }
                },
                onAutoConnectChange = { conn, autoConnect -> viewModel.updateConnection(conn.copy(autoConnect = autoConnect)) },
                isConnected = status?.isConnected ?: false,
                isConnecting = status?.isConnecting ?: false,
                error = status?.error,
              )
            }
          }
        }

      AddConnectionSheet(
        isOpen = showAddSheet,
        onDismiss = { showAddSheet = false },
        onSave = { connection -> viewModel.addConnection(connection); showAddSheet = false },
      )

      editingConnection?.let { connection ->
        EditConnectionSheet(
          connection = connection,
          isOpen = true,
          onDismiss = { editingConnection = null },
          onSave = { updatedConnection -> viewModel.updateConnection(updatedConnection); editingConnection = null },
        )
      }
    }
  }
}

@Composable
private fun StreamLinkSection(
  onPlayLink: (String) -> Unit,
) {
  val context = LocalContext.current
  var linkUrl by rememberSaveable { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Stream Link",
      style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.3).sp),
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      tonalElevation = 1.dp
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
          value = linkUrl,
          onValueChange = { linkUrl = it },
          label = { Text("Video URL") },
          placeholder = { Text("https://example.com/video.mp4") },
          leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = MaterialTheme.shapes.large
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
          FilledTonalButton(
            onClick = {
              val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
              clipboardManager?.primaryClip?.let { if (it.itemCount > 0) linkUrl = it.getItemAt(0).text?.toString() ?: "" }
            },
            shape = MaterialTheme.shapes.medium
          ) {
            Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Paste", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = { if (linkUrl.isNotBlank()) { onPlayLink(linkUrl); linkUrl = "" } },
            enabled = linkUrl.isNotBlank(),
            shape = MaterialTheme.shapes.medium
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
