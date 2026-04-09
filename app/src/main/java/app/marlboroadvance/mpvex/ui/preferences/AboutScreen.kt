package app.marlboroadvance.mpvex.ui.preferences

import android.content.ClipData
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object AboutScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val preferences = koinInject<AppearancePreferences>()

    val darkMode by preferences.darkMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDark = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDarkTheme
    }
    val backgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background

    Surface(
      modifier = Modifier.fillMaxSize(),
      color = backgroundColor
    ) {
      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
              scrolledContainerColor = Color.Transparent
            ),
            title = {
              Text(
                text = "About",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
              )
            },
            navigationIcon = {
              IconButton(onClick = backstack::removeLastOrNull) {
                Icon(
                  Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            }
          )
        },
      ) { paddingValues ->
        ProvidePreferenceLocals {
          // No scroll — fixed layout that fills the screen
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 20.dp)
              .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {

            // ── Hero Section ─────────────────────────────────────
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Circular icon with subtle surface background
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(88.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  AndroidView(
                    factory = { ctx ->
                      ImageView(ctx).apply {
                        setImageResource(R.mipmap.ic_launcher)
                      }
                    },
                    modifier = Modifier
                      .size(60.dp)
                      .clip(CircleShape)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "MpvFlux",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
              )

              Text(
                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Text(
                text = "forked from marlboro-advance/mpvEx",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
              )
            }

            // ── Info Card ────────────────────────────────────────
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
              Column(modifier = Modifier.padding(vertical = 4.dp)) {
                AboutInfoRow(
                  label = "MPV",
                  value = Utils.VERSIONS.mpv
                )
                HorizontalDivider(
                  modifier = Modifier.padding(horizontal = 20.dp),
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                AboutInfoRow(
                  label = "FFmpeg",
                  value = Utils.VERSIONS.ffmpeg
                )
                HorizontalDivider(
                  modifier = Modifier.padding(horizontal = 20.dp),
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                AboutInfoRow(
                  label = "Device",
                  value = Build.MODEL
                )
                HorizontalDivider(
                  modifier = Modifier.padding(horizontal = 20.dp),
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                AboutInfoRow(
                  label = "Android",
                  value = Build.VERSION.RELEASE
                )
              }
            }

            // ── Bottom Action ────────────────────────────────────
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  scope.launch {
                    clipboard.setClipEntry(
                      ClipEntry(
                        ClipData.newPlainText(
                          "Debug Info",
                          collectDeviceInfo()
                        )
                      )
                    )
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                )
              ) {
                Icon(
                  imageVector = Icons.Outlined.ContentCopy,
                  contentDescription = null,
                  modifier = Modifier
                    .size(18.dp)
                    .padding(end = 0.dp)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                  text = "Copy Debug Info",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }
    }
  }

  @Composable
  private fun AboutInfoRow(label: String, value: String) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End
      )
    }
  }
}
