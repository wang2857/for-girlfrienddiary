package com.synn.diary.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp

/**
 * 手绘小熊心情贴纸。
 *
 * 头不是正圆，是四条贝塞尔拼出来的、左右略不对称的圆脸；耳朵一高一低，
 * 像随手画的。白色贴纸边 + 淡彩 + 粗黑线五官，和工具栏的线稿是一套笔触。
 */
@Composable
fun MoodIcon(mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawBear(mood) }
}

private val INK = Color(0xFF2A2522)

private fun DrawScope.drawBear(mood: Mood) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f + s * 0.02f

    val head = Path().apply {
        moveTo(cx - s * 0.355f, cy - s * 0.06f)
        cubicTo(cx - s * 0.43f, cy + s * 0.20f, cx - s * 0.21f, cy + s * 0.365f, cx, cy + s * 0.365f)
        cubicTo(cx + s * 0.21f, cy + s * 0.365f, cx + s * 0.43f, cy + s * 0.20f, cx + s * 0.355f, cy - s * 0.06f)
        cubicTo(cx + s * 0.32f, cy - s * 0.31f, cx + s * 0.13f, cy - s * 0.40f, cx, cy - s * 0.40f)
        cubicTo(cx - s * 0.13f, cy - s * 0.40f, cx - s * 0.32f, cy - s * 0.31f, cx - s * 0.355f, cy - s * 0.06f)
        close()
    }
    // 耳朵一高一低，手绘感
    val earL = Offset(cx - s * 0.27f, cy - s * 0.315f)
    val earR = Offset(cx + s * 0.25f, cy - s * 0.345f)
    val earRr = s * 0.115f
    val earLr = s * 0.125f

    // 1) 白色贴纸边
    drawCircle(Color.White, radius = earLr + s * 0.055f, center = earL)
    drawCircle(Color.White, radius = earRr + s * 0.055f, center = earR)
    drawPath(
        Path().apply { addOval(Rect(center = Offset(cx, cy), radius = s * 0.42f)) },
        Color.White,
    )

    // 2) 淡彩
    val fill = lerp(mood.color, Color.White, 0.42f)
    drawCircle(fill, radius = earLr, center = earL)
    drawCircle(fill, radius = earRr, center = earR)
    drawPath(head, fill)

    // 3) 描边
    val outline = Stroke(width = s * 0.06f, cap = StrokeCap.Round)
    drawCircle(INK, radius = earLr, center = earL, style = outline)
    drawCircle(INK, radius = earRr, center = earR, style = outline)
    drawPath(head, INK, style = outline)

    // 4) 浅色口鼻区
    val muzzle = lerp(fill, Color.White, 0.55f)
    drawOval(
        muzzle,
        topLeft = Offset(cx - s * 0.155f, cy + s * 0.03f),
        size = Size(s * 0.31f, s * 0.22f),
    )

    drawFace(mood, cx, cy, s)
}

private fun DrawScope.drawFace(mood: Mood, cx: Float, cy: Float, s: Float) {
    val eyeDx = s * 0.15f
    val eyeY = cy - s * 0.10f
    val line = Stroke(width = s * 0.055f, cap = StrokeCap.Round)

    fun dots() {
        drawCircle(INK, radius = s * 0.032f, center = Offset(cx - eyeDx, eyeY))
        drawCircle(INK, radius = s * 0.032f, center = Offset(cx + eyeDx, eyeY))
    }

    fun closedEyes(up: Boolean) {
        val w = s * 0.105f
        val h = s * 0.075f
        listOf(-eyeDx, eyeDx).forEach { dx ->
            drawArc(
                color = INK,
                startAngle = if (up) 200f else 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx + dx - w / 2, eyeY - h / 2),
                size = Size(w, h),
                style = line,
            )
        }
    }

    // 鼻子 + 嘴：小熊的嘴是"人"字形，跟着情绪变
    val noseY = cy + s * 0.075f
    drawOval(
        INK,
        topLeft = Offset(cx - s * 0.045f, noseY - s * 0.028f),
        size = Size(s * 0.09f, s * 0.056f),
    )
    val mouthTop = noseY + s * 0.03f

    fun smileMouth() {
        drawLine(INK, Offset(cx, mouthTop), Offset(cx, mouthTop + s * 0.035f), line.width, StrokeCap.Round)
        drawArc(
            color = INK,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - s * 0.09f, mouthTop + s * 0.005f),
            size = Size(s * 0.18f, s * 0.10f),
            style = line,
        )
    }

    fun flatMouth() {
        drawLine(
            INK,
            Offset(cx - s * 0.055f, mouthTop + s * 0.03f),
            Offset(cx + s * 0.055f, mouthTop + s * 0.03f),
            line.width,
            StrokeCap.Round,
        )
    }

    fun frownMouth() {
        drawArc(
            color = INK,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - s * 0.08f, mouthTop + s * 0.05f),
            size = Size(s * 0.16f, s * 0.075f),
            style = line,
        )
    }

    val tear = Color(0xFF6FB7F2)

    when (mood) {
        Mood.HAPPY -> { closedEyes(up = true); smileMouth() }
        Mood.EXCITED -> {
            listOf(-eyeDx, eyeDx).forEach { dx ->
                drawLine(INK, Offset(cx + dx - s * 0.055f, eyeY), Offset(cx + dx + s * 0.055f, eyeY), line.width, StrokeCap.Round)
                drawLine(INK, Offset(cx + dx, eyeY - s * 0.055f), Offset(cx + dx, eyeY + s * 0.055f), line.width, StrokeCap.Round)
            }
            drawOval(INK, topLeft = Offset(cx - s * 0.055f, mouthTop), size = Size(s * 0.11f, s * 0.085f))
        }
        Mood.CALM -> { dots(); smileMouth() }
        Mood.MEH -> { dots(); flatMouth() }
        Mood.TIRED -> { closedEyes(up = false); drawCircle(INK, radius = s * 0.035f, center = Offset(cx, mouthTop + s * 0.03f)) }
        Mood.ANNOYED -> {
            drawLine(INK, Offset(cx - eyeDx - s * 0.06f, eyeY - s * 0.09f), Offset(cx - eyeDx + s * 0.05f, eyeY - s * 0.04f), line.width, StrokeCap.Round)
            drawLine(INK, Offset(cx + eyeDx - s * 0.05f, eyeY - s * 0.04f), Offset(cx + eyeDx + s * 0.06f, eyeY - s * 0.09f), line.width, StrokeCap.Round)
            dots()
            flatMouth()
        }
        Mood.DOWN -> { dots(); frownMouth() }

        Mood.SAD -> {
            // 哭得明显：八字眉 + 闭眼 + 两道粗眼泪 + 张开往下撇的嘴
            val browY = eyeY - s * 0.085f
            drawLine(INK, Offset(cx - eyeDx - s * 0.07f, browY + s * 0.03f), Offset(cx - eyeDx + s * 0.05f, browY - s * 0.02f), line.width, StrokeCap.Round)
            drawLine(INK, Offset(cx + eyeDx - s * 0.05f, browY - s * 0.02f), Offset(cx + eyeDx + s * 0.07f, browY + s * 0.03f), line.width, StrokeCap.Round)
            closedEyes(up = true)
            // 两道眼泪从眼角流到下巴
            listOf(-eyeDx, eyeDx).forEach { dx ->
                val x = cx + dx * 0.9f
                drawLine(
                    tear,
                    Offset(x, eyeY + s * 0.06f),
                    Offset(x + dx * 0.10f, cy + s * 0.30f),
                    strokeWidth = s * 0.075f,
                    cap = StrokeCap.Round,
                )
            }
            // 张开、向下撇的嘴
            val mouth = Path().apply {
                moveTo(cx - s * 0.085f, mouthTop + s * 0.02f)
                cubicTo(cx - s * 0.04f, mouthTop + s * 0.11f, cx + s * 0.04f, mouthTop + s * 0.11f, cx + s * 0.085f, mouthTop + s * 0.02f)
                cubicTo(cx + s * 0.03f, mouthTop + s * 0.05f, cx - s * 0.03f, mouthTop + s * 0.05f, cx - s * 0.085f, mouthTop + s * 0.02f)
                close()
            }
            drawPath(mouth, INK)
        }
    }
}
