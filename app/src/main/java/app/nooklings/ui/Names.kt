package app.nooklings.ui

import android.content.Context
import app.nooklings.R
import app.nooklings.domain.Gift
import app.nooklings.domain.Lodger
import app.nooklings.domain.Pet
import app.nooklings.domain.Phase
import app.nooklings.domain.Tag
import app.nooklings.domain.Wx

/** String resources for the things of the house. */
object Names {
    fun pet(p: Pet) = when (p) {
        Pet.CAT -> R.string.pet_cat; Pet.DORMOUSE -> R.string.pet_dormouse; Pet.MOTH -> R.string.pet_moth
        Pet.BULLFINCH -> R.string.pet_bullfinch; Pet.HEDGEHOG -> R.string.pet_hedgehog; Pet.RACCOON -> R.string.pet_raccoon
    }

    /** Whom a jar feeds: «Кота», "the cat". */
    fun acc(p: Pet) = when (p) {
        Pet.CAT -> R.string.acc_cat; Pet.DORMOUSE -> R.string.acc_dormouse; Pet.MOTH -> R.string.acc_moth
        Pet.BULLFINCH -> R.string.acc_bullfinch; Pet.HEDGEHOG -> R.string.acc_hedgehog; Pet.RACCOON -> R.string.acc_raccoon
    }

    /** Whom a gift is from: «Кота», "the cat". */
    fun gen(p: Pet) = when (p) {
        Pet.CAT -> R.string.gen_cat; Pet.DORMOUSE -> R.string.gen_dormouse; Pet.MOTH -> R.string.gen_moth
        Pet.BULLFINCH -> R.string.gen_bullfinch; Pet.HEDGEHOG -> R.string.gen_hedgehog; Pet.RACCOON -> R.string.gen_raccoon
    }

    fun home(p: Pet) = when (p) {
        Pet.CAT -> R.string.home_cat; Pet.DORMOUSE -> R.string.home_dormouse; Pet.MOTH -> R.string.home_moth
        Pet.BULLFINCH -> R.string.home_bullfinch; Pet.HEDGEHOG -> R.string.home_hedgehog; Pet.RACCOON -> R.string.home_raccoon
    }

    fun arrive(p: Pet) = when (p) {
        Pet.CAT -> R.string.arrive_cat; Pet.DORMOUSE -> R.string.arrive_dormouse; Pet.MOTH -> R.string.arrive_moth
        Pet.BULLFINCH -> R.string.arrive_bullfinch; Pet.HEDGEHOG -> R.string.arrive_hedgehog; Pet.RACCOON -> R.string.arrive_raccoon
    }

    /** What a lodger's state looks like: cold and asleep, so-so, or full. */
    fun say(l: Lodger) = when (l.pet) {
        Pet.CAT -> if (l.cold) R.string.cold_cat else if (l.fed) R.string.full_cat else R.string.mid_cat
        Pet.DORMOUSE -> if (l.cold) R.string.cold_dormouse else if (l.fed) R.string.full_dormouse else R.string.mid_dormouse
        Pet.MOTH -> if (l.cold) R.string.cold_moth else if (l.fed) R.string.full_moth else R.string.mid_moth
        Pet.BULLFINCH -> if (l.cold) R.string.cold_bullfinch else if (l.fed) R.string.full_bullfinch else R.string.mid_bullfinch
        Pet.HEDGEHOG -> if (l.cold) R.string.cold_hedgehog else if (l.fed) R.string.full_hedgehog else R.string.mid_hedgehog
        Pet.RACCOON -> if (l.cold) R.string.cold_raccoon else if (l.fed) R.string.full_raccoon else R.string.mid_raccoon
    }

    fun tag(t: Tag) = when (t) {
        Tag.SKY -> R.string.tag_sky; Tag.WALK -> R.string.tag_walk; Tag.TEA -> R.string.tag_tea; Tag.WARM -> R.string.tag_warm
        Tag.QUIET -> R.string.tag_quiet; Tag.NEW -> R.string.tag_new; Tag.PEOPLE -> R.string.tag_people; Tag.HANDS -> R.string.tag_hands
    }

    fun tagHint(t: Tag) = when (t) {
        Tag.SKY -> R.string.tag_sky_d; Tag.WALK -> R.string.tag_walk_d; Tag.TEA -> R.string.tag_tea_d; Tag.WARM -> R.string.tag_warm_d
        Tag.QUIET -> R.string.tag_quiet_d; Tag.NEW -> R.string.tag_new_d; Tag.PEOPLE -> R.string.tag_people_d; Tag.HANDS -> R.string.tag_hands_d
    }

    /** The label a jar gets unless it's renamed. */
    fun label(t: Tag) = when (t) {
        Tag.SKY -> R.string.label_sky; Tag.WALK -> R.string.label_walk; Tag.TEA -> R.string.label_tea; Tag.WARM -> R.string.label_warm
        Tag.QUIET -> R.string.label_quiet; Tag.NEW -> R.string.label_new; Tag.PEOPLE -> R.string.label_people; Tag.HANDS -> R.string.label_hands
    }

    fun gift(g: Gift) = when (g) {
        Gift.RUG -> R.string.gift_rug; Gift.CANDLE -> R.string.gift_candle; Gift.POSTCARD -> R.string.gift_postcard
        Gift.ROWAN -> R.string.gift_rowan; Gift.LEAF -> R.string.gift_leaf; Gift.GARLAND -> R.string.gift_garland
    }

    fun wx(w: Wx) = when (w) {
        Wx.CLEAR -> R.string.wx_clear; Wx.CLOUDY -> R.string.wx_cloudy; Wx.OVERCAST -> R.string.wx_overcast; Wx.DRIZZLE -> R.string.wx_drizzle
        Wx.RAIN -> R.string.wx_rain; Wx.SNOW -> R.string.wx_snow; Wx.FOG -> R.string.wx_fog; Wx.STORM -> R.string.wx_storm
    }

    fun phase(p: Phase) = when (p) { Phase.NIGHT -> R.string.ph_night; Phase.DAWN -> R.string.ph_dawn; Phase.DAY -> R.string.ph_day; Phase.EVENING -> R.string.ph_evening }
}

/** «3 дня», "3 days". */
object Plural {
    fun ru(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10; val m100 = n % 100
        val w = when { m10 == 1 && m100 != 11 -> one; m10 in 2..4 && m100 !in 12..14 -> few; else -> many }
        return "$n $w"
    }

    private fun en(n: Int, w: String) = "$n $w" + if (n == 1) "" else "s"

    fun days(n: Int, ru: Boolean) = if (ru) ru(n, "день", "дня", "дней") else en(n, "day")
    fun jars(n: Int, ru: Boolean) = if (ru) ru(n, "банка", "банки", "банок") else en(n, "jar")
    fun minutes(n: Int, ru: Boolean) = if (ru) ru(n, "минуту", "минуты", "минут") else en(n, "minute")
    fun seconds(n: Int, ru: Boolean) = if (ru) ru(n, "секунду", "секунды", "секунд") else en(n, "second")
}

fun Context.str(id: Int, vararg a: Any): String = getString(id, *a)
