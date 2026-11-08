package com.synn.diary.ui.editor

import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.synn.diary.R
import com.synn.diary.data.media.FontStore

/**
 * 正文可选字体。
 *
 * 界面本身一律走系统字体（按钮、标题、设置项），只有"写的内容"能换字体——
 * 日记的正文应该是用户挑的字体，App 的壳不该跟着变。
 */
sealed interface ContentFont {

    val id: String
    val label: String

    /** null = 跟随系统默认 */
    fun family(store: FontStore): FontFamily?

    data object System : ContentFont {
        override val id = "system"
        override val label = "系统默认"
        override fun family(store: FontStore): FontFamily? = null
    }

    data object Serif : ContentFont {
        override val id = "serif"
        override val label = "系统衬线"
        override fun family(store: FontStore) = FontFamily.Serif
    }

    data object Mono : ContentFont {
        override val id = "mono"
        override val label = "系统等宽"
        override fun family(store: FontStore) = FontFamily.Monospace
    }

    /** 自带的霞鹜文楷（SIL OFL），只留 GB2312 常用字，3.4 MB。 */
    data object WenKai : ContentFont {
        override val id = "wenkai"
        override val label = "霞鹜文楷"
        override fun family(store: FontStore) = wenkaiFamily
    }

    data class Custom(val fileName: String) : ContentFont {
        override val id = "file:$fileName"
        override val label: String = fileName
        override fun family(store: FontStore): FontFamily? = cache[fileName]
    }

    companion object {
        private const val PREFIX = "file:"

        private val cache = mutableMapOf<String, FontFamily>()

        // 自带字体只解析一次：弹窗里有样本、正文也会用到，别每次重组都 new
        private val wenkaiFamily by lazy { FontFamily(Font(R.font.lxgw_wenkai)) }

        /** 已导入的字体在第一次用到时解析，之后复用。 */
        fun custom(fileName: String, store: FontStore): Custom {
            cache.getOrPut(fileName) {
                runCatching {
                    // Typeface.createFromFile 对坏文件会退回默认字体，这里再守一道
                    FontFamily(Typeface.createFromFile(store.file(fileName)))
                }.getOrElse { FontFamily.Default }
            }
            return Custom(fileName)
        }

        fun from(id: String?, store: FontStore): ContentFont = when {
            id.isNullOrBlank() || id == System.id -> System
            id == Serif.id -> Serif
            id == Mono.id -> Mono
            id == WenKai.id -> WenKai
            id.startsWith(PREFIX) -> custom(id.removePrefix(PREFIX), store)
            else -> System
        }

        val presets = listOf(System, Serif, Mono, WenKai)
    }
}
