package com.synn.diary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.synn.diary.R

/**
 * 正文字体：霞鹜文楷（LXGW WenKai）。
 *
 * 只给正文、标题这类"读的内容"用；按钮、标签仍走系统字体，
 * 免得界面元素被手写体带得太软。
 */
object DiaryFonts {
    val body: FontFamily? = FontFamily(
        Font(R.font.lxgw_wenkai, FontWeight.Normal),
    )
}

/** 把正文类样式换成文楷，并把行高按中文字体调松一点。 */
fun Typography.withBodyFont(family: FontFamily?): Typography {
    if (family == null) return this
    fun TextStyle.useFont(): TextStyle = copy(fontFamily = family)

    return copy(
        displayLarge = displayLarge.useFont(),
        displayMedium = displayMedium.useFont(),
        displaySmall = displaySmall.useFont(),
        headlineLarge = headlineLarge.useFont(),
        headlineMedium = headlineMedium.useFont(),
        headlineSmall = headlineSmall.useFont(),
        titleLarge = titleLarge.useFont(),
        titleMedium = titleMedium.useFont(),
        titleSmall = titleSmall.useFont(),
        bodyLarge = bodyLarge.useFont(),
        bodyMedium = bodyMedium.useFont(),
        bodySmall = bodySmall.useFont(),
    )
}
