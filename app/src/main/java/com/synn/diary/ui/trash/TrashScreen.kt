package com.synn.diary.ui.trash

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.components.DoodleDialog
import com.synn.diary.ui.components.DoodleTextButton
import com.synn.diary.data.body.BodyMarkers
import com.synn.diary.data.db.EntryWithTags
import com.synn.diary.ui.components.DoodleTopBar
import com.synn.diary.ui.components.doodleBorder
import com.synn.diary.util.Dates

private val RETENTION_OPTIONS = listOf(
    7 to "1 周",
    30 to "1 个月",
    365 to "1 年",
    0 to "永久保留",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val retention by viewModel.retentionDays.collectAsStateWithLifecycle()

    var pendingPurge by remember { mutableStateOf<EntryWithTags?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DoodleTopBar(
                title = "回收站",
                onBack = onBack,
                centerTitle = true,
                actions = {
                    if (items.isNotEmpty()) {
                        DoodleTextButton(onClick = { confirmEmpty = true }) { Text("清空") }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // 保留策略
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "自动清理",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RETENTION_OPTIONS.forEach { (days, label) ->
                    FilterChip(
                        selected = retention == days,
                        onClick = { viewModel.setRetention(days) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = if (retention <= 0) {
                    "不会自动清理，只会攒在这里，需要手动彻底删除"
                } else {
                    "超过 ${RETENTION_OPTIONS.firstOrNull { it.first == retention }?.second ?: "1 个月"}的条目会被自动彻底删除"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "回收站是空的",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.entry.id }) { item ->
                        TrashRow(
                            item = item,
                            onRestore = { viewModel.restore(item.entry.id) },
                            onPurge = { pendingPurge = item },
                        )
                    }
                }
            }
        }
    }

    pendingPurge?.let { target ->
        DoodleDialog(
            onDismissRequest = { pendingPurge = null },
            title = { Text("彻底删除？") },
            text = { Text("删除后无法恢复，图片也会一并删掉。") },
            confirmButton = {
                DoodleTextButton(
                    onClick = {
                        viewModel.purge(target.entry.id)
                        pendingPurge = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                DoodleTextButton(onClick = { pendingPurge = null }) { Text("取消") }
            },
        )
    }

    if (confirmEmpty) {
        DoodleDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("清空回收站？") },
            text = { Text("${items.size} 条记录会被彻底删除，无法恢复。") },
            confirmButton = {
                DoodleTextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        confirmEmpty = false
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                DoodleTextButton(onClick = { confirmEmpty = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TrashRow(
    item: EntryWithTags,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    val entry = item.entry
    // 上锁的日记在回收站里也不能露内容——否则"删掉再来看回收站"就绕过了验证
    val preview = if (entry.locked) "" else BodyMarkers.strip(entry.body)
    val title = when {
        entry.locked -> "已上锁"
        else -> entry.title.ifBlank { preview.ifBlank { "（空）" } }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .doodleBorder(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                corner = 14.dp,
                width = 1.2.dp,
                seed = entry.id.toInt(),
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Dates.fullDate(entry.entryDate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "删除于 ${Dates.time(entry.deletedAt ?: 0L)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            DoodleTextButton(onClick = onRestore) { Text("还原") }
            DoodleTextButton(onClick = onPurge) {
                Text("彻底删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
