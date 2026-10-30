package app.nooklings.ui.art

import app.nooklings.domain.Pet
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** How a lodger looks this frame: [f] is the 0..3 idle frame; cold ones sleep pale in a scarf, fed ones glow. */
data class PetPose(val f: Int = 0, val blink: Boolean = false, val sleep: Boolean = false, val cold: Boolean = false, val fed: Boolean = false)

/**
 * The six lodgers, drawn from shapes the way the room is: masks lit from the top left, a coloured
 * outline, and a few hand-placed pixels for eyes, noses and stripes.
 */
object PetArt {
    fun size(p: Pet): IntArray = when (p) {
        Pet.CAT -> intArrayOf(36, 24)
        Pet.DORMOUSE -> intArrayOf(24, 18)
        Pet.MOTH -> intArrayOf(24, 20)
        Pet.BULLFINCH -> intArrayOf(22, 22)
        Pet.HEDGEHOG -> intArrayOf(28, 22)
        Pet.RACCOON -> intArrayOf(26, 22)
    }

    private fun ink(p: Pet) = rgb(when (p) {
        Pet.CAT -> 0x3A1A0C; Pet.DORMOUSE -> 0x4A2414; Pet.MOTH -> 0x5A4636
        Pet.BULLFINCH -> 0x141418; Pet.HEDGEHOG -> 0x3A2414; Pet.RACCOON -> 0x1E1E24
    })

    private val cache = HashMap<Pair<Pet, PetPose>, Buf>()

    fun draw(p: Pet, o: PetPose): Buf = synchronized(cache) {
        cache.getOrPut(p to o) {
            val (w, h) = size(p)
            Buf(w, h).also { b ->
                when (p) {
                    Pet.CAT -> cat(b, o); Pet.DORMOUSE -> dormouse(b, o); Pet.MOTH -> moth(b, o)
                    Pet.BULLFINCH -> bullfinch(b, o); Pet.HEDGEHOG -> hedgehog(b, o); Pet.RACCOON -> raccoon(b, o)
                }
                b.outline(ink(p))
            }
        }
    }

    /** A portrait for cards: awake, fed, one pixel of air around. */
    fun portrait(p: Pet, dark: Boolean = false): Buf {
        val src = draw(p, PetPose(fed = true))
        val b = Buf(src.w + 2, src.h + 2)
        b.blit(src, 1, 1)
        if (dark) b.silhouette(rgb(0x1A0E08))
        return b
    }

    private val RED = rgb(0xC2402F)
    private val PINK = rgb(0xF0A0A0)
    private val NOSE = rgb(0xE8798A)
    private val WHITE = rgb(0xFFFFFF)

    private fun cat(b: Buf, o: PetPose) {
        val f = ramp(rgb(0xE08A34)); val br = o.f % 2; val hd = if (o.sleep) 2 else 0
        val tip = if (o.fed && o.f == 1) 16 else 19
        b.lit(f) {
            ell(21, 16 - br, 12, 6 + br, it)
            ell(9, 12 + hd, 7, 6, it)
            for (ax in intArrayOf(4, 13)) { px(ax, 3 + hd, it); rect(ax - 1, 4 + hd, 3, 1, it); rect(ax - 1, 5 + hd, 4, 1, it) }
            rect(14, 21, 18, 2, it); ell(32, tip, 2, 3, it)
            ell(5, 20, 3, 2, it); ell(11, 21, 3, 1, it)
        }
        for (sx in intArrayOf(17, 21, 25, 29)) b.line(sx, 11 - 2 * br + if (sx > 27) 1 else 0, sx + 1, 14 - br, f.d)
        b.px(8, 7 + hd, f.d); b.px(10, 7 + hd, f.d); b.px(9, 8 + hd, f.d)
        var x = 17; while (x < 31) { b.rect(x, 21, 2, 2, f.d); x += 4 }
        b.ell(8, 15 + hd, 3, 2, rgb(0xF6DCB0))
        b.px(4, 5 + hd, PINK); b.px(13, 5 + hd, PINK)
        if (o.sleep || o.blink) { b.rect(4, 12 + hd, 3, 1, rgb(0x3A1A0C)); b.rect(10, 12 + hd, 3, 1, rgb(0x3A1A0C)) }
        else for (ex in intArrayOf(4, 10)) { b.rect(ex, 10 + hd, 3, 3, rgb(0x8AC848)); b.rect(ex + 1, 10 + hd, 1, 3, rgb(0x1A1008)); b.px(ex, 10 + hd, rgb(0xE8FFD0)) }
        b.px(8, 14 + hd, NOSE)
        b.ell(5, 21, 2, 1, rgb(0xF6DCB0))
        if (o.cold) { b.chill(); b.scarf(13, 8 + hd, 3, 10, 15, 18 + hd, 2, 4, RED) }
    }

    private fun dormouse(b: Buf, o: PetPose) {
        val f = ramp(rgb(0xC98B4E))
        b.lit(f) { ell(5, 4, 3, 3, it); ell(18, 4, 3, 3, it); ell(12, 10, 8, 6, it); ell(6, 16, 2, 1, it); ell(18, 16, 2, 1, it) }
        b.ell(5, 4, 1, 1, PINK); b.ell(18, 4, 1, 1, PINK)
        b.ell(12, 13, 5, 2, rgb(0xF2DFBD)); b.rect(12, 5, 1, 3, f.d)
        val e = rgb(0x1D120A)
        if (o.sleep || o.blink) { b.rect(7, 10, 3, 1, e); b.rect(14, 10, 3, 1, e) }
        else { b.rect(7, 8, 3, 3, e); b.rect(14, 8, 3, 3, e); b.px(7, 8, WHITE); b.px(14, 8, WHITE) }
        b.px(12, 12, NOSE); b.px(6, 12, PINK); b.px(18, 12, PINK)
        b.rect(5, 16, 3, 1, rgb(0xF2DFBD)); b.rect(17, 16, 3, 1, rgb(0xF2DFBD))
        if (o.cold) { b.chill(); b.scarf(5, 14, 14, 2, 16, 16, 2, 2, RED) }
    }

    private fun moth(b: Buf, o: PetPose) {
        val wg = ramp(rgb(0xDCCBA4)); val body = ramp(rgb(0xF3ECDC))
        val fl = if (o.f % 2 == 1 && !o.sleep) 1 else 0
        b.lit(wg) { ell(6 + fl, 9, 5, 4 - fl, it); ell(18 - fl, 9, 5, 4 - fl, it); ell(7 + fl, 15, 3, 2, it); ell(17 - fl, 15, 3, 2, it) }
        for ((cx, s) in listOf(6 + fl to 1, 18 - fl to -1)) {
            b.ell(cx, 9, 1, 1, rgb(0x9A7A4A)); b.px(cx, 9, rgb(0xF6ECD0))
            b.px(cx - 3 * s, 7, wg.d); b.px(cx - 4 * s, 10, wg.d); b.px(cx + s, 12, wg.d)
        }
        b.lit(body) { ell(12, 12, 3, 6, it); ell(12, 5, 3, 3, it) }
        b.rect(10, 8, 5, 1, WHITE)
        var y = 11; while (y < 17) { b.px(11 + if (y % 4 != 0) 1 else 0, y, rgb(0xE0D6C0)); y += 2 }
        val ant = rgb(0x7A6450); val feather = rgb(0xA89070)
        b.line(11, 2, 8, 0, ant); b.line(13, 2, 16, 0, ant); b.px(9, 0, feather); b.px(10, 2, feather); b.px(15, 0, feather); b.px(14, 2, feather)
        val e = rgb(0x1D140C)
        if (o.sleep || o.blink) { b.rect(10, 5, 2, 1, e); b.rect(13, 5, 2, 1, e) }
        else { b.rect(10, 4, 2, 2, e); b.rect(13, 4, 2, 2, e); b.px(10, 4, WHITE); b.px(13, 4, WHITE) }
        b.ring(9, 3, 4, 4, rgb(0xC79A4A)); b.ring(12, 3, 4, 4, rgb(0xC79A4A))
        for (lx in intArrayOf(10, 14)) b.rect(lx, 18, 1, 2, ant)
        if (o.cold) { b.chill(); b.scarf(9, 8, 7, 2, 13, 10, 2, 3, RED) }
    }

    private fun bullfinch(b: Buf, o: PetPose) {
        val g = ramp(rgb(0x7D8592)); val k = ramp(rgb(0x2A2A34)); val r = ramp(rgb(0xE0533B))
        val sl = if (o.sleep) 1 else 0
        val hu = if (o.f % 4 == 2 && !o.sleep) -1 else 0
        val hy = 7 + sl * 2 + hu
        b.lit(k) { rect(15, 13, 6, 3, it); px(21, 14, it) }
        b.lit(g) { ell(11, 13, 7 + sl, 6 + sl, it); ell(8, hy, 4, 4, it) }
        b.ell(10, 15, 5, 4, r.b); b.ell(9, 14, 2, 1, r.l); b.rect(7, 19, 6, 1, r.d)
        b.ell(14, 12, 4, 3, k.b); b.rect(11, 12, 6, 1, rgb(0xE8E8F0)); b.rect(12, 13, 5, 1, rgb(0xB9C0CC))
        b.ell(8, hy - 2, 4, 2, k.b); b.ell(7, hy + 3, 2, 1, r.b); b.rect(4, hy, 2, 2, k.b)
        b.rect(2, hy, 2, 2, rgb(0x3A3A44)); b.px(2, hy, rgb(0x5A5A66))
        if (o.fed && o.f % 4 == 3 && !o.sleep) b.px(1, hy + 1, rgb(0x3A3A44))
        if (o.sleep || o.blink) b.rect(6, hy + 1, 2, 1, rgb(0x0C0C10)) else { b.px(7, hy + 1, rgb(0x0C0C10)); b.px(8, hy + 1, WHITE) }
        val leg = rgb(0x5A4A3A)
        b.line(10, 19, 10, 21, leg); b.line(13, 19, 13, 21, leg); b.px(9, 21, leg); b.px(12, 21, leg)
        if (o.cold) { b.chill(); b.scarf(5, hy + 3, 7, 2, 10, hy + 5, 2, 3, rgb(0x4F7AA3)) }
    }

    private fun hedgehog(b: Buf, o: PetPose) {
        val sp = ramp(rgb(0x7A5434)); val fc = ramp(rgb(0xECD3A6))
        val sn = if (o.f % 4 == 1 && !o.sleep) 1 else 0
        b.lit(sp) {
            ell(17, 12, 10, 8, it)
            var a = 150
            while (a <= 390) { val r = Math.toRadians(a.toDouble()); px((17 + cos(r) * 11.5).roundToInt(), (12 + sin(r) * 9.3).roundToInt(), it); a += 13 }
        }
        for (i in 0 until 46) {
            val x = 8 + hashI("e$i") % 19; val y = 4 + hashI("y$i") % 14
            if (b.filled(x, y) && b.filled(x + 1, y - 1)) { b.px(x, y, sp.dd); b.px(x + 1, y - 1, sp.l) }
        }
        b.lit(fc) { ell(8, 14, 6, 5, it); rect(2, 14, 5, 3, it) }
        b.rect(0, 14 - sn, 2, 2, rgb(0x2A1A10)); b.px(0, 14 - sn, rgb(0x6A5A4A))
        if (o.sleep || o.blink) b.rect(5, 12, 3, 1, rgb(0x1D120A)) else { b.rect(6, 11, 2, 2, rgb(0x1D120A)); b.px(6, 11, WHITE) }
        b.ell(10, 9, 1, 1, rgb(0xC9A070)); b.px(7, 15, PINK); b.px(8, 15, PINK)
        b.ell(6, 20, 2, 1, fc.d); b.ell(12, 20, 2, 1, fc.d)
        if (o.fed) { b.px(20, 2, rgb(0xD9622B)); b.rect(19, 3, 3, 1, rgb(0xD9622B)); b.px(20, 4, rgb(0xA8401C)) }
        if (o.cold) { b.chill(); b.scarf(8, 17, 8, 2, 13, 19, 2, 3, RED) }
    }

    private fun raccoon(b: Buf, o: PetPose) {
        val g = ramp(rgb(0x8D8F99))
        val tw = if (o.f % 4 == 3 && !o.sleep) 1 else 0
        b.lit(g) { ell(6, 4 + tw, 3, 3, it); ell(19, 4, 3, 3, it); ell(12, 11, 9, 7, it); ell(4, 19, 3, 2, it); ell(20, 19, 3, 2, it) }
        b.px(6, 4 + tw, rgb(0x3A3A42)); b.px(19, 4, rgb(0x3A3A42)); b.rect(12, 4, 1, 5, rgb(0x5A5C66))
        b.ell(8, 11, 3, 2, rgb(0x34343C)); b.ell(16, 11, 3, 2, rgb(0x34343C))
        b.rect(5, 8, 5, 1, rgb(0xF0F0F0)); b.rect(14, 8, 5, 1, rgb(0xF0F0F0))
        b.ell(12, 15, 4, 2, rgb(0xF0F0F0)); b.rect(11, 13, 3, 2, rgb(0x141418)); b.px(11, 13, rgb(0x5A5A66))
        if (o.sleep || o.blink) { b.rect(7, 11, 3, 1, rgb(0x9A9CA6)); b.rect(15, 11, 3, 1, rgb(0x9A9CA6)) }
        else { b.rect(8, 10, 2, 2, rgb(0x0C0C10)); b.rect(16, 10, 2, 2, rgb(0x0C0C10)); b.px(8, 10, WHITE); b.px(16, 10, WHITE) }
        b.ell(4, 19, 2, 1, rgb(0x3A3A42)); b.ell(20, 19, 2, 1, rgb(0x3A3A42)); b.px(3, 14, rgb(0xF0F0F0)); b.px(21, 14, rgb(0xF0F0F0))
        if (o.cold) { b.chill(); b.scarf(5, 16, 15, 2, 16, 18, 2, 3, RED) }
    }
}
