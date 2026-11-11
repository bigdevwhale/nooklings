package app.nooklings

import android.graphics.Bitmap
import app.nooklings.domain.Pet
import app.nooklings.ui.art.PetArt
import app.nooklings.ui.art.PetPose
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every lodger in every pose on one sheet, scaled up 8× (run with `-Prender`): app/build/art/pets.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderPets {
    private val poses = listOf(
        PetPose(f = 0), PetPose(f = 1), PetPose(f = 2), PetPose(f = 3), PetPose(blink = true),
        PetPose(fed = true, f = 1), PetPose(fed = true, f = 3), PetPose(sleep = true), PetPose(sleep = true, cold = true),
    )

    @Test fun sheet() {
        val s = 8; val cell = 40
        val w = poses.size * cell * s; val h = Pet.entries.size * cell * s
        val px = IntArray(w * h) { if (((it % w) / (cell * s) + (it / w) / (cell * s)) % 2 == 0) 0xFF3A2A22.toInt() else 0xFF4A3A30.toInt() }
        for ((row, p) in Pet.entries.withIndex()) for ((col, o) in poses.withIndex()) {
            val b = PetArt.draw(p, o)
            val ox = col * cell + (cell - b.w) / 2; val oy = row * cell + (cell - b.h) / 2
            for (j in 0 until b.h) for (i in 0 until b.w) {
                val c = b.d[j * b.w + i]
                if (c ushr 24 == 0) continue
                for (dy in 0 until s) for (dx in 0 until s) px[((oy + j) * s + dy) * w + (ox + i) * s + dx] = c or 0xFF000000.toInt()
            }
        }
        val img = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
        File(out, "pets.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
