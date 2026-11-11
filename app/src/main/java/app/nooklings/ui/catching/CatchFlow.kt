package app.nooklings.ui.catching

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.nooklings.BuildConfig
import app.nooklings.R
import app.nooklings.container
import app.nooklings.ru
import app.nooklings.data.Files
import app.nooklings.data.Jar
import app.nooklings.data.Meter
import app.nooklings.data.Recorder
import app.nooklings.data.Steps
import app.nooklings.domain.Catch
import app.nooklings.domain.Pet
import app.nooklings.domain.Tag
import app.nooklings.ui.Home
import app.nooklings.ui.Names
import app.nooklings.ui.Nav
import app.nooklings.ui.Plural
import app.nooklings.ui.art.Art
import app.nooklings.ui.art.JarArt
import app.nooklings.ui.art.PetArt
import app.nooklings.ui.art.PetPose
import app.nooklings.ui.art.look
import app.nooklings.ui.art.rememberTick
import app.nooklings.ui.components.BufImage
import app.nooklings.ui.components.Field
import app.nooklings.ui.components.Kind
import app.nooklings.ui.components.PixelButton
import app.nooklings.ui.components.pixelBox
import app.nooklings.ui.theme.C
import app.nooklings.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.UUID

/** What was caught before it's named: a recording, a photo's light, a walk's steps. */
private data class Cap(val kind: Catch, val sound: String? = null, val photo: String? = null, val color: Int = 0, val steps: Int = 0)

private sealed class Step {
    data object Pick : Step()
    data object Sound : Step()
    data object Quiet : Step()
    data object Light : Step()
    data object Walk : Step()
    data class Name(val cap: Cap) : Step()
    data class Seal(val cap: Cap, val tag: Tag) : Step()
}

/**
 * The net: catch a moment that is already happening. Sound, light, quiet or steps; then one tap for
 * what it was; then it's rolled into a jar and goes on the shelf.
 */
@Composable
fun CatchFlow(h: Home, nav: Nav, toRoom: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf<Step>(Step.Pick) }
    val close = nav::close
    BackHandler { if (step == Step.Pick) close() else step = Step.Pick }
    Column(
        Modifier.fillMaxSize().background(C.woodD).clickable(remember { MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val s = step) {
            Step.Pick -> Pick(onMode = { step = it }, close)
            Step.Sound -> SoundStep({ step = Step.Name(it) }) { step = Step.Pick }
            Step.Quiet -> QuietStep({ step = Step.Seal(Cap(Catch.QUIET), Tag.QUIET) }) { step = Step.Pick }
            Step.Light -> LightStep({ step = Step.Name(it) }) { step = Step.Pick }
            Step.Walk -> WalkStep({ step = Step.Seal(it, Tag.WALK) }) { step = Step.Pick }
            is Step.Name -> NameStep(h, s.cap, { step = Step.Seal(s.cap, it) }) { s.cap.sound?.let { Files.sound(ctx, it).delete() }; s.cap.photo?.let { Files.photo(ctx, it).delete() }; step = Step.Pick }
            is Step.Seal -> SealStep(s.cap, s.tag) { close(); toRoom() }
        }
    }
}

@Composable
private fun Heading(title: String, sub: String? = null) {
    Text(title, style = T.Display.copy(color = C.brass), textAlign = TextAlign.Center)
    if (sub != null) Text(sub, style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 340.dp))
}

@Composable
private fun Pick(onMode: (Step) -> Unit, close: () -> Unit) {
    Heading(stringResource(R.string.c_title), stringResource(R.string.c_sub))
    for (row in listOf(listOf(Triple(Step.Sound, R.string.m_sound, R.string.m_sound_d), Triple(Step.Light, R.string.m_light, R.string.m_light_d)),
        listOf(Triple(Step.Quiet, R.string.m_quiet, R.string.m_quiet_d), Triple(Step.Walk, R.string.m_walk, R.string.m_walk_d)))) {
        Row(Modifier.fillMaxWidth().widthIn(max = 360.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((s, t, d) in row) Column(Modifier.weight(1f).pixelBox(C.brass).clickable { onMode(s) }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(t), style = T.Title)
                Text(stringResource(d), style = T.Small.copy(color = C.brassD))
            }
        }
    }
    PixelButton(stringResource(R.string.c_not_now), close, kind = Kind.PALE, small = true)
}

/** Asks for a permission once on entry; [ok] tells whether it's granted. */
@Composable
private fun rememberPermission(perm: String?, onResult: (Boolean) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return remember(perm) {
        {
            if (perm == null || ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED) onResult(true)
            else launcher.launch(perm)
        }
    }
}

@Composable
private fun Wave(levels: List<Float>) {
    Canvas(Modifier.fillMaxWidth().widthIn(max = 320.dp).height(96.dp)) {
        val n = 40
        val p = size.width / (n * 2)
        val tail = levels.takeLast(n)
        tail.forEachIndexed { i, v ->
            val hh = ((kotlin.math.sqrt(v) * 1.6f).coerceIn(.03f, 1f) * size.height / p).toInt().coerceAtLeast(1) * p
            drawRect(C.brass, Offset(i * 2 * p, (size.height - hh) / 2), Size(p, hh))
            drawRect(C.brassL, Offset(i * 2 * p, (size.height - hh) / 2), Size(p, p))
        }
    }
}

@Composable
private fun SoundStep(done: (Cap) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf<Boolean?>(null) }
    val ask = rememberPermission(Manifest.permission.RECORD_AUDIO) { granted = it }
    LaunchedEffect(Unit) { ask() }
    val levels = remember { mutableStateListOf<Float>() }
    var left by remember { mutableIntStateOf(10) }
    var failed by remember { mutableStateOf(false) }
    val rec = remember { Recorder(ctx) }
    DisposableEffect(Unit) { onDispose { rec.cancel() } }
    // The key is read once: the permission may be granted while the first effect is still starting.
    val ok = granted
    LaunchedEffect(ok) {
        if (ok != true) return@LaunchedEffect
        if (!rec.start()) { failed = true; return@LaunchedEffect }
        val t0 = System.currentTimeMillis()
        while (isActive && System.currentTimeMillis() - t0 < 10_000) {
            levels += rec.level(); left = 10 - ((System.currentTimeMillis() - t0) / 1000).toInt(); delay(70)
        }
        val f = rec.stop()
        if (f == null) failed = true else done(Cap(Catch.SOUND, sound = f.name))
    }
    Heading(stringResource(R.string.c_listen))
    when {
        granted == false -> Text(stringResource(R.string.c_mic_denied), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
        failed -> Text(stringResource(R.string.c_mic_fail), style = T.Body.copy(color = C.crimL), textAlign = TextAlign.Center)
        else -> {
            Wave(levels)
            Text("$left", style = T.Number)
            Text(stringResource(R.string.c_listen_hint), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
        }
    }
    PixelButton(stringResource(R.string.cancel), back, kind = Kind.PALE, small = true)
}

@Composable
private fun QuietStep(done: () -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val need = if (BuildConfig.DEMO) 20 else 180
    var granted by remember { mutableStateOf<Boolean?>(null) }
    val ask = rememberPermission(Manifest.permission.RECORD_AUDIO) { granted = it }
    LaunchedEffect(Unit) { ask() }
    var got by remember { mutableFloatStateOf(0f) }
    var loud by remember { mutableStateOf(false) }
    val meter = remember { Meter() }
    DisposableEffect(Unit) { onDispose { meter.stop() } }
    // The key is read once: the permission may be granted while the first effect is still starting.
    val ok = granted
    LaunchedEffect(ok) {
        if (ok != true) return@LaunchedEffect
        val listening = meter.start()
        var last = System.currentTimeMillis(); var noisy = 0f
        while (isActive && got < need) {
            val l = if (listening) meter.level() else { delay(100); 0f }
            val now = System.currentTimeMillis(); val dt = (now - last) / 1000f; last = now
            if (l > .05f) { noisy += dt; if (noisy > .4f) { got = 0f; noisy = 0f; loud = true } } else { noisy = 0f; got += dt }
        }
        if (got >= need) done()
    }
    val tick = rememberTick()
    Heading(stringResource(R.string.c_hush))
    BufImage(PetArt.draw(Pet.DORMOUSE, PetPose(f = PetArt.idle(tick * Art.FRAME_MS / 1000.0, 1), sleep = true)), scale = 6.dp)
    if (granted == false) Text(stringResource(R.string.c_mic_denied), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
    else {
        val left = (need - got).toInt().coerceAtLeast(0)
        Text("%d:%02d".format(left / 60, left % 60), style = T.Number)
        Text(if (loud) stringResource(R.string.c_hush_loud) else stringResource(R.string.c_hush_hint, if (need >= 60) Plural.minutes(need / 60, ctx.ru) else Plural.seconds(need, ctx.ru)),
            style = T.Body.copy(color = if (loud) C.crimL else C.paperD), textAlign = TextAlign.Center)
    }
    PixelButton(stringResource(R.string.cancel), back, kind = Kind.PALE, small = true)
}

@Composable
private fun LightStep(done: (Cap) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<File?>(null) }
    fun took(name: String?) { if (name != null) done(Cap(Catch.LIGHT, photo = name, color = Files.lightOf(ctx, name))) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pending
        if (ok && f != null && f.length() > 0) took(Files.savePhoto(ctx, f.readBytes()))
        f?.delete(); pending = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> if (uri != null) took(Files.savePhoto(ctx, uri)) }
    Heading(stringResource(R.string.m_light), stringResource(R.string.c_light_hint))
    PixelButton(stringResource(R.string.c_shoot), {
        val f = File(File(ctx.cacheDir, "shots").apply { mkdirs() }, UUID.randomUUID().toString() + ".jpg")
        pending = f
        runCatching { camera.launch(FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)) }.onFailure { pending = null }
    }, kind = Kind.BRASS)
    PixelButton(stringResource(R.string.c_pick), { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, kind = Kind.WOOD, small = true)
    PixelButton(stringResource(R.string.cancel), back, kind = Kind.PALE, small = true)
}

@Composable
private fun WalkStep(done: (Cap) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val perm = if (Build.VERSION.SDK_INT >= 29) Manifest.permission.ACTIVITY_RECOGNITION else null
    var granted by remember { mutableStateOf<Boolean?>(null) }
    val ask = rememberPermission(perm) { granted = it }
    LaunchedEffect(Unit) { ask() }
    var steps by remember { mutableStateOf<Long?>(null) }
    var total by remember { mutableStateOf<Long?>(null) }
    // The key is read once: the permission may be granted while the first effect is still starting.
    val ok = granted
    LaunchedEffect(ok) {
        if (ok != true) return@LaunchedEffect
        val t = Steps.total(ctx) ?: return@LaunchedEffect
        val s = c.settings.now()
        if (s.stepBase < 0 || t < s.stepBase) c.settings.update { it.copy(stepBase = t, stepBaseAt = System.currentTimeMillis()) }
        total = t; steps = t - maxOf(0, if (s.stepBase < 0 || t < s.stepBase) t else s.stepBase)
    }
    Heading(stringResource(R.string.m_walk))
    when {
        !Steps.available(ctx) -> Text(stringResource(R.string.c_walk_none), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
        granted == false -> Text(stringResource(R.string.c_walk_perm), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
        steps != null -> {
            Text(stringResource(R.string.c_walk_n), style = T.Body.copy(color = C.paperD))
            Text("%,d".format(steps), style = T.Number)
            if (steps!! >= Steps.MIN_MANUAL) PixelButton(stringResource(R.string.c_roll), {
                c.write { settings.update { it.copy(stepBase = total ?: it.stepBase, stepBaseAt = System.currentTimeMillis()) } }
                done(Cap(Catch.WALK, steps = steps!!.toInt()))
            }, kind = Kind.CRIM)
            else Text(stringResource(R.string.c_walk_few, Steps.MIN_MANUAL), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
            Text(stringResource(R.string.c_walk_auto), style = T.Small.copy(color = C.paperD), textAlign = TextAlign.Center)
        }
    }
    PixelButton(stringResource(R.string.cancel), back, kind = Kind.PALE, small = true)
}

@Composable
private fun NameStep(h: Home, cap: Cap, pick: (Tag) -> Unit, back: () -> Unit) {
    val tags = Tag.entries.filter { it != Tag.QUIET && (cap.kind == Catch.SOUND || it != Tag.WALK) }
    Heading(stringResource(R.string.c_what))
    for (row in tags.chunked(2)) Row(Modifier.fillMaxWidth().widthIn(max = 380.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (t in row) {
            val warm = cap.kind == Catch.LIGHT && t == Tag.WARM
            Row(Modifier.weight(1f).pixelBox(if (warm) C.crim else C.brass).clickable { pick(t) }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BufImage(PetArt.portrait(t.pet, dark = t.pet !in h.lodgers), scale = 1.dp)
                Column {
                    Text(stringResource(Names.tag(t)), style = T.Body.copy(color = if (warm) C.paper else C.ink))
                    Text(stringResource(Names.tagHint(t)), style = T.Small.copy(color = if (warm) C.paperD else C.brassD))
                }
            }
        }
        if (row.size == 1) Box(Modifier.weight(1f))
    }
    PixelButton(stringResource(R.string.c_toss), back, kind = Kind.PALE, small = true)
}

@Composable
private fun SealStep(cap: Cap, tag: Tag, done: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    var draft by remember { mutableStateOf<Jar?>(null) }
    var label by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val j = c.draft(tag, cap.kind, ctx.getString(Names.label(tag))).copy(sound = cap.sound, photo = cap.photo, color = cap.color, steps = cap.steps)
        val first = c.firstSnow(j)
        draft = if (first) j.copy(label = ctx.getString(R.string.label_first_snow), rare = true) else j
        label = draft!!.label
    }
    val tick = rememberTick()
    var start by remember { mutableIntStateOf(-1) }
    LaunchedEffect(draft != null) { if (draft != null) start = tick }
    val j = draft ?: return
    val k = if (start < 0) 0 else tick - start
    Heading(stringResource(R.string.c_seal))
    BufImage(JarArt.draw(j.look(), 0, fill = (k * Art.FRAME_MS / 720f).coerceAtMost(1f), lid = k * Art.FRAME_MS > 900), scale = 9.dp)
    Field(label, { label = it.take(40) }, stringResource(Names.label(tag)), Modifier.widthIn(max = 320.dp), style = T.Label)
    Text(listOfNotNull(j.wx?.let { stringResource(Names.wx(it)) }, j.temp?.let { (if (it > 0) "+" else "") + it + "°" }, j.place.takeIf { it.isNotBlank() }).joinToString(" · "),
        style = T.Small.copy(color = C.paperD))
    PixelButton(stringResource(R.string.c_shelf), {
        val final = j.copy(label = label.trim().ifEmpty { j.label })
        c.write { roll(final) }
        done()
    }, kind = Kind.CRIM)
}
