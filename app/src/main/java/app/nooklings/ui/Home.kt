package app.nooklings.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import app.nooklings.data.DAY
import app.nooklings.data.Jar
import app.nooklings.data.Opening
import app.nooklings.data.Settings
import app.nooklings.domain.Dreams
import app.nooklings.domain.House
import app.nooklings.domain.Lodger
import app.nooklings.domain.Pet
import app.nooklings.domain.Phase
import app.nooklings.domain.Sky
import app.nooklings.domain.Wx
import java.time.Instant
import java.time.ZoneId

/** Everything the screens read, made once per change. */
class Home(val jars: List<Jar>, val openings: List<Opening>, val s: Settings, val now: Long, val ru: Boolean) {
    val lodgers: Map<Pet, Lodger> = House.lodgers(jars, openings, s.started.takeIf { it > 0 } ?: now, now)
    val dream = Dreams.byKey(s.dream)?.takeIf { it.pet in lodgers }
    fun jar(id: Long) = jars.firstOrNull { it.id == id }
    fun openedToday(j: Jar) = openings.any { it.jar == j.id && sameDay(it.at, now) }

    /** Live phase from the sun at the window's town (or the time zone), weather from the last answer. */
    val phase: Phase
    val wx: Wx
    val temp: Int?
    init {
        val p = s.place ?: Sky.guess()
        phase = Sky.phase(now, p.lat, p.lon)
        val o = s.outside?.takeIf { s.liveWeather && now - it.at < 3 * 60 * 60 * 1000L }
        wx = o?.wx ?: Wx.CLEAR
        temp = o?.temp
    }
    val autumn = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).monthValue in 9..11
}

fun sameDay(a: Long, b: Long): Boolean {
    val z = ZoneId.systemDefault()
    return Instant.ofEpochMilli(a).atZone(z).toLocalDate() == Instant.ofEpochMilli(b).atZone(z).toLocalDate()
}

fun daysSince(ms: Long, now: Long) = ((now - ms) / DAY).toInt()

/** A toast with a heading, queued so several can follow one another. */
data class Note(val title: String, val body: String)

/** Little things screens ask of each other. */
@Stable
class Bus {
    val notes = mutableStateListOf<Note>()
    var said by mutableStateOf("" to 0L)
        private set

    fun note(title: String, body: String) { notes += Note(title, body) }
    fun say(t: String) { said = t to System.nanoTime() }
}

val LocalBus = staticCompositionLocalOf { Bus() }
