package app.nooklings.ui.pantry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nooklings.R
import app.nooklings.container
import app.nooklings.data.Jar
import app.nooklings.ui.Home
import app.nooklings.ui.Nav
import app.nooklings.ui.Plural
import app.nooklings.ui.art.JarArt
import app.nooklings.ui.art.look
import app.nooklings.ui.components.BufImage
import app.nooklings.ui.components.PixelButton
import app.nooklings.ui.components.tray
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T
import java.text.DateFormat
import java.util.Date

/** Every jar ever caught, newest first; two of them can be cooked into jam. */
@Composable
fun PantryScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var picking by remember { mutableStateOf<List<Long>?>(null) }
    val jars = remember(h.jars) { h.jars.sortedByDescending { it.at } }

    fun brew(a: Jar, b: Jar) {
        val l = ctx.getString(R.string.label_jam, if (h.ru) a.label.lowercase() else a.label, if (h.ru) b.label.lowercase() else b.label)
        c.write { brew(a, b, l) }
        nav.tab(app.nooklings.ui.Tab.ROOM)
    }

    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.tab_pantry), style = T.Title.copy(color = C.brass))
                    Text("  " + Plural.jars(jars.size, h.ru), style = T.Small.copy(color = C.paperD))
                }
                if (jars.size >= 2) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(stringResource(if (picking != null) R.string.cancel else R.string.p_brew), { picking = if (picking != null) null else emptyList() }, small = true)
                    Text(if (picking != null) stringResource(R.string.p_pick, picking!!.size) else stringResource(R.string.p_brew_hint), style = T.Small.copy(color = C.paperD))
                }
                if (jars.isEmpty()) Text(stringResource(R.string.p_empty), style = T.Body.copy(color = C.paperD))
            }
        }
        items(jars, key = { it.id }) { j ->
            val sel = picking?.contains(j.id) == true
            Column(
                Modifier.fillMaxWidth().tray(if (sel) C.brass else C.ink).clickable {
                    val p = picking
                    if (p == null) nav.jar(j.id)
                    else {
                        val next = if (j.id in p) p - j.id else p + j.id
                        if (next.size == 2) { picking = null; brew(jars.first { it.id == next[0] }, jars.first { it.id == next[1] }) } else picking = next
                    }
                }.padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                BufImage(JarArt.draw(j.look()), scale = 5.dp)
                Text(j.label, style = T.LabelSmall, textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(j.at)), style = T.Small.copy(color = C.paperD), maxLines = 1)
            }
        }
    }
}
