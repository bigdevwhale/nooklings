package app.nooklings.domain

import app.nooklings.data.HOUR
import app.nooklings.data.Jar
import app.nooklings.data.Opening
import java.time.Instant
import java.time.ZoneId

/** A lodger now: when they came and how full they are (0..100). Nobody ever dies: a hungry one just sleeps in a scarf. */
data class Lodger(val pet: Pet, val since: Long, val food: Double) {
    val cold: Boolean get() = food < COLD
    val fed: Boolean get() = food >= FED

    companion object {
        const val COLD = 30.0
        const val FED = 70.0
    }
}

/**
 * The household is never stored: it is replayed from the jars and the openings, so it can't drift.
 * The cat and the dormouse live here from the start; everyone else comes with the first jar of their kind.
 */
object House {
    val FIRST = listOf(Pet.CAT, Pet.DORMOUSE)
    const val START = 60.0
    const val ARRIVE = 72.0
    const val FEED = 45.0
    const val OPEN = 8.0
    /** Fullness lost per hour (a jar lasts about two days); a third more in the cold months. */
    const val DECAY = 1.0

    fun lodgers(jars: List<Jar>, opens: List<Opening>, started: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): Map<Pet, Lodger> {
        val t0 = minOf(started, jars.minOfOrNull { it.at } ?: started)
        val food = LinkedHashMap<Pet, Double>()
        val since = HashMap<Pet, Long>()
        for (p in FIRST) { food[p] = START; since[p] = t0 }
        val events = (jars.map { it.at to it } + opens.map { it.at to it }).filter { it.first <= now }.sortedBy { it.first }
        var t = t0
        fun decay(to: Long) {
            if (to <= t) return
            val hours = (to - t).toDouble() / HOUR
            val rate = DECAY * if (winter(to, zone)) 1.3 else 1.0
            for (p in food.keys) food[p] = (food.getValue(p) - hours * rate).coerceAtLeast(0.0)
            t = to
        }
        for ((at, e) in events) {
            decay(at)
            when (e) {
                is Jar -> for (p in e.tags.map { it.pet }.distinct()) {
                    if (p !in food) { food[p] = ARRIVE; since[p] = at } else food[p] = (food.getValue(p) + FEED).coerceAtMost(100.0)
                }
                is Opening -> for (p in food.keys) food[p] = (food.getValue(p) + OPEN).coerceAtMost(100.0)
            }
        }
        decay(now)
        return Pet.entries.filter { it in food }.associateWith { Lodger(it, since.getValue(it), food.getValue(it)) }
    }

    fun winter(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone).monthValue in setOf(11, 12, 1, 2, 3)

    /** Everyone home and full in the evening: they pile up by the stove. */
    fun pile(lodgers: Map<Pet, Lodger>, phase: Phase) =
        (phase == Phase.EVENING || phase == Phase.NIGHT) && lodgers.size > 1 && lodgers.values.all { it.food >= 60 }
}
