package com.synn.diary.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synn.diary.data.AppContainer
import com.synn.diary.ui.calendar.CalendarScreen
import com.synn.diary.ui.calendar.CalendarViewModel
import com.synn.diary.ui.theme.DiaryTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarUiTest {

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
    fun 点标题弹出年月选择器并可以跳转() {
        val vm = CalendarViewModel(container.entryRepository)
        rule.setContent {
            DiaryTheme {
                CalendarScreen(viewModel = vm, onOpenEntry = {}, onSelectTab = {})
            }
        }

        val now = YearMonth.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
        val title = now.format(formatter)
        rule.onNodeWithText(title).performClick()
        rule.onNodeWithText("跳到某个月").assertExists()

        rule.onNodeWithText("1月").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("${now.year}年1月").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 今天按钮存在() {
        val vm = CalendarViewModel(container.entryRepository)
        rule.setContent {
            DiaryTheme {
                CalendarScreen(viewModel = vm, onOpenEntry = {}, onSelectTab = {})
            }
        }
        rule.onNodeWithText("今天").assertExists()
    }
}
