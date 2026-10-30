package app.nooklings.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import app.nooklings.ui.art.Buf
import app.nooklings.ui.art.mix
import app.nooklings.ui.art.rgb
import java.io.File
import java.util.UUID

/** Photos of light and recorded sounds live in the app's own folders, never in the gallery. */
object Files {
    fun photos(c: Context) = File(c.filesDir, "photos").apply { mkdirs() }
    fun sounds(c: Context) = File(c.filesDir, "sounds").apply { mkdirs() }
    fun photo(c: Context, name: String) = File(photos(c), name)
    fun sound(c: Context, name: String) = File(sounds(c), name)

    /** A fresh file for ten seconds of sound. */
    fun newSound(c: Context): File = File(sounds(c), UUID.randomUUID().toString() + ".m4a")

    fun delete(c: Context, j: Jar) {
        j.photo?.let { runCatching { photo(c, it).delete() } }
        j.sound?.let { runCatching { sound(c, it).delete() } }
    }

    /** Keeps a photo at a modest size, upright. Returns its file name. */
    fun savePhoto(c: Context, bytes: ByteArray): String? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return null
        var s = 1
        while (o.outWidth / (s * 2) >= 900 || o.outHeight / (s * 2) >= 900) s *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) ?: return null
        val rot = runCatching { ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
        val up = if (rot == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
        val name = UUID.randomUUID().toString() + ".jpg"
        photo(c, name).outputStream().use { up.compress(Bitmap.CompressFormat.JPEG, 86, it) }
        name
    }.getOrNull()

    fun savePhoto(c: Context, uri: Uri): String? = runCatching { c.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { savePhoto(c, it) } }.getOrNull()

    private fun small(c: Context, name: String, size: Int): Bitmap? = runCatching {
        val b = BitmapFactory.decodeFile(photo(c, name).path, BitmapFactory.Options().apply { inSampleSize = 8 }) ?: return null
        val side = minOf(b.width, b.height)
        val sq = Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
        Bitmap.createScaledBitmap(sq, size, size, true)
    }.getOrNull()

    /**
     * The light of a photo: the average of its bright pixels (or of all of them in a dim picture),
     * a little more saturated, so a lamp gives amber and a twilight gives violet.
     */
    fun lightOf(c: Context, name: String): Int {
        val b = small(c, name, 24) ?: return 0
        var r = 0L; var g = 0L; var bl = 0L; var n = 0
        var br = 0L; var bg = 0L; var bb = 0L; var bn = 0
        for (y in 0 until 24) for (x in 0 until 24) {
            val p = b.getPixel(x, y)
            val pr = (p shr 16) and 255; val pg = (p shr 8) and 255; val pb = p and 255
            r += pr; g += pg; bl += pb; n++
            if (pr + pg + pb > 420) { br += pr; bg += pg; bb += pb; bn++ }
        }
        val (ar, ag, ab) = if (bn > 20) Triple(br / bn, bg / bn, bb / bn) else Triple(r / n, g / n, bl / n)
        val lum = (ar + ag + ab) / 3.0
        fun sat(v: Long) = (lum + (v - lum) * 1.5).toInt()
        return rgb(sat(ar), sat(ag), sat(ab))
    }

    /** The photo as a little picture in pixels, posterised to the room's palette. */
    fun pixels(c: Context, name: String, size: Int = 32): Buf? {
        val b = small(c, name, size) ?: return null
        val out = Buf(size, size)
        for (y in 0 until size) for (x in 0 until size) {
            val p = b.getPixel(x, y)
            fun q(v: Int) = (v / 36) * 36 + 18
            out.p(x, y, mix(rgb(q((p shr 16) and 255), q((p shr 8) and 255), q(p and 255)), rgb(0x2A160E), .06f))
        }
        return out
    }
}
