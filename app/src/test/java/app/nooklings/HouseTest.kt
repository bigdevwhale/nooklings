package app.nooklings

import app.nooklings.data.DAY
import app.nooklings.data.HOUR
import app.nooklings.data.Jar
import app.nooklings.data.Opening
import app.nooklings.data.Steps
import app.nooklings.data.Synth
import app.nooklings.domain.Catch
import app.nooklings.domain.Dreams
import app.nooklings.domain.House
import app.nooklings.domain.Pet
import app.nooklings.domain.Phase
import app.nooklings.domain.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HouseTest {
    private val utc = ZoneOffset.UTC
    /** A day in July, so the cold months don't speed up hunger. */
    private val t0 = LocalDate.of(2026, 7, 1).atStartOfDay(utc).toInstant().toEpochMilli()

    private fun jar(tag: Tag, at: Long, tag2: Tag? = null) = Jar(tag = tag, tag2 = tag2, kind = Catch.SOUND, label = "", at = at)

    @Test fun catAndDormouseLiveHereFromTheStart() {
        val h = House.lodgers(emptyList(), emptyList(), t0, t0, utc)
        assertEquals(setOf(Pet.CAT, Pet.DORMOUSE), h.keys)
        assertEquals(House.START, h.getValue(Pet.CAT).food, 1e-9)
    }

    @Test fun hungerGrowsAndNobodyGoesBelowZero() {
        val h = House.lodgers(emptyList(), emptyList(), t0, t0 + 10 * HOUR, utc)
        assertEquals(House.START - 10 * House.DECAY, h.getValue(Pet.CAT).food, 1e-6)
        val later = House.lodgers(emptyList(), emptyList(), t0, t0 + 30 * DAY, utc)
        assertEquals(0.0, later.getValue(Pet.DORMOUSE).food, 1e-9)
        assertTrue(later.getValue(Pet.DORMOUSE).cold)
    }

    @Test fun aJarFeedsItsLodgerAndBringsNewOnes() {
        val h = House.lodgers(listOf(jar(Tag.TEA, t0 + HOUR), jar(Tag.SKY, t0 + 2 * HOUR)), emptyList(), t0, t0 + 2 * HOUR, utc)
        assertEquals(minOf(100.0, House.START - House.DECAY + House.FEED) - House.DECAY, h.getValue(Pet.CAT).food, 1e-6)
        assertEquals(House.ARRIVE, h.getValue(Pet.HEDGEHOG).food, 1e-6)
        assertEquals(t0 + 2 * HOUR, h.getValue(Pet.HEDGEHOG).since)
        assertFalse(Pet.MOTH in h)
    }

    @Test fun jamFeedsBothAndFoodIsCapped() {
        val jars = (0 until 5).map { jar(Tag.QUIET, t0 + it * 60_000L, tag2 = Tag.TEA) }
        val h = House.lodgers(jars, emptyList(), t0, t0 + 5 * 60_000L, utc)
        assertTrue(h.getValue(Pet.DORMOUSE).food <= 100.0)
        assertTrue(h.getValue(Pet.CAT).fed)
    }

    @Test fun openingWarmsEveryone() {
        val now = t0 + HOUR
        val a = House.lodgers(emptyList(), emptyList(), t0, now, utc)
        val b = House.lodgers(emptyList(), listOf(Opening(jar = 1, at = now)), t0, now, utc)
        assertEquals(House.OPEN, b.getValue(Pet.CAT).food - a.getValue(Pet.CAT).food, 1e-6)
    }

    @Test fun winterIsHungrier() {
        val jan = LocalDate.of(2026, 1, 10).atStartOfDay(utc).toInstant().toEpochMilli()
        val h = House.lodgers(emptyList(), emptyList(), jan, jan + 10 * HOUR, utc)
        assertEquals(House.START - 10 * House.DECAY * 1.3, h.getValue(Pet.CAT).food, 1e-6)
    }

    @Test fun pileOnlyInTheEveningWhenAllAreFull() {
        val jars = listOf(jar(Tag.TEA, t0), jar(Tag.QUIET, t0))
        val h = House.lodgers(jars, emptyList(), t0, t0 + HOUR, utc)
        assertTrue(House.pile(h, Phase.EVENING))
        assertFalse(House.pile(h, Phase.DAY))
        assertFalse(House.pile(House.lodgers(emptyList(), emptyList(), t0, t0 + 2 * DAY, utc), Phase.NIGHT))
    }

    @Test fun dreamsRoundTripAndSkipTheLastDreamer() {
        for (p in Pet.entries) for (d in Dreams.of(p)) assertEquals(d, Dreams.byKey(d.key))
        assertNull(Dreams.byKey("nonsense"))
        val d = Dreams.roll(listOf(Pet.CAT, Pet.MOTH), Pet.CAT, .1, .5)
        assertEquals(Pet.MOTH, d?.pet)
        for (p in Pet.entries) assertTrue(Dreams.of(p).all { it.tag in p.eats })
    }

    @Test fun stepsMakeAWalkOnlyWhenThereAreEnough() {
        val now = t0
        assertNull(Steps.judge(1000, -1, 0, now).walk)
        assertEquals(1000L, Steps.judge(1000, -1, 0, now).base)
        assertNull(Steps.judge(2000, 1000, now - HOUR, now).walk)
        assertEquals(3000, Steps.judge(4000, 1000, now - HOUR, now).walk)
        // after a reboot the counter starts from zero again
        assertEquals(50L, Steps.judge(50, 9000, now - HOUR, now).base)
        // a slow trickle over the window doesn't count as a walk
        val slid = Steps.judge(2000, 1000, now - 4 * HOUR, now)
        assertNull(slid.walk); assertEquals(2000L, slid.base)
    }

    @Test fun everyKindHasASound() {
        for (t in Tag.entries) {
            val pcm = Synth.render(listOf(t), seconds = 3.0)
            assertNotNull(pcm)
            assertTrue("$t is silent", pcm.any { kotlin.math.abs(it.toInt()) > 1000 })
        }
    }
}
