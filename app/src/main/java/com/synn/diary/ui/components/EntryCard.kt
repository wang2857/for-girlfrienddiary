package com.synn.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import coil3.compose.AsyncImage
import com.synn.diary.data.body.BodyMarkers
import com.synn.diary.data.db.EntryWithTags
import com.synn.diary.ui.LocalImageFiles
import com.synn.diary.ui.mood.Mood
import com.synn.diary.util.Dates

@Composable
fun EntryCard(item: EntryWithTags, onClick: () -> Unit) {
    val entry = item.entry
    val mood = Mood.fromEmoji(entry.mood)
    // 心情原色有的很浅（比如开心是琥珀色），直接当文字色对比度不够，
    // 按明暗往黑/白方向压一档，保留色相但保证能读。
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = mood?.let { m ->
        lerp(m.color, if (dark) Color.White else Color.Black, if (dark) 0.25f else 0.3f)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        // 卡片底色跟着心情走：一眼扫下去能看出这段时间的情绪分布
        colors = if (mood != null) {
            CardDefaults.cardColors(containerColor = mood.color.copy(alpha = 0.16f))
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Dates.dayShort(entry.entryDate),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent ?: MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = Dates.weekdayShort(entry.entryDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 农历跟在后头，同样靠左
                Dates.lunar(entry.entryDate)?.let { lunar ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = lunar,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                // 天气（只要天气，不显示地点）
                entry.weatherText?.takeIf { it.isNotBlank() }?.let { weather ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = weather,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (entry.favorite) {
                    Text(text = "★", style = MaterialTheme.typography.labelLarge, color = mood?.color ?: MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                }
                if (entry.locked) {
                    Text(text = "🔒", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(4.dp))
                }
                entry.mood?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                }
            }

            if (entry.locked) {
                // 上锁的日记在列表里只露时间，正文、图片、标签都不渲染
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🔒 已上锁 · 解锁后查看",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                if (entry.title.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val preview = BodyMarkers.strip(entry.body)
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
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
                            .clip(RoundedCornerShape(12.dp)),
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
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
