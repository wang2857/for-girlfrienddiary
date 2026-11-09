package com.synn.diary.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/** 当前心情图标风格；MainActivity 从设置里注入。 */
val LocalMoodStyle = compositionLocalOf { MoodStyle.BEAR }

/**
 * 心情图标：四套画法共用同一套"表情语义"，只换脸型和笔触。
 *
 * - 小熊：贝塞尔圆脸 + 耳朵，白贴纸边
 * - 鹅黄：黄色椭圆脸 + 虚线裁切边（参考 emoji 贴纸）
 * - 圆环：深色粗圆环 + 白脸，线条极简
 * - 蜡笔：彩色粗描边 + 白脸，边缘故意画得不圆
 */
@Composable
fun MoodIcon(
    mood: Mood,
    size: Dp,
    modifier: Modifier = Modifier,
    style: MoodStyle? = null,
) {
    val resolved = style ?: LocalMoodStyle.current
    Canvas(modifier.size(size)) {
        when (resolved) {
            MoodStyle.BEAR -> drawBear(mood)
            MoodStyle.YELLOW -> drawYellow(mood)
            MoodStyle.RING -> drawRing(mood)
            MoodStyle.CRAYON -> drawCrayon(mood)
        }
    }
}

private val BEAR_INK = Color(0xFF2A2522)

// ---------------------------------------------------------------- 小熊

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
    val earL = Offset(cx - s * 0.27f, cy - s * 0.315f)
    val earR = Offset(cx + s * 0.25f, cy - s * 0.345f)
    val earLr = s * 0.125f
    val earRr = s * 0.115f

    drawCircle(Color.White, radius = earLr + s * 0.055f, center = earL)
    drawCircle(Color.White, radius = earRr + s * 0.055f, center = earR)
    drawCircle(Color.White, radius = s * 0.42f, center = Offset(cx, cy))

    val fill = lerp(mood.color, Color.White, 0.42f)
    drawCircle(fill, radius = earLr, center = earL)
    drawCircle(fill, radius = earRr, center = earR)
    drawPath(head, fill)

    val outline = Stroke(width = s * 0.06f, cap = StrokeCap.Round)
    drawCircle(BEAR_INK, radius = earLr, center = earL, style = outline)
    drawCircle(BEAR_INK, radius = earRr, center = earR, style = outline)
    drawPath(head, BEAR_INK, style = outline)

    drawOval(
        lerp(fill, Color.White, 0.55f),
        topLeft = Offset(cx - s * 0.155f, cy + s * 0.03f),
        size = Size(s * 0.31f, s * 0.22f),
    )

    val eyeDx = s * 0.15f
    val eyeY = cy - s * 0.10f
    val line = Stroke(width = s * 0.055f, cap = StrokeCap.Round)
    when (mood) {
        Mood.HAPPY -> { closedEyes(BEAR_INK, cx, eyeDx, eyeY, s, up = true, line); bearSmile(BEAR_INK, cx, cy, s, line) }
        Mood.EXCITED -> {
            sparkleEyes(BEAR_INK, cx, eyeDx, eyeY, s, line)
            drawOval(BEAR_INK, topLeft = Offset(cx - s * 0.05f, cy + s * 0.10f), size = Size(s * 0.10f, s * 0.08f))
        }
        Mood.CALM -> { dotEyes(BEAR_INK, cx, eyeDx, eyeY, s); bearSmile(BEAR_INK, cx, cy, s, line) }
        Mood.MEH -> { dotEyes(BEAR_INK, cx, eyeDx, eyeY, s); lineMouth(BEAR_INK, cx, cy, s, line) }
        Mood.TIRED -> { closedEyes(BEAR_INK, cx, eyeDx, eyeY, s, up = false, line); dotMouth(BEAR_INK, cx, cy, s) }
        Mood.ANNOYED -> {
            brows(BEAR_INK, cx, eyeDx, eyeY, s, line)
            dotEyes(BEAR_INK, cx, eyeDx, eyeY, s)
            lineMouth(BEAR_INK, cx, cy, s, line)
        }
        Mood.DOWN -> { dotEyes(BEAR_INK, cx, eyeDx, eyeY, s); frown(BEAR_INK, cx, cy, s, line) }
        Mood.SAD -> {
            brows(BEAR_INK, cx, eyeDx, eyeY, s, line, sad = true)
            closedEyes(BEAR_INK, cx, eyeDx, eyeY, s, up = true, line)
            tears(TEAR_BLUE, cx, eyeDx, eyeY, s, heavy = true)
            wailMouth(BEAR_INK, cx, cy, s)
        }
    }
}

// ---------------------------------------------------------------- 鹅黄

private fun DrawScope.drawYellow(mood: Mood) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rx = s * 0.40f
    val ry = s * 0.34f
    val face = lerp(Color(0xFFFFD84D), mood.color, 0.18f)

    // 虚线裁切边
    drawOval(
        color = Color(0xFF6E5A2A).copy(alpha = 0.55f),
        topLeft = Offset(cx - rx - s * 0.055f, cy - ry - s * 0.055f),
        size = Size((rx + s * 0.055f) * 2, (ry + s * 0.055f) * 2),
        style = Stroke(
            width = s * 0.022f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.055f, s * 0.05f)),
        ),
    )
    drawOval(face, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
    // 顶部高光
    drawOval(
        Color.White.copy(alpha = 0.35f),
        topLeft = Offset(cx - rx * 0.55f, cy - ry * 0.78f),
        size = Size(rx * 0.9f, ry * 0.45f),
    )

    val ink = Color(0xFF4A3A18)
    val eyeDx = s * 0.15f
    val eyeY = cy - s * 0.05f
    val line = Stroke(width = s * 0.045f, cap = StrokeCap.Round)
    // 腮红
    drawOval(Color(0xFFFF8A80).copy(alpha = 0.55f), Offset(cx - s * 0.30f, cy + s * 0.06f), Size(s * 0.11f, s * 0.06f))
    drawOval(Color(0xFFFF8A80).copy(alpha = 0.55f), Offset(cx + s * 0.19f, cy + s * 0.06f), Size(s * 0.11f, s * 0.06f))

    when (mood) {
        Mood.HAPPY -> { closedEyes(ink, cx, eyeDx, eyeY, s, up = true, line); bigSmile(ink, cx, cy, s) }
        Mood.EXCITED -> { sparkleEyes(ink, cx, eyeDx, eyeY, s, line); bigSmile(ink, cx, cy, s) }
        Mood.CALM -> { ovalEyes(ink, cx, eyeDx, eyeY, s); smallSmile(ink, cx, cy, s, line) }
        Mood.MEH -> { lineEyes(ink, cx, eyeDx, eyeY, s, line); lineMouth(ink, cx, cy, s, line) }
        Mood.TIRED -> { closedEyes(ink, cx, eyeDx, eyeY, s, up = false, line); dotMouth(ink, cx, cy, s) }
        Mood.ANNOYED -> { brows(ink, cx, eyeDx, eyeY, s, line); ovalEyes(ink, cx, eyeDx, eyeY, s); lineMouth(ink, cx, cy, s, line) }
        Mood.DOWN -> { ovalEyes(ink, cx, eyeDx, eyeY, s); frown(ink, cx, cy, s, line) }
        Mood.SAD -> {
            brows(ink, cx, eyeDx, eyeY, s, line, sad = true)
            ovalEyes(ink, cx, eyeDx, eyeY, s)
            tears(TEAR_BLUE, cx, eyeDx, eyeY, s, heavy = true)
            frown(ink, cx, cy, s, line)
        }
    }
}

// ---------------------------------------------------------------- 圆环

private fun DrawScope.drawRing(mood: Mood) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val ring = Color(0xFF4A3A32)

    drawCircle(Color.White, radius = s * 0.44f, center = Offset(cx, cy))
    drawCircle(ring, radius = s * 0.38f, center = Offset(cx, cy), style = Stroke(width = s * 0.12f))

    val ink = ring
    val eyeDx = s * 0.12f
    val eyeY = cy - s * 0.06f
    val line = Stroke(width = s * 0.04f, cap = StrokeCap.Round)

    when (mood) {
        Mood.HAPPY -> { closedEyes(ink, cx, eyeDx, eyeY, s, up = true, line); smallSmile(ink, cx, cy, s, line) }
        Mood.EXCITED -> { sparkleEyes(ink, cx, eyeDx, eyeY, s, line); dotMouth(ink, cx, cy, s) }
        Mood.CALM -> { dotEyes(ink, cx, eyeDx, eyeY, s, small = true); smallSmile(ink, cx, cy, s, line) }
        Mood.MEH -> { lineEyes(ink, cx, eyeDx, eyeY, s, line); lineMouth(ink, cx, cy, s, line) }
        Mood.TIRED -> { closedEyes(ink, cx, eyeDx, eyeY, s, up = false, line); lineMouth(ink, cx, cy, s, line) }
        Mood.ANNOYED -> { brows(ink, cx, eyeDx, eyeY, s, line); dotEyes(ink, cx, eyeDx, eyeY, s, small = true); lineMouth(ink, cx, cy, s, line) }
        Mood.DOWN -> { dotEyes(ink, cx, eyeDx, eyeY, s, small = true); frown(ink, cx, cy, s, line) }
        Mood.SAD -> {
            brows(ink, cx, eyeDx, eyeY, s, line, sad = true)
            dotEyes(ink, cx, eyeDx, eyeY, s, small = true)
            tears(TEAR_BLUE, cx, eyeDx, eyeY, s, heavy = false)
            frown(ink, cx, cy, s, line)
        }
    }
}

// ---------------------------------------------------------------- 蜡笔

private fun DrawScope.drawCrayon(mood: Mood) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = s * 0.40f
    val ink = mood.color

    // 抖动圆：每 30° 一个点，半径随机偏一点，像用蜡笔描的
    val wobble = Path()
    val points = 12
    for (i in 0..points) {
        val a = (i % points) * (2 * PI / points)
        val jitter = (sin(i * 12.9898).toFloat() * 0.5f) * s * 0.035f
        val x = cx + (r + jitter) * cos(a).toFloat()
        val y = cy + (r + jitter) * sin(a).toFloat()
        if (i == 0) wobble.moveTo(x, y) else wobble.lineTo(x, y)
    }
    wobble.close()
    drawPath(wobble, Color.White)
    drawPath(wobble, ink, style = Stroke(width = s * 0.085f, cap = StrokeCap.Round))

    val eyeDx = s * 0.13f
    val eyeY = cy - s * 0.06f
    val line = Stroke(width = s * 0.06f, cap = StrokeCap.Round)

    when (mood) {
        Mood.HAPPY -> { closedEyes(ink, cx, eyeDx, eyeY, s, up = true, line); smallSmile(ink, cx, cy, s, line) }
        Mood.EXCITED -> { sparkleEyes(ink, cx, eyeDx, eyeY, s, line); dotMouth(ink, cx, cy, s) }
        Mood.CALM -> { dotEyes(ink, cx, eyeDx, eyeY, s); smallSmile(ink, cx, cy, s, line) }
        Mood.MEH -> { lineEyes(ink, cx, eyeDx, eyeY, s, line); lineMouth(ink, cx, cy, s, line) }
        Mood.TIRED -> { closedEyes(ink, cx, eyeDx, eyeY, s, up = false, line); lineMouth(ink, cx, cy, s, line) }
        Mood.ANNOYED -> { brows(ink, cx, eyeDx, eyeY, s, line); dotEyes(ink, cx, eyeDx, eyeY, s); lineMouth(ink, cx, cy, s, line) }
        Mood.DOWN -> { dotEyes(ink, cx, eyeDx, eyeY, s); frown(ink, cx, cy, s, line) }
        Mood.SAD -> { dotEyes(ink, cx, eyeDx, eyeY, s); tears(TEAR_BLUE, cx, eyeDx, eyeY, s, heavy = true); wailMouth(ink, cx, cy, s) }
    }
}

// ---------------------------------------------------------------- 共用五官

private val TEAR_BLUE = Color(0xFF6FB7F2)

private fun DrawScope.dotEyes(ink: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float, small: Boolean = false) {
    val r = s * if (small) 0.032f else 0.042f
    drawCircle(ink, radius = r, center = Offset(cx - eyeDx, eyeY))
    drawCircle(ink, radius = r, center = Offset(cx + eyeDx, eyeY))
}

private fun DrawScope.ovalEyes(ink: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float) {
    val w = s * 0.09f
    val h = s * 0.11f
    listOf(-eyeDx, eyeDx).forEach { dx ->
        drawOval(ink, topLeft = Offset(cx + dx - w / 2, eyeY - h / 2), size = Size(w, h))
        drawCircle(Color.White, radius = s * 0.018f, center = Offset(cx + dx - w * 0.18f, eyeY - h * 0.18f))
    }
}

private fun DrawScope.lineEyes(ink: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float, line: Stroke) {
    listOf(-eyeDx, eyeDx).forEach { dx ->
        drawLine(ink, Offset(cx + dx - s * 0.05f, eyeY), Offset(cx + dx + s * 0.05f, eyeY), line.width, StrokeCap.Round)
    }
}

private fun DrawScope.closedEyes(ink: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float, up: Boolean, line: Stroke) {
    val w = s * 0.11f
    val h = s * 0.075f
    listOf(-eyeDx, eyeDx).forEach { dx ->
        drawArc(
            color = ink,
            startAngle = if (up) 200f else 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + dx - w / 2, eyeY - h / 2),
            size = Size(w, h),
            style = line,
        )
    }
}

private fun DrawScope.sparkleEyes(ink: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float, line: Stroke) {
    listOf(-eyeDx, eyeDx).forEach { dx ->
        drawLine(ink, Offset(cx + dx - s * 0.055f, eyeY), Offset(cx + dx + s * 0.055f, eyeY), line.width, StrokeCap.Round)
        drawLine(ink, Offset(cx + dx, eyeY - s * 0.055f), Offset(cx + dx, eyeY + s * 0.055f), line.width, StrokeCap.Round)
    }
}

private fun DrawScope.brows(ink: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float, line: Stroke, sad: Boolean = false) {
    val y = eyeY - s * 0.085f
    val drop = if (sad) s * 0.03f else 0f
    drawLine(ink, Offset(cx - eyeDx - s * 0.06f, y + drop), Offset(cx - eyeDx + s * 0.05f, y - drop), line.width, StrokeCap.Round)
    drawLine(ink, Offset(cx + eyeDx - s * 0.05f, y - drop), Offset(cx + eyeDx + s * 0.06f, y + drop), line.width, StrokeCap.Round)
}

private fun DrawScope.smallSmile(ink: Color, cx: Float, cy: Float, s: Float, line: Stroke) {
    drawArc(
        ink, 20f, 140f, false,
        topLeft = Offset(cx - s * 0.07f, cy + s * 0.07f),
        size = Size(s * 0.14f, s * 0.09f),
        style = line,
    )
}

private fun DrawScope.bigSmile(ink: Color, cx: Float, cy: Float, s: Float) {
    val mouth = Path().apply {
        moveTo(cx - s * 0.10f, cy + s * 0.07f)
        cubicTo(cx - s * 0.06f, cy + s * 0.16f, cx + s * 0.06f, cy + s * 0.16f, cx + s * 0.10f, cy + s * 0.07f)
        close()
    }
    drawPath(mouth, ink)
}

private fun DrawScope.lineMouth(ink: Color, cx: Float, cy: Float, s: Float, line: Stroke) {
    drawLine(ink, Offset(cx - s * 0.06f, cy + s * 0.11f), Offset(cx + s * 0.06f, cy + s * 0.11f), line.width, StrokeCap.Round)
}

private fun DrawScope.dotMouth(ink: Color, cx: Float, cy: Float, s: Float) {
    drawCircle(ink, radius = s * 0.035f, center = Offset(cx, cy + s * 0.11f))
}

private fun DrawScope.frown(ink: Color, cx: Float, cy: Float, s: Float, line: Stroke) {
    drawArc(
        ink, 200f, 140f, false,
        topLeft = Offset(cx - s * 0.075f, cy + s * 0.11f),
        size = Size(s * 0.15f, s * 0.08f),
        style = line,
    )
}

private fun DrawScope.bearSmile(ink: Color, cx: Float, cy: Float, s: Float, line: Stroke) {
    drawLine(ink, Offset(cx, cy + s * 0.09f), Offset(cx, cy + s * 0.12f), line.width, StrokeCap.Round)
    drawArc(
        ink, 20f, 140f, false,
        topLeft = Offset(cx - s * 0.09f, cy + s * 0.115f),
        size = Size(s * 0.18f, s * 0.10f),
        style = line,
    )
}

private fun DrawScope.wailMouth(ink: Color, cx: Float, cy: Float, s: Float) {
    val mouth = Path().apply {
        moveTo(cx - s * 0.085f, cy + s * 0.08f)
        cubicTo(cx - s * 0.04f, cy + s * 0.17f, cx + s * 0.04f, cy + s * 0.17f, cx + s * 0.085f, cy + s * 0.08f)
        cubicTo(cx + s * 0.03f, cy + s * 0.11f, cx - s * 0.03f, cy + s * 0.11f, cx - s * 0.085f, cy + s * 0.08f)
        close()
    }
    drawPath(mouth, ink)
}

private fun DrawScope.tears(color: Color, cx: Float, eyeDx: Float, eyeY: Float, s: Float, heavy: Boolean) {
    listOf(-eyeDx, eyeDx).forEach { dx ->
        val x = cx + dx * 0.92f
        drawLine(
            color,
            Offset(x, eyeY + s * 0.05f),
            Offset(x + dx * 0.12f, eyeY + s * (if (heavy) 0.30f else 0.22f)),
            strokeWidth = s * (if (heavy) 0.07f else 0.05f),
            cap = StrokeCap.Round,
        )
    }
}
