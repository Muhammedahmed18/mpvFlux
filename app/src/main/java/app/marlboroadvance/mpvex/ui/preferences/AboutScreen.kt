package app.marlboroadvance.mpvex.ui.preferences

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object AboutScreen : Screen {

    private const val GITHUB_URL = "https://github.com/Muhammedahmed18/mpvFlux"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val clipboard = LocalClipboard.current
        val uriHandler = LocalUriHandler.current
        val scope = rememberCoroutineScope()

        val backgroundColor = rememberPreferenceBackgroundColor()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor,
        ) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                topBar = {
                    PreferenceTopBar(
                        title = {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = backstack::removeLastOrNull) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        containerColor = backgroundColor,
                    )
                },
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = paddingValues.calculateTopPadding(),
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { HeroSection() }
                    item { DescriptionCard() }

                    item { SectionHeader(text = "Core Highlights") }
                    item { HighlightsGrid() }

                    item { SectionHeader(text = "Connect & Share") }
                    item {
                        ActionRow(
                            icon = Icons.Outlined.Code,
                            label = "Explore GitHub Source Code",
                            onClick = { uriHandler.openUri(GITHUB_URL) },
                        )
                    }

                    item { SectionHeader(text = "System Info") }
                    item { SystemGrid() }
                    item { LibPlaceboCard() }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
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
                                .height(52.dp),
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
                            )
                        }
                    }
                    item {
                        Text(
                            text = "Forked from marlboro-advance/mpvEx",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HeroSection() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Soft "glow" rings (glass approximation — no real blur over the SurfaceView)
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            shape = CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = CircleShape,
                        ),
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "mpvFlux",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            val cleanVersion = BuildConfig.VERSION_NAME.substringBefore('-')
            val channel = if (BuildConfig.DEBUG) "Debug" else "Stable"
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = "v$cleanVersion ($channel)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }

    @Composable
    private fun DescriptionCard() {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "mpvFlux is a fast, modern video player built on libmpv — with " +
                    "hardware-accelerated decoding, libplacebo GPU rendering, and a clean " +
                    "Material You interface tuned for AMOLED black.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            )
        }
    }

    @Composable
    private fun SectionHeader(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
    }

    @Composable
    private fun HighlightsGrid() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Bolt,
                    title = "High-Performance",
                    subtitle = "libmpv hardware decoding",
                )
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.AutoAwesome,
                    title = "libplacebo",
                    subtitle = "GPU shaders & HDR",
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Movie,
                    title = "Format Support",
                    subtitle = "MKV · MP4 · AVI · HEVC · AV1",
                )
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Palette,
                    title = "Material You",
                    subtitle = "Dynamic, AMOLED-black UI",
                )
            }
        }
    }

    @Composable
    private fun HighlightCard(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        title: String,
        subtitle: String,
    ) {
        OutlinedCard(modifier = modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun ActionRow(
        icon: ImageVector,
        label: String,
        onClick: () -> Unit,
    ) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }

    @Composable
    private fun SystemGrid() {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
        val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Android,
                        label = "Android",
                        value = "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
                    )
                    VerticalDivider(color = dividerColor)
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PhoneAndroid,
                        label = "Device",
                        value = Build.MODEL,
                    )
                }
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Memory,
                        label = "Hardware",
                        value = Build.HARDWARE,
                    )
                    VerticalDivider(color = dividerColor)
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.DeveloperBoard,
                        label = "ABI",
                        value = abi,
                    )
                }
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Code,
                        label = "MPV",
                        value = Utils.VERSIONS.mpv,
                    )
                    VerticalDivider(color = dividerColor)
                    MetricCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Movie,
                        label = "FFmpeg",
                        value = Utils.VERSIONS.ffmpeg,
                    )
                }
            }
        }
    }

    @Composable
    private fun LibPlaceboCard() {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            MetricCell(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Settings,
                label = "libplacebo",
                value = Utils.VERSIONS.libPlacebo,
            )
        }
    }

    @Composable
    private fun MetricCell(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        label: String,
        value: String,
    ) {
        Row(
            modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}
