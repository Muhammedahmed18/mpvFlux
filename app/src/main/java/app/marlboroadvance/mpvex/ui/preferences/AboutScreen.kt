package app.marlboroadvance.mpvex.ui.preferences

import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import `is`.xyz.mpv.Utils
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
    
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Scaffold(
      topBar = {
        TopAppBar(
          title = { 
            Text(
              text = "About",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            ) 
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
              )
            }
          }
        )
      },
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Icon (Original Color/Launcher Icon)
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor),
          contentAlignment = Alignment.Center
        ) {
          AndroidView(
            factory = { ctx ->
              ImageView(ctx).apply {
                setImageResource(R.mipmap.ic_launcher)
              }
            },
            modifier = Modifier.size(48.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "MpvFlux",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = textColor
        )

        Text(
          text = "forked from: marlboro-advance/MpvFlux",
          style = MaterialTheme.typography.bodySmall,
          color = secondaryTextColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Device Information Label
        Text(
          text = "Device Information",
          style = MaterialTheme.typography.labelMedium,
          color = secondaryTextColor,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Structured Info List Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = cardColor
        ) {
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            InfoItem("App Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA})", textColor, secondaryTextColor)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
            InfoItem("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", textColor, secondaryTextColor)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
            InfoItem("Model", Build.MODEL, textColor, secondaryTextColor)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
            InfoItem("MPV Version", Utils.VERSIONS.mpv, textColor, secondaryTextColor)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
            InfoItem("FFmpeg Version", Utils.VERSIONS.ffmpeg, textColor, secondaryTextColor)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Copy Debug Info Button
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              clipboardManager.setText(AnnotatedString(collectDeviceInfo()))
            },
          shape = RoundedCornerShape(16.dp),
          color = cardColor
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = textColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Copy Debug Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
              )
              Text(
                text = "Used for reporting issues",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
              )
            }
          }
        }
      }
    }
  }

  @Composable
  private fun InfoItem(label: String, value: String, textColor: Color, secondaryTextColor: Color) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = secondaryTextColor,
        fontSize = 11.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
