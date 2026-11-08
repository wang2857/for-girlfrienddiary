package com.synn.diary.ui.editor

import androidx.compose.ui.graphics.Color

/** 纸面花纹。全部用 Canvas 画，不额外带图片资源。 */
enum class PaperPattern { NONE, DOTS, HEARTS, STARS, GRID }

/**
 * 日记纸张。
 *
 * 两种来源：预设（颜色 + 花纹）和自定义图片（用户从相册选的）。
 * 库里统一存字符串：预设存 id（如 `peach`），自定义存 `img:<文件名>`。
 */
sealed interface EntryBackground {

    val id: String
    val label: String

    val isDefault: Boolean get() = id == PRESET_DEFAULT

    /*** 预设纸：底色 + 花纹 */
    data class Preset(
        override val id: String,
        override val label: String,
        val light: Color,
        val dark: Color,
        val pattern: PaperPattern = PaperPattern.NONE,
    ) : EntryBackground {
        fun color(isDark: Boolean): Color = if (isDark) dark else light
    }

    /** 自定义图片纸 */
    data class Custom(val fileName: String) : EntryBackground {
        override val id: String get() = PREFIX_CUSTOM + fileName
        override val label: String get() = "自定义"
    }

    companion object {
        private const val PRESET_DEFAULT = "default"
        private const val PREFIX_CUSTOM = "img:"

        /** 预设列表，顺序即选择器里的顺序。 */
        val presets: List<Preset> = listOf(
            Preset(PRESET_DEFAULT, "默认", Color.Transparent, Color.Transparent),
            Preset("cream", "米黄", Color(0xFFFBF3E0), Color(0xFF2A2620), PaperPattern.DOTS),
            Preset("mint", "薄荷", Color(0xFFE7F6F0), Color(0xFF1C2A26), PaperPattern.DOTS),
            Preset("peach", "蜜桃", Color(0xFFFFF0EC), Color(0xFF2E2321), PaperPattern.HEARTS),
            Preset("sakura", "樱花", Color(0xFFFFF1F6), Color(0xFF2E2228), PaperPattern.HEARTS),
            Preset("sky", "晴空", Color(0xFFE8F2FF), Color(0xFF1C2430), PaperPattern.STARS),
            Preset("butter", "奶黄", Color(0xFFFFF7DE), Color(0xFF2B2719), PaperPattern.STARS),
            Preset("grid", "格纸", Color(0xFFF7F7F7), Color(0xFF232323), PaperPattern.GRID),
        )

        val default: EntryBackground = presets.first()

        fun from(id: String?): EntryBackground {
            if (id.isNullOrBlank()) return default
            if (id.startsWith(PREFIX_CUSTOM)) {
                return Custom(id.removePrefix(PREFIX_CUSTOM))
            }
            return presets.firstOrNull { it.id == id } ?: default
        }
    }
}
