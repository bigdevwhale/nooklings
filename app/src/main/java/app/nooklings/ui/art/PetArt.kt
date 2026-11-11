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
        Pet.BULLFINCH -> 0x25232E; Pet.HEDGEHOG -> 0x3A2414; Pet.RACCOON -> 0x30303B
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

    /**
     * The idle frame at [t] seconds: a slow breath (out, then in) of a few seconds instead of frames
     * flicking twice a second, and every third breath a short flourish (a tail, an ear, a head up).
     */
    fun idle(t: Double, seed: Int): Int {
        val s = seed and 0x7FFFFFFF
        val c = t / (3.0 + (s % 5) * .25) + (s % 97) / 97.0
        val n = kotlin.math.floor(c).toInt(); val ph = c - n
        return when {
            ph < .45 -> 0
            (n + s) % 3 == 0 && ph in .78..0.9 -> if ((n / 3 + s) % 2 == 0) 3 else 2
            else -> 1
        }
    }

    /** A portrait for cards: awake, fed, one pixel of air around. */
    fun portrait(p: Pet, dark: Boolean = false): Buf {
        val src = draw(p, PetPose(fed = true))
        val b = Buf(src.w + 2, src.h + 2)
        b.blit(src, 1, 1)
        if (dark) b.silhouette(rgb(0x0C0E20))
        return b
    }

    private val RED = rgb(0xC2402F)
    private val PINK = rgb(0xF0A0A0)
    private val NOSE = rgb(0xE8798A)
    private val WHITE = rgb(0xFFFFFF)

    private fun cat(b: Buf, o: PetPose) {
        val fur = ramp(rgb(0xDF913E)); val cream = rgb(0xFFE1AF)
        val nap = o.sleep || o.cold; val dy = if (nap) 2 else 0
        val breath = if (!nap && o.f % 2 == 1) 1 else 0
        val stripe = rgb(0xAA602F); val eye = rgb(0x493020)
        b.lit(fur) { ell(23, 16 - breath, 10, 5 + breath, it); ell(9, 12 + dy, 7, 6, it)
            rect(4, 5 + dy, 3, 5, it); rect(3, 5 + dy, 2, 3, it); px(3, 4 + dy, it)
            rect(12, 5 + dy, 3, 5, it); rect(14, 5 + dy, 2, 3, it); px(15, 4 + dy, it)
            ell(7, 20, 4, 2, it); ell(15, 21, 4, 1, it) }
        b.line(4, 6 + dy, 5, 8 + dy, PINK); b.line(14, 8 + dy, 15, 6 + dy, PINK)
        for (x in intArrayOf(19, 24, 29)) { b.rect(x, 11 - breath, 2, 3, stripe); b.px(x + 1, 14 - breath, stripe) }
        b.rect(8, 7 + dy, 2, 2, stripe); b.rect(6, 8 + dy, 1, 2, stripe); b.rect(11, 8 + dy, 1, 2, stripe)
        b.ell(8, 16 + dy, 4, 2, cream)
        if (nap || o.blink) { b.line(4, 13 + dy, 6, 14 + dy, eye); b.line(11, 14 + dy, 13, 13 + dy, eye) }
        else for (x in intArrayOf(4, 11)) { b.rect(x, 11, 3, 3, rgb(0x80AF52)); b.rect(x + 1, 11, 1, 3, eye); b.px(x, 11, cream) }
        b.rect(8, 15 + dy, 2, 1, NOSE); b.px(9, 16 + dy, stripe)
        if (o.fed && !nap) { b.px(8, 17, stripe); b.px(10, 17, stripe) }
        b.rect(4, 20, 5, 2, cream); b.rect(6, 21, 1, 1, fur.d)
        // The tail wraps around the loaf, with a rounded, animated tip.
        val ty = if (!nap && o.f == 3) 18 else 19
        b.lit(fur) { ell(26, 20, 7, 2, it); ell(32, ty, 2, 2, it) }
        b.rect(23, 19, 2, 3, stripe); b.rect(28, 19, 2, 3, stripe)
        b.rect(18, 21, 4, 1, cream)
        if (o.cold) { b.chill(); b.scarf(14, 12, 3, 8, 16, 19, 2, 3, RED) }
    }

    private fun dormouse(b: Buf, o: PetPose) {
        val fur = ramp(rgb(0xC68D58)); val cream = rgb(0xFFE6BB); val dark = rgb(0x493025)
        val nap = o.sleep || o.cold; val dy = if (nap) 1 else 0
        val br = if (!nap && o.f % 2 == 1) 1 else 0
        b.lit(fur) { ell(12, 11, 8, 5, it); ell(5, 4 + dy, 3, 3, it); ell(18, 4 + dy, 3, 3, it)
            ell(11, 9 + dy - br, 7, 5, it); ell(6, 16, 2, 1, it); ell(17, 16, 2, 1, it) }
        b.ell(5, 4 + dy, 2, 2, rgb(0xEBA79A)); b.ell(18, 4 + dy, 2, 2, rgb(0xEBA79A))
        b.ell(11, 13 + dy, 5, 2, cream); b.rect(9, 5 + dy, 3, 1, fur.l)
        if (nap || o.blink) { b.line(6, 10 + dy, 8, 11 + dy, dark); b.line(14, 11 + dy, 16, 10 + dy, dark) }
        else for (x in intArrayOf(6, 14)) { b.ell(x + 1, 9, 1, 1, dark); b.px(x, 8, cream) }
        b.rect(10, 12 + dy, 2, 1, NOSE); b.px(11, 13 + dy, dark)
        b.rect(5, 12 + dy, 2, 1, PINK); b.rect(16, 12 + dy, 2, 1, PINK)
        b.rect(5, 16, 3, 1, cream); b.rect(16, 16, 3, 1, cream)
        if (o.fed && !nap) { b.rect(10, 14, 2, 1, WHITE); b.ell(7, 14, 1, 1, cream); b.ell(15, 14, 1, 1, cream) }
        if (o.cold) { b.chill(); b.scarf(5, 14, 14, 2, 16, 16, 2, 2, RED) }
    }

    private fun moth(b: Buf, o: PetPose) {
        val wing = ramp(rgb(0xDCC797)); val fluff = ramp(rgb(0xF5E8CB)); val gold = rgb(0xA67B44)
        val nap = o.sleep || o.cold; val fl = if (nap || o.f % 2 == 1) 1 else 0
        val hy = if (nap) 1 else 0
        b.lit(wing) { ell(6 + fl, 9, 5 - fl, 4, it); ell(18 - fl, 9, 5 - fl, 4, it)
            ell(7 + fl, 14, 3, 3, it); ell(17 - fl, 14, 3, 3, it) }
        for (x in intArrayOf(6 + fl, 18 - fl)) {
            b.ell(x, 9, 2, 2, gold); b.ell(x, 8, 1, 1, rgb(0xF9E8BC))
            b.line(x - 1, 13, x, 15, wing.d)
        }
        b.lit(fluff) { ell(12, 13, 3, 5, it); ell(12, 6 + hy, 4, 3, it); rect(8, 8, 9, 2, it) }
        b.rect(10, 10, 2, 5, rgb(0xFFF3DB)); b.rect(11, 16, 2, 1, fluff.d)
        val ant = rgb(0x917554)
        b.line(10, 3 + hy, 8, 1 + hy, ant); b.rect(6, hy, 3, 2, wing.l)
        b.line(14, 3 + hy, 16, 1 + hy, ant); b.rect(16, hy, 3, 2, wing.l)
        val e = rgb(0x554132)
        // Stepped corners make the spectacles round at native resolution.
        for (x in intArrayOf(8, 13)) {
            b.rect(x + 1, 4 + hy, 3, 1, gold); b.rect(x + 1, 8 + hy, 3, 1, gold)
            b.rect(x, 5 + hy, 1, 3, gold); b.rect(x + 4, 5 + hy, 1, 3, gold)
            if (nap || o.blink) b.rect(x + 1, 6 + hy, 3, 1, e)
            else { b.rect(x + 2, 5, 2, 2, e); b.px(x + 2, 5, WHITE) }
        }
        b.px(12, 6 + hy, gold); b.rect(11, 9 + hy, 3, 1, if (o.fed) PINK else fluff.l)
        b.line(10, 17, 9, 18, ant); b.line(14, 17, 15, 18, ant)
        if (o.cold) { b.chill(); b.scarf(9, 10, 7, 2, 13, 12, 2, 3, RED) }
    }

    private fun bullfinch(b: Buf, o: PetPose) {
        val grey = ramp(rgb(0x92939F)); val black = ramp(rgb(0x34333E)); val red = ramp(rgb(0xDF6550))
        val nap = o.sleep || o.cold; val hy = if (nap) 9 else 6 + if (o.f == 2) -1 else 0
        val leg = rgb(0x80614A)
        b.line(9, 18, 9, 20, leg); b.rect(7, 21, 4, 1, leg)
        b.line(13, 18, 13, 20, leg); b.rect(12, 21, 3, 1, leg)
        b.lit(black) { rect(16, 14, 4, 4, it); rect(19, 16, 2, 2, it) }
        b.lit(grey) { ell(11, 13, 7, 6, it) }
        b.lit(red) { ell(9, 14, 5, 5, it); ell(6, 11, 2, 3, it) }
        b.ell(8, 13, 2, 2, rgb(0xF28568))
        b.lit(black) { ell(15, 12, 3, 4, it) }
        b.line(13, 14, 16, 16, grey.d); b.rect(12, 11, 6, 1, rgb(0xE7DCCD))
        b.lit(black) { ell(7, hy, 4, 3, it); rect(3, hy, 3, 3, it) }
        b.ell(8, hy + 3, 2, 1, grey.l)
        b.rect(1, hy + 1, 3, 2, rgb(0x6C5B50)); b.rect(1, hy + 1, 2, 1, rgb(0xB49C7E))
        if (nap || o.blink) b.rect(5, hy + 1, 3, 1, grey.l)
        else { b.rect(5, hy, 2, 2, rgb(0x201F29)); b.px(5, hy, WHITE) }
        if (o.fed && !nap) b.rect(4, hy + 3, 2, 1, red.l)
        if (o.cold) { b.chill(); b.scarf(4, hy + 3, 8, 2, 10, hy + 5, 2, 3, rgb(0x5B8B9B)) }
    }

    private fun hedgehog(b: Buf, o: PetPose) {
        val sp = ramp(rgb(0x926744)); val face = ramp(rgb(0xF0D6AA)); val dark = rgb(0x493124)
        val nap = o.sleep || o.cold; val dy = if (nap) 1 else 0
        b.lit(sp) { ell(17, 12, 9, 7, it)
            for (x in 10..22 step 3) { rect(x, 4, 2, 4, it); rect(x + 1, 3 + (x % 2), 1, 2, it) }
            rect(25, 9, 2, 7, it) }
        // Ordered little quills follow the dome; no random speckling.
        for (y in 7..16 step 3) for (x in 12..24 step 4) {
            val qx = x + if (y % 2 == 0) 1 else 0
            if (b.filled(qx + 1, y - 1) && b.filled(qx + 1, y + 2)) {
                b.line(qx, y, qx + 1, y + 2, sp.dd); b.line(qx - 1, y - 1, qx, y + 1, sp.l)
            }
        }
        b.lit(face) { ell(8, 14 + dy, 5, 4, it); rect(2, 14 + dy, 5, 3, it); ell(7, 20, 2, 1, it); ell(15, 20, 2, 1, it) }
        val ear = if (!nap && o.f == 3) 1 else 0
        b.lit(sp) { ell(11, 10 + dy - ear, 2, 2, it) }; b.rect(10, 9 + dy - ear, 2, 2, rgb(0xD6A57D))
        val sniff = if (!nap && o.f % 2 == 1) 1 else 0
        b.rect(1, 14 + dy - sniff, 2, 2, dark); b.px(1, 14 + dy - sniff, sp.l)
        if (nap || o.blink) b.rect(5, 13 + dy, 3, 1, dark)
        else { b.rect(6, 12, 2, 2, dark); b.px(6, 12, WHITE) }
        b.rect(7, 16 + dy, 2, 1, PINK)
        if (!nap && o.f == 1) b.rect(3, 17, 2, 1, face.d)
        if (o.fed) { b.line(19, 5, 20, 2, dark); b.ell(21, 3, 2, 1, rgb(0x8FAD5D)); b.ell(19, 5, 1, 1, rgb(0xD16A45)) }
        if (o.cold) { b.chill(); b.scarf(8, 17, 8, 2, 13, 19, 2, 3, RED) }
    }

    private fun raccoon(b: Buf, o: PetPose) {
        val fur = ramp(rgb(0x99949A)); val mask = rgb(0x46414C); val cream = rgb(0xF3E8D9)
        val nap = o.sleep || o.cold; val dy = if (nap) 2 else 0
        val tw = if (!nap && o.f % 2 == 1) 1 else 0
        // A curled striped tail peeks around the right haunch.
        b.lit(fur) { ell(20, 16 - tw, 4, 4, it) }
        b.rect(21, 13 - tw, 3, 2, mask); b.rect(20, 17 - tw, 4, 2, mask)
        b.lit(fur) { ell(11, 15, 7, 5, it); ell(6, 4 + dy, 3, 3, it); ell(17, 4 + dy, 3, 3, it)
            ell(11, 10 + dy, 8, 5, it); ell(5, 19, 2, 1, it); ell(16, 19, 2, 1, it) }
        b.rect(5, 3 + dy, 2, 3, mask); b.rect(17, 3 + dy, 2, 3, mask)
        b.ell(11, 16, 4, 3, cream)
        b.line(4, 8 + dy, 9, 7 + dy, cream); b.line(13, 7 + dy, 18, 8 + dy, cream)
        b.ell(7, 10 + dy, 3, 2, mask); b.ell(15, 10 + dy, 3, 2, mask)
        b.rect(10, 6 + dy, 2, 5, cream)
        b.ell(11, 13 + dy, 4, 2, cream); b.rect(10, 12 + dy, 3, 1, mask); b.px(11, 13 + dy, mask)
        if (nap || o.blink) { b.rect(5, 10 + dy, 3, 1, fur.l); b.rect(14, 10 + dy, 3, 1, fur.l) }
        else for (x in intArrayOf(6, 14)) { b.rect(x, 9, 2, 2, rgb(0x2F2934)); b.px(x, 9, WHITE) }
        b.ell(5, 19, 2, 1, mask); b.ell(16, 19, 2, 1, mask)
        b.rect(4, 18, 2, 1, fur.l); b.rect(15, 18, 2, 1, fur.l)
        if (o.fed && !nap) { b.ell(8, 16, 1, 1, fur.l); b.ell(14, 16, 1, 1, fur.l) }
        if (!nap && o.f == 3) b.rect(17, 5, 2, 1, fur.l)
        if (o.cold) { b.chill(); b.scarf(4, 16, 15, 2, 16, 18, 2, 3, RED) }
    }
}