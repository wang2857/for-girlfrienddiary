package com.synn.diary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 右滑露出操作按钮的容器。
 *
 * 交互：卡片整体右移，操作面板从左侧整体滑入，两者同步。
 * 松手后用 spring 弹性吸附到「全开 / 全关」两态。
 *
 * 拖拽期间只改普通状态（不启动协程），松手才交给 Animatable——
 * 早先每个 delta 都 `launch { snapTo }`，松手的 animateTo 会被残留的 snapTo 打断，
 * 表现就是"滑到哪停哪"，没有回弹。
 */
@Composable
fun SwipeActionsContainer(
    actionWidth: Dp,
    actions: @Composable RowScope.(close: () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val maxOffset = with(density) { actionWidth.toPx() }
    val shape = RoundedCornerShape(16.dp)

    val animated = remember { Animatable(0f) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val current = if (dragging) dragValue else animated.value

    fun settleTo(target: Float, from: Float, velocity: Float = 0f) {
        scope.launch {
            animated.snapTo(from)
            dragging = false
            animated.animateTo(
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    val close: () -> Unit = { settleTo(0f, animated.value) }

    // 外层统一裁剪成圆角：卡片右移后超出容器的部分被圆角切掉，
    // 视觉上仍然是圆角，而不是直角
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
    ) {
        // 面板拿到与容器同高，便于三个按钮铺满
        Box(modifier = Modifier.matchParentSize()) {
            Row(
                modifier = Modifier
                    .offset { IntOffset((current - maxOffset).roundToInt(), 0) }
                    .width(actionWidth)
                    .fillMaxHeight()
                    // 面板自己是完整的圆角矩形（四角都圆），右侧留一点空隙
                    // 让它和卡片分开，不然两块圆角贴在一起看不出层次
                    .padding(end = PANEL_GAP)
                    .clip(shape),
            ) {
                actions(close)
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(current.roundToInt(), 0) }
                // 卡片本身是半透明的心情色，必须垫一层不透明底，
                // 否则没划开时也能透过卡片看到背后的按钮
                .clip(shape)
                .background(MaterialTheme.colorScheme.background)
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (!dragging) {
                            dragging = true
                            dragValue = animated.value
                        }
                        val raw = dragValue + delta
                        // 超过按钮宽度后加阻尼，像橡皮筋一样拖不太动
                        dragValue = if (raw > maxOffset) {
                            maxOffset + (raw - maxOffset) * OVERSHOOT_DAMPING
                        } else {
                            raw.coerceAtLeast(0f)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val target = when {
                            velocity > FLING_VELOCITY -> maxOffset
                            velocity < -FLING_VELOCITY -> 0f
                            dragValue > maxOffset / 2 -> maxOffset
                            else -> 0f
                        }
                        settleTo(target, dragValue, velocity)
                    },
                ),
        ) {
            content()
        }
    }
}

/** 右滑露出的单个操作按钮。 */
@Composable
fun RowScope.SwipeAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private val PANEL_GAP = 8.dp
private const val OVERSHOOT_DAMPING = 0.28f
private const val FLING_VELOCITY = 900f
