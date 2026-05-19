package app.marlboroadvance.mpvex.ui.theme

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.drawToBitmap
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import org.koin.compose.koinInject
import kotlin.math.hypot

// ============================================================================
// Theme Transition Animation State & Components
// ============================================================================
class ThemeTransitionState {
    var isAnimating by mutableStateOf(false)
        private set
    var clickPosition by mutableStateOf(Offset.Zero)
        private set
    var screenshotBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var animationProgress = Animatable(0f)
        private set
    
    private var captureView: View? = null
    
    fun setView(view: View) {
        captureView = view
    }
    
    fun startTransition(position: Offset) {
        if (isAnimating) return
        
        captureView?.let { view ->
            try {
                val bitmap = view.drawToBitmap()
                screenshotBitmap = bitmap
                clickPosition = position
                isAnimating = true
            } catch (e: Exception) {
                screenshotBitmap = null
                isAnimating = false
            }
        }
    }
    
    fun finishTransition() {
        val oldBitmap = screenshotBitmap
        screenshotBitmap = null
        clickPosition = Offset.Zero
        isAnimating = false
        oldBitmap?.recycle()
    }
    
    suspend fun resetProgress() {
        animationProgress.snapTo(0f)
    }
}

val LocalThemeTransitionState = staticCompositionLocalOf<ThemeTransitionState?> { null }

@Composable
fun rememberThemeTransitionState(): ThemeTransitionState {
    return remember { ThemeTransitionState() }
}

@Composable
private fun ThemeTransitionOverlay(
    state: ThemeTransitionState,
    content: @Composable () -> Unit
) {
    content()
}

private class CircularRevealShape(
    private val progress: Float,
    private val center: Offset,
    private val containerSize: Size,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val actualCenter = if (center == Offset.Zero) {
            Offset(size.width / 2f, size.height / 2f)
        } else {
            center
        }
        
        val maxRadius = longestDistanceToCorner(size, actualCenter) * 1.1f
        val currentRadius = maxRadius * progress
        
        val path = android.graphics.Path().apply {
            addRect(0f, 0f, size.width, size.height, android.graphics.Path.Direction.CW)
            addCircle(actualCenter.x, actualCenter.y, currentRadius, android.graphics.Path.Direction.CCW)
        }
        
        return androidx.compose.ui.graphics.Outline.Generic(
            path.asComposePath()
        )
    }
    
    private fun longestDistanceToCorner(size: Size, center: Offset): Float {
        val topLeft = hypot(center.x, center.y)
        val topRight = hypot(size.width - center.x, center.y)
        val bottomLeft = hypot(center.x, size.height - center.y)
        val bottomRight = hypot(size.width - center.x, size.height - center.y)
        return maxOf(topLeft, topRight, bottomLeft, bottomRight)
    }
}

private fun android.graphics.Path.asComposePath(): androidx.compose.ui.graphics.Path {
    val composePath = androidx.compose.ui.graphics.Path()
    composePath.asAndroidPath().set(this)
    return composePath
}

@Composable
private fun ThemeTransitionContent(content: @Composable () -> Unit) {
    val state = LocalThemeTransitionState.current
    
    if (state != null) {
        ThemeTransitionOverlay(state = state, content = content)
    } else {
        content()
    }
}

// ============================================================================
// Main Theme
// ============================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MpvexTheme(content: @Composable () -> Unit) {
    val preferences = koinInject<AppearancePreferences>()
    val darkMode by preferences.darkMode.collectAsState()
    val amoledMode by preferences.amoledMode.collectAsState()
    val appTheme by preferences.appTheme.collectAsState()
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val useDarkTheme = when (darkMode) {
        DarkMode.Dark -> true
        DarkMode.Light -> false
        DarkMode.System -> darkTheme
    }

    val colorScheme = when {
        appTheme.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            when {
                useDarkTheme && amoledMode -> {
                    dynamicDarkColorScheme(context).copy(
                        background = backgroundPureBlack,
                        surface = surfacePureBlack,
                        surfaceDim = surfaceDimPureBlack,
                        surfaceBright = surfaceBrightPureBlack,
                        surfaceContainerLowest = surfaceContainerLowestPureBlack,
                        surfaceContainerLow = surfaceContainerLowPureBlack,
                        surfaceContainer = surfaceContainerPureBlack,
                        surfaceContainerHigh = surfaceContainerHighPureBlack,
                        surfaceContainerHighest = surfaceContainerHighestPureBlack,
                    )
                }
                useDarkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        useDarkTheme && amoledMode -> appTheme.getAmoledColorScheme()
        useDarkTheme -> appTheme.getDarkColorScheme()
        else -> appTheme.getLightColorScheme()
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalThemeTransitionState provides rememberThemeTransitionState(),
    ) {
        ThemeTransitionContent {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = AppTypography,
                shapes = AppShapes, // Apply M3 Expressive Shapes
                motionScheme = MotionScheme.expressive(), // Already using expressive motion
                content = content,
            )
        }
    }
}

enum class DarkMode(
    @StringRes val titleRes: Int,
) {
    Dark(R.string.pref_appearance_darkmode_dark),
    Light(R.string.pref_appearance_darkmode_light),
    System(R.string.pref_appearance_darkmode_system),
}

private const val RIPPLE_DRAGGED_ALPHA = .5f
private const val RIPPLE_FOCUSED_ALPHA = .6f
private const val RIPPLE_HOVERED_ALPHA = .4f
private const val RIPPLE_PRESSED_ALPHA = .6f

@OptIn(ExperimentalMaterial3Api::class)
val playerRippleConfiguration
    @Composable get() =
        RippleConfiguration(
            color = MaterialTheme.colorScheme.primaryContainer,
            rippleAlpha =
            RippleAlpha(
                draggedAlpha = RIPPLE_DRAGGED_ALPHA,
                focusedAlpha = RIPPLE_FOCUSED_ALPHA,
                hoveredAlpha = RIPPLE_HOVERED_ALPHA,
                pressedAlpha = RIPPLE_PRESSED_ALPHA,
            ),
        )
