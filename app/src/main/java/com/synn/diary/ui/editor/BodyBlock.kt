package com.synn.diary.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.synn.diary.data.body.BodyMarkers
import com.synn.diary.data.body.ImageAlign
import com.synn.diary.data.body.ImageRef
import java.util.UUID

/**
 * 编辑器里的正文块。图片是独立的一块插在文本块之间，
 * 所以"图片固定在任意位置"就等价于"图片块在列表里的位置"。
 */
sealed interface BodyBlock {
    val id: String

    data class Text(override val id: String, val value: TextFieldValue) : BodyBlock
    data class Image(
        override val id: String,
        val fileName: String,
        val widthPercent: Int = 100,
        val align: ImageAlign = ImageAlign.CENTER,
    ) : BodyBlock
}

fun newBlockId(): String = UUID.randomUUID().toString()

/** 正文 → 块列表：按标记切开，文本段各自成块。 */
fun parseBlocks(body: String): List<BodyBlock> {
    val blocks = mutableListOf<BodyBlock>()
    var cursor = 0

    for (match in BodyMarkers.matches(body)) {
        if (match.range.first > cursor) {
            val text = body.substring(cursor, match.range.first)
            blocks += BodyBlock.Text(newBlockId(), TextFieldValue(text, TextRange(text.length)))
        }
        val ref = BodyMarkers.ref(match)
        blocks += BodyBlock.Image(newBlockId(), ref.fileName, ref.widthPercent, ref.align)
        cursor = match.range.last + 1
    }

    if (cursor < body.length) {
        val text = body.substring(cursor)
        blocks += BodyBlock.Text(newBlockId(), TextFieldValue(text, TextRange(text.length)))
    }

    // 首尾各留一个文本块：否则开头或结尾是图片时，没有地方下光标
    if (blocks.isEmpty() || blocks.first() is BodyBlock.Image) {
        blocks.add(0, BodyBlock.Text(newBlockId(), TextFieldValue("")))
    }
    if (blocks.last() is BodyBlock.Image) {
        blocks += BodyBlock.Text(newBlockId(), TextFieldValue(""))
    }
    return blocks
}

fun serializeBlocks(blocks: List<BodyBlock>): String = buildString {
    blocks.forEach { block ->
        when (block) {
            is BodyBlock.Text -> append(block.value.text)
            is BodyBlock.Image -> append(
                BodyMarkers.marker(ImageRef(block.fileName, block.widthPercent, block.align)),
            )
        }
    }
}
