package app.nooklings.data

import app.nooklings.domain.Tag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Jars with no recording of their own (light, quiet, walks, jam, the demo) still make a sound when
 * opened: rain, a kettle, the stove crackling, pages, voices, needles, steps, a clock in the quiet.
 * Nine seconds of mono PCM, made from noise through simple filters.
 */
object Synth {
    const val RATE = 22050

    /** An RBJ biquad: low-pass, high-pass or band-pass. */
    private class Biquad(type: Char, f: Double, q: Double) {
        private val b0: Double; private val b1: Double; private val b2: Double; private val a1: Double; private val a2: Double
        private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0
        init {
            val w = 2 * PI * f / RATE
            val al = sin(w) / (2 * q)
            val c = cos(w)
            val a0 = 1 + al
            when (type) {
                'l' -> { b0 = (1 - c) / 2 / a0; b1 = (1 - c) / a0; b2 = b0 }
                'h' -> { b0 = (1 + c) / 2 / a0; b1 = -(1 + c) / a0; b2 = b0 }
                else -> { b0 = al / a0; b1 = 0.0; b2 = -al / a0 }
            }
            a1 = -2 * c / a0; a2 = (1 - al) / a0
        }
        fun f(x: Double): Double { val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2; x2 = x1; x1 = x; y2 = y1; y1 = y; return y }
    }

    private class Mix(val n: Int, val rnd: Random) {
        val out = DoubleArray(n)
        private var brown = 0.0
        fun white() = rnd.nextDouble() * 2 - 1
        fun brown(): Double { brown = (brown + .02 * white()) / 1.02; return brown * 3.5 }

        /** Filtered noise all the way through; [gain] may change over time. */
        fun bed(type: Char, f: Double, q: Double, deep: Boolean = false, gain: (Int) -> Double) {
            val bq = Biquad(type, f, q)
            for (i in 0 until n) out[i] += bq.f(if (deep) brown() else white()) * gain(i)
        }

        /** A short burst of filtered noise: a raindrop, a crackle, a step, a tick. */
        fun click(t: Double, gain: Double, f: Double, len: Double) {
            val bq = Biquad('b', f, 1.5)
            val s = (t * RATE).toInt(); val m = (len * RATE * 5).toInt()
            for (k in 0 until m) { val i = s + k; if (i >= n) break; out[i] += bq.f(white()) * gain * exp(-k / (len * RATE / 3)) * 3 }
        }

        fun tone(t: Double, len: Double, gain: (Double) -> Double, freq: (Double) -> Double) {
            var ph = 0.0
            val s = (t * RATE).toInt(); val m = (len * RATE).toInt()
            for (k in 0 until m) { val i = s + k; if (i >= n) break; val tt = k.toDouble() / RATE; ph += 2 * PI * freq(tt) / RATE; out[i] += sin(ph) * gain(tt) }
        }
    }

    fun render(tags: List<Tag>, seconds: Double = 9.0, seed: Int = 7): ShortArray {
        val n = (seconds * RATE).toInt()
        val m = Mix(n, Random(seed))
        val r = m.rnd
        fun at() = r.nextDouble() * seconds
        for (tag in tags.distinct()) when (tag) {
            Tag.SKY -> { m.bed('b', 1400.0, .6) { .3 }; m.bed('l', 500.0, .5) { .2 }; repeat(140) { m.click(at(), .1 + r.nextDouble() * .2, 2500 + r.nextDouble() * 3000, .02) } }
            Tag.WALK -> { var t = .3; while (t < seconds) { m.click(t, .7, 180.0, .12); m.click(t + .01, .3, 2600.0, .08); t += .55 }; m.bed('h', 3000.0, .5) { .02 } }
            Tag.TEA -> {
                m.bed('l', 400.0, .5, deep = true) { .3 }
                repeat(60) { val t = r.nextDouble() * 3.5; val f = 300 + r.nextDouble() * 500; m.tone(t, .05, { k -> .04 * exp(-k * 60) }, { f }) }
                m.tone(3.0, 4.5, { k -> when { k < 2 -> .06 * k / 2; else -> .06 * (1 - (k - 2) / 2.5).coerceAtLeast(0.0) } }, { k -> 1600 + 700 * (k / 3).coerceAtMost(1.0) })
            }
            Tag.WARM -> { m.bed('l', 300.0, .5, deep = true) { .45 }; repeat(90) { m.click(at(), .1 + r.nextDouble() * .5, 1500 + r.nextDouble() * 3000, .015 + r.nextDouble() * .03) } }
            Tag.QUIET -> { m.bed('l', 200.0, .5, deep = true) { .1 }; var t = .5; while (t < seconds) { m.click(t, .25, 3500.0, .015); t += 1.0 } }
            Tag.NEW -> {
                var t = .4
                while (t < seconds) {
                    val s = t + r.nextDouble() * .4
                    val bq = Biquad('b', 3500.0, .6)
                    val i0 = (s * RATE).toInt()
                    for (k in 0 until (.45 * RATE).toInt()) { val i = i0 + k; if (i >= n) break; val e = if (k < .08 * RATE) k / (.08 * RATE) else exp(-(k - .08 * RATE) / (.08 * RATE)); m.out[i] += bq.f(m.white()) * .35 * e * 2 }
                    t += 1.6
                }
            }
            Tag.PEOPLE -> repeat(3) { v ->
                val targets = DoubleArray(n)
                var t = v * .3; var cur = 0.0
                while (t < seconds) { val next = t + .12 + r.nextDouble() * .16; val g = if (r.nextDouble() < .35) 0.0 else .5 + r.nextDouble() * .9; for (i in (t * RATE).toInt() until minOf(n, (next * RATE).toInt())) targets[i] = g; t = next }
                val k = 1 - exp(-1.0 / (.04 * RATE))
                m.bed('b', 300.0 + v * 260, 5.0) { i -> cur += (targets[i] - cur) * k; cur }
            }
            Tag.HANDS -> { m.bed('l', 250.0, .5, deep = true) { .15 }; var t = .3; while (t < seconds) { m.click(t, .4, 2800.0, .02); m.click(t + .05, .25, 3400.0, .015); t += .36 } }
        }
        // fade in and out, then keep it under full scale
        val peak = m.out.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
        val gain = minOf(1.0, .85 / peak)
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val env = minOf(1.0, t / 1.0, (seconds - t) / 1.5).coerceAtLeast(0.0)
            (m.out[i] * gain * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
    }
}
