package com.synn.diary.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片落盘：一律搬运到 App 私有目录，不留外部 Uri。
 *
 * 外部 Uri（尤其相册里的）随时可能失效——用户删了原图，日记里的照片就没了。
 * 所以插入时就复制一份、按长边缩到 1600 以内、按 EXIF 摆正，再存。
 */
class ImageStore(context: Context) {

    private val context = context.applicationContext
    private val dir = File(this.context.filesDir, "images").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 只量尺寸：inJustDecodeBounds 模式下 decodeStream 本来就返回 null，不能拿它判空
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = resolver.openInputStream(uri) ?: return@runCatching null
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)

            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            } ?: return@runCatching null

            val orientation = resolver.openInputStream(uri)?.use { stream ->
                runCatching {
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val upright = rotate(bitmap, orientation)

            val name = "${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (upright !== bitmap) bitmap.recycle()
            upright.recycle()
            name
        }.onFailure { Log.w("ImageStore", "import failed: $uri", it) }
            .onSuccess { Log.d("ImageStore", "imported $uri -> $it") }
            .getOrNull()
    }

    /** 目录里现有的文件（文件名 → 最后修改时间毫秒）。 */
    fun listFiles(): Map<String, Long> =
        dir.listFiles()?.filter { it.isFile }?.associate { it.name to it.lastModified() } ?: emptyMap()

    suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        file(name).delete()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxEdge) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun rotate(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private companion object {
        const val MAX_EDGE = 1600
    }
}
