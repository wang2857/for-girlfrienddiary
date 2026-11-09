package com.synn.diary.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 开发用：往库里塞几条中文样例，方便看排版。
 *
 * 默认不跑（`connectedDebugAndroidTest` 会跳过）；要跑得显式传参：
 * ```
 * adb shell am instrument -w -e class com.synn.diary.data.SampleDataTest -e seed 1 \
 *   com.synn.diary.debug.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 */
@RunWith(AndroidJUnit4::class)
class SampleDataTest {

    @Test
    fun seed() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seed") == "1")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)
        val repo = container.entryRepository

        suspend fun put(title: String, body: String, mood: String?) {
            val id = repo.create()
            repo.save(id, title, body, mood, null, null)
        }

        put(
            title = "山里的雨",
            body = "下午三点开始下雨，屋檐下一排水线。\n\n" +
                "把窗开着听了一会儿，风把纸页吹得哗哗响。想起小时候在姥姥家，也是这样趴在窗台上看雨。",
            mood = "😌",
        )
        put(
            title = "读完《活着》",
            body = "合上书的时候天已经黑了。福贵最后牵着那头老牛走远，余华写得真狠，" +
                "不给你一个可以哭出来的地方，就那么平平地讲完了。\n\n" +
                "明天想把书里划的几句抄进本子里。",
            mood = "🥲",
        )
        put(
            title = "关于夏天的一些碎片",
            body = "西瓜、蒲扇、傍晚的风、楼下小孩的叫声。\n" +
                "冰箱里冻着的绿豆汤，喝到最后一口都是沙的。",
            mood = "😊",
        )
        container.close()
    }
}
