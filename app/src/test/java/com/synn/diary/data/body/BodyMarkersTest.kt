package com.synn.diary.data.body

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyMarkersTest {

    @Test
    fun `标记能原样解析回来`() {
        val ref = ImageRef("a1b2-c3.jpg", 60, ImageAlign.RIGHT)
        val body = "前面${BodyMarkers.marker(ref)}后面"
        val parsed = BodyMarkers.firstRef(body)
        assertEquals(ref, parsed)
        assertEquals("前面后面", BodyMarkers.strip(body))
    }

    @Test
    fun `老格式标记仍兼容，按满宽居中`() {
        val parsed = BodyMarkers.firstRef("图：[[img:old.jpg]]")
        assertEquals("old.jpg", parsed?.fileName)
        assertEquals(100, parsed?.widthPercent)
        assertEquals(ImageAlign.CENTER, parsed?.align)
    }

    @Test
    fun `宽度会被夹在 20 到 100 之间`() {
        assertEquals(20, BodyMarkers.firstRef("[[img:x.jpg|1|c]]")?.widthPercent)
        assertEquals(100, BodyMarkers.firstRef("[[img:x.jpg|999|c]]")?.widthPercent)
        assertEquals(20, BodyMarkers.marker(ImageRef("x.jpg", 5)).let {
            BodyMarkers.firstRef(it)?.widthPercent
        })
    }

    @Test
    fun `非法文件名不匹配，不会当成图片`() {
        assertNull(BodyMarkers.firstRef("[[img:../etc/passwd]]"))
        assertNull(BodyMarkers.firstRef("[[img:name with space.jpg]]"))
        assertTrue(BodyMarkers.allImages("[[img:a.jpg]][[img:../x]]").containsExactly("a.jpg"))
    }

    @Test
    fun `一页里多张图按顺序取`() {
        val body = "[[img:1.jpg|50|l]]中间[[img:2.jpg|80|r]]"
        assertEquals(listOf("1.jpg", "2.jpg"), BodyMarkers.allImages(body))
        assertEquals("中间", BodyMarkers.strip(body))
    }

    private fun List<String>.containsExactly(expected: String): Boolean =
        size == 1 && first() == expected
}
