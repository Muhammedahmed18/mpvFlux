package app.marlboroadvance.mpvex.ui.preferences

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import org.koin.compose.koinInject

@Serializable
object AboutScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val appearancePrefs = koinInject<AppearancePreferences>()

        val darkMode by appearancePrefs.darkMode.collectAsState()
        val systemDarkTheme = isSystemInDarkTheme()
        val isDark = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = backstack::removeLastOrNull) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                HeroSection()

                Spacer(modifier = Modifier.height(24.dp))

                MetricGrid()

                Spacer(modifier = Modifier.weight(1f))

                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("Debug Info", collectDeviceInfo())),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Copy Debug Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Forked from marlboro-advance/mpvEx",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }

    @Composable
    private fun HeroSection() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp),
                tonalElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                        contentDescription = "App Icon",
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MpvFlux",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = BuildConfig.GIT_SHA,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
    }

    @Composable
    private fun MetricGrid() {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Code,
                        label = "MPV",
                        value = Utils.VERSIONS.mpv,
                    )
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Movie,
                        label = "FFmpeg",
                        value = Utils.VERSIONS.ffmpeg,
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PhoneAndroid,
                        label = "Device",
                        value = Build.MODEL,
                    )
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Android,
                        label = "Android",
                        value = "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
                    )
                }
            }
        }
    }

    @Composable
    private fun MetricCell(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        label: String,
        value: String,
    ) {
        Column(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
