package app.nooklings.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nooklings.R
import app.nooklings.container
import app.nooklings.domain.Pet
import app.nooklings.domain.Tag
import app.nooklings.ui.art.JarArt
import app.nooklings.ui.art.JarLook
import app.nooklings.ui.art.PetArt
import app.nooklings.ui.art.PetPose
import app.nooklings.ui.art.rememberTick
import app.nooklings.ui.components.BufImage
import app.nooklings.ui.components.Kind
import app.nooklings.ui.components.PixelButton
import app.nooklings.ui.components.Says
import app.nooklings.ui.settings.PlacePicker
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T

/** Three pages: who the lodgers are, how jars are caught, and the town outside the window. */
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var town by rememberSaveable { mutableStateOf<String?>(null) }
    val tick = rememberTick()
    Column(Modifier.fillMaxSize().background(C.wood).imePadding()) {
        Text(stringResource(R.string.app_title), style = T.Display.copy(color = C.brass), modifier = Modifier.statusBarsPadding().padding(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                when (page) {
                    0 -> for (p in Pet.entries) BufImage(PetArt.draw(p, PetPose(f = (tick / 6 + p.ordinal) % 4, blink = (tick + p.ordinal * 7) % 40 < 2, fed = true)), scale = 2.dp)
                    1 -> for (t in listOf(Tag.SKY, Tag.TEA, Tag.WARM, Tag.QUIET, Tag.PEOPLE)) BufImage(JarArt.draw(JarLook(t.color, t), (tick / 6) % 2), scale = 5.dp)
                    else -> BufImage(PetArt.draw(Pet.BULLFINCH, PetPose(f = (tick / 6) % 4, fed = true)), scale = 4.dp)
                }
            }
            when (page) {
                0 -> { Says(stringResource(R.string.ob_1)); Text(stringResource(R.string.ob_1b), style = T.Body.copy(color = C.paper)) }
                1 -> { Says(stringResource(R.string.ob_2)); Text(stringResource(R.string.ob_2b), style = T.Body.copy(color = C.paper)) }
                else -> {
                    Says(stringResource(R.string.ob_3))
                    Text(stringResource(R.string.ob_3b), style = T.Small.copy(color = C.paperD))
                    town?.let { Text(stringResource(R.string.place_now, it), style = T.Body.copy(color = C.brass)) }
                    PlacePicker { p -> town = p.name; c.write { settings.update { it.copy(place = p) }; refreshWeather(force = true) } }
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) PixelButton(stringResource(R.string.back), { page-- }, kind = Kind.PALE)
            Box(Modifier.weight(1f))
            PixelButton(stringResource(when { page == 2 && town == null -> R.string.skip; page == 2 -> R.string.ob_start; else -> R.string.next }), {
                if (page < 2) page++ else c.write { settings.update { it.copy(onboarded = true, started = System.currentTimeMillis()) } }
            }, kind = if (page == 2) Kind.CRIM else Kind.BRASS)
        }
    }
}
