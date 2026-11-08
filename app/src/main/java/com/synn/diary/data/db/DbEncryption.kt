package com.synn.diary.data.db

import android.util.Log
import java.io.File

/**
 * 数据库加密的"守门员"。
 *
 * 当前策略很干脆：**只认加密库**。0.33.0 之前留下的明文库不再迁移，
 * 发现就删掉，让 Room 重新建一个加密库（老数据按产品决定放弃）。
 */
object DbEncryption {

    private const val TAG = "DbEncryption"
    private const val PLAINTEXT_MAGIC = "SQLite format 3"

    fun isPlaintext(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        file.inputStream().use { input ->
            if (input.read(header) < 16) return false
        }
        return String(header, Charsets.US_ASCII).startsWith(PLAINTEXT_MAGIC)
    }

    /**
     * 见到明文库就丢掉（连同遗留的 -wal / -shm），返回是否真的删了。
     *
     * 不做迁移的原因：明文库里可能残留旧版本的字段/脏页，迁移代码本身也是风险源；
     * 既然已经从产品上决定不兼容老数据，直接重建更干净。
     */
    fun discardPlaintext(file: File): Boolean {
        if (!isPlaintext(file)) return false
        Log.w(TAG, "发现旧的明文库，丢弃并重新创建加密库")
        val deleted = file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
        Log.w(TAG, "明文库已删除：$deleted")
        return true
    }
}
