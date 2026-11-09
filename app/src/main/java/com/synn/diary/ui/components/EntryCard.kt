package com.synn.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.synn.diary.data.body.BodyMarkers
import com.synn.diary.data.db.EntryWithTags
import com.synn.diary.ui.LocalImageFiles
import com.synn.diary.ui.components.DoodleLockIcon
import com.synn.diary.ui.components.DoodleStarIcon
import com.synn.diary.ui.components.doodleBorder
import com.synn.diary.ui.mood.Mood
import com.synn.diary.ui.mood.MoodIcon
import com.synn.diary.util.Dates

@Composable
fun EntryCard(item: EntryWithTags, onClick: () -> Unit) {
    val entry = item.entry
    val mood = Mood.fromEmoji(entry.mood)

    // 心情原色有的很浅（比如开心是琥珀色），直接当文字色对比度不够，
    // 按明暗往黑/白方向压一档，保留色相但保证能读。
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = mood?.let { m ->
        // 底色心情色更浓了（浅 30% / 深 34%），日期文字跟着加深，保证 4.8:1 以上
        lerp(m.color, if (dark) Color.White else Color.Black, if (dark) 0.45f else 0.65f)
    }

    // 卡片底色跟着心情走；描边是手绘的，不用 Material 的 Card（它的边太"机"）
    val cardColor = mood?.let { m ->
        lerp(
            MaterialTheme.colorScheme.surfaceVariant,
            m.color,
            if (dark) 0.34f else 0.30f,
        )
    } ?: MaterialTheme.colorScheme.surfaceVariant
    val inkLine = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.30f else 0.24f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .doodleBorder(color = inkLine, corner = 20.dp, seed = entry.id.toInt())
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // —— 抬头：日期是主信息，其余附注降级成一行小字 ——
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Dates.dayShort(entry.entryDate),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = accent ?: MaterialTheme.colorScheme.onSurface,
                )

                val metaParts = listOfNotNull(
                    Dates.weekdayShort(entry.entryDate),
                    Dates.lunar(entry.entryDate),
                    entry.weatherText?.takeIf { it.isNotBlank() },
                )
                if (metaParts.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = metaParts.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.weight(1f))

                if (entry.favorite) {
                    Icon(
                        imageVector = DoodleStarIcon,
                        contentDescription = "已收藏",
                        modifier = Modifier.size(14.dp),
                        tint = accent ?: MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (entry.locked) {
                    Icon(
                        imageVector = DoodleLockIcon,
                        contentDescription = "已上锁",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // 心情贴纸直接贴上去：自带白边，不再需要灰色圆底
                mood?.let { current ->
                    MoodIcon(mood = current, size = 27.dp)
                }
            }

            if (entry.locked) {
                // 上锁的日记在列表里只露时间，正文、图片、标签都不渲染
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DoodleLockIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "已上锁 · 解锁后查看",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                if (entry.title.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val preview = BodyMarkers.strip(entry.body)
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BodyMarkers.firstImage(entry.body)?.let { imageName ->
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = LocalImageFiles.current(imageName),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.take(4).forEach { TagPill(it.name) }
                    }
                }
            }
        }
    }
}

@Composable
fun TagPill(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(50)),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
