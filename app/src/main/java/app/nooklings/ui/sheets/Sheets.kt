package app.nooklings.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nooklings.R
import app.nooklings.container
import app.nooklings.data.Files
import app.nooklings.data.Jar
import app.nooklings.data.Player
import app.nooklings.domain.Catch
import app.nooklings.domain.Lodger
import app.nooklings.ui.Home
import app.nooklings.ui.LocalBus
import app.nooklings.ui.Names
import app.nooklings.ui.Nav
import app.nooklings.ui.Plural
import app.nooklings.ui.art.JarArt
import app.nooklings.ui.art.PetArt
import app.nooklings.ui.art.look
import app.nooklings.ui.components.BufImage
import app.nooklings.ui.components.Kind
import app.nooklings.ui.components.PixelButton
import app.nooklings.ui.components.Says
import app.nooklings.ui.components.Sheet
import app.nooklings.ui.daysSince
import app.nooklings.ui.lodgers.Bar
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T
import java.text.DateFormat
import java.util.Date

/** A jar up close: its label, when and in what weather, what's inside; open it to listen, or throw it away. */
@Composable
fun JarSheet(h: Home, j: Jar, nav: Nav, toRoom: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    var sure by remember { mutableStateOf(false) }
    val photo = remember(j.photo) { j.photo?.let { Files.pixels(ctx, it) } }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BufImage(JarArt.draw(j.look()), scale = 7.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(j.label, style = T.Label)
                Text(meta(h, j), style = T.Small)
                val who = j.tags.map { it.pet }.distinct().joinToString(", ") { p ->
                    if (p in h.lodgers) ctx.getString(Names.acc(p)) else ctx.getString(R.string.j_someone_new)
                }
                Text(stringResource(R.string.j_feeds, who) + if (j.rare) " · " + stringResource(R.string.j_rare) else "", style = T.Small.copy(color = C.crim))
            }
        }
        photo?.let { BufImage(it, scale = 4.dp) }
        Text(when {
            j.kind == Catch.BREW -> stringResource(R.string.j_inside_jam)
            j.sound != null -> stringResource(R.string.j_inside_sound)
            j.kind == Catch.LIGHT -> stringResource(R.string.j_inside_light)
            j.kind == Catch.QUIET -> stringResource(R.string.j_inside_quiet)
            j.kind == Catch.WALK && j.steps > 0 -> stringResource(R.string.j_inside_walk, j.steps)
            else -> stringResource(R.string.j_inside_other)
        }, style = T.Body)
        if (h.openedToday(j)) PixelButton(stringResource(R.string.j_opened_today), {}, Modifier.fillMaxWidth(), enabled = false)
        else PixelButton(stringResource(R.string.j_open), {
            Player.play(ctx, j)
            c.write { open(j) }
            bus.note(ctx.getString(R.string.t_opened), ctx.getString(R.string.t_opened_b, j.label))
            nav.close(); toRoom()
        }, Modifier.fillMaxWidth(), Kind.CRIM)
        PixelButton(stringResource(if (sure) R.string.j_delete_sure else R.string.j_delete), {
            if (!sure) sure = true else { c.write { delete(j) }; nav.close() }
        }, Modifier.fillMaxWidth(), Kind.PALE, small = true)
    }
}

private fun meta(h: Home, j: Jar): String = buildList {
    add(DateFormat.getDateInstance(DateFormat.LONG).format(Date(j.at)))
    if (j.place.isNotBlank()) add(j.place)
    j.temp?.let { add((if (it > 0) "+" else "") + it + "°") }
}.joinToString(" · ")

/** A lodger up close: where they live, what they eat, how full, what they dream of, what they brought. */
@Composable
fun PetSheet(h: Home, l: Lodger, nav: Nav) {
    val ctx = LocalContext.current
    val p = l.pet
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BufImage(PetArt.portrait(p), scale = 4.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Names.pet(p)), style = T.Title)
                Text(stringResource(R.string.ps_lives, stringResource(Names.home(p))), style = T.Small)
                Text(stringResource(R.string.l_eats, p.eats.joinToString(", ") { ctx.getString(Names.tag(it)).lowercase() }), style = T.Small)
            }
        }
        Bar(l.food.toFloat() / 100)
        val d = h.dream?.takeIf { it.pet == p }
        Says(d?.text(h.ru) ?: stringResource(Names.say(l)), who = stringResource(if (d != null) R.string.dream_label else Names.pet(p)))
        val n = h.jars.count { j -> j.tags.any { it.pet == p } }
        Text(stringResource(R.string.ps_days, Plural.days(maxOf(1, daysSince(l.since, h.now)), h.ru)) + "\n" + stringResource(R.string.ps_jars, n), style = T.Small)
        Text(if (p.gift in h.s.gifts) stringResource(R.string.ps_gift, stringResource(Names.gift(p.gift))) else stringResource(R.string.ps_gift_none), style = T.Small)
    }
}
