package com.synn.diary.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/** 当前用的心情图标风格；在 MainActivity 里由设置注入。 */
val LocalMoodStyle = compositionLocalOf { MoodStyle.EMOJI }

/**
 * 心情图标：emoji 直接用系统字体，其余三种是手画的。
 *
 * 画法统一在 [drawMood]，只改"头"的形状和表情，保证三种风格的表情语义一致。
 */
@Composable
fun MoodIcon(mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    val style = LocalMoodStyle.current
    if (style == MoodStyle.EMOJI) {
        Text(
            text = mood.emoji,
            fontSize = (size.value * 0.86f).sp,
            modifier = modifier,
        )
        return
    }
    Canvas(modifier.size(size)) { drawMood(style, mood) }
}

private fun DrawScope.drawMood(style: MoodStyle, mood: Mood) {
    val s = this.size.minDimension
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    // 深色底上的紫、蓝用浅色墨，浅黄/浅绿用深色墨，保证五官看得清
    val ink = if (mood.color.luminance() > 0.55f) Color(0xFF3B2A26) else Color(0xFFFFF7F2)

    when (style) {
        MoodStyle.FACE -> {
            drawCircle(mood.color, radius = s * 0.46f, center = Offset(cx, cy))
            drawBlush(ink, cx, cy, s)
        }

        MoodStyle.CAT -> {
            val ear = Path().apply {
                moveTo(cx - s * 0.40f, cy - s * 0.30f)
                lineTo(cx - s * 0.34f, cy - s * 0.52f)
                lineTo(cx - s * 0.14f, cy - s * 0.38f)
                close()
            }
            val ear2 = Path().apply {
                moveTo(cx + s * 0.40f, cy - s * 0.30f)
                lineTo(cx + s * 0.34f, cy - s * 0.52f)
                lineTo(cx + s * 0.14f, cy - s * 0.38f)
                close()
            }
            drawPath(ear, mood.color)
            drawPath(ear2, mood.color)
            drawCircle(mood.color, radius = s * 0.44f, center = Offset(cx, cy + s * 0.02f))
            // 小三角鼻子
            val nose = Path().apply {
                moveTo(cx - s * 0.05f, cy + s * 0.09f)
                lineTo(cx + s * 0.05f, cy + s * 0.09f)
                lineTo(cx, cy + s * 0.145f)
                close()
            }
            drawPath(nose, ink.copy(alpha = 0.85f))
            // 胡须
            val whisker = Stroke(width = s * 0.03f, cap = StrokeCap.Round)
            listOf(-0.06f, 0.06f).forEach { dy ->
                drawLine(
                    ink.copy(alpha = 0.7f),
                    Offset(cx - s * 0.30f, cy + s * dy),
                    Offset(cx - s * 0.48f, cy + s * (dy - 0.04f)),
                    strokeWidth = whisker.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    ink.copy(alpha = 0.7f),
                    Offset(cx + s * 0.30f, cy + s * dy),
                    Offset(cx + s * 0.48f, cy + s * (dy - 0.04f)),
                    strokeWidth = whisker.width,
                    cap = StrokeCap.Round,
                )
            }
            drawBlush(ink, cx, cy + s * 0.02f, s)
        }

        MoodStyle.BEAN -> {
            // 小豆芽：椭圆身子 + 头顶一片芽
            val bodyTop = cy - s * 0.30f
            drawOval(
                mood.color,
                topLeft = Offset(cx - s * 0.42f, bodyTop),
                size = Size(s * 0.84f, s * 0.80f),
            )
            drawCircle(
                Color.White.copy(alpha = 0.28f),
                radius = s * 0.07f,
                center = Offset(cx - s * 0.18f, bodyTop + s * 0.14f),
            )
            // 茎
            drawLine(
                Color(0xFF7CB342),
                Offset(cx, bodyTop + s * 0.02f),
                Offset(cx, bodyTop - s * 0.16f),
                strokeWidth = s * 0.035f,
                cap = StrokeCap.Round,
            )
            // 两片叶子
            drawOval(
                Color(0xFF8BC34A),
                topLeft = Offset(cx - s * 0.16f, bodyTop - s * 0.20f),
                size = Size(s * 0.15f, s * 0.10f),
            )
            drawOval(
                Color(0xFF9CCC65),
                topLeft = Offset(cx + s * 0.01f, bodyTop - s * 0.22f),
                size = Size(s * 0.15f, s * 0.10f),
            )
            drawBlush(ink, cx, bodyTop + s * 0.30f, s, scale = 0.8f)
        }

        MoodStyle.EMOJI -> Unit
    }

    val faceCy = if (style == MoodStyle.BEAN) cy + s * 0.10f else cy
    drawFace(style, mood, ink, cx, faceCy, s)
}

private fun DrawScope.drawBlush(ink: Color, cx: Float, cy: Float, s: Float, scale: Float = 1f) {
    val blush = Color(0xFFFF8A80).copy(alpha = 0.35f)
    drawCircle(blush, radius = s * 0.07f * scale, center = Offset(cx - s * 0.28f * scale, cy + s * 0.13f))
    drawCircle(blush, radius = s * 0.07f * scale, center = Offset(cx + s * 0.28f * scale, cy + s * 0.13f))
}

private fun DrawScope.drawFace(
    style: MoodStyle,
    mood: Mood,
    ink: Color,
    cx: Float,
    cy: Float,
    s: Float,
) {
    val eyeDx = if (style == MoodStyle.BEAN) s * 0.16f else s * 0.18f
    val eyeY = cy - s * (if (style == MoodStyle.BEAN) 0.06f else 0.04f)
    val mouthY = cy + s * (if (style == MoodStyle.BEAN) 0.16f else 0.20f)
    val stroke = Stroke(width = s * 0.065f, cap = StrokeCap.Round)

    fun dotEyes() {
        listOf(-eyeDx, eyeDx).forEach { dx ->
            drawCircle(ink, radius = s * 0.075f, center = Offset(cx + dx, eyeY))
            // 一点高光，眼睛看起来有神
            drawCircle(
                Color.White.copy(alpha = 0.85f),
                radius = s * 0.028f,
                center = Offset(cx + dx - s * 0.025f, eyeY - s * 0.028f),
            )
        }
    }

    fun closedEyes(up: Boolean) {
        val half = s * 0.075f
        val top = if (up) Offset(cx - eyeDx, eyeY + half * 0.6f) else Offset(cx - eyeDx, eyeY - half * 0.6f)
        drawArc(
            color = ink,
            startAngle = if (up) 200f else 20f,
            sweepAngle = if (up) 140f else 140f,
            useCenter = false,
            topLeft = Offset(top.x - half, top.y - half),
            size = Size(half * 2, half * 2),
            style = stroke,
        )
        val topR = if (up) Offset(cx + eyeDx, eyeY + half * 0.6f) else Offset(cx + eyeDx, eyeY - half * 0.6f)
        drawArc(
            color = ink,
            startAngle = if (up) 200f else 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(topR.x - half, topR.y - half),
            size = Size(half * 2, half * 2),
            style = stroke,
        )
    }

    fun smile() {
        val w = s * 0.26f
        drawArc(
            color = ink,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - w, mouthY - w * 0.7f),
            size = Size(w * 2, w * 1.4f),
            style = Stroke(width = s * 0.07f, cap = StrokeCap.Round),
        )
    }

    fun frown() {
        val w = s * 0.24f
        drawArc(
            color = ink,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - w, mouthY - w * 0.2f),
            size = Size(w * 2, w * 1.2f),
            style = Stroke(width = s * 0.07f, cap = StrokeCap.Round),
        )
    }

    fun flatMouth() {
        drawLine(
            ink,
            Offset(cx - s * 0.16f, mouthY),
            Offset(cx + s * 0.16f, mouthY),
            strokeWidth = s * 0.07f,
            cap = StrokeCap.Round,
        )
    }

    when (mood) {
        Mood.HAPPY -> {
            closedEyes(up = true)
            smile()
        }

        Mood.EXCITED -> {
            // 星星眼：两条交叉短线
            listOf(-eyeDx, eyeDx).forEach { dx ->
                drawLine(ink, Offset(cx + dx - s * 0.05f, eyeY), Offset(cx + dx + s * 0.05f, eyeY), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
                drawLine(ink, Offset(cx + dx, eyeY - s * 0.05f), Offset(cx + dx, eyeY + s * 0.05f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
            }
            drawOval(ink, topLeft = Offset(cx - s * 0.10f, mouthY - s * 0.04f), size = Size(s * 0.20f, s * 0.15f))
        }

        Mood.CALM -> {
            dotEyes()
            smile()
        }

        Mood.MEH -> {
            dotEyes()
            flatMouth()
        }

        Mood.TIRED -> {
            closedEyes(up = false)
            drawCircle(ink.copy(alpha = 0.9f), radius = s * 0.055f, center = Offset(cx, mouthY + s * 0.02f))
        }

        Mood.ANNOYED -> {
            // 斜眉 + 平嘴
            drawLine(ink, Offset(cx - eyeDx - s * 0.06f, eyeY - s * 0.10f), Offset(cx - eyeDx + s * 0.06f, eyeY - s * 0.04f), strokeWidth = s * 0.045f, cap = StrokeCap.Round)
            drawLine(ink, Offset(cx + eyeDx - s * 0.06f, eyeY - s * 0.04f), Offset(cx + eyeDx + s * 0.06f, eyeY - s * 0.10f), strokeWidth = s * 0.045f, cap = StrokeCap.Round)
            dotEyes()
            drawLine(ink, Offset(cx - s * 0.14f, mouthY), Offset(cx + s * 0.14f, mouthY), strokeWidth = s * 0.055f, cap = StrokeCap.Round)
        }

        Mood.DOWN -> {
            dotEyes()
            frown()
        }

        Mood.SAD -> {
            dotEyes()
            frown()
            // 一滴眼泪
            drawCircle(
                Color(0xFF64B5F6),
                radius = s * 0.05f,
                center = Offset(cx - eyeDx - s * 0.02f, eyeY + s * 0.12f),
            )
        }
    }
}
