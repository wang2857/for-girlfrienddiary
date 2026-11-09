package com.synn.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.sin

/**
 * 手绘描边：四条边各用一小段二次曲线，端点略微抖动。
 *
 * 不用 `stroke = BorderStroke` 那种笔直的线——参考图里的边框都是手画的、不太齐。
 */
fun Modifier.doodleBorder(
    color: Color,
    corner: Dp = 18.dp,
    width: Dp = 1.3.dp,
    seed: Int = 0,
): Modifier = drawBehind {
    val w = width.toPx()
    val inset = w / 2
    val rr = corner.toPx().coerceAtMost(minOf(size.width, size.height) / 2 - inset)
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    val wobble = w * 0.9f

    fun jitter(i: Int): Float {
        val x = sin(seed * 12.9898f + i * 78.233f) * 43758.5453f
        return (x - floor(x)) * 2f - 1f
    }

    val path = Path()

    fun edge(x1: Float, y1: Float, x2: Float, y2: Float, i: Int) {
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        // 沿法线方向偏一点点
        val dx = x2 - x1
        val dy = y2 - y1
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val nx = -dy / len * jitter(i) * wobble
        val ny = dx / len * jitter(i) * wobble
        path.quadraticBezierTo(mx + nx, my + ny, x2, y2)
    }

    path.moveTo(l + rr, t)
    edge(l + rr, t, r - rr, t, 1)
    path.quadraticBezierTo(r, t, r, t + rr)
    edge(r, t + rr, r, b - rr, 2)
    path.quadraticBezierTo(r, b, r - rr, b)
    edge(r - rr, b, l + rr, b, 3)
    path.quadraticBezierTo(l, b, l, b - rr)
    edge(l, b - rr, l, t + rr, 4)
    path.quadraticBezierTo(l, t, l + rr, t)

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** 手绘筛选片：底色 + 手绘描边，替代 M3 的 FilterChip（它的直角边太规整）。 */
@Composable
fun DoodleChip(
    selected: Boolean,
    onClick: () -> Unit,
    seed: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val border = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .doodleBorder(border, corner = 50.dp, width = 1.2.dp, seed = seed)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 手绘分隔线：一条略微起伏的线。 */
fun Modifier.doodleDivider(
    color: Color,
    width: Dp = 1.dp,
    seed: Int = 0,
): Modifier = drawBehind {
    val w = width.toPx()
    val y = size.height / 2f
    val path = Path()
    path.moveTo(0f, y)
    val segments = 3
    for (i in 1..segments) {
        val x0 = size.width * (i - 1) / segments
        val x1 = size.width * i / segments
        path.quadraticBezierTo(
            (x0 + x1) / 2f,
            y + (if (i % 2 == 0) 1 else -1) * w * (0.8f + 0.2f * seed),
            x1,
            y,
        )
    }
    drawPath(path, color, style = Stroke(w, cap = StrokeCap.Round))
}

// ---- 手绘线稿图标 ----

val DoodleSearchIcon: ImageVector by lazy {
    doodleVector(
        "DoodleSearch",
        "M11 4.6a6.4 6.4 0 1 0 0 12.8 6.4 6.4 0 0 0 0-12.8z",
        "M15.8 15.8l4 4",
    )
}

val DoodleBackIcon: ImageVector by lazy {
    doodleVector(
        "DoodleBack",
        "M19.4 12H5.2",
        "M10.6 6.4L5 12l5.6 5.6",
    )
}

val DoodleStarIcon: ImageVector by lazy {
    doodleVector(
        "DoodleStar",
        "M12 4.6l2.1 4.9 5.3.5-4 3.6 1.2 5.2L12 16l-4.6 2.8 1.2-5.2-4-3.6 5.3-.5z",
    )
}

val DoodleLockIcon: ImageVector by lazy {
    doodleVector(
        "DoodleLock",
        "M6.4 10.6h11.2v8.8H6.4z",
        "M9 10.6V8a3 3 0 0 1 6 0v2.6",
        "M12 14v2.4",
    )
}

val DoodleCheckIcon: ImageVector by lazy {
    doodleVector(
        "DoodleCheck",
        "M5.4 12.6l4.2 4.2L19 7.2",
    )
}

private fun doodleVector(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { p ->
            addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
