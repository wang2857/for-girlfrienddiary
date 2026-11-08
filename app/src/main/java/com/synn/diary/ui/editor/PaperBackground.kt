package com.synn.diary.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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
import kotlin.math.cos
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
                AsyncImage(
                    model = LocalBackgroundFiles.current(background.fileName),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // 蒙版：让照片退到背景，文字仍然清楚
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (dark) 0.82f else 0.76f,
                            ),
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
