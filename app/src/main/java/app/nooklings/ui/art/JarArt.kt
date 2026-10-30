package app.nooklings.ui.art

import app.nooklings.data.Jar
import app.nooklings.domain.Tag

/** What a jar looks like: its colour, its kind (and the second kind of jam), and whether it's rare. */
data class JarLook(val tint: Int, val tag: Tag, val tag2: Tag? = null, val rare: Boolean = false)

fun Jar.look() = JarLook(tint, tag, tag2, rare)

/**
 * A jar, 9×14: glass, the caught moment filling it in its colour with a pattern of its kind
 * (drops of rain, a tea leaf, sparks of light, a page of text), and a gingham cloth over the lid.
 */
object JarArt {
    const val W = 9
    const val H = 14

    private val cache = HashMap<Triple<JarLook, Int, Int>, Buf>()

    fun draw(j: JarLook, f: Int = 0, fill: Float = 1f, lid: Boolean = true): Buf {
        val key = Triple(j, f, (fill * 100).toInt() * 2 + if (lid) 1 else 0)
        return synchronized(cache) { cache.getOrPut(key) { paint(j, f, fill, lid) } }
    }

    private val GLASS = rgb(0xCFE2E6)
    private val WHITE = rgb(0xFFFFFF)

    private fun paint(j: JarLook, f: Int, fill: Float, lid: Boolean): Buf {
        val b = Buf(W, H)
        val r = ramp(j.tint)
        b.rect(1, 4, 7, 9, GLASS); b.rect(2, 3, 5, 1, GLASS)
        val top = Math.round(13 - 7 * fill)
        if (fill > 0) {
            b.rect(1, top, 7, 13 - top, r.b); b.rect(1, top, 7, 1, r.l)
            j.tag2?.let { t2 -> val r2 = ramp(t2.color); val m = maxOf(top + 1, 10); b.rect(1, m, 7, 13 - m, r2.b); b.rect(1, m, 7, 1, r2.l) }
            val sp = mix(j.tint, WHITE, .7f)
            if (fill >= 1f) when (j.tag) {
                Tag.SKY -> { b.px(2 + f, 8, rgb(0xE8F4FF)); b.px(5, 10 - f, rgb(0xE8F4FF)); b.px(6 - f, 7, rgb(0xE8F4FF)) }
                Tag.WALK -> { b.px(3, 9, rgb(0xE8A22E)); b.px(5 + f, 11, rgb(0xD9622B)) }
                Tag.TEA -> { b.rect(1, 11, 7, 2, r.d); b.px(4 + f, 9 - f, rgb(0x5A7A2A)) }
                Tag.WARM -> { b.px(3 + f, 8, rgb(0xFFF4C0)); b.px(5, 10, rgb(0xFFF4C0)); b.px(6 - f, 11, sp) }
                Tag.QUIET -> b.px(4, 8 + f, WHITE)
                Tag.NEW -> { b.rect(2, 8, 4, 1, r.d); b.rect(3, 10, 3, 1, r.d); b.px(6, 7 + f, sp) }
                Tag.PEOPLE -> { b.px(2 + f * 3, 9, rgb(0xFFE060)); b.px(5 - f, 11, rgb(0xFFE060)) }
                Tag.HANDS -> { b.line(1, 11, 7, 8 + f, r.l); b.px(3, 12, r.l) }
            }
        }
        b.rect(2, 4, 1, 2, WHITE)
        var y = maxOf(top, 6); while (y < 12) { b.px(2, y, mix(j.tint, WHITE, .55f)); y += 2 }
        b.rect(1, 12, 7, 1, if (fill > 0) r.dd else rgb(0xA8C0C8))
        if (lid) {
            b.rect(1, 1, 7, 3, rgb(0xC2402F))
            for ((a, c) in listOf(2 to 1, 4 to 1, 6 to 1, 3 to 2, 5 to 2, 7 to 2)) b.px(a, c, rgb(0xF2E6C8))
            b.rect(0, 3, 9, 1, rgb(0xA8301F)); b.px(1, 3, rgb(0xE8D8A0)); b.px(7, 3, rgb(0xE8D8A0))
        }
        b.outline(rgb(0x2A160E))
        if (j.rare && f == 1) b.px(8, 1, WHITE)
        return b
    }
}
