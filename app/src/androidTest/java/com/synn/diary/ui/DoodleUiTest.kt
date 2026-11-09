package com.synn.diary.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synn.diary.ui.components.DoodleChip
import com.synn.diary.ui.components.DoodleDialog
import com.synn.diary.ui.components.DoodleSwitch
import com.synn.diary.ui.components.DoodleTextButton
import com.synn.diary.ui.theme.DiaryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 手绘控件的交互语义：开关能切换、chip 能点、对话框按钮能回调。 */
@RunWith(AndroidJUnit4::class)
class DoodleUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun 开关点击会翻转() {
        var checked = false
        rule.setContent {
            DiaryTheme {
                DoodleSwitch(checked = checked, onCheckedChange = { checked = it })
            }
        }
        rule.onNode(androidx.compose.ui.test.isToggleable()).performClick()
        rule.runOnIdle { assertTrue(checked) }
    }

    @Test
    fun chip_点击回调() {
        var clicks = 0
        rule.setContent {
            DiaryTheme {
                DoodleChip(selected = false, onClick = { clicks++ }, seed = 1) {
                    Text("全部")
                }
            }
        }
        rule.onNodeWithText("全部").performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun 对话框按钮各自回调() {
        var confirmed = 0
        var dismissed = 0
        rule.setContent {
            DiaryTheme {
                DoodleDialog(
                    onDismissRequest = { dismissed++ },
                    title = { Text("删除这条记录？") },
                    text = { Text("会先进回收站") },
                    confirmButton = {
                        DoodleTextButton(onClick = { confirmed++ }) { Text("删除") }
                    },
                    dismissButton = {
                        DoodleTextButton(onClick = { dismissed++ }) { Text("取消") }
                    },
                )
            }
        }
        rule.onNodeWithText("删除").performClick()
        rule.runOnIdle { assertEquals(1, confirmed) }
        rule.onNodeWithText("取消").performClick()
        rule.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test
    fun 禁用的按钮点了不回调() {
        var clicks = 0
        rule.setContent {
            DiaryTheme {
                DoodleTextButton(onClick = { clicks++ }, enabled = false) { Text("导出") }
            }
        }
        rule.onNodeWithText("导出").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(0, clicks) }
    }
}
