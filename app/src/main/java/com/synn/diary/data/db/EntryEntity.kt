package com.synn.diary.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条日记。
 *
 * - [entryDate] 与 [createdAt] 刻意分开：用 epochDay 存"日记归属日"，晚上 11 点补写昨天
 *   也能归到正确的那天，日历和"那年今日"都依赖它。
 * - [uuid] 现在没用，是给以后的导出合并 / 同步留的去重键。
 * - [deletedAt] 软删除，先进回收站，避免误删半小时写的东西。
 */
@Entity(
    tableName = "entry",
    indices = [
        Index(value = ["entryDate"]),
        Index(value = ["uuid"], unique = true),
    ],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String,
    val entryDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String,
    val body: String,
    val mood: String? = null,
    val deletedAt: Long? = null,
    // 写这条时的环境，创建时自动抓一次，之后不再变（改日记不会重抓）
    val weatherText: String? = null,
    val locationText: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // 纸张底色，存 EntryBackground.id；null = 默认
    val background: String? = null,
    // 正文字体，存 ContentFont.id（system/serif/mono/wenkai/file:<文件名>）；null = 系统默认
    val font: String? = null,
    val favorite: Boolean = false,
    val locked: Boolean = false,
)
