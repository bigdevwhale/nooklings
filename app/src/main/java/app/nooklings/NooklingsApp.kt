package app.nooklings

import android.app.Application
import android.content.Context
import app.nooklings.data.DAY
import app.nooklings.data.HOUR
import app.nooklings.data.DemoSeed
import app.nooklings.data.Files
import app.nooklings.data.Jar
import app.nooklings.data.NookDb
import app.nooklings.data.Opening
import app.nooklings.data.SettingsStore
import app.nooklings.data.Steps
import app.nooklings.domain.Catch
import app.nooklings.domain.Dream
import app.nooklings.domain.Dreams
import app.nooklings.domain.Gift
import app.nooklings.domain.House
import app.nooklings.domain.Pet
import app.nooklings.domain.Sky
import app.nooklings.domain.Tag
import app.nooklings.domain.Wx
import app.nooklings.ui.art.mix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** What a new jar did: who came, who ate, which dream came true and what it brought. */
data class Outcome(val jar: Jar, val arrived: List<Pet>, val fed: List<Pet>, val dream: Dream?, val gift: Gift?, val stamp: Long = System.nanoTime())

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = NookDb.create(app)
    val jars = db.jars()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True while the demo build fills its pantry, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    /** The last jar put on the shelf, for the room to fly it in and say what happened. */
    val landed = MutableStateFlow<Outcome?>(null)

    /** The last jar opened, for the room to gather everyone by the stove. */
    val opened = MutableStateFlow(0L)

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    /** Asks for the weather if a town is set and the last answer is over half an hour old. */
    suspend fun refreshWeather(force: Boolean = false) {
        val s = settings.now()
        val p = s.place ?: return
        if (!s.liveWeather) return
        if (!force && System.currentTimeMillis() - (s.outside?.at ?: 0) < 30 * 60 * 1000L) return
        val o = runCatching { withContext(Dispatchers.IO) { Sky.fetch(p.lat, p.lon) } }.onFailure { android.util.Log.w("Nooklings", "weather", it) }.getOrNull() ?: return
        settings.update { it.copy(outside = o) }
    }

    /** A new jar stamped with now: the weather outside, the town, and whether it's a rare one. */
    suspend fun draft(tag: Tag, kind: Catch, label: String, now: Long = System.currentTimeMillis()): Jar {
        val s = settings.now()
        val o = s.outside?.takeIf { s.liveWeather && now - it.at < 3 * 60 * 60 * 1000L }
        val rare = o?.wx == Wx.SNOW || o?.wx == Wx.STORM
        return Jar(tag = tag, kind = kind, label = label, at = now, wx = o?.wx, temp = o?.temp, place = s.place?.name.orEmpty(), rare = rare)
    }

    /** Is this the first snow of the season? Then the jar gets that name. */
    suspend fun firstSnow(j: Jar): Boolean =
        j.tag == Tag.SKY && j.wx == Wx.SNOW && jars.list().none { it.wx == Wx.SNOW && j.at - it.at < 120 * DAY }

    /** Puts a jar on the shelf: lodgers eat, someone may move in, a dream may come true. */
    suspend fun roll(j: Jar): Outcome {
        val s = settings.now()
        val now = j.at
        val before = House.lodgers(jars.list(), jars.openingList(), s.started, now)
        val id = jars.insert(j)
        val jar = j.copy(id = id)
        val pets = jar.tags.map { it.pet }.distinct()
        val arrived = pets.filter { it !in before }
        val fed = pets.filter { it in before }
        val d = Dreams.byKey(s.dream)?.takeIf { it.pet in before && it.tag in jar.tags }
        val gift = d?.pet?.gift?.takeIf { it !in s.gifts }
        val present = before.keys + arrived
        val next = if ((d != null || s.dream == null) && Random.nextDouble() < .7) Dreams.roll(present, d?.pet ?: Dreams.byKey(s.dream)?.pet, Random.nextDouble(), Random.nextDouble()) else null
        settings.update {
            it.copy(
                dream = if (d != null || it.dream == null) next?.key else it.dream,
                dreamAt = if (d != null || it.dream == null) now else it.dreamAt,
                gifts = it.gifts + listOfNotNull(gift),
            )
        }
        return Outcome(jar, arrived, fed, d, gift).also { landed.value = it }
    }

    /** Two jars cooked into jam: a new jar of both kinds, feeding both lodgers. */
    suspend fun brew(a: Jar, b: Jar, label: String): Outcome {
        val base = draft(a.tag, Catch.BREW, label)
        val c = mix(a.tint, b.tint, .5f)
        return roll(base.copy(tag2 = if (b.tag != a.tag) b.tag else null, color = c, rare = a.rare || b.rare))
    }

    suspend fun open(j: Jar) {
        jars.open(Opening(jar = j.id, at = System.currentTimeMillis()))
        opened.value = System.currentTimeMillis()
    }

    suspend fun delete(j: Jar) {
        jars.forgetOpenings(j.id)
        jars.delete(j)
        Files.delete(app, j)
    }

    /** A day without a dream: someone starts dreaming again. */
    suspend fun dreamIfIdle(now: Long = System.currentTimeMillis()) {
        val s = settings.now()
        if (s.dream != null || now - s.dreamAt < DAY) return
        val present = House.lodgers(jars.list(), jars.openingList(), s.started, now).keys
        val d = Dreams.roll(present, null, Random.nextDouble(), Random.nextDouble()) ?: return
        settings.update { it.copy(dream = d.key, dreamAt = now) }
    }

    /** Recordings and photos left by a catch that was never put on the shelf (an hour old or more). */
    suspend fun tidy(now: Long = System.currentTimeMillis()) {
        val keep = jars.list().flatMap { listOfNotNull(it.sound, it.photo) }.toSet()
        for (f in listOf(Files.sounds(app), Files.photos(app)).flatMap { it.listFiles()?.toList().orEmpty() })
            if (f.name !in keep && now - f.lastModified() > HOUR) f.delete()
    }

    fun walks(on: Boolean) {
        write { settings.update { it.copy(autoWalks = on) } }
        Steps.schedule(app, on)
    }
}

class NooklingsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.write { tidy() }
        if (BuildConfig.DEMO) container.scope.launch { try { DemoSeed.seedIfEmpty(this@NooklingsApp, container) } finally { container.seeding.value = false } }
    }
}

val Context.container: AppContainer get() = (applicationContext as NooklingsApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
