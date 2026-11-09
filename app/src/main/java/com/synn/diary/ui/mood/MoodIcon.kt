package com.synn.diary.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp

/**
 * 手绘贴纸风的心情表情：白色描边打底 + 淡彩填充 + 粗黑线五官 + 腮红。
 *
 * 尺寸很小（列表里 15dp），所以五官只保留"一眼能认出情绪"的部分：
 * 闭眼笑 / 星星眼 / 平嘴 / 皱眉 / 眼泪。
 */
@Composable
fun MoodIcon(mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawSticker(mood) }
}

private val INK = Color(0xFF2A2522)

private fun DrawScope.drawSticker(mood: Mood) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = s * 0.40f
    val dark = false // 贴纸自带白边，深色底上也一样画

    // 1) 白色贴纸边
    drawCircle(Color.White, radius = r + s * 0.075f, center = Offset(cx, cy))
    // 2) 淡彩填充：把心情色往白里调，保留色相但不刺眼
    val fill = lerp(mood.color, Color.White, 0.45f)
    drawCircle(fill, radius = r, center = Offset(cx, cy))
    // 3) 粗描边
    drawCircle(
        INK,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = s * 0.065f),
    )

    drawFace(mood, cx, cy, r, s)
}

private fun DrawScope.drawFace(mood: Mood, cx: Float, cy: Float, r: Float, s: Float) {
    val eyeDx = r * 0.40f
    val eyeY = cy - r * 0.10f
    val mouthY = cy + r * 0.34f
    val line = Stroke(width = s * 0.06f, cap = StrokeCap.Round)

    fun dots() {
        drawCircle(INK, radius = s * 0.036f, center = Offset(cx - eyeDx, eyeY))
        drawCircle(INK, radius = s * 0.036f, center = Offset(cx + eyeDx, eyeY))
    }

    fun closedEyes(up: Boolean) {
        val w = r * 0.26f
        val h = r * 0.20f
        val top = Offset(cx - eyeDx - w / 2, eyeY - h / 2)
        drawArc(
            color = INK,
            startAngle = if (up) 200f else 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = top,
            size = Size(w, h),
            style = line,
        )
        drawArc(
            color = INK,
            startAngle = if (up) 200f else 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + eyeDx - w / 2, eyeY - h / 2),
            size = Size(w, h),
            style = line,
        )
    }

    fun smile() {
        val w = r * 0.42f
        drawArc(
            color = INK,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - w / 2, mouthY - w * 0.30f),
            size = Size(w, w * 0.62f),
            style = line,
        )
    }

    fun frown() {
        val w = r * 0.36f
        drawArc(
            color = INK,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - w / 2, mouthY - w * 0.10f),
            size = Size(w, w * 0.52f),
            style = line,
        )
    }

    fun flat() {
        drawLine(
            INK,
            Offset(cx - r * 0.22f, mouthY),
            Offset(cx + r * 0.22f, mouthY),
            strokeWidth = line.width,
            cap = StrokeCap.Round,
        )
    }

    fun blush() {
        val c = Color(0xFFFF8A80).copy(alpha = 0.35f)
        drawCircle(c, radius = r * 0.13f, center = Offset(cx - r * 0.55f, cy + r * 0.18f))
        drawCircle(c, radius = r * 0.13f, center = Offset(cx + r * 0.55f, cy + r * 0.18f))
    }

    when (mood) {
        Mood.HAPPY -> { closedEyes(up = true); smile(); blush() }

        Mood.EXCITED -> {
            // 星星眼：两条交叉短线 + 张开的小嘴
            listOf(-eyeDx, eyeDx).forEach { dx ->
                drawLine(INK, Offset(cx + dx - r * 0.12f, eyeY), Offset(cx + dx + r * 0.12f, eyeY), line.width, StrokeCap.Round)
                drawLine(INK, Offset(cx + dx, eyeY - r * 0.12f), Offset(cx + dx, eyeY + r * 0.12f), line.width, StrokeCap.Round)
            }
            drawOval(
                INK,
                topLeft = Offset(cx - r * 0.14f, mouthY - r * 0.10f),
                size = Size(r * 0.28f, r * 0.22f),
            )
            blush()
        }

        Mood.CALM -> { dots(); smile(); blush() }

        Mood.MEH -> { dots(); flat() }

        Mood.TIRED -> {
            closedEyes(up = false)
            drawCircle(INK, radius = r * 0.10f, center = Offset(cx, mouthY))
        }

        Mood.ANNOYED -> {
            // 斜眉 + 平嘴
            drawLine(INK, Offset(cx - eyeDx - r * 0.14f, eyeY - r * 0.22f), Offset(cx - eyeDx + r * 0.12f, eyeY - r * 0.10f), line.width, StrokeCap.Round)
            drawLine(INK, Offset(cx + eyeDx - r * 0.12f, eyeY - r * 0.10f), Offset(cx + eyeDx + r * 0.14f, eyeY - r * 0.22f), line.width, StrokeCap.Round)
            dots()
            flat()
        }

        Mood.DOWN -> { dots(); frown() }

        Mood.SAD -> {
            dots(); frown()
            drawCircle(
                Color(0xFF64B5F6),
                radius = r * 0.10f,
                center = Offset(cx - eyeDx, eyeY + r * 0.30f),
            )
        }
    }
}
