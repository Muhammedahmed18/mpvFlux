package app.marlboroadvance.mpvex.ui.preferences

import android.content.pm.PackageManager
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable

@Serializable
object AboutScreen : Screen {
  @Suppress("DEPRECATION")
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val clipboardManager = LocalClipboardManager.current
    val packageManager: PackageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName?.substringBefore('-') ?: packageInfo.versionName ?: BuildConfig.VERSION_NAME
    val buildType = BuildConfig.BUILD_TYPE

    Scaffold(
      topBar = {
        TopAppBar(
          title = { 
            Text(
              text = stringResource(id = R.string.pref_about_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
            ) 
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                contentDescription = null,
              )
            }
          },
        )
      },
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App Icon and Basic Info
        Surface(
          modifier = Modifier.size(96.dp),
          shape = MaterialTheme.shapes.extraLarge,
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            AndroidView(
              factory = { ctx ->
                ImageView(ctx).apply {
                  setImageResource(R.mipmap.ic_launcher)
                }
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = "mpvExtended",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = "v$versionName ($buildType)",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Device Info Card
        Text(
          text = "Device Information",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              clipboardManager.setText(AnnotatedString(collectDeviceInfo()))
            },
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = null
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "System Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = collectDeviceInfo(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Tap to copy",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.End
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
          text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} mpvExtended Contributors",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}
