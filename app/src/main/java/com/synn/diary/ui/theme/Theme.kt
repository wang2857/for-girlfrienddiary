package com.synn.diary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.style.LineBreak
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
    secondaryContainer = Color(0xFFDCE9DF),
    onSecondaryContainer = Color(0xFF1B3A2D),
    surface = Color(0xFFFBF8F3),
    background = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFEDE7DE),
    onSurfaceVariant = Color(0xFF4A4A44),
    outline = Color(0xFFB9B2A6),
    // M3 的容器色默认是紫灰的，纸感配色要自己补齐，否则关掉动态取色后卡片会发紫
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFAF4),
    surfaceContainer = Color(0xFFF6F1E7),
    surfaceContainerHigh = Color(0xFFF0EADE),
    surfaceContainerHighest = Color(0xFFEAE3D6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6D4C0),
    onPrimary = Color(0xFF0C1F18),
    primaryContainer = Color(0xFF2A4A3F),
    onPrimaryContainer = Color(0xFFC6E8D9),
    secondaryContainer = Color(0xFF2B3A31),
    onSecondaryContainer = Color(0xFFD6E7DA),
    surface = Color(0xFF14150F),
    background = Color(0xFF14150F),
    onSurface = Color(0xFFE5E2DC),
    surfaceVariant = Color(0xFF2C2C27),
    onSurfaceVariant = Color(0xFFC8C5BD),
    outline = Color(0xFF6E6E64),
    surfaceContainerLowest = Color(0xFF0F100B),
    surfaceContainerLow = Color(0xFF191A13),
    surfaceContainer = Color(0xFF1E1F17),
    surfaceContainerHigh = Color(0xFF25261D),
    surfaceContainerHighest = Color(0xFF2C2D24),
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
    // 中文断行：按段落规则走 ICU 的 LineBreaker，句号、逗号、后引号不会单独掉到行首
    val base = Typography()
    fun androidx.compose.ui.text.TextStyle.cjk() = copy(lineBreak = LineBreak.Paragraph)
    val typography = base.copy(
        displayLarge = base.displayLarge.cjk(),
        displayMedium = base.displayMedium.cjk(),
        displaySmall = base.displaySmall.cjk(),
        headlineLarge = base.headlineLarge.cjk(),
        headlineMedium = base.headlineMedium.cjk(),
        headlineSmall = base.headlineSmall.cjk(),
        titleLarge = base.titleLarge.cjk(),
        titleMedium = base.titleMedium.cjk(),
        titleSmall = base.titleSmall.cjk(),
        bodyLarge = base.bodyLarge.cjk(),
        bodyMedium = base.bodyMedium.cjk(),
        bodySmall = base.bodySmall.cjk(),
        labelLarge = base.labelLarge.cjk(),
        labelMedium = base.labelMedium.cjk(),
        labelSmall = base.labelSmall.cjk(),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        // 界面文案一律用手机系统字体（中文 ROM 通常是厂商的思源黑体定制版）；
        // 正文字体是"每条日记自己的属性"，由编辑器单独覆盖
        typography = typography,
        shapes = DiaryShapes,
        content = content,
    )
}
