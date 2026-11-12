package com.xstudio.app.core.designsystem

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val XDarkBg = Color(0xFF0A0A0C)
val XBlack = Color(0xFF0A0A0C)
val XSurface = Color(0xFF111113)
val XSurfaceElevated = Color(0xFF1B1B1E)
val XWhite = Color(0xFFFFFFFF)
val XWhiteMuted = Color(0xFFB3B3B3)
val XWhiteDim = Color(0xFF666666)
val XGlassFill = Color(0x1AFFFFFF)
val XGlassBorder = Color(0x2EFFFFFF)
val XGlassHighlight = Color(0x3DFFFFFF)
val XStatusOnline = Color(0xFF5BE3A8)

private val XStudioColorScheme = darkColorScheme(
    primary = XWhite,
    onPrimary = XDarkBg,
    secondary = XWhiteMuted,
    onSecondary = XDarkBg,
    tertiary = XWhite,
    background = XDarkBg,
    onBackground = XWhite,
    surface = XSurface,
    onSurface = XWhite,
    surfaceVariant = XSurfaceElevated,
    onSurfaceVariant = XWhiteMuted,
    outline = XGlassBorder
)

val XStudioTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = (-0.8).sp,
        color = XWhite
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp,
        color = XWhite
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = XWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = XWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = XWhiteMuted
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp,
        color = XWhite
    )
)

val LocalLowRamDevice = staticCompositionLocalOf { false }

fun Context.isLowRamOrBudgetDevice(): Boolean {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    return am.isLowRamDevice || totalRamGb < 4.5 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
}

@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg_orbs")
    val float1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val float2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -70f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    Canvas(modifier = modifier.fillMaxSize().background(XDarkBg)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x28FFFFFF), Color.Transparent),
                center = Offset(size.width * 0.15f + float1, size.height * 0.08f + float1 * 0.4f),
                radius = size.maxDimension * 0.55f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x1EC8C8D2), Color.Transparent),
                center = Offset(size.width * 0.88f + float2, size.height * 0.35f - float2 * 0.3f),
                radius = size.maxDimension * 0.5f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x18FFFFFF), Color.Transparent),
                center = Offset(size.width * 0.25f - float1 * 0.4f, size.height * 0.88f),
                radius = size.maxDimension * 0.6f
            )
        )
    }
}

@Composable
fun XStudioTheme(
    pageScale: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isLowRam = remember(context) { context.isLowRamOrBudgetDevice() }
    val currentDensity = LocalDensity.current
    val scaledDensity = remember(currentDensity, pageScale) {
        Density(
            density = currentDensity.density * pageScale.coerceIn(0.80f, 1.20f),
            fontScale = currentDensity.fontScale * pageScale.coerceIn(0.80f, 1.20f)
        )
    }

    CompositionLocalProvider(
        LocalLowRamDevice provides isLowRam,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = XStudioColorScheme,
            typography = XStudioTypography
        ) {
            Box(modifier = Modifier.fillMaxSize().background(XDarkBg)) {
                AmbientBackdrop()
                content()
            }
        }
    }
}

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    borderWidth: Dp = 1.dp,
    alpha: Float = 0.10f
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (alpha * 1.4f).coerceAtMost(0.25f)),
                Color.White.copy(alpha = (alpha * 0.5f).coerceAtMost(0.12f))
            )
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(XGlassHighlight, Color(0x1AFFFFFF), XGlassBorder)
        ),
        shape = shape
    )

fun Modifier.squircleTile(
    shape: Shape = RoundedCornerShape(32),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(Color(0x38FFFFFF), Color(0x0FFFFFFF))
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(Color(0x80FFFFFF), Color(0x26FFFFFF))
        ),
        shape = shape
    )

fun Modifier.qaGlass(
    shape: Shape = RoundedCornerShape(34.dp),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(Color(0x21FFFFFF), Color(0x08FFFFFF)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
        ),
        shape = shape
    )
