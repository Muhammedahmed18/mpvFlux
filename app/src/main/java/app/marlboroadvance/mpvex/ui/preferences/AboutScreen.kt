package app.marlboroadvance.mpvex.ui.preferences

import android.content.ClipData
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
        val appearancePrefs = koinInject<AppearancePreferences>()
        
        val darkMode by appearancePrefs.darkMode.collectAsState()
        val systemDarkTheme = isSystemInDarkTheme()
        val isDark = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }
        
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backstack::removeLastOrNull) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
        ) { paddingValues ->
            ProvidePreferenceLocals {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Hero Section ─────────────────────────────────────
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(112.dp),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                                contentDescription = "App Icon",
                                contentScale = ContentScale.Inside,
                                modifier = Modifier
                                    .size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "MpvFlux",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Build: ${BuildConfig.GIT_SHA}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // ── Information Cards ────────────────────────────────
                    PreferenceSectionHeader(title = "Engine Details")
                    PreferenceCard {
                        AboutListItem(
                            label = "MPV core",
                            value = Utils.VERSIONS.mpv,
                            icon = Icons.Outlined.Code
                        )
                        PreferenceDivider()
                        AboutListItem(
                            label = "FFmpeg",
                            value = Utils.VERSIONS.ffmpeg,
                            icon = Icons.Outlined.Info
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PreferenceSectionHeader(title = "Device Info")
                    PreferenceCard {
                        AboutListItem(
                            label = "Model",
                            value = Build.MODEL,
                            icon = Icons.Outlined.PhoneAndroid
                        )
                        PreferenceDivider()
                        AboutListItem(
                            label = "Android Version",
                            value = "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
                            icon = Icons.Outlined.Info
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Actions ──────────────────────────────────────────
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "button_scale"
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Debug Info", collectDeviceInfo())))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(64.dp)
                            .scale(buttonScale),
                        interactionSource = interactionSource,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy Debug Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Forked from marlboro-advance/mpvEx",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun AboutListItem(label: String, value: String, icon: ImageVector) {
        ListItem(
            leadingContent = { 
                Icon(
                    icon, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) 
            },
            headlineContent = { 
                Text(
                    label, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold 
                ) 
            },
            trailingContent = {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
