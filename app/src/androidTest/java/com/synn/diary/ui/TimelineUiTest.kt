package com.synn.diary.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synn.diary.data.AppContainer
import com.synn.diary.ui.theme.DiaryTheme
import com.synn.diary.ui.timeline.TimelineScreen
import com.synn.diary.ui.timeline.TimelineViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimelineUiTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var container: AppContainer

    @Before
    fun setUp() {
        container = AppContainer(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        container.close()
    }

    @Test
    fun 列表显示日记和手绘铅笔() = runBlocking {
        val repo = container.entryRepository
        val id = repo.create()
        repo.save(id, "UI 测试标题", "第一行\n第二行\n第三行", "😄", null, null)

        val vm = TimelineViewModel(repo)
        rule.setContent {
            DiaryTheme {
                TimelineScreen(
                    viewModel = vm,
                    lockManager = container.lockManager,
                    onOpenEntry = {},
                    onCreateEntry = {},
                    onOpenSearch = {},
                    onSelectTab = {},
                )
            }
        }

        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("UI 测试标题").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("写一笔").assertExists()
        // 心情筛选图标带无障碍描述
        rule.onNodeWithContentDescription("开心").assertExists()

        repo.purge(id)
    }
}
