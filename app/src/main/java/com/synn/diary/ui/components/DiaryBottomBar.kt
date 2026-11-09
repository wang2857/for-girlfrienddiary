package com.synn.diary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 药丸的当前位置，跨页面共享。
 *
 * 底栏是每个页面各自的 Scaffold 里画的，切换 tab 时旧栏淡出、新栏淡入。
 * 如果把位置存在新栏自己的 state 里，它会以"目标位置"初始化，看起来就是直接跳过去；
 * 用进程内单例记住上一次的位置，新栏就能从旧位置接着滑。
 */
object TabIndicator {
    var position: Float by mutableFloatStateOf(0f)
}

private val TABS = listOf(
    "时间线" to "timeline",
    "日历" to "calendar",
    "设置" to "settings",
)

/**
 * 底部 tab：一块药丸在三个 tab 之间滑过去。
 *
 * 没用 M3 的 NavigationBar（它强制要图标），也没用三个各自淡入的药丸——
 * 那样切换时是"这边淡出、那边淡入"，不是真正的移动。这里只画**一块**药丸，
 * 位置和宽度都按动画进度插值，所以是连续滑过去的。
 */
@Composable
fun DiaryBottomBar(currentRoute: String, onSelect: (String) -> Unit) {
    val glass = LocalGlassEffect.current
    val content: @Composable () -> Unit = {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(58.dp),
        ) {
            // 顶上手绘的一道细线，和纸面的手账感呼应
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .doodleDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                        seed = 3,
                    ),
            )

            val selectedIndex = TABS.indexOfFirst { it.second == currentRoute }.coerceAtLeast(0)

            // 量一下三个标签的宽度：药丸要刚好裹住字，切到不同 tab 时宽度也要过渡
            val measurer = rememberTextMeasurer()
            val labelStyle = MaterialTheme.typography.labelLarge
            val density = LocalDensity.current
            val pillWidths = remember(labelStyle, density) {
                TABS.map { (label, _) ->
                    with(density) {
                        (measurer.measure(label, labelStyle.copy(fontWeight = FontWeight.SemiBold))
                            .size.width + 40.dp.roundToPx()).toDp()
                    }
                }
            }

            // 用一个浮点索引驱动：位置和宽度都从它插值，切换时不会出现"先变宽再移动"
            val animated = remember { Animatable(TabIndicator.position) }
            LaunchedEffect(selectedIndex) {
                // 比页面淡入（220ms）长一点：页面切完还能看见药丸滑到位
                animated.animateTo(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec = tween(460, easing = FastOutSlowInEasing),
                ) {
                    TabIndicator.position = value
                }
            }
            val position = animated.value
            val cell = maxWidth / TABS.size
            val from = floor(position).toInt().coerceIn(0, TABS.lastIndex)
            val to = ceil(position).toInt().coerceIn(0, TABS.lastIndex)
            val fraction = position - from
            val pillWidth: Dp = pillWidths[from] + (pillWidths[to] - pillWidths[from]) * fraction
            val pillX: Dp = cell * position + (cell - pillWidth) / 2

            if (glass) {
                GlassSurface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    alpha = 0.72f,
                ) {}
            }

            // 药丸：一块，跟着 position 滑
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = pillX, y = 0.dp)
                    .width(pillWidth)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                TABS.forEachIndexed { index, (label, route) ->
                    TabItem(
                        label = label,
                        selected = index == selectedIndex,
                        progress = (1f - kotlin.math.abs(position - index)).coerceIn(0f, 1f),
                        onClick = { onSelect(route) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }

    if (glass) {
        content()
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            content()
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    selected: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = lerp(
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.onPrimaryContainer,
                progress,
            ),
        )
    }
}
