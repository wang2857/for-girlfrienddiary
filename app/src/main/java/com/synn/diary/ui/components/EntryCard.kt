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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        // 卡片底色跟着心情走：一眼扫下去能看出这段时间的情绪分布。
        // 深色模式下调高一点，否则心情色会被深底吃掉、看起来发灰。
        colors = if (mood != null) {
            CardDefaults.cardColors(
                containerColor = mood.color.copy(alpha = if (dark) 0.22f else 0.16f),
            )
        } else {
            CardDefaults.cardColors()
        },
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
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.weight(1f))

                if (entry.favorite) {
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent ?: MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (entry.locked) {
                    Text(text = "🔒", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(4.dp))
                }

                // 心情放在浅色圆底里：emoji 直接压在彩色卡片上会糊
                entry.mood?.let { emoji ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                (mood?.color ?: Color.Transparent)
                                    .copy(alpha = if (dark) 0.26f else 0.18f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                    }
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val preview = BodyMarkers.strip(entry.body)
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
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
