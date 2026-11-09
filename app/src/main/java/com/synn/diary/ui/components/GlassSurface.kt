package com.synn.diary.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/** 当前是否开启玻璃材质；由设置注入。 */
val LocalGlassEffect = androidx.compose.runtime.compositionLocalOf { false }

/**
 * 玻璃材质：半透明底 + 顶部高光 + 一圈发丝描边。
 *
 * 真正的背景模糊靠窗口自己（见 [GlassDialogBlur]），Compose 这一层只负责"看起来像块玻璃"。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    alpha: Float = 0.62f,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = androidx.compose.material3.MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tint = if (dark) Color(0xFF1B1D18) else Color(0xFFFFFDF7)
    val highlight = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)
    val borderColor = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(tint.copy(alpha = if (dark) 0.72f else alpha))
            .background(
                Brush.verticalGradient(
                    0f to highlight.copy(alpha = highlight.alpha * 0.7f),
                    0.35f to Color.Transparent,
                ),
            )
            .border(0.8.dp, borderColor, shape),
        content = content,
    )
}

/**
 * 让对话框窗口自己做背景模糊（API 31+）。
 *
 * 这是 OriginOS 7 / Android 12+ 上唯一对第三方开放的真实模糊；
 * 不支持的机器会安静地什么都不做。
 */
@Composable
fun GlassDialogBlur(radiusDp: Int = 18) {
    val view = LocalView.current
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@LaunchedEffect
        runCatching {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
            window.setBackgroundBlurRadius(with(density) { radiusDp.dp.roundToPx() })
        }
    }
}
