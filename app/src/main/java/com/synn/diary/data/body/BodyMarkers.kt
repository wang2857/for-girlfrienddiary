package com.synn.diary.data.body

/** 图片在正文里的水平位置。竖直位置由它在文本流中的顺序决定。 */
enum class ImageAlign(val code: String) {
    LEFT("l"),
    CENTER("c"),
    RIGHT("r"),
    ;

    companion object {
        fun from(code: String?): ImageAlign = entries.firstOrNull { it.code == code } ?: CENTER
    }
}

data class ImageRef(
    val fileName: String,
    val widthPercent: Int = 100,
    val align: ImageAlign = ImageAlign.CENTER,
)

/**
 * 正文里图片的占位标记：`[[img:<文件名>|宽度百分比|对齐]]`。
 *
 * 尺寸和对齐直接写进标记，所以不需要改表结构，也不用额外的关联表；
 * 老标记 `[[img:文件名]]` 仍然能解析，按满宽居中处理。
 *
 * 用标记而不是数据库偏移量的理由：标记能跟着文字一起被剪切、粘贴、移动，
 * 坏了最多是图片不显示，不会破坏正文。
 */
object BodyMarkers {

    val pattern = Regex("""\[\[img:([A-Za-z0-9._-]+)(?:\|(\d{1,3})\|([lcr]))?]]""")

    fun marker(ref: ImageRef): String =
        "[[img:${ref.fileName}|${ref.widthPercent.coerceIn(20, 100)}|${ref.align.code}]]"

    fun matches(body: String): List<MatchResult> = pattern.findAll(body).toList()

    fun ref(match: MatchResult): ImageRef = ImageRef(
        fileName = match.groupValues[1],
        widthPercent = match.groupValues[2].toIntOrNull()?.coerceIn(20, 100) ?: 100,
        align = ImageAlign.from(match.groupValues[3].takeIf { it.isNotBlank() }),
    )

    fun firstRef(body: String): ImageRef? = pattern.find(body)?.let { ref(it) }

    fun allImages(body: String): List<String> =
        pattern.findAll(body).mapNotNull { it.groups[1]?.value }.toList()

    fun firstImage(body: String): String? = firstRef(body)?.fileName

    /** 给列表预览、搜索结果用：把标记去掉，别让 `[[img:...]]` 出现在摘要里。 */
    fun strip(body: String): String = body.replace(pattern, "").trim()
}
