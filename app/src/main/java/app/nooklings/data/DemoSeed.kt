package app.nooklings.data

import android.content.Context
import app.nooklings.AppContainer
import app.nooklings.domain.Catch
import app.nooklings.domain.Dreams
import app.nooklings.domain.Gift
import app.nooklings.domain.Pet
import app.nooklings.domain.Place
import app.nooklings.domain.Tag
import app.nooklings.domain.Wx
import app.nooklings.ru

/** The demo build's pantry: three weeks of caught moments, every lodger home and a few gifts in the room. */
object DemoSeed {
    private class J(val tag: Tag, val kind: Catch, val ru: String, val en: String, val daysAgo: Double, val wx: Wx, val temp: Int,
                    val tag2: Tag? = null, val color: Int = 0, val steps: Int = 0, val rare: Boolean = false)

    private val JARS = listOf(
        J(Tag.TEA, Catch.SOUND, "Чай из заварника", "Tea from the pot", 21.0, Wx.RAIN, 9),
        J(Tag.WARM, Catch.LIGHT, "Лампа над столом", "The lamp over the table", 19.0, Wx.OVERCAST, 8, color = 0xF2B45A),
        J(Tag.QUIET, Catch.QUIET, "Тишина в воскресенье", "A quiet Sunday", 17.0, Wx.CLEAR, 7),
        J(Tag.SKY, Catch.SOUND, "Дождь по зонту", "Rain on the umbrella", 15.0, Wx.RAIN, 8),
        J(Tag.WALK, Catch.WALK, "Листья в парке", "Leaves in the park", 3.5, Wx.CLOUDY, 6, steps = 6240),
        J(Tag.NEW, Catch.SOUND, "Книжный на Литейном", "The bookshop on the corner", 2.6, Wx.DRIZZLE, 7),
        J(Tag.TEA, Catch.SOUND, "Свисток чайника", "The kettle whistling", 9.0, Wx.CLEAR, 5),
        J(Tag.PEOPLE, Catch.SOUND, "Смех на кухне", "Laughing in the kitchen", 8.0, Wx.CLEAR, 5),
        J(Tag.HANDS, Catch.SOUND, "Пирог с бабушкой", "Baking with grandma", 6.0, Wx.OVERCAST, 4),
        J(Tag.WARM, Catch.LIGHT, "Закат на кухне", "Sunset in the kitchen", 5.0, Wx.CLEAR, 4, color = 0xF08A5A),
        J(Tag.QUIET, Catch.QUIET, "Утро, все спят", "Morning, everyone asleep", 0.7, Wx.FOG, 3),
        J(Tag.SKY, Catch.SOUND, "Первый снег", "First snow", 1.4, Wx.SNOW, -1, rare = true),
        J(Tag.HANDS, Catch.SOUND, "Связала один ряд", "One row of the scarf", 0.9, Wx.OVERCAST, 1),
        J(Tag.PEOPLE, Catch.SOUND, "Звонок маме", "Calling mum", 0.5, Wx.CLEAR, 2),
        J(Tag.SKY, Catch.BREW, "Варенье: дождь и чай", "Jam: rain and tea", 0.8, Wx.CLEAR, 2, tag2 = Tag.TEA, color = 0xA2924A),
        J(Tag.WARM, Catch.LIGHT, "Гирлянда на окне", "Lights in the window", 0.3, Wx.CLEAR, 1, color = 0xF6C86A),
    )

    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        if (c.jars.count() > 0) return
        val now = System.currentTimeMillis()
        val ru = ctx.ru
        val place = if (ru) "Москва" else "Moscow"
        c.jars.insertAll(JARS.map {
            Jar(tag = it.tag, tag2 = it.tag2, kind = it.kind, label = if (ru) it.ru else it.en, at = now - (it.daysAgo * DAY).toLong(),
                wx = it.wx, temp = it.temp, place = place, color = it.color, steps = it.steps, rare = it.rare)
        })
        c.settings.update {
            it.copy(onboarded = true, started = now - 30 * DAY, place = Place(place, if (ru) "Россия" else "Russia", 55.75, 37.62),
                dream = Dreams.of(Pet.MOTH)[0].key, dreamAt = now - 2 * HOUR,
                gifts = setOf(Gift.RUG, Gift.CANDLE, Gift.ROWAN, Gift.GARLAND, Gift.LEAF))
        }
    }
}
