package app.nooklings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.nooklings.data.DemoSeed
import app.nooklings.domain.Pet
import app.nooklings.domain.Phase
import app.nooklings.domain.Sky
import app.nooklings.ui.NookRoot
import app.nooklings.ui.Overlay
import app.nooklings.ui.Tab
import app.nooklings.ui.art.Art
import app.nooklings.ui.onboarding.Onboarding
import app.nooklings.ui.theme.NookTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo pantry, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    /** Today in Moscow at a given phase: just before sunset by default, the lamp on and the sky pink. */
    private fun at(phase: Phase): Long {
        val day = System.currentTimeMillis() / 86_400_000L * 86_400_000L
        return (0 until 24 * 60).map { day + it * 60_000L }.first { Sky.phase(it, 55.75, 37.62) == phase && (phase != Phase.EVENING || Sky.sunAlt(it, 55.75, 37.62) < 3) }
    }

    @Before fun still() { Art.still = true }

    private fun seed(): app.nooklings.data.Settings = runBlocking {
        val app = ApplicationProvider.getApplicationContext<NooklingsApp>()
        DemoSeed.seedIfEmpty(app, app.container)
        app.container.settings.now()
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { NookTheme { content() } }
        val title = ApplicationProvider.getApplicationContext<NooklingsApp>().getString(R.string.app_title)
        rule.waitUntil(15_000) { rule.mainClock.advanceTimeBy(100); rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, phase: Phase = Phase.EVENING, overlay: Overlay? = null) {
        val s = seed()
        shot(name) { NookRoot(s, at(phase), tab, overlay) }
    }

    @Test fun welcome() = shot("0-welcome") { Onboarding() }
    @Test fun room() = screen("1-room", Tab.ROOM)
    @Test fun day() = screen("2-room-day", Tab.ROOM, Phase.DAY)
    @Test fun pantry() = screen("3-pantry", Tab.PANTRY)
    @Test fun lodgers() = screen("4-lodgers", Tab.LODGERS)
    @Test fun jar() = screen("5-jar", Tab.ROOM, Phase.DAY, Overlay.Jar(12))
    @Test fun lodger() = screen("6-lodger", Tab.ROOM, Phase.DAY, Overlay.Lodger(Pet.MOTH))
    @Test fun catching() = screen("7-catch", Tab.ROOM, overlay = Overlay.Catch)
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }
