package com.synn.diary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 兜底配色：偏纸感的米白 + 墨绿，避免默认紫。
private val LightColors = lightColorScheme(
    primary = Color(0xFF3F6B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6E8D9),
    onPrimaryContainer = Color(0xFF0C1F18),
    surface = Color(0xFFFBF8F3),
    background = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFEDE7DE),
    onSurfaceVariant = Color(0xFF4A4A44),
    outline = Color(0xFFB9B2A6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6D4C0),
    onPrimary = Color(0xFF0C1F18),
    primaryContainer = Color(0xFF2A4A3F),
    onPrimaryContainer = Color(0xFFC6E8D9),
    surface = Color(0xFF14150F),
    background = Color(0xFF14150F),
    onSurface = Color(0xFFE5E2DC),
    surfaceVariant = Color(0xFF2C2C27),
    onSurfaceVariant = Color(0xFFC8C5BD),
    outline = Color(0xFF6E6E64),
)

/** 统一圆角与字体：日记类应用要"纸感"，所以不用默认的 M3 圆角梯度。 */
private val DiaryShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        // 正文用文楷，标题和数字仍走系统字体——中文字体在大字号标题上会显得软
        typography = Typography().withBodyFont(DiaryFonts.body),
        shapes = DiaryShapes,
        content = content,
    )
}
