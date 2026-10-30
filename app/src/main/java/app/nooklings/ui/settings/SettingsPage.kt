package app.nooklings.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.nooklings.BuildConfig
import app.nooklings.R
import app.nooklings.container
import app.nooklings.data.Settings
import app.nooklings.data.Steps
import app.nooklings.domain.Place
import app.nooklings.domain.Sky
import app.nooklings.ru
import app.nooklings.ui.Nav
import app.nooklings.ui.components.Chip
import app.nooklings.ui.components.Field
import app.nooklings.ui.components.Flow
import app.nooklings.ui.components.Kind
import app.nooklings.ui.components.Page
import app.nooklings.ui.components.Panel
import app.nooklings.ui.components.PixelButton
import app.nooklings.ui.components.Section
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "nooklings"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** A town search for the window. */
@Composable
fun PlacePicker(onPicked: (Place) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    fun go() {
        if (q.isNotBlank()) scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Sky.search(q, if (ctx.ru) "ru" else "en") } }
            failed = r.isFailure
            results = r.getOrDefault(emptyList())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(q, { q = it }, stringResource(R.string.place_search), Modifier.weight(1f), onDone = ::go)
            PixelButton(stringResource(R.string.search), ::go, small = true)
        }
        if (failed) Text(stringResource(R.string.place_fail), style = T.Small.copy(color = C.crimL))
        for (p in results) Panel(Modifier.fillMaxWidth(), onClick = { results = emptyList(); q = p.name; focus.clearFocus(); onPicked(p) }) {
            Text(p.name, style = T.Body)
            Text(p.region, style = T.Small)
        }
    }
}

@Composable
fun SettingsPage(s: Settings, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    val stepsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) c.walks(true) }
    Page(stringResource(R.string.settings), nav::close) {
        Section(stringResource(R.string.set_window))
        Text(s.place?.let { stringResource(R.string.place_now, it.name) } ?: stringResource(R.string.place_none), style = T.Body.copy(color = C.paper))
        Text(stringResource(R.string.place_hint), style = T.Small.copy(color = C.paperD))
        PlacePicker { picked -> c.write { settings.update { it.copy(place = picked, outside = null) }; refreshWeather(force = true) } }
        Flow {
            Chip(stringResource(R.string.set_wx_live), s.liveWeather, { c.write { settings.update { it.copy(liveWeather = true) }; refreshWeather(force = true) } }, dark = true)
            Chip(stringResource(R.string.set_wx_fair), !s.liveWeather, { c.write { settings.update { it.copy(liveWeather = false) } } }, dark = true)
        }
        if (s.place != null) PixelButton(stringResource(R.string.place_off), { c.write { settings.update { it.copy(place = null, outside = null) } } }, Modifier.fillMaxWidth(), kind = Kind.PALE, small = true)

        Section(stringResource(R.string.set_walks))
        Text(stringResource(if (Steps.available(ctx)) R.string.set_walks_hint else R.string.c_walk_none, Steps.MIN_WALK), style = T.Small.copy(color = C.paperD))
        if (Steps.available(ctx)) Flow {
            Chip(stringResource(R.string.set_walks_on), s.autoWalks, {
                if (Build.VERSION.SDK_INT >= 29) stepsPerm.launch(Manifest.permission.ACTIVITY_RECOGNITION) else c.walks(true)
            }, dark = true)
            Chip(stringResource(R.string.set_walks_off), !s.autoWalks, { c.walks(false) }, dark = true)
        }

        Section(stringResource(R.string.set_lang))
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
        Flow {
            for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                Chip(stringResource(l), cur == tag, { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }, dark = true)
        }

        Section(stringResource(R.string.community))
        Text(stringResource(R.string.community_text), style = T.Small.copy(color = C.paperD))
        for ((label, sub, url) in listOf(
            Triple(R.string.community_bug, R.string.community_bug_sub, Community.BUG),
            Triple(R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
            Triple(R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
            Triple(R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
        )) Panel(Modifier.fillMaxWidth(), onClick = { open(url) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Body); Text(stringResource(sub), style = T.Small) }
                Text("›", style = T.Heading.copy(color = C.crim))
            }
        }

        Section(stringResource(R.string.about))
        Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small.copy(color = C.paperD))
        PixelButton(stringResource(R.string.source), { open("https://github.com/bigdevwhale/nooklings") }, kind = Kind.WOOD, small = true)
    }
}
