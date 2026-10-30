package app.nooklings.ui.art

import app.nooklings.domain.Gift
import app.nooklings.domain.Lodger
import app.nooklings.domain.Pet
import app.nooklings.domain.Phase
import app.nooklings.domain.Wx
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** A jar on the pantry shelf. */
class ShelfJar(val id: Long, val look: JarLook)

/** What the room shows: who lives here and how they are, the jars, the gifts, and outside. */
class RoomModel(
    val lodgers: Map<Pet, Lodger>,
    /** The newest jars, oldest first; the shelves hold [RoomScene.SLOTS]. */
    val jars: List<ShelfJar>,
    val gifts: Set<Gift>,
    val phase: Phase,
    val wx: Wx,
    val temp: Int? = null,
    val wind: Double = 8.0,
    val autumn: Boolean = false,
)

/** What moves this frame beyond the idle loop. */
class RoomFx(
    /** Everyone by the stove: full in the evening, or listening to an opened jar. */
    val pile: Boolean = false,
    /** The stove flares while a jar is open, and those who came to listen are awake. */
    val flare: Boolean = false,
    /** A jar flying to its slot: which, how far along (0..1). */
    val flyId: Long? = null,
    val fly: Double = 0.0,
    /** Lodgers fading in as they arrive (0..1). */
    val fade: Map<Pet, Float> = emptyMap(),
    /** Hearts over lodgers who just ate: pet and age in seconds. */
    val hearts: List<Pair<Pet, Double>> = emptyList(),
    /** Seconds since the jar landed, for the sparks. */
    val landedAge: Double? = null,
    /** Wall-clock time for the clock on the wall. */
    val clock: Long = System.currentTimeMillis(),
)

sealed class Spot {
    data class Lodger(val pet: Pet) : Spot()
    data class Jar(val id: Long) : Spot()
    data object Pantry : Spot()
    data object Stove : Spot()
}

/**
 * The room, 176 art pixels wide: the window with the real sky, the pantry of glowing jars, the tiled
 * stove with the cat on top, the table with the lamp and the dormouse's cup, the hedgehog's valenok
 * and the raccoon's basket on the rug. The light follows the phase: lamp and fire at night.
 */
class RoomScene(val m: RoomModel, val H: Int = BASE_H) {
    companion object {
        const val W = 176
        const val BASE_H = 192
        const val MAX_H = 268
        const val SLOTS = 18
        private val PLANKS = intArrayOf(30, 52, 74)
        private const val GLASS = 0x00010203
        private val LAMP = intArrayOf(46, 90)
        private val FIRE = intArrayOf(149, 120)
        private val CANDLE = intArrayOf(57, 92)
        private val FIRE_C = intArrayOf(0xE2542A, 0xF29A33, 0xFFD76A, 0xFFF2B0, 0xFF7A3A)
        private val EMIT = (FIRE_C.toList() + listOf(0xFFE6A0, 0xF6C86A, 0xE0A848)).toSet()
        val POS = mapOf(Pet.CAT to intArrayOf(138, 22), Pet.DORMOUSE to intArrayOf(62, 80), Pet.MOTH to intArrayOf(34, 61),
            Pet.BULLFINCH to intArrayOf(34, 33), Pet.HEDGEHOG to intArrayOf(0, 125), Pet.RACCOON to intArrayOf(104, 135))
        val PILE = mapOf(Pet.CAT to intArrayOf(62, 146), Pet.DORMOUSE to intArrayOf(92, 154), Pet.MOTH to intArrayOf(68, 134),
            Pet.BULLFINCH to intArrayOf(100, 116), Pet.HEDGEHOG to intArrayOf(40, 152), Pet.RACCOON to intArrayOf(96, 137))
        private val PILE_ORDER = listOf(Pet.MOTH, Pet.RACCOON, Pet.BULLFINCH, Pet.CAT, Pet.HEDGEHOG, Pet.DORMOUSE)
        fun slot(k: Int) = intArrayOf(71 + (k % 6) * 8, PLANKS[k / 6] - 14)
        private val HEART = listOf(".r.r.", "rrrrr", "rrrrr", ".rrr.", "..r..")
        private val ZED = listOf("zzzz", "..z.", ".z..", "zzzz")
        private val NOTE = listOf(".nn", ".n.", ".n.", "nn.", "nn.")
    }

    private val bg = Buf(W, H)
    private val fg = Buf(W, H)
    private val sky = Buf(W, H)
    private val has = m.gifts
    private val shelf = m.jars.takeLast(SLOTS)
    private val hits = ArrayList<Pair<IntArray, Spot>>()

    /** On a taller screen the floor is deeper: what stands on it moves down by half the extra. */
    private val fy = (H - BASE_H) / 2

    init {
        wall(bg); floor(bg)
        bg.blit(Buf(W, H).also { f ->
            rug(f)
            f.ell(14, 146, 7, 2, rgb(0x22222C))
            f.ell(117, 156, 13, 3, rgb(0x3A2410)); f.line(126, 140, 130, 156, rgb(0xC8C8D0)); f.line(128, 141, 131, 156, rgb(0x9A9AA8))
        }, 0, fy)
        window(bg); pantry(bg); stove(bg); clockBody(bg); table(bg); decor(bg)
        bg.ell(74, 96, 9, 1, rgb(0x6A3A1A))
        cup(fg)
        fg.blit(Buf(W, H).also { valenki(it); basket(it) }, 0, fy)
    }

    /** Where a lodger sits: its own spot, or its place in the pile; the floor ones follow the floor. */
    fun at(p: Pet, pile: Boolean): IntArray {
        val a = (if (pile) PILE else POS).getValue(p)
        return if (pile || p == Pet.HEDGEHOG || p == Pet.RACCOON) intArrayOf(a[0], a[1] + fy) else a
    }

    // ---------- the still parts ----------

    private fun wall(b: Buf) {
        b.rect(0, 0, W, 120, rgb(0xD9B98A))
        var x = 0; while (x < W) { b.rect(x, 0, 4, 120, rgb(0xD3B282)); x += 10 }
        var y = 10; var k = 0
        while (y < 114) {
            x = if (k % 2 == 1) 12 else 7
            while (x < W) { val c = rgb(0xC49264); b.px(x, y - 1, c); b.px(x - 1, y, c); b.px(x + 1, y, c); b.px(x, y + 1, c); b.px(x, y, rgb(0xB84A3A)); x += 10 }
            y += 14; k++
        }
        b.rect(0, 0, W, 3, rgb(0x8A5A34)); b.rect(0, 3, W, 1, rgb(0x6A4226))
        for (i in 0 until W) if (dith(i, 116, .5)) b.px(i, 116, rgb(0xC09A64))
        b.rect(0, 117, W, 5, rgb(0x6A4024)); b.rect(0, 117, W, 1, rgb(0x8A5A34)); b.rect(0, 121, W, 1, rgb(0x3A2012))
    }

    private fun floor(b: Buf) {
        var y = 122; var h = 4; var row = 0
        while (y < H) {
            val base = if (row % 2 == 1) rgb(0x9A6238) else rgb(0x8E5A32)
            b.rect(0, y, W, h, base); b.rect(0, y, W, 1, rgb(0x5E3820)); b.rect(0, y + 1, W, 1, mix(base, rgb(0xC88A54), .4f))
            var x = (row * 23) % 41; while (x < W) { b.rect(x, y + 1, 1, h - 1, rgb(0x6A4024)); x += 41 }
            x = (row * 13) % 9; if (h > 3) while (x < W) { b.px(x, y + 2 + x % (h - 2), rgb(0x7E4E2A)); x += 9 + row % 3 }
            y += h; h += row % 2; row++
        }
    }

    private fun rug(b: Buf) {
        val cx = 88; val cy = 170
        b.ell(cx, cy, 56, 16, rgb(0xE8B840)); b.ell(cx, cy, 54, 15, rgb(0x9A4430)); b.ell(cx, cy, 45, 11, rgb(0x7A3024)); b.ell(cx, cy, 33, 7, rgb(0x9A4430))
        if (Gift.RUG in has) { b.ell(cx, cy, 26, 5, rgb(0x2F5A6B)); b.ell(cx, cy, 18, 3, rgb(0xE8B840)); for (i in -3..3) b.px(cx + i * 7, cy, rgb(0xF2E6C8)) }
        var a = 0; while (a < 360) { val r = Math.toRadians(a.toDouble()); b.px(cx + cos(r) * 50, cy + sin(r) * 13, rgb(0xE8B840)); a += 12 }
        var y = cy - 5; while (y < cy + 6) { b.px(cx - 58, y, rgb(0xE8D8A0)); b.px(cx + 58, y, rgb(0xE8D8A0)); y += 2 }
    }

    private fun window(b: Buf) {
        b.rect(2, 4, 62, 2, rgb(0xE8B840)); b.rect(2, 4, 62, 1, rgb(0xFFF0A0)); b.rect(0, 3, 3, 4, rgb(0xB07C1C)); b.rect(63, 3, 3, 4, rgb(0xB07C1C))
        b.rect(10, 8, 48, 46, rgb(0x6A4024)); b.rect(11, 9, 46, 44, rgb(0x9A6A3E))
        for (j in 12 until 50) for (i in 14 until 54) b.d[j * W + i] = GLASS or (0x01 shl 24)
        b.rect(33, 12, 2, 38, rgb(0x9A6A3E)); b.rect(14, 30, 40, 2, rgb(0x9A6A3E)); b.px(33, 12, rgb(0xB88A58))
        b.rect(6, 54, 56, 4, rgb(0xB07A48)); b.rect(6, 54, 56, 1, rgb(0xD09A60)); b.rect(6, 58, 56, 1, rgb(0x5A341C))
        if (m.wx == Wx.SNOW) { b.rect(8, 53, 52, 1, rgb(0xF4F8FF)); var sx = 9; while (sx < 60) { b.px(sx, 52, rgb(0xF4F8FF)); sx += 5 } }
        for ((x0, dir) in listOf(3 to 1, 64 to -1)) {
            for (y in 6 until 64) {
                val w = if (y < 28) 9 else if (y < 34) 4 else if (y < 42) 6 else 8
                val a = if (dir > 0) x0 else x0 - w + 1
                b.rect(a, y, w, 1, rgb(0x4F7A4A))
                var k = 1; while (k < w) { b.px(a + k, y, rgb(0x3A5E38)); k += 3 }
                b.px(if (dir > 0) a else a + w - 1, y, rgb(0x6A9A5E))
                if (y == 63) { k = 0; while (k < w) { b.px(a + k, y + 1, rgb(0x3A5E38)); k += 2 } }
            }
            val tx = if (dir > 0) x0 else x0 - 5
            b.rect(tx, 30, 6, 2, rgb(0xE8B840)); b.px(tx, 30, rgb(0xFFF0A0))
        }
        // герань на подоконнике
        b.lit(ramp(rgb(0xB85A3A))) { rect(16, 47, 9, 7, it); rect(15, 46, 11, 2, it) }
        b.ell(20, 42, 5, 3, rgb(0x4F8A3A)); b.ell(18, 41, 2, 2, rgb(0x6AAA4A))
        for ((a, c) in listOf(17 to 38, 21 to 37, 24 to 40)) { b.ell(a, c, 1, 1, rgb(0xE04A5A)); b.px(a, c - 1, rgb(0xFF8A8A)) }
        if (Gift.ROWAN in has) {
            for ((a, c) in listOf(52 to 52, 54 to 51, 53 to 53, 55 to 53, 51 to 53, 54 to 49)) b.px(a, c, rgb(0xE0442A))
            b.px(53, 50, rgb(0xFF8A6A)); b.line(54, 47, 56, 45, rgb(0x5A3A1C)); b.px(57, 46, rgb(0x4F8A3A)); b.px(58, 45, rgb(0x4F8A3A))
        }
    }

    private fun pantry(b: Buf) {
        b.lit(ramp(rgb(0x8A5A34))) { rect(68, 6, 54, 72, it) }
        b.rect(71, 12, 48, 62, rgb(0x4A2A16)); var y = 15; while (y < 74) { b.rect(71, y, 48, 1, rgb(0x3E2210)); y += 6 }
        b.rect(66, 4, 58, 4, rgb(0xB07A48)); b.rect(66, 4, 58, 1, rgb(0xD09A60)); b.rect(66, 8, 58, 1, rgb(0x5A341C))
        var x = 68; while (x < 122) { b.px(x, 9, rgb(0xD09A60)); b.px(x + 1, 10, rgb(0xB07A48)); x += 6 }
        for (py in PLANKS) {
            b.rect(70, py, 50, 3, rgb(0xB07A48)); b.rect(70, py, 50, 1, rgb(0xD09A60)); b.rect(70, py + 3, 50, 1, rgb(0x5A341C))
            x = 71; while (x < 119) { b.px(x, py + 3, rgb(0xF2E6C8)); b.px(x + 1, py + 4, rgb(0xF2E6C8)); x += 3 }
        }
    }

    private fun stove(b: Buf) {
        b.lit(ramp(rgb(0xE9E1D2))) { rect(136, 0, 24, 46, it) }
        b.rect(134, 34, 28, 4, rgb(0xD7CCB9)); b.rect(134, 34, 28, 1, rgb(0xF4EEE2)); b.rect(134, 38, 28, 1, rgb(0xB8AD99))
        b.rect(144, 16, 8, 5, rgb(0xB07C1C)); b.rect(145, 17, 6, 1, rgb(0xE8B840)); b.px(148, 19, rgb(0x4A2414))
        b.lit(ramp(rgb(0xDED4C2))) { rect(122, 46, 54, 4, it) }
        b.lit(ramp(rgb(0xEFE7DA))) { rect(124, 50, 52, 98, it) }
        var ty = 54
        while (ty < 94) {
            for (tx in intArrayOf(127, 137, 147, 157, 167)) {
                b.rect(tx, ty, 9, 9, rgb(0xE9E0CC)); b.rect(tx + 8, ty, 1, 9, rgb(0xCFC4AE)); b.rect(tx, ty + 8, 9, 1, rgb(0xCFC4AE))
                b.px(tx + 4, ty + 4, rgb(0x2F5A9A))
                for ((a, c) in listOf(4 to 2, 4 to 6, 2 to 4, 6 to 4)) b.px(tx + a, ty + c, rgb(0x5A82C6))
                for ((a, c) in listOf(1 to 1, 7 to 1, 1 to 7, 7 to 7)) b.px(tx + a, ty + c, rgb(0x9AB4DC))
            }
            ty += 10
        }
        b.rect(124, 96, 52, 3, rgb(0xD7CCB9)); b.rect(124, 96, 52, 1, rgb(0xF4EEE2)); b.rect(124, 99, 52, 1, rgb(0xB8AD99))
        b.rect(132, 104, 34, 32, rgb(0x5A4C40)); b.ell(149, 106, 17, 4, rgb(0x5A4C40))
        b.rect(135, 107, 28, 27, rgb(0x1A100A)); b.ell(149, 108, 14, 3, rgb(0x1A100A))
        b.rect(132, 104, 1, 32, rgb(0x7A6A5A)); b.ell(149, 102, 2, 1, rgb(0xE8B840))
        b.rect(138, 128, 22, 3, rgb(0x5A3018)); b.rect(140, 126, 18, 2, rgb(0x6A3A1C)); b.px(138, 129, rgb(0xC89A6A)); b.px(159, 129, rgb(0xC89A6A))
        b.rect(130, 136, 38, 3, rgb(0x4A3E34)); b.rect(130, 136, 38, 1, rgb(0x6A5E54))
        b.rect(124, 140, 52, 8, rgb(0xC7BBA6)); b.rect(124, 140, 52, 1, rgb(0xDDD2C0)); b.rect(124, 147, 52, 1, rgb(0x8A7E6A))
        b.line(120, 150, 128, 98, rgb(0x3A2A20)); b.line(121, 150, 129, 98, rgb(0x5A4030))
        b.rect(125, 96, 8, 2, rgb(0x6A6A72)); b.line(125, 96, 123, 91, rgb(0x6A6A72)); b.line(132, 96, 134, 91, rgb(0x6A6A72))
        for ((lx, ly) in listOf(156 to 158, 163 to 158, 170 to 158, 159 to 152, 166 to 152, 163 to 146)) {
            b.ell(lx, ly, 3, 3, rgb(0x5A3018)); b.ell(lx, ly, 2, 2, rgb(0xC89A6A)); b.px(lx, ly, rgb(0x8A5A34))
        }
    }

    private fun clockBody(b: Buf) {
        b.lit(ramp(rgb(0x8A5A34))) { rect(102, 94, 13, 12, it); for (i in 0 until 4) rect(105 - i, 90 + i, 7 + 2 * i, 1, it) }
        b.line(105, 106, 105, 113, rgb(0x6A4410)); b.line(111, 106, 111, 110, rgb(0x6A4410))
        for ((a, c) in listOf(105 to 115, 111 to 112)) { b.ell(a, c, 1, 2, rgb(0x6A4410)); b.px(a, c - 1, rgb(0xB07C1C)) }
    }

    private fun table(b: Buf) {
        for (lx in intArrayOf(40, 89)) { b.rect(lx, 112, 3, 36, rgb(0x6A3A20)); b.rect(lx, 112, 1, 36, rgb(0x8A5A34)) }
        b.rect(32, 102, 68, 10, rgb(0xF2E6C8))
        var y = 104
        while (y < 112) { var x = 32; while (x < 100) { if (((x - 32) / 3 + (y - 104) / 3) % 2 == 0) b.rect(x, y, 3, 3, rgb(0xD86A5A)); x += 3 }; y += 3 }
        y = 104; while (y < 112) { b.rect(32, y, 68, 1, rgb(0xE89A8A)); y += 6 }
        b.rect(32, 102, 68, 2, rgb(0xF8F0DC)); b.rect(32, 101, 68, 1, rgb(0xE2D6BC))
        var x = 32; while (x < 100) { b.px(x, 112, rgb(0xF2E6C8)); b.px(x + 1, 113, rgb(0xF2E6C8)); x += 3 }
        b.ell(46, 103, 5, 1, rgb(0xB07C1C)); b.px(43, 103, rgb(0xE8B840))
        b.rect(45, 92, 2, 11, rgb(0xB07C1C)); b.px(45, 94, rgb(0xE8B840)); b.px(45, 98, rgb(0xE8B840))
        for (i in 0 until 12) {
            val hw = 6 + (i * .42).toInt(); val yy = 80 + i
            b.rect(46 - hw, yy, 2 * hw + 1, 1, if (i < 2) rgb(0xFFE6A0) else if (i < 9) rgb(0xF6C86A) else rgb(0xE0A848))
            b.px(46 - hw, yy, rgb(0xC08030)); b.px(46 + hw, yy, rgb(0xC08030))
        }
        x = 35; while (x <= 57) { b.px(x, 92, rgb(0xC08030)); x += 2 }
        b.rect(40, 92, 13, 1, rgb(0xFFF2B0))
    }

    private fun decor(b: Buf) {
        if (Gift.POSTCARD in has) {
            b.lit(ramp(rgb(0xF2E6C8))) { rect(6, 68, 16, 12, it) }
            b.rect(8, 70, 12, 6, rgb(0x6A9AD8)); b.rect(8, 74, 12, 2, rgb(0x4F8A3A)); b.rect(13, 71, 2, 4, rgb(0xE8B840)); b.px(17, 71, rgb(0xFFFFFF)); b.rect(13, 66, 2, 2, rgb(0xC2402F))
        }
        if (Gift.LEAF in has) {
            for ((x, y, w) in listOf(Triple(24, 84, 1), Triple(22, 85, 5), Triple(21, 86, 7), Triple(22, 87, 5), Triple(23, 88, 3))) b.rect(x, y, w, 1, rgb(0xD9622B))
            b.px(24, 86, rgb(0xA8401C)); b.line(24, 89, 24, 91, rgb(0x7A3A1A)); b.px(22, 85, rgb(0xF0904A))
        }
        if (Gift.CANDLE in has) {
            b.rect(54, 102, 7, 1, rgb(0xB07C1C)); b.rect(55, 101, 5, 1, rgb(0xE8B840)); b.rect(56, 94, 3, 7, rgb(0xF5ECD6)); b.px(56, 94, rgb(0xFFFFFF)); b.rect(58, 94, 1, 7, rgb(0xD8CCB0))
        }
        if (Gift.GARLAND in has) for (i in 0 until 13) {
            val x = (5 + i * 4.6).toInt(); val y = 7 + (sin(i / 12.0 * PI) * 4).roundToInt()
            b.px(x, y, rgb(0x2D3A24))
            if (i > 0) b.line((5 + (i - 1) * 4.6).toInt(), 7 + (sin((i - 1) / 12.0 * PI) * 4).roundToInt(), x, y, rgb(0x2D3A24))
        }
    }

    private fun cup(b: Buf) {
        b.lit(ramp(rgb(0xF5F0E8))) { rect(64, 97, 21, 6, it); ell(74, 102, 10, 2, it) }
        b.rect(64, 96, 21, 1, rgb(0xFFFFFF)); b.rect(64, 98, 21, 1, rgb(0x4F7AA3))
        for ((a, c) in listOf(67 to 101, 72 to 100, 77 to 101, 82 to 100)) b.px(a, c, rgb(0xC2402F))
        b.ring(85, 97, 4, 5, rgb(0xE8E2D6)); b.px(88, 98, rgb(0xB8B0A0))
        b.ell(74, 105, 13, 1, rgb(0xE8E2D6)); b.rect(62, 106, 25, 1, rgb(0xC8C0B0))
    }

    private fun valenki(b: Buf) {
        b.lit(ramp(rgb(0x5C6070))) { rect(7, 147, 15, 22, it); ell(12, 170, 10, 4, it); rect(2, 166, 14, 8, it) }
        for (y in 148 until 174) for (x in 2 until 23) if (dith(x * 3, y, .12) && !dith(x, y * 5, .5) && b.filled(x, y)) b.px(x, y, rgb(0x4C5060))
        b.rect(7, 147, 15, 2, rgb(0x70768A))
        for ((x, y, c) in listOf(Triple(10, 156, 0xD84A3A), Triple(14, 156, 0xE8B840), Triple(18, 156, 0xD84A3A), Triple(12, 158, 0x4F8A3A), Triple(16, 158, 0x4F8A3A))) b.px(x, y, rgb(c.toLong()))
    }

    private fun basket(b: Buf) {
        b.lit(ramp(rgb(0xC8964E))) { rect(104, 157, 27, 14, it); ell(117, 170, 13, 3, it) }
        var y = 158; while (y < 171) { var x = 105; while (x < 131) { b.px(x + if (y % 4 != 0) 2 else 0, y, rgb(0x9A6A30)); x += 4 }; y += 2 }
        b.rect(103, 155, 29, 2, rgb(0xE0B06A)); b.rect(103, 157, 29, 1, rgb(0x8A5A28))
        b.lit(ramp(rgb(0x4F7AA3))) { ell(137, 169, 4, 4, it) }
        b.line(134, 167, 140, 171, rgb(0x2F5A8A)); b.line(135, 166, 139, 172, rgb(0x7AA0D0)); b.line(133, 169, 130, 164, rgb(0x4F7AA3))
        b.lit(ramp(rgb(0xD84A3A))) { ell(100, 172, 3, 3, it) }
        b.line(102, 171, 105, 168, rgb(0xD84A3A))
    }

    // ---------- what moves ----------

    private fun heat(fx: RoomFx) = min(1.5, .3 + .7 * ((m.lodgers[Pet.CAT]?.food ?: 50.0) / 100) + if (fx.flare) .5 else 0.0)

    private fun fire(b: Buf, t: Double, fx: RoomFx) {
        val h0 = heat(fx); val n = 26
        val r = Random((t * 11).toInt())
        for (i in 0 until n) {
            val base = 1 - kotlin.math.abs(i - n / 2.0) / (n / 2.0 + 2)
            val h = max(1, ((4 + r.nextDouble() * 10) * h0 * base).roundToInt())
            val x = 136 + i; val y = 127
            b.rect(x, y - h, 1, h, rgb(0xE2542A))
            if (h > 2) b.rect(x, y - (h * .65).roundToInt(), 1, (h * .65).roundToInt(), rgb(0xF29A33))
            if (h > 4) b.rect(x, y - (h * .35).roundToInt(), 1, (h * .35).roundToInt(), rgb(0xFFD76A))
            if (h > 8) b.px(x, y - (h * .2).roundToInt(), rgb(0xFFF2B0))
        }
        repeat(5) { b.px(138 + r.nextInt(22), 129 + r.nextInt(2), rgb(0xFF7A3A)) }
    }

    private fun clock(b: Buf, t: Double, now: Long) {
        b.ell(108, 100, 4, 4, rgb(0xF4ECD8))
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val mins = c.get(Calendar.MINUTE)
        val ha = ((c.get(Calendar.HOUR) + mins / 60.0) / 12 * 2 * PI) - PI / 2
        val ma = mins / 60.0 * 2 * PI - PI / 2
        b.line(108, 100, 108 + (cos(ma) * 3).roundToInt(), 100 + (sin(ma) * 3).roundToInt(), rgb(0x2A160E))
        b.line(108, 100, 108 + (cos(ha) * 2).roundToInt(), 100 + (sin(ha) * 2).roundToInt(), rgb(0x7E2418))
        val a = sin(t * 3.1) * .32
        val bx = 108 + (sin(a) * 8).roundToInt(); val by = 106 + (cos(a) * 8).roundToInt()
        b.line(108, 106, bx, by, rgb(0x6A4410)); b.ell(bx, by, 1, 1, rgb(0xE8B840))
    }

    private fun skyCol(k: Double): Int {
        val c = when (m.phase) {
            Phase.EVENING -> if (k < .5) mix(rgb(0x6A4A8A), rgb(0xE87A6A), (k * 2).toFloat()) else mix(rgb(0xE87A6A), rgb(0xF8C888), ((k - .5) * 2).toFloat())
            Phase.DAWN -> if (k < .5) mix(rgb(0x4A5A98), rgb(0xC890B0), (k * 2).toFloat()) else mix(rgb(0xC890B0), rgb(0xF8D0A0), ((k - .5) * 2).toFloat())
            Phase.DAY -> mix(rgb(0x6AA8E8), rgb(0xC8E8F8), k.toFloat())
            Phase.NIGHT -> mix(rgb(0x0C1230), rgb(0x2A3468), k.toFloat())
        }
        val grey = when (m.wx) { Wx.CLEAR -> 0f; Wx.CLOUDY -> .12f; Wx.OVERCAST -> .55f; Wx.DRIZZLE -> .5f; Wx.RAIN -> .6f; Wx.SNOW -> .5f; Wx.FOG -> .7f; Wx.STORM -> .75f }
        return mix(c, when (m.phase) { Phase.NIGHT -> rgb(0x161A26); Phase.DAY -> rgb(0xA8B0B8); else -> rgb(0x8A8090) }, grey)
    }

    private var flash = false

    private fun drawSky(e: Buf, t: Double) {
        e.clear()
        val p = m.phase; val wx = m.wx
        val x0 = 14; val y0 = 12; val gw = 40; val gh = 38
        for (y in y0 until y0 + gh) e.rect(x0, y, gw, 1, skyCol((y - y0) / gh.toDouble()))
        val open = wx == Wx.CLEAR || wx == Wx.CLOUDY
        if (open) when (p) {
            Phase.NIGHT -> {
                for (i in 0 until 16) { val sx = x0 + 1 + hashI("s$i") % 38; val sy = y0 + 1 + hashI("y$i") % 22; if (((t * 2 + i).toInt()) % 5 != 0) e.px(sx, sy, if (i % 3 != 0) rgb(0xC8D0F0) else rgb(0xFFFFFF)) }
                e.ell(44, 20, 4, 4, rgb(0xF0ECD0)); e.ell(46, 19, 3, 3, skyCol(.2))
            }
            Phase.DAY -> { e.ell(46, 20, 4, 4, rgb(0xFFF8D0)); e.ell(46, 20, 2, 2, rgb(0xFFFFFF)) }
            Phase.EVENING -> { e.ell(40, 40, 5, 5, rgb(0xFFE0A0)); e.ell(40, 40, 3, 3, rgb(0xFFF4D0)) }
            Phase.DAWN -> { e.ell(22, 41, 5, 5, rgb(0xFFD8B0)); e.ell(22, 41, 3, 3, rgb(0xFFF0D8)) }
        }
        val nC = when (wx) { Wx.CLEAR -> 1; Wx.CLOUDY -> 3; Wx.OVERCAST -> 6; Wx.DRIZZLE -> 5; Wx.RAIN -> 6; Wx.SNOW -> 5; Wx.FOG -> 3; Wx.STORM -> 7 }
        val heavy = nC > 4
        val cc = when (p) {
            Phase.NIGHT -> if (heavy) rgb(0x2A3044) else rgb(0x3A4260)
            Phase.DAY -> if (heavy) rgb(0xC4C8D0) else rgb(0xFFFFFF)
            Phase.EVENING -> if (heavy) rgb(0x9A8090) else rgb(0xF0A8A0)
            Phase.DAWN -> if (heavy) rgb(0xA89AA8) else rgb(0xF8C8C0)
        }
        val cs = mix(cc, rgb(0), .15f)
        val dark = if (wx == Wx.STORM) mix(cc, rgb(0), .3f) else cc
        for (i in 0 until nC) {
            val h1 = hashI("c$i")
            val sp = (.4 + (h1 % 5) / 10.0) * (1 + m.wind / 20)
            val cx = x0 - 10 + ((h1 % 60 + t * sp) % (gw + 20))
            val cy = (y0 + 3 + (h1 ushr 4) % (if (heavy) 12 else 18)).toDouble()
            val rw = 5 + (h1 ushr 8) % 5
            e.ell(cx, cy + 1, rw, 2, cs); e.ell(cx, cy, rw, 2, dark); e.ell(cx - 2, cy - 1, rw - 2, 2, dark); e.ell(cx + 2, cy - 2, rw - 3, 2, dark)
        }
        val roof = when (p) { Phase.DAY -> if (heavy) rgb(0x7A8898) else rgb(0x8AA0B8); Phase.NIGHT -> rgb(0x080C1C); else -> rgb(0x5A3A5A) }
        for ((hx, w, h) in listOf(Triple(14, 8, 7), Triple(21, 6, 10), Triple(27, 9, 6), Triple(35, 6, 9), Triple(41, 7, 5), Triple(47, 7, 8))) {
            e.rect(hx, y0 + gh - h, w, h, roof); e.px(hx + 2, y0 + gh - h - 1, roof)
            if (wx == Wx.SNOW) e.rect(hx, y0 + gh - h, w, 1, rgb(0xF0F4FF))
        }
        if (p != Phase.DAY || heavy) for ((a, b) in listOf(23 to 45, 30 to 47, 37 to 46, 16 to 47, 50 to 45)) e.px(a, b, if (p == Phase.NIGHT) rgb(0xFFD070) else rgb(0xFFC890))
        if (wx == Wx.RAIN || wx == Wx.STORM || wx == Wx.DRIZZLE) {
            val n = if (wx == Wx.DRIZZLE) 14 else if (wx == Wx.STORM) 46 else 32
            val len = if (wx == Wx.DRIZZLE) 1 else 2
            val col = if (p == Phase.NIGHT) rgb(0x5A6A98) else rgb(0xA8BCD8)
            for (i in 0 until n) {
                val h1 = hashI("r$i")
                val y = y0 + floor((h1 % gh + t * (if (wx == Wx.DRIZZLE) 40 else 95)) % gh).toInt()
                val x = x0 + ((h1 ushr 6) % gw + ((y - y0) * .3).toInt()) % gw
                e.rect(x, max(y0, y - len), 1, len + 1, col)
            }
        }
        if (wx == Wx.SNOW) for (i in 0 until 34) {
            val h1 = hashI("f$i")
            val y = y0 + floor((h1 % gh + t * (7 + h1 % 6)) % gh).toInt()
            val x = x0 + floor((((h1 ushr 6) % gw) + sin(t * 1.3 + i) * 2 + gw) % gw).toInt()
            e.px(x, y, rgb(0xFFFFFF))
        }
        if (open && m.autumn) for (i in 0 until 3) {
            val ph = (t * 6 + i * 13) % gh
            val x = x0 + ((i * 13 + ph * .5 + sin(t + i * 2) * 3 + gw) % gw).toInt()
            e.rect(x, (y0 + ph).toInt(), 2, 1, if (i % 2 == 1) rgb(0xD9622B) else rgb(0xE8A22E))
        }
        if (wx == Wx.FOG) { val fc = if (p == Phase.NIGHT) rgb(0x4A5068) else rgb(0xD8DCE0); for (y in y0 until y0 + gh) for (x in x0 until x0 + gw) if (dith(x + (t * 2).toInt(), y, .3 + .3 * (y - y0) / gh)) e.px(x, y, fc) }
        if ((m.temp != null && m.temp <= 0) || wx == Wx.SNOW) for (y in y0 until y0 + gh) for (x in x0 until x0 + gw) {
            val edge = minOf(minOf(x - x0, x0 + gw - 1 - x), minOf(y0 + gh - 1 - y, y - y0))
            if (edge < 5 && dith(x, y, (5 - edge) / 7.0)) e.px(x, y, rgb(0xE8F2FF))
        }
        flash = wx == Wx.STORM && t % 6.3 < .14 && (t / 6.3).toInt() % 2 == 0
        if (flash) e.rect(x0, y0, gw, gh, rgb(0xF4F4FF))
    }

    private fun q(v: Double, n: Int, x: Int, y: Int): Double { val s = v * n; val f = floor(s); return (f + if (dith(x, y, s - f)) 1 else 0) / n }

    private fun light(b: Buf, e: Buf, t: Double, fx: RoomFx) {
        val dim = when (m.wx) { Wx.CLEAR -> 0.0; Wx.CLOUDY -> .04; Wx.OVERCAST -> .12; Wx.DRIZZLE -> .12; Wx.RAIN -> .16; Wx.SNOW -> .06; Wx.FOG -> .12; Wx.STORM -> .22 }
        val p: DoubleArray? = when (m.phase) {
            Phase.DAY -> if (dim < .05) null else doubleArrayOf(1 - dim, 1 - dim, 1 - dim * .5, dim * 1.5)
            Phase.DAWN -> doubleArrayOf(.92 - dim, .8 - dim, .84 - dim, .2)
            Phase.EVENING -> doubleArrayOf(.94 - dim, .78 - dim, .68 - dim, .22)
            Phase.NIGHT -> doubleArrayOf(.30, .32, .52, 1.0)
        }
        val flick = .82 + .12 * sin(t * 9) + .06 * sin(t * 23)
        val h0 = heat(fx)
        val fl = if (flash) .45 else 0.0
        val candle = Gift.CANDLE in has
        for (y in 0 until H) for (x in 0 until W) {
            val i = y * W + x
            val c = b.d[i]
            val key = c and 0xFFFFFF
            if (c ushr 24 == 1 && key == GLASS) { b.d[i] = e.d[i] or Buf.OPAQUE; continue }
            if ((p == null && fl == 0.0) || key in EMIT) continue
            val a0 = p?.get(0) ?: 1.0; val a1 = p?.get(1) ?: 1.0; val a2 = p?.get(2) ?: 1.0; val lw = p?.get(3) ?: 0.0
            var ll = max(0.0, 1 - hypot((x - LAMP[0]).toDouble(), (y - LAMP[1]) * 1.2) / 88); ll = q(ll * ll, 4, x, y) * lw
            var sl = max(0.0, 1 - hypot((x - FIRE[0]).toDouble(), (y - FIRE[1]) * 1.1) / 80); sl = q(sl * sl, 4, x, y) * lw * h0 * flick
            var cl = 0.0
            if (candle) { cl = max(0.0, 1 - hypot((x - CANDLE[0]).toDouble(), (y - CANDLE[1]).toDouble()) / 30); cl = q(cl * cl, 3, x, y) * lw * .8 * flick }
            b.d[i] = rgb((Buf.ch(c, 16) * (a0 + ll + sl * 1.1 + cl + fl)).toInt(), (Buf.ch(c, 8) * (a1 + ll * .78 + sl * .6 + cl * .7 + fl)).toInt(),
                (Buf.ch(c, 0) * (a2 + ll * .38 + sl * .2 + cl * .3 + fl * 1.1)).toInt())
        }
    }

    /** Adds light: an ellipse of [col] laid on top additively. */
    private fun Buf.glow(cx: Double, cy: Double, r: Int, col: Int) {
        val ry = max(1, (r * .8).roundToInt())
        for (yy in -ry..ry) {
            val w = (r * kotlin.math.sqrt(max(0.0, 1 - (yy * yy) / ((ry + .5) * (ry + .5))))).roundToInt()
            for (xx in -w..w) {
                val x = (cx + xx).roundToInt(); val y = (cy + yy).roundToInt()
                if (x !in 0 until this.w || y !in 0 until h) continue
                val c = d[y * this.w + x]
                d[y * this.w + x] = rgb(Buf.ch(c, 16) + Buf.ch(col, 16), Buf.ch(c, 8) + Buf.ch(col, 8), Buf.ch(c, 0) + Buf.ch(col, 0))
            }
        }
    }

    private fun Buf.sprite(rows: List<String>, col: Int, x: Int, y: Int, a: Float = 1f) {
        rows.forEachIndexed { j, r -> r.forEachIndexed { i, ch -> if (ch != '.') p(x + i, y + j, col, a) } }
    }

    fun pose(p: Pet, t: Double, listening: Boolean = false): PetPose {
        val l = m.lodgers.getValue(p)
        val h = hashI(p.name)
        val f = ((t * 2).toInt() + h % 4) % 4
        val sleep = l.cold || (!listening && ((m.phase == Phase.NIGHT && p != Pet.MOTH) || (p == Pet.DORMOUSE && l.fed)))
        return PetPose(f, ((t + h % 7) % 4.4) < .15, sleep, l.cold, l.fed)
    }

    fun draw(b: Buf, t: Double, fx: RoomFx) {
        hits.clear()
        b.copyFrom(bg)
        fire(b, t, fx)
        clock(b, t, fx.clock)
        val placed = ArrayList<Triple<Pet, IntArray, PetPose>>()
        fun put(p: Pet, at: IntArray) {
            val o = pose(p, t, fx.pile && fx.flare)
            val img = PetArt.draw(p, o)
            val dy = if (!o.sleep && o.f == 2 && p != Pet.BULLFINCH) -1 else 0
            b.blit(img, at[0], at[1] + dy, fx.fade[p] ?: 1f)
            hits += intArrayOf(at[0] - 2, at[1] - 2, img.w + 4, img.h + 4) to Spot.Lodger(p)
            placed += Triple(p, at, o)
        }
        if (!fx.pile) for (p in Pet.entries) if (p in m.lodgers) put(p, at(p, false))
        b.blit(fg, 0, 0)
        if (fx.pile) for (p in PILE_ORDER) if (p in m.lodgers) put(p, at(p, true))
        drawSky(sky, t)
        light(b, sky, t, fx)

        // банки светятся сами
        val night = m.phase != Phase.DAY
        shelf.forEachIndexed { k, j ->
            val (x, y) = slot(k)
            hits += intArrayOf(x, y, JarArt.W, JarArt.H) to Spot.Jar(j.id)
            if (j.id == fx.flyId && fx.fly < 1) return@forEachIndexed
            if (night) b.glow(x + 4.0, y + 9.0, if (j.look.rare) 7 else 5, mix(j.look.tint, rgb(0), if (j.look.rare) .55f else .72f))
            b.blit(JarArt.draw(j.look), x, y)
            // a slow glint breathing in the glass instead of pixels jumping
            val g = (sin(t * (if (j.look.rare) 1.6 else .9) + (j.id % 17) * .7) + 1) / 2
            b.p(x + 3, y + 7, rgb(0xFFFFFF), (g * g * if (j.look.rare) .9 else .55).toFloat())
        }
        if (Gift.CANDLE in has) { val fl = (t * 6).toInt() % 2; b.px(57, 93, rgb(0xFFCF5A)); b.px(57, 92 - fl, rgb(0xFFF2B0)); if (fl == 1) b.px(57, 91, rgb(0xFFCF5A)) }
        if (Gift.GARLAND in has) for (i in 0 until 13) {
            val x = (5 + i * 4.6).toInt(); val y = 8 + (sin(i / 12.0 * PI) * 4).roundToInt()
            if ((i + (t * 1.5).toInt()) % 4 != 0) b.px(x, y, rgb(intArrayOf(0xFF6B5B, 0xFFD36B, 0x7BE0A0, 0x6BB8FF)[i % 4].toLong()))
            if (night) b.glow(x.toDouble(), y.toDouble(), 2, rgb(0x2A2010))
        }
        for ((p, at, o) in placed) {
            val img = PetArt.draw(p, o)
            if (o.fed && night) b.glow(at[0] + img.w / 2.0, at[1] + img.h / 2.0, img.w / 2 + 2, rgb(0x241808))
            if (o.sleep) {
                val ph = (t * .5 + (hashI(p.name) % 10) / 10.0) % 1.0
                b.sprite(ZED, if (o.cold) rgb(0x9AA6D8) else rgb(0xE8E0F8), at[0] + img.w - 2 + (ph * 3).toInt(), at[1] - 3 - (ph * 8).toInt(), (1 - ph).toFloat())
            }
            if (p == Pet.BULLFINCH && o.fed && !o.sleep && o.f == 3) b.sprite(NOTE, rgb(0xFFF0A0), at[0] - 4, at[1] - 2)
        }
        for ((p, age) in fx.hearts) {
            if (age !in 0.0..2.2 || p !in m.lodgers) continue
            val at = at(p, fx.pile); val w = PetArt.size(p)[0]
            b.sprite(HEART, rgb(0xFF5A6A), at[0] + w / 2 - 2, at[1] - 4 - (age / 2.2 * 16).toInt(), (1 - age / 2.2).toFloat())
        }
        // банка летит на полку
        val k = shelf.indexOfFirst { it.id == fx.flyId }
        if (k >= 0 && fx.fly < 1) {
            val e = fx.fly.coerceIn(0.0, 1.0)
            val (x1, y1) = slot(k)
            val x = 84 + (x1 - 84) * e; val y = 110 + (y1 - 110) * e - sin(e * PI) * 30
            b.glow(x + 4, y + 9, 8, mix(shelf[k].look.tint, rgb(0), .5f))
            b.blit(JarArt.draw(shelf[k].look), x.roundToInt(), y.roundToInt())
        }
        fx.landedAge?.let { a ->
            if (k >= 0 && a in 0.0..1.0) {
                val (x1, y1) = slot(k)
                for (i in 0 until 8) {
                    val hh = hashI("sp$i"); val vx = (hh % 60 - 30).toDouble(); val vy = -(hh / 60 % 50).toDouble()
                    b.px(x1 + 4 + vx * a, y1 + 6 + vy * a + 60 * a * a, rgb(0xFFF0A0))
                }
            }
        }
    }

    /** What lies under an art pixel: a lodger first, then a jar, then the pantry or the stove. */
    fun hit(x: Double, y: Double): Spot? {
        for ((r, s) in hits.asReversed()) if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]) return s
        if (x in 68.0..122.0 && y in 4.0..78.0) return Spot.Pantry
        if (x in 122.0..176.0 && y in 46.0..148.0) return Spot.Stove
        return null
    }
}
