package app.nooklings.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.nooklings.domain.Gift
import app.nooklings.domain.Outside
import app.nooklings.domain.Place
import app.nooklings.domain.Wx
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** When the cat and the dormouse moved in. */
    val started: Long = 0,
    /** The town outside the window; null keeps the sun from the time zone and no weather. */
    val place: Place? = null,
    val outside: Outside? = null,
    val liveWeather: Boolean = true,
    /** The current dream, as [app.nooklings.domain.Dream.key], and when it began. */
    val dream: String? = null,
    val dreamAt: Long = 0,
    val gifts: Set<Gift> = emptySet(),
    /** Walks are caught by the step counter on their own. */
    val autoWalks: Boolean = false,
    /** The step counter's reading at the start of the current window, and when that was. */
    val stepBase: Long = -1,
    val stepBaseAt: Long = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read(): Settings {
        val lat = this[LAT]
        val lon = this[LON]
        val at = this[WX_AT] ?: 0L
        return Settings(
            onboarded = this[ONBOARDED] ?: false,
            started = this[STARTED] ?: 0L,
            place = if (lat != null && lon != null) Place(this[PLACE] ?: "", this[REGION] ?: "", lat, lon) else null,
            outside = if (at > 0) Outside(runCatching { Wx.valueOf(this[WX] ?: "") }.getOrDefault(Wx.CLEAR), this[TEMP]?.takeIf { it != Int.MIN_VALUE },
                this[WIND] ?: 8.0, this[RISE] ?: 0, this[SET] ?: 0, at) else null,
            liveWeather = this[LIVE] ?: true,
            dream = this[DREAM]?.takeIf { it.isNotBlank() },
            dreamAt = this[DREAM_AT] ?: 0L,
            gifts = this[GIFTS]?.split(',')?.mapNotNull { n -> Gift.entries.firstOrNull { it.name == n } }?.toSet() ?: emptySet(),
            autoWalks = this[WALKS] ?: false,
            stepBase = this[STEP_BASE] ?: -1L,
            stepBaseAt = this[STEP_AT] ?: 0L,
        )
    }

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[STARTED] = s.started
            val p = s.place
            if (p != null) { it[PLACE] = p.name; it[REGION] = p.region; it[LAT] = p.lat; it[LON] = p.lon }
            else { it.remove(PLACE); it.remove(REGION); it.remove(LAT); it.remove(LON) }
            val o = s.outside
            if (o != null) { it[WX] = o.wx.name; it[TEMP] = o.temp ?: Int.MIN_VALUE; it[WIND] = o.wind; it[RISE] = o.rise; it[SET] = o.set; it[WX_AT] = o.at }
            else it.remove(WX_AT)
            it[LIVE] = s.liveWeather
            it[DREAM] = s.dream ?: ""
            it[DREAM_AT] = s.dreamAt
            it[GIFTS] = s.gifts.map { g -> g.name }.sorted().joinToString(",")
            it[WALKS] = s.autoWalks
            it[STEP_BASE] = s.stepBase
            it[STEP_AT] = s.stepBaseAt
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val STARTED = longPreferencesKey("started")
        val PLACE = stringPreferencesKey("place")
        val REGION = stringPreferencesKey("region")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val WX = stringPreferencesKey("wx")
        val TEMP = intPreferencesKey("temp")
        val WIND = doublePreferencesKey("wind")
        val RISE = longPreferencesKey("rise")
        val SET = longPreferencesKey("set")
        val WX_AT = longPreferencesKey("wx_at")
        val LIVE = booleanPreferencesKey("live")
        val DREAM = stringPreferencesKey("dream")
        val DREAM_AT = longPreferencesKey("dream_at")
        val GIFTS = stringPreferencesKey("gifts")
        val WALKS = booleanPreferencesKey("walks")
        val STEP_BASE = longPreferencesKey("step_base")
        val STEP_AT = longPreferencesKey("step_at")
    }
}
