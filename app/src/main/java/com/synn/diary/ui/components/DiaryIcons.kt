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
