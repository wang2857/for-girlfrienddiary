package com.synn.diary.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.synn.diary.ui.LocalBackgroundFiles
import java.io.File
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * 纸面：预设是"底色 + 花纹"，自定义是一张图 + 一层半透明蒙版
 * （蒙版是必须的，否则照片上的深色区域会让正文读不清）。
 */
@Composable
fun PaperBackdrop(
    background: EntryBackground,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val patternColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.06f else 0.042f)

    Box(modifier) {
        when (background) {
            is EntryBackground.Custom -> {
                // 蒙版浓度按照片本身的亮度来：亮的照片少盖一点，暗的多盖一点，
                // 比固定 76%/82% 更稳——深色照片不会把字吃掉，亮照片也不会糊成一片灰。
                val backgroundFile = LocalBackgroundFiles.current(background.fileName)
                val imageLuminance by produceState<Float?>(null, background.fileName) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { averageLuminance(backgroundFile) }.getOrNull()
                    }
                }
                AsyncImage(
                    model = LocalBackgroundFiles.current(background.fileName),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // 蒙版：让照片退到背景，文字仍然清楚
                val surfaceLum = MaterialTheme.colorScheme.surface.luminance()
                val maskAlpha = imageLuminance?.let { lum ->
                    if (dark) {
                        // 深色模式：把照片压到 ~10% 亮度，浅色字才够对比
                        ((lum - 0.10f) / (lum - surfaceLum).coerceAtLeast(0.01f))
                            .coerceIn(0.45f, 0.90f)
                    } else {
                        // 浅色模式：抬到 ~72% 亮度就够深色字读了，再多就把照片盖没了
                        ((0.72f - lum) / (surfaceLum - lum).coerceAtLeast(0.01f))
                            .coerceIn(0.55f, 0.85f)
                    }
                } ?: if (dark) 0.82f else 0.76f
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = maskAlpha),
                        ),
                )
            }

            is EntryBackground.Preset -> {
                if (!background.isDefault) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(background.color(dark))
                            .drawBehind { drawPaperPattern(background.pattern, patternColor) },
                    )
                } else {
                    Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
        content()
    }
}

/** 花纹：全部按固定间距平铺，颜色很淡，只当质感不当装饰。 */
fun DrawScope.drawPaperPattern(pattern: PaperPattern, color: Color) {
    when (pattern) {
        PaperPattern.NONE -> Unit

        PaperPattern.DOTS -> {
            val step = 22.dp.toPx()
            val r = 1.6.dp.toPx()
            var y = step
            var row = 0
            while (y < size.height) {
                var x = if (row % 2 == 0) step else step * 1.5f
                while (x < size.width) {
                    drawCircle(color = color, radius = r, center = Offset(x, y))
                    x += step
                }
                y += step
                row++
            }
        }

        PaperPattern.GRID -> {
            val step = 26.dp.toPx()
            val stroke = 1.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
                y += step
            }
        }

        PaperPattern.HEARTS -> {
            val step = 78.dp.toPx()
            val s = 5.5.dp.toPx()
            var y = step
            var row = 0
            while (y < size.height) {
                var x = if (row % 2 == 0) step else step * 1.5f
                while (x < size.width) {
                    drawPath(heartPath(Offset(x, y), s), color)
                    x += step
                }
                y += step
                row++
            }
        }

        PaperPattern.STARS -> {
            val step = 84.dp.toPx()
            val s = 4.dp.toPx()
            var y = step
            var row = 0
            while (y < size.height) {
                var x = if (row % 2 == 0) step else step * 1.5f
                while (x < size.width) {
                    drawPath(starPath(Offset(x, y), s), color)
                    x += step
                }
                y += step
                row++
            }
        }
    }
}

private fun heartPath(center: Offset, s: Float): Path {
    val path = Path()
    val top = center.y - s * 0.5f
    path.moveTo(center.x, center.y + s * 0.75f)
    path.cubicTo(
        center.x - s * 1.6f, top - s * 0.2f,
        center.x - s * 0.35f, top - s * 0.9f,
        center.x, top + s * 0.15f,
    )
    path.cubicTo(
        center.x + s * 0.35f, top - s * 0.9f,
        center.x + s * 1.6f, top - s * 0.2f,
        center.x, center.y + s * 0.75f,
    )
    path.close()
    return path
}

private fun starPath(center: Offset, s: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val angle = Math.PI / 5 * i - Math.PI / 2
        val radius = if (i % 2 == 0) s else s * 0.45f
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}


/** 缩到 32×32 取平均相对亮度；算不出来就返回 null，调用方会退回固定蒙版。 */
private fun averageLuminance(file: File): Float? {
    if (!file.exists()) return null
    val options = BitmapFactory.Options().apply { inSampleSize = 16 }
    val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
    return try {
        var sum = 0f
        var count = 0
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) {
                val c = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(c) / 255f
                val g = android.graphics.Color.green(c) / 255f
                val b = android.graphics.Color.blue(c) / 255f
                sum += 0.2126f * r + 0.7152f * g + 0.0722f * b
                count++
            }
        }
        if (count == 0) null else sum / count
    } finally {
        bitmap.recycle()
    }
}
