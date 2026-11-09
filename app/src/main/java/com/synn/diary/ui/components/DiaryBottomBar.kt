package com.synn.diary.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 顶部底部的两个 tab。没有用 M3 的 NavigationBar：它强制要图标，而这里只有文字更干净。 */
@Composable
fun DiaryBottomBar(currentRoute: String, onSelect: (String) -> Unit) {
    val glass = LocalGlassEffect.current
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
        ) {
            TabItem("时间线", "timeline", currentRoute, onSelect, Modifier.weight(1f))
            TabItem("日历", "calendar", currentRoute, onSelect, Modifier.weight(1f))
            TabItem("设置", "settings", currentRoute, onSelect, Modifier.weight(1f))
        }
    }
    if (glass) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            alpha = 0.72f,
        ) {
            content()
        }
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
    route: String,
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = currentRoute == route
    // 药丸指示器：选中时从透明淡出成主色浅底，宽度跟着内边距一起长大
    val pill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(240),
        label = "tab-pill",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(route) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    lerp(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primaryContainer,
                        pill,
                    ),
                )
                .padding(
                    horizontal = (12 + 8 * pill).dp,
                    vertical = (7 + 1 * pill).dp,
                ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = lerp(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    pill,
                ),
            )
        }
    }
}
