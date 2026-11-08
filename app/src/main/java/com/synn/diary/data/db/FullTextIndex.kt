package com.synn.diary.data.db

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 全文检索索引（FTS5 + trigram）。
 *
 * 为什么自己建表：Room 只认 `@Fts4`，没有 FTS5 注解。所以用 `CREATE VIRTUAL TABLE`
 * 自己建，并用触发器跟着 `entry` 走；查询在 DAO 里用 `@RawQuery` 发（Room 的
 * 编译期校验不认识虚拟表）。
 *
 * trigram 分词器对中文很友好：任意 3 个字符以上的子串都能命中，不需要分词词典。
 * 少于 3 个字符的查询（中文两个字很常见）由仓库层自动退回 LIKE。
 */
object FullTextIndex {

    private const val TAG = "FullTextIndex"

    /** 索引装好了没有；装不上时搜索走 LIKE 兜底。 */
    @Volatile
    var enabled: Boolean = false
        private set

    fun install(db: SupportSQLiteDatabase) {
        // 不能"装过一次就跳过"：每个库都要有自己的 entry_fts，测试里会开多个库
        runCatching {
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS entry_fts " +
                    "USING fts5(title, body, tokenize='trigram')",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS entry_fts_ai AFTER INSERT ON entry BEGIN " +
                    "INSERT INTO entry_fts(rowid, title, body) VALUES (new.id, new.title, new.body); END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS entry_fts_ad AFTER DELETE ON entry BEGIN " +
                    "DELETE FROM entry_fts WHERE rowid = old.id; END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS entry_fts_au AFTER UPDATE OF title, body ON entry BEGIN " +
                    "DELETE FROM entry_fts WHERE rowid = old.id; " +
                    "INSERT INTO entry_fts(rowid, title, body) VALUES (new.id, new.title, new.body); END",
            )

            // 条数对不上说明索引落后了（比如从更早的备份恢复），整表重建
            val difference = db.query(
                "SELECT (SELECT count(*) FROM entry) - (SELECT count(*) FROM entry_fts)",
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
            if (difference != 0) {
                Log.w(TAG, "索引与正文条数差 $difference，重建索引")
                db.execSQL("DELETE FROM entry_fts")
                db.execSQL("INSERT INTO entry_fts(rowid, title, body) SELECT id, title, body FROM entry")
            }
            enabled = true
            Log.i(TAG, "FTS5 索引就绪（trigram）")
        }.onFailure {
            enabled = false
            Log.e(TAG, "FTS5 不可用，搜索退回 LIKE", it)
        }
    }

    /** 把用户输入包成一个 FTS5 短语，避免 AND/OR/NEAR 之类的语法被当成操作符。 */
    fun matchQuery(keyword: String): String = "\"" + keyword.replace("\"", "\"\"") + "\""
}
