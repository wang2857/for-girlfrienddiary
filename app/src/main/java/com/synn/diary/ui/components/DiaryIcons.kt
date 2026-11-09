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

/** 工具栏图标：和心情表情同一套手绘线稿风（粗圆头描边，不用填充）。 */

val DoodleImageIcon: ImageVector by lazy {
    lineIcon(
        "DoodleImage",
        "M6.5 4.5h11a2 2 0 0 1 2 2v11a2 2 0 0 1 -2 2h-11a2 2 0 0 1 -2 -2v-11a2 2 0 0 1 2 -2z",
        "M4.6 16.6l4.2-4.2 3.2 3.2 2.6-2.6 4.8 4.6",
        "M9.2 9.1a1.2 1.2 0 1 0 0 0.1z",
    )
}

/** 横线：三条手画的线，最后一条短一点，像随手划的。 */
val DoodleLinesIcon: ImageVector by lazy {
    lineIcon(
        "DoodleLines",
        "M4.6 7.2h14.8",
        "M4.6 12h14.8",
        "M4.6 16.8h9.6",
    )
}

val DoodleTextIcon: ImageVector by lazy {
    lineIcon(
        "DoodleText",
        "M4.4 6.8h7.4",
        "M8.1 6.8v11.4",
        "M15.2 10.6v7.6",
        "M13.4 13.4h3.8",
    )
}

val DoodlePaletteIcon: ImageVector by lazy {
    lineIcon(
        "DoodlePalette",
        "M12 4.2c-4.3 0-7.8 3.4-7.8 7.7s3.5 7.7 7.8 7.7c1 0 1.7-.8 1.7-1.7 0-.5-.2-.9-.5-1.2" +
            "-.3-.3-.5-.7-.5-1.1 0-.9.8-1.7 1.7-1.7h1.4c2.5 0 4.5-2 4.5-4.5 0-3-3.4-5.2-8.3-5.2z",
        "M8.7 11.4a1 1 0 1 0 0 0.1z",
        "M12 8.3a1 1 0 1 0 0 0.1z",
        "M15.3 11.4a1 1 0 1 0 0 0.1z",
    )
}

val DoodleTrashIcon: ImageVector by lazy {
    lineIcon(
        "DoodleTrash",
        "M4.8 7.2h14.4",
        "M9.6 7.2V5.4h4.8v1.8",
        "M6.6 7.2l.9 12.2h9l.9-12.2",
        "M10 10.6v5.8",
        "M14 10.6v5.8",
    )
}

private fun lineIcon(name: String, vararg paths: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    paths.forEach { path ->
        addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
    }
}.build()

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

/** 线稿软盘：和工具栏那套笔触一致，替代原来实心的 SaveIcon。 */
val DoodleSaveIcon: ImageVector by lazy {
    lineIcon(
        "DoodleSave",
        "M5.4 4.8h11.4l3.2 3.2v11.2H5.4z",
        "M8.4 4.8v4.6h7.2V4.8",
        "M8.2 19.2v-5.4h7.6v5.4",
        "M9.4 15.2h4.8",
    )
}

/**
 * 手绘铅笔：笔杆是 45° 的平行四边形，笔尖是它的等腰三角形延长。
 *
 * 之前的版本笔尖是单独一段折线，角度和笔杆对不上，小尺寸下看起来"歪了"。
 * 现在所有点都由同一条轴线算出来：轴 (-1,1)，笔尖顶点、笔尖底边、笔杆两端同宽。
 */
val DoodlePencilIcon: ImageVector by lazy {
    lineIcon(
        "DoodlePencil",
        // 笔杆
        "M5.5 15.8L16.1 5.2L18.8 7.9L8.2 18.5Z",
        // 笔尖（底边 = 笔杆的短边）
        "M5.5 15.8L4.6 19.4L8.2 18.5Z",
        // 橡皮箍
        "M15.6 6.3L18.2 8.9",
    )
}
