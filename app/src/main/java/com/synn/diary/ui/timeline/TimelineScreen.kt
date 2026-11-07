package com.synn.diary.ui.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.components.DiaryBottomBar
import com.synn.diary.ui.components.EntryCard
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

    // 贴顶的永远是最顶层那级：有多年时贴"年"，只有一年时贴"月"。
    // 否则单一年的用户会完全没有贴顶标题，滚动时不知道自己在哪个月。
    val showYearHeaders = rows.any { it is TimelineRow.Year }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日记") },
                actions = {
                    TextButton(onClick = onOpenSearch) { Text("搜索") }
                },
            )
        },
        bottomBar = { DiaryBottomBar(currentRoute = "timeline", onSelect = onSelectTab) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateEntry,
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
                onClear = viewModel::clearFilters,
            )

            if (rows.isEmpty()) {
                EmptyTimeline(filtering = selectedTag != null || moodFilter != null || favoritesOnly)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                        style = MaterialTheme.typography.titleMedium,
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
                                        title = "${row.month}月",
                                        count = row.count,
                                        expanded = row.expanded,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        sticky = true,
                                        onClick = { viewModel.toggleGroup(row.key) },
                                    )
                                }
                            } else {
                                item(key = row.key) {
                                    GroupHeader(
                                        title = "${row.month}月",
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
                                            } else if (!lockManager.hasPin()) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("请先在「设置」里开启应用锁并设置密码")
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
            .then(if (sticky) Modifier.background(MaterialTheme.colorScheme.background) else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (expanded) "▾" else "▸",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(18.dp),
        )
        Text(text = title, style = style, color = color)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count 篇",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
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
            .padding(start = 18.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (expanded) "▾" else "▸",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "本周",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = "$count 篇",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
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
    onClear: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedTag == null && selectedMood == null && !favoritesOnly,
                onClick = onClear,
                label = { Text("全部") },
            )
        }

        item(key = "favorites") {
            FilterChip(
                selected = favoritesOnly,
                onClick = onToggleFavorites,
                label = { Text("★ 收藏") },
            )
        }

        items(tags, key = { "t${it.first}" }) { (id, name) ->
            FilterChip(
                selected = selectedTag == id,
                onClick = { onSelectTag(if (selectedTag == id) null else id) },
                label = { Text(name) },
            )
        }

        if (tags.isNotEmpty()) {
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
                Text(text = mood.emoji, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun EmptyTimeline(filtering: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (filtering) "这个标签下还没有记录" else "还没有记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (filtering) "点「全部」回到所有记录。" else "点右下角「写一笔」，今天就有第一行了。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
