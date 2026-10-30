package app.nooklings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nooklings.Outcome
import app.nooklings.R
import app.nooklings.container
import app.nooklings.data.Settings
import app.nooklings.domain.Pet
import app.nooklings.ru
import app.nooklings.ui.catching.CatchFlow
import app.nooklings.ui.components.pixelBox
import app.nooklings.ui.lodgers.LodgersScreen
import app.nooklings.ui.onboarding.Onboarding
import app.nooklings.ui.pantry.PantryScreen
import app.nooklings.ui.room.RoomScreen
import app.nooklings.ui.settings.SettingsPage
import app.nooklings.ui.sheets.JarSheet
import app.nooklings.ui.sheets.PetSheet
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T
import kotlinx.coroutines.delay

enum class Tab { ROOM, PANTRY, LODGERS }

/** What is open over the tabs. */
sealed class Overlay {
    data object Catch : Overlay()
    data class Jar(val id: Long) : Overlay()
    data class Lodger(val pet: Pet) : Overlay()
    data object Settings : Overlay()
}

class Nav(val go: (Overlay?) -> Unit, val tab: (Tab) -> Unit) {
    fun jar(id: Long) = go(Overlay.Jar(id))
    fun lodger(p: Pet) = go(Overlay.Lodger(p))
    fun catch() = go(Overlay.Catch)
    fun close() = go(null)
}

/** [clock], [tab0] and [overlay0] let renders open a fixed moment on a given screen; the app passes none. */
@Composable
fun NookRoot(s: Settings, clock: Long? = null, tab0: Tab = Tab.ROOM, overlay0: Overlay? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    if (!s.onboarded) { Onboarding(); return }
    val jars by c.jars.all().collectAsStateWithLifecycle(initialValue = null)
    val openings by c.jars.openings().collectAsStateWithLifecycle(initialValue = emptyList())
    var now by remember { mutableLongStateOf(clock ?: System.currentTimeMillis()) }
    LaunchedEffect(Unit) { if (clock == null) { c.dreamIfIdle(); while (true) { c.refreshWeather(); delay(60_000); now = System.currentTimeMillis() } } }
    val list = jars ?: return
    val h = remember(list, openings, s, now) { Home(list, openings, s, now, ctx.ru) }
    val bus = remember { Bus() }
    var tab by rememberSaveable { mutableStateOf(tab0) }
    var overlay by remember { mutableStateOf(overlay0) }
    val nav = remember { Nav({ overlay = it }, { tab = it; overlay = null }) }

    // A jar on the shelf: say who came, whose dream came true, what they brought.
    val landed by c.landed.collectAsStateWithLifecycle()
    LaunchedEffect(landed?.stamp) { landed?.let { if (System.nanoTime() - it.stamp < 60_000_000_000L) { delay(1000); announce(ctx, bus, it) } } }

    CompositionLocalProvider(LocalBus provides bus) {
        Box(Modifier.fillMaxSize().background(C.wood)) {
            Column(Modifier.fillMaxSize()) {
                Header(h) { overlay = Overlay.Settings }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.ROOM -> RoomScreen(h, nav)
                        Tab.PANTRY -> PantryScreen(h, nav)
                        Tab.LODGERS -> LodgersScreen(h, nav)
                    }
                }
                TabBar(tab) { tab = it }
            }
            when (val o = overlay) {
                Overlay.Catch -> CatchFlow(h, nav) { tab = Tab.ROOM }
                is Overlay.Jar -> h.jar(o.id)?.let { JarSheet(h, it, nav) { tab = Tab.ROOM } }
                is Overlay.Lodger -> h.lodgers[o.pet]?.let { PetSheet(h, it, nav) }
                Overlay.Settings -> SettingsPage(s, nav)
                null -> Unit
            }
            Notes(Modifier.align(Alignment.TopCenter))
        }
    }
}

private fun announce(ctx: android.content.Context, bus: Bus, o: Outcome) {
    for (p in o.arrived) bus.note(ctx.getString(R.string.t_knock), ctx.getString(Names.arrive(p)))
    o.dream?.let { bus.note(ctx.getString(R.string.t_dream), ctx.getString(R.string.t_dream_b, it.text(ctx.ru), o.jar.label)) }
    o.gift?.let { g -> o.dream?.let { bus.note(ctx.getString(R.string.t_gift), ctx.getString(R.string.t_gift_b, ctx.getString(Names.gen(it.pet)), ctx.getString(Names.gift(g)))) } }
    if (o.jar.rare) bus.note(ctx.getString(R.string.t_rare), ctx.getString(R.string.t_rare_b))
}

@Composable
private fun Header(h: Home, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.app_title), style = T.Display.copy(color = C.brass), maxLines = 1)
        val parts = buildList {
            h.s.place?.name?.takeIf { it.isNotBlank() }?.let { add(it) }
            h.temp?.let { add((if (it > 0) "+" else "") + it + "°") }
            add(stringResource(Names.wx(h.wx)))
        }
        Text(parts.joinToString(" · "), style = T.Small.copy(color = C.paperD), modifier = Modifier.weight(1f), maxLines = 2)
        val label = stringResource(R.string.settings)
        Box(Modifier.size(36.dp).background(C.woodD).clickable(onClick = onSettings).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val p = size.width / 9
                fun r(x: Int, y: Int, w: Int = 1, hh: Int = 1) = drawRect(C.brass, Offset(x * p, y * p), Size(w * p, hh * p))
                r(1, 1, 7, 7); for ((x, y) in listOf(3 to 0, 5 to 0, 0 to 3, 0 to 5, 8 to 3, 8 to 5, 3 to 8, 5 to 8)) r(x, y)
                drawRect(C.woodD, Offset(3 * p, 3 * p), Size(3 * p, 3 * p))
                for ((x, y) in listOf(1 to 1, 7 to 1, 1 to 7, 7 to 7)) drawRect(C.woodD, Offset(x * p, y * p), Size(p, p))
            }
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(C.woodD).navigationBarsPadding().padding(vertical = 6.dp)) {
        for ((t, l) in listOf(Tab.ROOM to R.string.tab_room, Tab.PANTRY to R.string.tab_pantry, Tab.LODGERS to R.string.tab_lodgers)) {
            val on = t == tab
            Text((if (on) "▸" else "") + stringResource(l), style = T.Button.copy(color = if (on) C.brass else C.muted), textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 8.dp), maxLines = 1)
        }
    }
}

/** Notes come one after another at the top, each for a few seconds; a tap hides one. */
@Composable
private fun Notes(modifier: Modifier) {
    val bus = LocalBus.current
    val n = bus.notes.firstOrNull() ?: return
    LaunchedEffect(n) { delay(3600); bus.notes.remove(n) }
    Column(modifier.statusBarsPadding().padding(top = 56.dp, start = 14.dp, end = 14.dp).fillMaxWidth().pixelBox(C.paper)
        .clickable { bus.notes.remove(n) }.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(n.title, style = T.Body.copy(color = C.crim))
        Text(n.body, style = T.Say)
    }
}
