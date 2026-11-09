package com.synn.diary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.components.DiaryBottomBar
import com.synn.diary.ui.components.EntryCard
import com.synn.diary.util.Dates
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
private val monthTitleFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenEntry: (Long) -> Unit,
    onSelectTab: (String) -> Unit,
) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val dayInfo by viewModel.dayInfo.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val dayEntries by viewModel.dayEntries.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now().toEpochDay() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("日历") }) },
        bottomBar = { DiaryBottomBar(currentRoute = "calendar", onSelect = onSelectTab) },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            MonthBar(
                title = month.format(monthTitleFormatter),
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                WEEK_LABELS.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // 换月时整块网格横向滑一小段：往前翻是向左，往回翻是向右
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val forward = targetState > initialState
                    val shift = if (forward) 1 else -1
                    (slideInHorizontally(tween(200)) { width -> shift * width / 5 } +
                        fadeIn(tween(180)))
                        .togetherWith(
                            slideOutHorizontally(tween(180)) { width -> -shift * width / 5 } +
                                fadeOut(tween(140)),
                        )
                },
                label = "calendar-month",
            ) { targetMonth ->
                MonthGrid(
                    month = targetMonth,
                    dayInfo = dayInfo,
                    selectedDay = selectedDay,
                    today = today,
                    onSelectDay = viewModel::selectDay,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            when {
                selectedDay == null -> CenterHint("点某一天，看那天写了什么")
                dayEntries.isEmpty() -> CenterHint("${Dates.dayShort(selectedDay!!)} 这天没有记录")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(dayEntries, key = { it.entry.id }) { item ->
                        EntryCard(item = item, onClick = { onOpenEntry(item.entry.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthBar(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    dayInfo: Map<Long, String?>,
    selectedDay: Long?,
    today: Long,
    onSelectDay: (Long) -> Unit,
) {
    val firstDay = month.atDay(1)
    // 周一为一周的第一天：DayOfWeek.value 是 1..7（周一=1）
    val offset = (firstDay.dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()
    val rows = (offset + daysInMonth + 6) / 7

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayNumber = index - offset + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..daysInMonth) {
                            val epochDay = firstDay.withDayOfMonth(dayNumber).toEpochDay()
                            val lunar = Dates.lunarLabel(epochDay)
                            DayCell(
                                dayNumber = dayNumber,
                                lunarLabel = lunar?.first.orEmpty(),
                                isLunarMonthStart = lunar?.second == true,
                                hasEntry = dayInfo.containsKey(epochDay),
                                mood = dayInfo[epochDay],
                                selected = epochDay == selectedDay,
                                isToday = epochDay == today,
                                onClick = { onSelectDay(epochDay) },
                            )
                        } else {
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    lunarLabel: String,
    isLunarMonthStart: Boolean,
    hasEntry: Boolean,
    mood: String?,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val circleColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val numberColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val lunarColor = when {
        selected || hasEntry -> MaterialTheme.colorScheme.primary
        // 农历初一那格写的是月份，颜色压重一点，扫一眼就能看出月份分界
        isLunarMonthStart -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 心情挂在数字圆圈的右上角，而不是格子角落：
            // 挂在格子角落时，7 号的心情看起来像 8 号的。
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .then(
                            if (isToday && !selected) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dayNumber.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = numberColor,
                        fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                if (mood != null) {
                    Text(
                        text = mood,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-5).dp),
                    )
                }
            }
            Text(
                text = lunarLabel,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = if (isLunarMonthStart) FontWeight.Medium else FontWeight.Normal,
                color = lunarColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CenterHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
