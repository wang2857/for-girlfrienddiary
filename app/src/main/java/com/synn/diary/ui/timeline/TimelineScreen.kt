package com.synn.diary.ui.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.components.DoodleDialog
import com.synn.diary.ui.components.DoodleTextButton
import com.synn.diary.ui.components.DiaryBottomBar
import com.synn.diary.ui.components.EntryCard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.synn.diary.data.db.EntryWithTags
import com.synn.diary.data.lock.LockManager
import com.synn.diary.ui.components.SwipeAction
import com.synn.diary.ui.components.SwipeActionsContainer
import com.synn.diary.ui.lock.UnlockEntryDialog
import com.synn.diary.ui.mood.Mood
import com.synn.diary.ui.components.DoodleChip
import com.synn.diary.ui.components.DoodleTopBar
import com.synn.diary.ui.components.doodleBorder
import com.synn.diary.ui.components.DoodleSearchIcon
import com.synn.diary.ui.components.DoodleStarIcon
import com.synn.diary.ui.mood.MoodIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    lockManager: LockManager,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectTab: (String) -> Unit,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val moodFilter by viewModel.moodFilter.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 解除单篇锁同样要验密码，不能点一下就解开
    var pendingUnlockRemoval by remember { mutableStateOf<EntryWithTags?>(null) }
    // 标签管理（长按会被 Chip 自己的点击吃掉，改成显式入口）
    var showTagManager by remember { mutableStateOf(false) }
    var pendingTagDelete by remember { mutableStateOf<Pair<Long, String>?>(null) }
    val loadedAll by viewModel.loadedAll.collectAsStateWithLifecycle()

    // 贴顶的永远是最顶层那级：有多年时贴"年"，只有一年时贴"月"。
    // 否则单一年的用户会完全没有贴顶标题，滚动时不知道自己在哪个月。
    val showYearHeaders = rows.any { it is TimelineRow.Year }

    Scaffold(
        topBar = {
            DoodleTopBar(
                title = "日记",
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = DoodleSearchIcon,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = { DiaryBottomBar(currentRoute = "timeline", onSelect = onSelectTab) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateEntry,
                modifier = Modifier.doodleBorder(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                    corner = 16.dp,
                    width = 1.2.dp,
                    seed = 7,
                ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text("写一笔")
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            FilterBar(
                tags = tags.map { it.id to it.name },
                selectedTag = selectedTag,
                onSelectTag = viewModel::selectTag,
                selectedMood = moodFilter,
                onToggleMood = viewModel::toggleMood,
                favoritesOnly = favoritesOnly,
                onToggleFavorites = viewModel::toggleFavoritesOnly,
                onManageTags = { showTagManager = true },
                onClear = viewModel::clearFilters,
            )

            if (rows.isEmpty()) {
                EmptyTimeline(filtering = selectedTag != null || moodFilter != null || favoritesOnly)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    // 卡片之间松一点，长列表更好扫
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rows.forEach { row ->
                        when (row) {
                            // 只有年这一级贴顶：月如果也贴顶，滚到下一年的内容时
                            // 上个月的标题会一直赖在顶上，看起来像在说错话。
                            is TimelineRow.Year -> if (showYearHeaders) {
                                stickyHeader(key = row.key) {
                                    GroupHeader(
                                        title = "${row.year}年",
                                        count = row.count,
                                        expanded = row.expanded,
                                        // 年：这一屏最大的标题，18sp 半粗
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.4.sp,
                                        ),
                                        sticky = true,
                                        onClick = { viewModel.toggleGroup(row.key) },
                                    )
                                }
                            } else {
                                Unit
                            }

                            is TimelineRow.Month -> if (!showYearHeaders) {
                                stickyHeader(key = row.key) {
                                    GroupHeader(
                                        title = monthLabel(row.month),
                                        count = row.count,
                                        expanded = row.expanded,
                                        // 月：15sp 中粗 + 主色，比年低一档
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 0.8.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        sticky = true,
                                        onClick = { viewModel.toggleGroup(row.key) },
                                    )
                                }
                            } else {
                                item(key = row.key) {
                                    GroupHeader(
                                        title = monthLabel(row.month),
                                        count = row.count,
                                        expanded = row.expanded,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        onClick = { viewModel.toggleGroup(row.key) },
                                    )
                                }
                            }

                            is TimelineRow.Week -> item(key = row.key) {
                                WeekHeader(
                                    label = row.label,
                                    count = row.count,
                                    expanded = row.expanded,
                                    isCurrent = row.isCurrent,
                                    onClick = { viewModel.toggleGroup(row.key) },
                                )
                            }

                            is TimelineRow.Entry -> item(key = row.key) {
                                val entry = row.item.entry
                                SwipeActionsContainer(
                                    actionWidth = 204.dp,
                                    actions = { close ->
                                        SwipeAction(
                                            label = "删除",
                                            color = MaterialTheme.colorScheme.error,
                                        ) {
                                            close()
                                            viewModel.delete(entry.id)
                                        }
                                        SwipeAction(
                                            label = if (entry.favorite) "取消收藏" else "收藏",
                                            color = Color(0xFFEF9A1B),
                                        ) {
                                            close()
                                            viewModel.toggleFavorite(entry.id, !entry.favorite)
                                        }
                                        SwipeAction(
                                            label = if (entry.locked) "解锁" else "上锁",
                                            color = Color(0xFF5C6BC0),
                                        ) {
                                            close()
                                            if (entry.locked) {
                                                pendingUnlockRemoval = row.item
                                            } else if (!lockManager.canLockEntries()) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("先在「设置」里开启应用锁（设 4 位密码），才能给日记上锁")
                                                }
                                            } else {
                                                viewModel.setLocked(entry.id, true)
                                            }
                                        }
                                    },
                                ) {
                                    EntryCard(
                                        item = row.item,
                                        onClick = { onOpenEntry(row.item.entry.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTagManager) {
        DoodleDialog(
            onDismissRequest = { showTagManager = false },
            title = { Text("管理标签") },
            text = {
                Column {
                    if (tags.isEmpty()) {
                        Text("还没有标签")
                    }
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tag.name, modifier = Modifier.weight(1f))
                            DoodleTextButton(onClick = { pendingTagDelete = tag.id to tag.name }) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                DoodleTextButton(onClick = { showTagManager = false }) { Text("完成") }
            },
        )
    }

    pendingTagDelete?.let { (tagId, name) ->
        DoodleDialog(
            onDismissRequest = { pendingTagDelete = null },
            title = { Text("删除标签「$name」？") },
            text = { Text("只删标签本身，日记内容不受影响。") },
            confirmButton = {
                DoodleTextButton(onClick = {
                    viewModel.deleteTag(tagId)
                    pendingTagDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                DoodleTextButton(onClick = { pendingTagDelete = null }) { Text("取消") }
            },
        )
    }

    pendingUnlockRemoval?.let { target ->
        UnlockEntryDialog(
            title = "解除这篇日记的锁定",
            verifyPin = lockManager::verifyPin,
            biometricEnabled = lockManager.biometricEnabled,
            onUnlocked = {
                viewModel.setLocked(target.entry.id, false)
                pendingUnlockRemoval = null
            },
            onDismiss = { pendingUnlockRemoval = null },
        )
    }
}

/** 年 / 月 两级标题共用一个组件，差别只在字号、颜色和是否贴顶。 */
@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    style: TextStyle,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    sticky: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (sticky) {
                    // 贴顶时底部渐隐：内容从下面滑过去不再"硬切"一条边
                    val bg = MaterialTheme.colorScheme.background
                    Modifier.background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to bg,
                            0.7f to bg,
                            1f to bg.copy(alpha = 0f),
                        ),
                    )
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 折叠指示：一个缓慢旋转的小箭头，展开时几乎看不出是控件
        Text(
            text = "›",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(16.dp)
                .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
        )
        Text(
            text = title,
            style = style.copy(letterSpacing = 0.6.sp),
            color = color,
        )
        Spacer(Modifier.weight(1f))
        // 篇数靠右、更淡，不跟标题抢视线
        Text(
            text = "$count 篇",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun WeekHeader(
    label: String,
    count: Int,
    expanded: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "›",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(14.dp)
                .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
        )
        Text(
            text = label,
            // 周：12sp 中等字重，比月份更轻，只当分隔
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            ),
            color = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        // 一条细线代替原来的"本周"胶囊，安静地分隔日期和篇数
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$count 篇",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun FilterBar(
    tags: List<Pair<Long, String>>,
    selectedTag: Long?,
    onSelectTag: (Long?) -> Unit,
    selectedMood: String?,
    onToggleMood: (String) -> Unit,
    favoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    onManageTags: () -> Unit,
    onClear: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            DoodleChip(
                selected = selectedTag == null && selectedMood == null && !favoritesOnly,
                onClick = onClear,
                seed = 1,
            ) { Text("全部") }
        }

        item(key = "favorites") {
            DoodleChip(
                selected = favoritesOnly,
                onClick = onToggleFavorites,
                seed = 2,
            ) {
                Icon(
                    imageVector = DoodleStarIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text("收藏")
            }
        }

        items(tags, key = { "t${it.first}" }) { (id, name) ->
            DoodleChip(
                selected = selectedTag == id,
                onClick = { onSelectTag(if (selectedTag == id) null else id) },
                seed = id.toInt(),
            ) { Text(name) }
        }

        if (tags.isNotEmpty()) {
            item(key = "manage") {
                AssistChip(
                    onClick = onManageTags,
                    label = { Text("管理") },
                )
            }
            item(key = "divider") {
                // 一条细细的分隔线，把"标签"和"心情"两组分开，避免以为点错了组
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                )
            }
        }

        items(Mood.entries, key = { "m${it.name}" }) { mood ->
            val selected = selectedMood == mood.emoji
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) mood.color.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    )
                    .then(
                        if (selected) Modifier.border(1.5.dp, mood.color, CircleShape) else Modifier,
                    )
                    .clickable { onToggleMood(mood.emoji) },
                contentAlignment = Alignment.Center,
            ) {
                MoodIcon(mood = mood, size = 22.dp)
            }
        }
    }
}

@Composable
private fun EmptyTimeline(filtering: Boolean) {
    // 按时间说一句人话，比"还没有记录"有温度
    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour in 5..10 -> "早安"
        hour in 11..13 -> "中午好"
        hour in 14..17 -> "下午好"
        hour in 18..22 -> "晚上好"
        else -> "夜深了"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (filtering) {
            Icon(
                imageVector = DoodleSearchIcon,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        } else {
            PaperSheet()
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = if (filtering) "这些条件下还没有记录" else "$greeting，今天还没写呢",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (filtering) "点「全部」回到所有记录。" else "哪怕一句话也算。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** 空状态的一张"空白纸"：几行手划的线，比一个 emoji 更说明这是本子。 */
@Composable
private fun PaperSheet() {
    val shape = RoundedCornerShape(16.dp)
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val dot = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(width = 128.dp, height = 164.dp)
            .graphicsLayer { rotationZ = -3f }
            .shadow(10.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                val startX = 22.dp.toPx()
                val step = 26.dp.toPx()
                val endY = size.height - 18.dp.toPx()
                var y = 54.dp.toPx()
                var index = 0
                while (y < endY) {
                    val endX = if (index == 3) size.width * 0.55f else size.width - 22.dp.toPx()
                    drawLine(ink, Offset(startX, y), Offset(endX, y), 1.4.dp.toPx())
                    y += step
                    index++
                }
                // 右上角一个小圆点，像页码
                drawCircle(dot, radius = 4.dp.toPx(), center = Offset(size.width - 26.dp.toPx(), 24.dp.toPx()))
            },
    )
}

/** 11 → 十一月：跟农历的"九月"呼应，比 "11月" 更像日记本。 */
private fun monthLabel(month: Int): String {
    val digits = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二")
    return (digits.getOrNull(month - 1) ?: "$month") + "月"
}
