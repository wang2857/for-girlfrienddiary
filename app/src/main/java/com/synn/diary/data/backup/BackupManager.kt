package com.synn.diary.data.backup

import android.content.Context
import android.net.Uri
import com.synn.diary.data.db.AppDatabase
import com.synn.diary.data.media.ImageStore
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 备份包里的清单，导入时用来校验。 */
data class BackupInfo(
    val exportedAt: Long,
    val schemaVersion: Int,
    val entryCount: Int,
    val imageCount: Int,
)

/**
 * 导出 / 导入备份。
 *
 * 包里就是 `diary.db` 文件本身 + `images/` 目录 + 一份 `manifest.json`。
 * 之所以直接搬文件而不是逐条导出，是因为要保住"全部字段 + 图片 + 将来的新列"，
 * 任何自定义文本格式都会在加字段时漏掉东西。
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val imageStore: ImageStore,
) {

    private val dbFile: File get() = context.getDatabasePath(DB_NAME)

    suspend fun export(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out -> writeZip(out) }
                ?: error("无法写入该位置")
        }
    }

    /**
     * 自动备份用：直接写到应用私有目录里的文件。
     * 和手动导出走同一段打包逻辑，免得两份格式慢慢长歪。
     */
    suspend fun exportToFile(file: File): BackupInfo = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".part")
        // 先写临时名再改名：中途没电也不会留下一个看起来完好的半个备份
        val info = temp.outputStream().buffered().use { out -> writeZip(out) }
        if (!temp.renameTo(file)) {
            temp.delete()
            error("备份文件写入失败")
        }
        info
    }

    private fun writeZip(out: java.io.OutputStream): BackupInfo {
        // 先把 WAL 合并回主库，否则导出的 db 可能缺最近的改动
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .use { it.moveToFirst() }

        val images = imageStore.listFiles()
        val entryCount = database.openHelper.readableDatabase
            .query("SELECT count(*) FROM entry").use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }

        val info = BackupInfo(
            exportedAt = System.currentTimeMillis(),
            schemaVersion = database.openHelper.readableDatabase.version,
            entryCount = entryCount,
            imageCount = images.size,
        )

        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(
                JSONObject()
                    .put("exportedAt", info.exportedAt)
                    .put("schemaVersion", info.schemaVersion)
                    .put("entryCount", info.entryCount)
                    .put("imageCount", info.imageCount)
                    .toString()
                    .toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DB_NAME))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            images.keys.forEach { name ->
                zip.putNextEntry(ZipEntry("$IMAGE_DIR/$name"))
                imageStore.file(name).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return info
    }

    /** 导入：解压到临时目录 → 校验 → 关库 → 覆盖文件。调用方负责重启应用。 */
    suspend fun import(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val staging = File(context.cacheDir, "restore").apply {
                deleteRecursively()
                mkdirs()
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // 防目录穿越：只接受白名单内的路径
                        val target = when {
                            name == MANIFEST -> File(staging, MANIFEST)
                            name == DB_NAME -> File(staging, DB_NAME)
                            name.startsWith("$IMAGE_DIR/") && !name.contains("..") ->
                                File(staging, name)
                            else -> null
                        }
                        if (target != null) {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("无法读取该文件")

            val manifestFile = File(staging, MANIFEST)
            val db = File(staging, DB_NAME)
            require(manifestFile.exists()) { "不是有效的备份包（缺少清单）" }
            require(db.exists()) { "不是有效的备份包（缺少数据库）" }
            require(db.length() > 0) { "备份包里的数据库是空的" }

            val json = JSONObject(manifestFile.readText())
            val schemaVersion = json.optInt("schemaVersion", 0)
            val currentVersion = database.openHelper.readableDatabase.version
            require(schemaVersion in 1..currentVersion) {
                "备份来自更新的版本（库 v$schemaVersion，本机 v$currentVersion），请先升级应用"
            }

            val info = BackupInfo(
                exportedAt = json.optLong("exportedAt", 0L),
                schemaVersion = schemaVersion,
                entryCount = json.optInt("entryCount", 0),
                imageCount = json.optInt("imageCount", 0),
            )

            // 关库再覆盖，否则 Room 的打开连接会写回旧页
            database.close()
            dbFile.parentFile?.mkdirs()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            db.copyTo(dbFile, overwrite = true)

            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            imagesDir.listFiles()?.forEach { it.delete() }
            File(staging, IMAGE_DIR).listFiles()?.forEach { src ->
                src.copyTo(File(imagesDir, src.name), overwrite = true)
            }

            staging.deleteRecursively()
            info
        }
    }

    private companion object {
        const val DB_NAME = "diary.db"
        const val MANIFEST = "manifest.json"
        const val IMAGE_DIR = "images"
    }
}
