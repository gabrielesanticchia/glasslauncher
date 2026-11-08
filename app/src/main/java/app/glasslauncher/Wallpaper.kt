package app.glasslauncher

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import kotlin.math.max

object Wallpaper {
    private fun file(ctx: Context) = File(ctx.filesDir, "wallpaper.jpg")

    fun save(ctx: Context, uri: Uri) {
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1
        while (o.outWidth / s > 2400 || o.outHeight / s > 2400) s *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) ?: return
        file(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }

    fun load(ctx: Context, w: Int, h: Int): Bitmap {
        val f = file(ctx)
        val src = if (f.exists()) BitmapFactory.decodeFile(f.path) else null
        return if (src != null) centerCrop(src, w, h) else generated(w, h)
    }

    private fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val scale = max(w / src.width.toFloat(), h / src.height.toFloat())
        val sw = (w / scale).toInt().coerceIn(1, src.width)
        val sh = (h / scale).toInt().coerceIn(1, src.height)
        val cropped = Bitmap.createBitmap(src, (src.width - sw) / 2, (src.height - sh) / 2, sw, sh)
        return Bitmap.createScaledBitmap(cropped, w, h, true)
    }

    /** Sfondo di default: mesh gradient viola/arancio/azzurro in stile iOS. */
    private fun generated(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFF140F4F.toInt(), 0xFF4A2A8A.toInt(), 0xFFA8307F.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        fun blob(x: Float, y: Float, r: Float, color: Int) {
            p.shader = RadialGradient(x * w, y * h, r * w, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(x * w, y * h, r * w, p)
        }
        blob(0.85f, 0.18f, 0.75f, 0xDDFF8A3D.toInt())
        blob(0.05f, 0.55f, 0.85f, 0xBB2DD4FF.toInt())
        blob(0.75f, 0.88f, 0.95f, 0xCCFF4D9A.toInt())
        blob(0.2f, 0.08f, 0.6f, 0xAA4D6BFF.toInt())
        return bmp
    }

    /** Sfocatura veloce (riduzione + box blur) usata come "retro" del Liquid Glass. */
    fun blurred(src: Bitmap, down: Int = 6, radius: Int = 5): Bitmap {
        val sw = (src.width / down).coerceAtLeast(2)
        val sh = (src.height / down).coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        repeat(3) { boxBlur(px, sw, sh, radius) }
        small.setPixels(px, 0, sw, 0, 0, sw, sh)
        return small
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(px.size)
        fun pass(src: IntArray, dst: IntArray, horizontal: Boolean) {
            val n = if (horizontal) w else h
            val lines = if (horizontal) h else w
            val d = 2 * r + 1
            for (l in 0 until lines) for (i in 0 until n) {
                var rr = 0; var g = 0; var b = 0
                for (k in -r..r) {
                    val j = (i + k).coerceIn(0, n - 1)
                    val c = src[if (horizontal) l * w + j else j * w + l]
                    rr += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
                }
                dst[if (horizontal) l * w + i else i * w + l] =
                    (0xFF shl 24) or ((rr / d) shl 16) or ((g / d) shl 8) or (b / d)
            }
        }
        pass(px, tmp, true); pass(tmp, px, false)
    }
}
