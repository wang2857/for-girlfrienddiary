package com.synn.diary.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synn.diary.data.AppContainer
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端：真的建一条带图和字体的日记 → 导出 zip → 彻底删掉 → 合并导入 → 全部回来。
 *
 * 跑在真机/模拟器上（SQLCipher + Keystore 都是真的），用应用自己的 container，
 * 测试数据用随机文件名，结束后清理。
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private lateinit var app: android.content.Context
    private lateinit var container: AppContainer
    private lateinit var zip: File
    private val imageName = "roundtrip-${UUID.randomUUID()}.jpg"
    private val fontName = "roundtrip-${UUID.randomUUID()}.ttf"

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        container = AppContainer(app)
        zip = File(app.cacheDir, "roundtrip-${UUID.randomUUID()}.zip")
        zip.delete()
    }

    @After
    fun tearDown() {
        runBlocking {
            val repo = container.entryRepository
            repo.timeline(limit = 500).first()
                .filter { it.entry.title == TITLE }
                .forEach { repo.purge(it.entry.id) }
            container.imageStore.file(imageName).delete()
            container.fontStore.file(fontName).delete()
            zip.delete()
        }
        container.close()
    }

    @Test
    fun `导出后删除再合并，正文图片字体都回来`() = runBlocking {
        val repo = container.entryRepository

        // 造素材：一张图、一个字体文件（内容不重要，测的是搬运）
        container.imageStore.file(imageName).writeBytes(ByteArray(128) { 1 })
        container.fontStore.file(fontName).writeBytes(ByteArray(128) { 2 })

        val id = repo.create()
        repo.save(
            id = id,
            title = TITLE,
            body = "正文里的图 [[img:$imageName|60|c]]",
            mood = null,
            background = null,
            font = "file:$fontName",
        )
        assertTrue(container.imageStore.file(imageName).exists())

        // 导出
        container.backupManager.exportToFile(zip)
        assertTrue("备份文件没生成", zip.exists() && zip.length() > 0)
        ZipFile(zip).use { zf ->
            assertNotNull("备份里没有数据库", zf.getEntry("diary.db"))
            assertNotNull("备份里没有正文图片", zf.getEntry("images/$imageName"))
            assertNotNull("备份里没有字体", zf.getEntry("fonts/$fontName"))
        }

        // 彻底删除：图片会跟着被删
        repo.purge(id)
        assertTrue("purge 应该把图删掉", !container.imageStore.file(imageName).exists())

        // 合并回来
        val mergeInfo = container.backupManager.merge(Uri.fromFile(zip)).getOrThrow()
        assertTrue("应该至少合并回 1 条，实际 ${mergeInfo.added}", mergeInfo.added >= 1)

        val restored = repo.timeline(limit = 500).first().firstOrNull { it.entry.title == TITLE }
        assertNotNull("合并后没找到这篇日记", restored)
        assertTrue("正文图片没恢复", container.imageStore.file(imageName).exists())
        assertTrue("字体文件没恢复", container.fontStore.file(fontName).exists())
    }

    private companion object {
        const val TITLE = "备份往返测试"
    }
}
