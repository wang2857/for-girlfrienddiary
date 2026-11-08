package com.synn.diary.data.backup

import android.content.Context
import android.database.Cursor
import com.synn.diary.data.db.DbEncryption
import android.net.Uri
import androidx.room.withTransaction
import com.synn.diary.data.body.BodyMarkers
import com.synn.diary.data.db.AppDatabase
import com.synn.diary.data.db.EntryEntity
import com.synn.diary.data.db.EntryTagEntity
import com.synn.diary.data.db.TagEntity
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
    val backgroundCount: Int = 0,
    /** 包里有没有用恢复码封的口令：没有就只能在本机恢复。 */
    val portable: Boolean = false,
)

/** 备份包的元信息，导入前用来决定要不要问恢复码。 */
data class BackupInspection(
    val schemaVersion: Int,
    val needsRecoveryCode: Boolean,
)

/** 合并导入的结果。 */
data class MergeInfo(
    val added: Int,
    val skipped: Int,
    val images: Int,
)

/**
 * 导出 / 导入 / 合并备份。
 *
 * 包里是 `diary.db` + `images/`（正文图片）+ `backgrounds/`（自定义纸张）+ 一份 `manifest.json`。
 * 直接搬文件而不是逐条导出，是为了保住"全部字段 + 图片 + 将来新加的列"，
 * 任何自定义文本格式都会在加字段时漏掉东西。
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val imageStore: ImageStore,
    private val dbPassphrase: String,
    /** 恢复码（明文）。有的话，导出时用它把数据库口令再封一份放进包里，换机才解得开。 */
    private val recoveryCodeProvider: () -> String?,
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
        val backgrounds = imageStore.listBackgrounds()
        val entryCount = database.openHelper.readableDatabase
            .query("SELECT count(*) FROM entry").use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }

        // 跨设备备份：把数据库口令用恢复码包一份放进包里；没有恢复码就只做本机备份
        val keyBlob = recoveryCodeProvider()?.let { code -> BackupKey.wrap(dbPassphrase, code) }

        val info = BackupInfo(
            exportedAt = System.currentTimeMillis(),
            schemaVersion = database.openHelper.readableDatabase.version,
            entryCount = entryCount,
            imageCount = images.size,
            backgroundCount = backgrounds.size,
            portable = keyBlob != null,
        )

        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(
                JSONObject()
                    .put("exportedAt", info.exportedAt)
                    .put("schemaVersion", info.schemaVersion)
                    .put("entryCount", info.entryCount)
                    .put("imageCount", info.imageCount)
                    .put("backgroundCount", info.backgroundCount)
                    .put("encrypted", true)
                    .put("keyWrap", if (keyBlob != null) "recovery" else "device")
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

            backgrounds.keys.forEach { name ->
                zip.putNextEntry(ZipEntry("$BACKGROUND_DIR/$name"))
                imageStore.backgroundFile(name).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }

            if (keyBlob != null) {
                zip.putNextEntry(ZipEntry(KEY_FILE))
                zip.write(keyBlob)
                zip.closeEntry()
            }
        }
        return info
    }

    /** 只看一眼备份包：版本、以及是不是需要恢复码。 */
    suspend fun inspect(uri: Uri): Result<BackupInspection> = withContext(Dispatchers.IO) {
        runCatching {
            val staging = unzipToStaging(uri)
            val manifest = File(staging, MANIFEST)
            require(manifest.exists()) { "不是有效的备份包（缺少清单）" }
            val json = JSONObject(manifest.readText())
            BackupInspection(
                schemaVersion = json.optInt("schemaVersion", 0),
                needsRecoveryCode = File(staging, KEY_FILE).exists(),
            )
        }
    }

    /**
     * 导入：解压到临时目录 → 校验 → （跨设备备份先换成本机口令）→ 关库 → 覆盖文件。
     * 调用方负责重启应用。
     */
    suspend fun import(uri: Uri, recoveryCode: String? = null): Result<BackupInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val staging = unzipToStaging(uri)
            val db = File(staging, DB_NAME)
            require(File(staging, MANIFEST).exists()) { "不是有效的备份包（缺少清单）" }
            require(db.exists()) { "不是有效的备份包（缺少数据库）" }
            require(db.length() > 0) { "备份包里的数据库是空的" }
            require(!DbEncryption.isPlaintext(db)) {
                "这是旧版本导出的明文备份，当前版本只接受加密备份"
            }

            val json = JSONObject(File(staging, MANIFEST).readText())
            val schemaVersion = json.optInt("schemaVersion", 0)
            val currentVersion = database.openHelper.readableDatabase.version
            require(schemaVersion <= currentVersion) {
                "备份来自更新的版本（库 v$schemaVersion，本机 v$currentVersion），请先升级应用"
            }
            // v8 起不再维护升级链：旧库导进来也会被"删表重建"，等于把数据吞了，
            // 所以直接拒绝，而不是让它悄悄变成空库
            require(schemaVersion >= MIN_BACKUP_SCHEMA) {
                "备份来自旧版本（库 v$schemaVersion），当前数据库已重排到 v$currentVersion，" +
                    "只接受 v$MIN_BACKUP_SCHEMA 之后导出的备份"
            }

            val info = BackupInfo(
                exportedAt = json.optLong("exportedAt", 0L),
                schemaVersion = schemaVersion,
                entryCount = json.optInt("entryCount", 0),
                imageCount = json.optInt("imageCount", 0),
                backgroundCount = json.optInt("backgroundCount", 0),
            )

            // 跨设备备份：包里带着用恢复码封起来的口令，先解开、把库改成这台手机的口令
            val keyFile = File(staging, KEY_FILE)
            if (keyFile.exists()) {
                val code = recoveryCode?.trim().orEmpty()
                require(code.isNotEmpty()) { "这个备份来自别的设备，需要恢复码才能解开" }
                val backupPassphrase = BackupKey.unwrap(keyFile.readBytes(), code)
                    ?: error("恢复码不对，解不开这个备份")
                rekeyBackupDatabase(db, backupPassphrase)
            }

            // 关库再覆盖，否则 Room 的打开连接会写回旧页
            database.close()
            dbFile.parentFile?.mkdirs()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            db.copyTo(dbFile, overwrite = true)

            replaceDir(File(staging, IMAGE_DIR), File(context.filesDir, IMAGE_DIR))
            replaceDir(File(staging, BACKGROUND_DIR), File(context.filesDir, BACKGROUND_DIR))

            staging.deleteRecursively()
            info
        }
    }

    /**
     * 合并导入：只把"备份里有、本机没有"的日记加进来，不动现有内容。
     *
     * 去重键就是一直闲置的 `uuid`——每篇日记创建时都会打一个，
     * 所以同一份备份导几次也不会重复。写库走正常 Room 通道，不需要重启应用。
     */
    suspend fun merge(uri: Uri): Result<MergeInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val staging = unzipToStaging(uri)
            val db = File(staging, DB_NAME)
            require(db.exists() && db.length() > 0) { "不是有效的备份包（缺少数据库）" }
            require(!DbEncryption.isPlaintext(db)) {
                "这是旧版本导出的明文备份，当前版本只接受加密备份"
            }

            // 备份包里的库可能是加密的（同一台设备导出的），也可能是老版本的明文
            val source = openSource(db)
            try {
                val rows = readSourceEntries(source)
                val tagNames = readSourceTags(source)

                var added = 0
                var skipped = 0
                var images = 0

                database.withTransaction {
                    val entryDao = database.entryDao()
                    val tagDao = database.tagDao()
                    val knownUuids = entryDao.allUuids().toHashSet()

                    rows.forEach { row ->
                        if (row.trashed || row.uuid.isBlank() || !knownUuids.add(row.uuid)) {
                            skipped++
                            return@forEach
                        }

                        val newId = entryDao.insert(row.toEntity())
                        added++

                        tagNames[row.sourceId].orEmpty().forEach { rawName ->
                            val name = rawName.trim()
                            if (name.isEmpty()) return@forEach
                            val tagId = tagDao.findByName(name)?.id ?: tagDao.insert(
                                TagEntity(name = name, createdAt = System.currentTimeMillis()),
                            )
                            if (tagId > 0) entryDao.attachTag(EntryTagEntity(newId, tagId))
                        }

                        BodyMarkers.allImages(row.body).forEach { name ->
                            if (copyIfMissing(File(staging, IMAGE_DIR), name, imageStore.file(name))) {
                                images++
                            }
                        }
                        row.background?.takeIf { it.startsWith(PREFIX_CUSTOM) }?.let { background ->
                            val name = background.removePrefix(PREFIX_CUSTOM)
                            copyIfMissing(
                                File(staging, BACKGROUND_DIR),
                                name,
                                imageStore.backgroundFile(name),
                            )
                        }
                    }
                }

                staging.deleteRecursively()
                MergeInfo(added = added, skipped = skipped, images = images)
            } finally {
                source.close()
            }
        }
    }

    /** 把备份库的加密口令换成本机的，之后才能直接放回 databases/。 */
    private fun rekeyBackupDatabase(file: File, backupPassphrase: String) {
        val db = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            file.path,
            backupPassphrase,
            null,
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
            null,
        )
        try {
            db.rawExecSQL("PRAGMA rekey = '$dbPassphrase'")
        } finally {
            db.close()
        }
    }

    // ---- 解包与读源库 ----

    /** 解压到 cache/restore（目录穿越只放行白名单内的路径）。 */
    private fun unzipToStaging(uri: Uri): File {
        val staging = File(context.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val target = when {
                        name == MANIFEST -> File(staging, MANIFEST)
                        name == DB_NAME -> File(staging, DB_NAME)
                        name == KEY_FILE -> File(staging, KEY_FILE)
                        name.startsWith("$IMAGE_DIR/") && !name.contains("..") -> File(staging, name)
                        name.startsWith("$BACKGROUND_DIR/") && !name.contains("..") -> File(staging, name)
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
        return staging
    }

    private data class SourceEntry(
        val sourceId: Long,
        val uuid: String,
        val entryDate: Long,
        val createdAt: Long,
        val updatedAt: Long,
        val title: String,
        val body: String,
        val mood: String?,
        val trashed: Boolean,
        val weatherText: String?,
        val locationText: String?,
        val latitude: Double?,
        val longitude: Double?,
        val background: String?,
        val favorite: Boolean,
        val locked: Boolean,
    ) {
        fun toEntity() = EntryEntity(
            uuid = uuid,
            entryDate = entryDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            title = title,
            body = body,
            mood = mood,
            deletedAt = null,
            weatherText = weatherText,
            locationText = locationText,
            latitude = latitude,
            longitude = longitude,
            background = background,
            favorite = favorite,
            locked = locked,
        )
    }

    /** 备份里的库一律是加密库（0.33.0 起）；明文备份直接拒绝。 */
    private fun openSource(db: File): Source {
        require(!DbEncryption.isPlaintext(db)) {
            "这是旧版本导出的明文备份，当前版本只接受加密备份"
        }
        val encrypted = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            db.path,
            dbPassphrase,
            null,
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
            null,
        )
        return object : Source {
            override fun query(sql: String): Cursor = encrypted.rawQuery(sql, null)
            override fun close() = encrypted.close()
        }
    }

    private interface Source {
        fun query(sql: String): Cursor
        fun close()
    }

    private fun readSourceEntries(source: Source): List<SourceEntry> {
        val rows = mutableListOf<SourceEntry>()
        source.query("SELECT * FROM entry").use { c ->
            while (c.moveToNext()) {
                rows += SourceEntry(
                    sourceId = c.longOr("id") ?: -1,
                    uuid = c.stringOr("uuid").orEmpty(),
                    entryDate = c.longOr("entryDate") ?: 0L,
                    createdAt = c.longOr("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = c.longOr("updatedAt") ?: System.currentTimeMillis(),
                    title = c.stringOr("title").orEmpty(),
                    body = c.stringOr("body").orEmpty(),
                    mood = c.stringOr("mood"),
                    trashed = c.longOr("deletedAt") != null,
                    weatherText = c.stringOr("weatherText"),
                    locationText = c.stringOr("locationText"),
                    latitude = c.doubleOr("latitude"),
                    longitude = c.doubleOr("longitude"),
                    background = c.stringOr("background"),
                    favorite = c.intOr("favorite") == 1,
                    locked = c.intOr("locked") == 1,
                )
            }
        }
        return rows
    }

    private fun readSourceTags(source: Source): Map<Long, List<String>> {
        val map = mutableMapOf<Long, MutableList<String>>()
        source.query(
            "SELECT et.entryId AS entryId, t.name AS name FROM entry_tag et " +
                "JOIN tag t ON t.id = et.tagId",
        ).use { c ->
            while (c.moveToNext()) {
                val entryId = c.longOr("entryId") ?: continue
                val name = c.stringOr("name") ?: continue
                map.getOrPut(entryId) { mutableListOf() }.add(name)
            }
        }
        return map
    }

    // ---- 小工具 ----

    /** 目标已存在就跳过：图片文件名是随机 UUID，同名即同图。 */
    private fun copyIfMissing(sourceDir: File, name: String, target: File): Boolean {
        if (!SAFE_NAME.matches(name) || target.exists()) return false
        val source = File(sourceDir, name)
        if (!source.isFile) return false
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        return true
    }

    private fun replaceDir(source: File?, target: File) {
        target.mkdirs()
        target.listFiles()?.forEach { it.delete() }
        source?.listFiles()?.forEach { it.copyTo(File(target, it.name), overwrite = true) }
    }

    private fun Cursor.stringOr(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }

    private fun Cursor.longOr(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getLong(index)
    }

    private fun Cursor.intOr(column: String): Int? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getInt(index)
    }

    private fun Cursor.doubleOr(column: String): Double? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getDouble(index)
    }

    private companion object {
        /** 与 AppDatabase.version 对齐：小于这个版本的备份一律不导入。 */
        const val MIN_BACKUP_SCHEMA = 8
        const val DB_NAME = "diary.db"
        const val MANIFEST = "manifest.json"
        const val KEY_FILE = "key.bin"
        const val IMAGE_DIR = "images"
        const val BACKGROUND_DIR = "backgrounds"
        const val PREFIX_CUSTOM = "img:"

        /** 只接受这种文件名，顺带挡掉 `../` 这类路径。 */
        val SAFE_NAME = Regex("^[A-Za-z0-9._-]+$")
    }
}
