package com.synn.diary.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 「保存」用的软盘图标。
 *
 * Material 的 `save` 在 material-icons-extended 里，为它多背 ~2 MB 不划算；
 * 这里的路径是从 Material 图标库里抄的一条 24×24 路径，直接用 PathParser 解析。
 * 颜色随便填（`Icon` 会用 tint 覆盖），只借形状。
 */
val SaveIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Save",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4z" +
                    "m-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z" +
                    "m3-10H5V5h10v4z",
            ),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

/** 工具栏用的几个手绘 24×24 图标，省掉 material-icons-extended 的几 MB。 */

val ImageIcon: ImageVector by lazy {
    vector(
        "Image",
        "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2z" +
            "M8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
    )
}

val TextFieldsIcon: ImageVector by lazy {
    vector("TextFields", "M2.5 4v3h5v12h3V7h5V4h-13zm19 5h-9v3h3v7h3v-7h3V9z")
}

val PaletteIcon: ImageVector by lazy {
    vector(
        "Palette",
        "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01" +
            "-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8z" +
            "m-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12z" +
            "m3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8z" +
            "m5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8z" +
            "m3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z",
    )
}

/** 横线：三条手画的线，最后一条短一点，像随手划的。 */
val RuledLinesIcon: ImageVector by lazy {
    vector("RuledLines", "M4 6.4h16v1.7H4zM4 11.15h16v1.7H4zM4 15.9h10.5v1.7H4z")
}

private fun vector(name: String, path: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
}.build()

/** 小锁：列表/锁屏/引导共用。 */
val LockIcon: ImageVector by lazy {
    vector(
        "Lock",
        "M12 17a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm6-7h-1V7a5 5 0 0 0-10 0v3H6" +
            "c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2z" +
            "M9 7a3 3 0 0 1 6 0v3H9V7z",
    )
}
