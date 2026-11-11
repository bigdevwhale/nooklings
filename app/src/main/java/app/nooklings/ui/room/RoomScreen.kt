package app.nooklings.ui.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nooklings.R
import app.nooklings.container
import app.nooklings.domain.House
import app.nooklings.domain.Pet
import app.nooklings.ui.Home
import app.nooklings.ui.LocalBus
import app.nooklings.ui.Names
import app.nooklings.ui.Nav
import app.nooklings.ui.Plural
import app.nooklings.ui.art.Art
import app.nooklings.ui.art.PixelCanvas
import app.nooklings.ui.art.RoomFx
import app.nooklings.ui.art.RoomModel
import app.nooklings.ui.art.RoomScene
import app.nooklings.ui.art.ShelfJar
import app.nooklings.ui.art.Spot
import app.nooklings.ui.art.look
import app.nooklings.ui.art.rememberTick
import app.nooklings.ui.components.Kind
import app.nooklings.ui.components.PixelButton
import app.nooklings.ui.components.sayBox
import app.nooklings.ui.components.tray
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T

private const val S_PER_TICK = Art.FRAME_MS / 1000.0
private const val GATHER_S = 14.0

/** The room, a line about how the lodgers are, and the one big button. */
@Composable
fun RoomScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val tick = rememberTick()

    // A new jar flies to its slot; hearts rise over who ate; newcomers fade in.
    val landed by c.landed.collectAsStateWithLifecycle()
    var landTick by remember { mutableIntStateOf(-10_000) }
    LaunchedEffect(landed?.stamp) { landed?.let { if (System.nanoTime() - it.stamp < 30_000_000_000L) landTick = tick } }
    val opened by c.opened.collectAsStateWithLifecycle()
    var openTick by remember { mutableIntStateOf(-10_000) }
    LaunchedEffect(opened) { if (opened != 0L && System.currentTimeMillis() - opened < 30_000) openTick = tick }

    val model = remember(h.jars, h.lodgers, h.phase, h.wx, h.s.gifts) {
        RoomModel(h.lodgers, h.jars.map { ShelfJar(it.id, it.look()) }, h.s.gifts, h.phase, h.wx, h.temp, h.s.outside?.wind ?: 8.0, h.autumn)
    }
    val gathering = (tick - openTick) * S_PER_TICK in 0.0..GATHER_S
    val pile = gathering || House.pile(h.lodgers, h.phase)

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val artH = ((maxHeight - 6.dp) / (maxWidth - 6.dp) * RoomScene.W).toInt().coerceIn(RoomScene.BASE_H, RoomScene.MAX_H)
            val scene = remember(model, artH) { RoomScene(model, artH) }
            Box(Modifier.tray().padding(3.dp)) {
                PixelCanvas(RoomScene.W, artH, Modifier.pointerInput(scene) {
                    detectTapGestures { o ->
                        when (val s = scene.hit(o.x / size.width * RoomScene.W.toDouble(), o.y / size.height * artH.toDouble())) {
                            is Spot.Lodger -> nav.lodger(s.pet)
                            is Spot.Jar -> nav.jar(s.id)
                            Spot.Pantry -> bus.say(ctx.getString(R.string.say_pantry, Plural.jars(h.jars.size, h.ru)))
                            Spot.Stove -> bus.say(ctx.getString(R.string.say_stove))
                            null -> Unit
                        }
                    }
                }, fitHeight = true) { b, tk ->
                    val t = tk * S_PER_TICK
                    val la = (tk - landTick) * S_PER_TICK
                    val out = landed?.takeIf { la in 0.0..6.0 }
                    val fly = if (out != null) (la / .9).coerceAtMost(1.0) else 1.0
                    val fed = if (out != null && la > .9) out.fed.map { it to la - .9 } else emptyList()
                    val fade = if (out != null) out.arrived.associateWith { ((la - .9) / 1.5).coerceIn(0.0, 1.0).toFloat() } else emptyMap()
                    scene.draw(b, t, RoomFx(
                        pile = pile, flare = (tk - openTick) * S_PER_TICK in 0.0..20.0,
                        flyId = out?.jar?.id, fly = fly, fade = fade, hearts = fed + fed.map { (p, a) -> p to a - .3 },
                        landedAge = if (out != null) la - .9 else null,
                        clock = h.now + (if (Art.still) 0L else tk * Art.FRAME_MS),
                    ))
                }
            }
        }
        Bubble(h, pile, gathering) { p -> nav.lodger(p) }
        PixelButton(stringResource(R.string.catch_cosy), nav::catch, Modifier.fillMaxWidth(), Kind.CRIM)
    }
}

/** One line under the room: what someone says, a dream, or how the hungriest lodger is. */
@Composable
private fun Bubble(h: Home, pile: Boolean, gathering: Boolean, onPet: (Pet) -> Unit) {
    val bus = LocalBus.current
    val tick = rememberTick()
    val (said, stamp) = bus.said
    var saidTick by remember { mutableIntStateOf(-10_000) }
    LaunchedEffect(stamp) { if (said.isNotEmpty()) saidTick = tick }
    val (who, text, pet) = when {
        said.isNotEmpty() && (tick - saidTick) * Art.FRAME_MS < 5_400 -> Triple(null, said, null)
        gathering -> Triple(null, stringResource(R.string.pile_listen), null)
        pile -> Triple(null, stringResource(R.string.pile_full), null)
        h.dream != null -> Triple(stringResource(R.string.dream_label), h.dream.text(h.ru), h.dream.pet)
        else -> {
            val l = h.lodgers.values.minBy { it.food }
            Triple(stringResource(Names.pet(l.pet)), stringResource(Names.say(l)), l.pet)
        }
    }
    Column(Modifier.fillMaxWidth().sayBox().clickable(enabled = pet != null) { pet?.let(onPet) }.padding(horizontal = 14.dp, vertical = 9.dp)) {
        who?.let { Text(it, style = T.Small.copy(color = C.crim)) }
        Text(text, style = T.Say)
    }
}
