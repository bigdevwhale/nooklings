package app.nooklings.ui.lodgers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nooklings.R
import app.nooklings.domain.Pet
import app.nooklings.ui.Home
import app.nooklings.ui.Names
import app.nooklings.ui.Nav
import app.nooklings.ui.art.PetArt
import app.nooklings.ui.components.BufImage
import app.nooklings.ui.components.tray
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T

/** Who lives here and who hasn't come yet: the latter are shadows with a hint of what they'd smell. */
@Composable
fun LodgersScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.tab_lodgers), style = T.Title.copy(color = C.brass))
                Text("  " + stringResource(R.string.l_count, h.lodgers.size, Pet.entries.size), style = T.Small.copy(color = C.paperD))
            }
        }
        items(Pet.entries) { p ->
            val l = h.lodgers[p]
            val img = remember(p, l != null) { PetArt.portrait(p, dark = l == null) }
            val tags = p.eats.joinToString(", ") { t -> ctx.getString(Names.tag(t)).lowercase() }
            Column(
                Modifier.fillMaxWidth().tray().then(if (l != null) Modifier.clickable { nav.lodger(p) } else Modifier).padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Every tile the same size: the picture in a fixed frame, two lines of text, the bar always there.
                Box(Modifier.height(80.dp), contentAlignment = Alignment.BottomCenter) { BufImage(img, scale = 3.dp) }
                Text(if (l != null) stringResource(Names.pet(p)) else "???", style = T.Heading.copy(color = C.paper), maxLines = 1)
                Text(stringResource(if (l != null) R.string.l_eats else R.string.l_will, tags), style = T.Small.copy(color = C.paperD), textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
                Bar(l?.food?.toFloat()?.div(100) ?: 0f, Modifier.alpha(if (l != null) 1f else 0f))
            }
        }
    }
}

/** How full a lodger is, in brass. */
@Composable
fun Bar(v: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(10.dp).background(C.ink).padding(2.dp)) {
        Box(Modifier.fillMaxSize().background(C.wood))
        Box(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).height(6.dp).background(C.brass))
    }
}
