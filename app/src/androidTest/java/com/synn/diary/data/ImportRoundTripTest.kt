package com.synn.diary.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 整包覆盖导入（不是合并）的端到端测试。
 *
 * 这条会**真的换掉本机数据库**，等于重置应用数据——这是 import 的定义决定的，
 * 所以测试放在独立的类里，用自己在测试里创建的 AppContainer，跑完把测试数据清掉。
 * 其它测试各自建 container，互不影响。
 */
@RunWith(AndroidJUnit4::class)
class ImportRoundTripTest {

    private lateinit var context: Context
    private var container: AppContainer? = null
    private lateinit var zip: File
    private val imageName = "import-${UUID.randomUUID()}.jpg"
    private val fontName = "import-${UUID.randomUUID()}.ttf"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        zip = File(context.cacheDir, "import-${UUID.randomUUID()}.zip")
        zip.delete()
    }

    @After
    fun tearDown() {
        runBlocking {
            val c = open()
            c.entryRepository.timeline(limit = 500).first()
                .filter { it.entry.title == TITLE }
                .forEach { c.entryRepository.purge(it.entry.id) }
            c.imageStore.file(imageName).delete()
            c.fontStore.file(fontName).delete()
        }
        closeCurrent()
        zip.delete()
    }

    @Test
    fun `覆盖导入恢复快照，导出之后写的条目会消失`() = runBlocking {
        val source = open()
        val repo = source.entryRepository

        // 快照内容：一条带图 + 自定义字体的日记
        source.imageStore.file(imageName).writeBytes(ByteArray(128) { 1 })
        source.fontStore.file(fontName).writeBytes(ByteArray(128) { 2 })
        val id = repo.create()
        repo.save(
            id = id,
            title = TITLE,
            body = "快照正文 [[img:$imageName|60|c]]",
            mood = null,
            background = null,
            font = "file:$fontName",
        )

        val exported = source.backupManager.exportToFile(zip)
        assertTrue("备份没生成", zip.exists() && zip.length() > 0)
        assertTrue("快照里至少要有一条", exported.entryCount >= 1)
        closeCurrent()

        // 导出之后再写一条：覆盖导入之后它必须消失
        val later = open()
        val laterId = later.entryRepository.create()
        later.entryRepository.save(laterId, AFTER_TITLE, "导出之后才写的", null, null, null)
        closeCurrent()

        // 执行覆盖导入（会关库、换文件、替换图片/字体目录）
        val importer = open()
        val imported = importer.backupManager.import(Uri.fromFile(zip), recoveryCode = null).getOrThrow()
        closeCurrent()
        assertTrue("导入结果条数对不上", imported.entryCount >= 1)

        // 导入之后库是新的，重新开 container 读
        val restored = open()
        val entries = restored.entryRepository.timeline(limit = 500).first()
        assertNotNull("覆盖导入后没找到快照里的日记", entries.firstOrNull { it.entry.title == TITLE })
        assertNull("导出后写的条目不该出现在快照里", entries.firstOrNull { it.entry.title == AFTER_TITLE })
        assertTrue("快照里的图片没恢复", restored.imageStore.file(imageName).exists())
        assertTrue("快照里的字体没恢复", restored.fontStore.file(fontName).exists())
    }

    private fun open(): AppContainer =
        AppContainer(context).also { container = it }

    private fun closeCurrent() {
        container?.close()
        container = null
    }

    private companion object {
        const val TITLE = "覆盖恢复测试"
        const val AFTER_TITLE = "导出之后才写的"
    }
}
