@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.phenomemental.pocketbeach.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.Towns
import com.phenomemental.pocketbeach.WhereHeStands
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Mark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/*
 * Sundial 0.10, Today (spine:pocket-beach 1.1111787192, 1.1111787193 and the
 * decisions at 1.11117871991): the dial, live on the real sun; tap it for
 * your days, hold it for where they rest; the day before offered first
 * when its words still wait; her one question, or Sundial's own while she
 * isn't ready; the keep card. Her conversation follows beneath, in the
 * thread AskScreen draws.
 */

/** The beach's day (UTC): one today, everywhere a day is meant (1.111178719915). */
fun beachToday(): LocalDate = Keep.dayOf(System.currentTimeMillis())

/** Sundial's own question for the part of the day, by the phone's clock: "What’s with you this morning?" (sundial.html:2783-2791). */
fun ownQuestion(model: Model, nowMs: Long = System.currentTimeMillis()): String {
    val t = java.time.Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()).toLocalTime()
    val part = minOf(8, (t.toSecondOfDay() / 9600))
    val words = listOf("tonight", "this dawn", "this early morning", "this morning", "at midday", "this afternoon", "this late afternoon", "this evening", "tonight")[part]
    return Words.sundial(model.app, "1.2", "What’s with you {part}?", "part" to words)
}

@Composable
fun TodayDial(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    val asking = app.asking
    val context = LocalContext.current

    // The tick: every 30 s the light and shadow move, eased (sundial.html:2787-2800).
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); tick = System.currentTimeMillis() } }
    val today = Keep.dayOf(tick)
    // (b) A new day: older words let go; yesterday's offered first.
    LaunchedEffect(today) { withContext(Dispatchers.IO) { runCatching { app.waiting.letGo(today) } } }

    val place = app.where.place
    val days by produceState<List<Keep.KeptDay>>(emptyList(), model.version, today, asking.lastKept) {
        value = withContext(Dispatchers.IO) { runCatching { keptDays(model) }.getOrDefault(emptyList()) }
    }
    val moments by produceState<List<Long>>(emptyList(), model.version, today, app.waiting.all) {
        value = withContext(Dispatchers.IO) { runCatching { asking.momentsOf(today).map { it.atMs } }.getOrDefault(emptyList()) }
    }
    val state = DialState(days, moments, place?.lat, place?.lon, ZoneId.systemDefault(), tick, asking.lastKept?.let { Keep.dayAddress(it) })
    var pooled by rememberSaveable { mutableStateOf(false) }
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    // Sundial 0.10.1: held, the whole dial comes into view, clear of the bar beneath, so no caption is under it.
    val whole = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    LaunchedEffect(pooled) { if (pooled) runCatching { whole.bringIntoView() } }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Dial(
            state, answering = asking.stage is Asking.Stage.Thinking || asking.keeping,
            onTap = { if (pooled) { pooled = false; picked = null } else nav.go(Screen.Days) },
            onHold = { pooled = !pooled; picked = null },
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f).bringIntoViewRequester(whole),
            pooled = pooled, picked = picked, onPool = { picked = if (picked == it) null else it },
        )
    }
    if (pooled) {
        val i = picked
        if (i == null) Text(Words.sundial(app, "4.2", "where your days have come to rest"), style = Type.aside, color = ink.soft, modifier = Modifier.testTag("pools-said"))
        else {
            val pool = Mark.POOLS[i]
            val resting = Keep.restingIn(days, i + 1, n = 9)
            Text(pool.phrase, style = Type.heading.copy(fontStyle = FontStyle.Italic), color = ink.ink, modifier = Modifier.testTag("pool-phrase"))
            if (resting.isEmpty()) {
                Text(Words.sundial(app, "4.1", "Nothing has come to rest at {phrase}.", "phrase" to pool.phrase), style = Type.aside, color = ink.soft)
                Text(pool.question, style = Type.body, color = ink.ink)
            } else for ((d, body) in resting.asReversed().take(3)) Text("${d.title}\n${Words.quoted(body, 240)}", style = Type.small, color = ink.ink)
        }
    } else if (days.isNotEmpty() || moments.isNotEmpty()) {
        Text(Words.sundial(app, "1.1", "tap the sundial for your days · hold it for where they rest"), style = Type.aside, color = ink.soft, modifier = Modifier.fillMaxWidth().testTag("dial-invite"))
    }

    // In summer, between midnight and one, the beach's day hasn't turned: said gently, in summer only.
    // True where summer time is an hour ahead of the beach's clock (Britain, Ireland, Portugal); elsewhere the day turns at another hour, and nothing is said.
    val localToday = LocalDate.now()
    val instant = java.time.Instant.ofEpochMilli(tick)
    val rules = ZoneId.systemDefault().rules
    if (localToday != today && rules.isDaylightSavings(instant) && rules.getOffset(instant).totalSeconds == 3600) {
        Text(Words.sundial(app, "1.3", "Your day turns at 1 am in summer."), style = Type.small, color = ink.soft, modifier = Modifier.testTag("summer"))
    }

    // Where he stands: asked once, the phone's rough location; else a town in Settings.
    WhereAsk(model, nav)

    // (b) The day before, offered first, before today's question.
    val yesterday = app.waiting.yesterday(today)
    val card = asking.keepCard
    if (yesterday != null && card == null && !asking.keeping) {
        Column(Modifier.fillMaxWidth().testTag("yesterday-offer"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Words.sundial(app, "2.5", "You spoke yesterday and didn't keep it."), style = Type.body.copy(fontStyle = FontStyle.Italic), color = ink.ink)
            Action("Keep yesterday", { asking.keepToday(yesterday) }, Modifier.fillMaxWidth(), tag = "keep-yesterday")
        }
    } else if (asking.history.none { Keep.dayOf(it.at) == today } && card == null) {
        // Her one question, with his tomorrow line before it; Sundial's own while she isn't ready.
        val g = asking.greeting?.takeIf { it.day == today }
        g?.tomorrow?.let { Text("You said: “Tomorrow, $it.”", style = Type.body.copy(fontStyle = FontStyle.Italic), color = ink.ink, modifier = Modifier.testTag("greet-said")) }
        Text(g?.question ?: ownQuestion(model, tick), style = if (g?.question != null) Type.body.copy(fontSize = Type.heading.fontSize) else Type.heading.copy(fontStyle = FontStyle.Italic), color = ink.ink, modifier = Modifier.testTag(if (g?.question != null) "greet-question" else "own-question"))
    }

    // Sundial 0.10.1 (item 11): the one keep card, from his tap of Keep today or his words asking for it, brought into view as it is set out.
    val cardInView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    LaunchedEffect(card?.walk) { if (card != null) { delay(300); runCatching { cardInView.bringIntoView() } } }
    card?.let { k ->
        Column(Modifier.fillMaxWidth().bringIntoViewRequester(cardInView), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KeepCardView(model, k.card, asking.keeping, onPlace = { asking.placeKeep(it) }, onTick = { asking.tickKeep(it) }, onKeep = { asking.keepIt() }, onNotNow = { asking.notNowKeep() }, onPlaceMoment = { i, n -> asking.placeKeepMoment(i, n) })
            k.fellBack?.let { Text(it, style = Type.small, color = ink.crimson) }
        }
    }
    if (asking.keeping && card == null) Text("Setting out your day…", style = Type.aside, color = ink.soft, modifier = Modifier.testTag("keeping"))
    // Sundial 0.10.1: a day kept shows as its shell, its title beneath; only what went wrong is said in words.
    asking.keptShell?.takeIf { card == null && !asking.keeping }?.let { KeptShellView(model, nav, it) }
    asking.keepNote?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("keep-note")) }
}

/** The Keep today button, beside Send: Sundial's small heart and its words (sundial.html:2479-2482). */
@Composable
fun KeepTodayButton(model: Model, enabled: Boolean) {
    val ink = LocalInk.current
    Row(
        Modifier.border(1.dp, ink.crimson, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, role = Role.Button) { model.app.asking.keepToday() }
            .padding(horizontal = 10.dp, vertical = 8.dp).testTag("keep-today"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SmallHeart(Modifier.size(22.dp, 18.dp))
        Text(Words.sundial(model.app, "2.1", "Keep today"), style = Type.action.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = if (enabled) ink.crimson else ink.soft)
    }
}

/**
 * Her reply in Sundial's shape: the reflection upright, the question in
 * italic beneath (sundial.html:2294-2301). Sundial 0.10.2: her words as she
 * wrote them, once checked: a closing sentence ending in "?" is the question,
 * in italic, and what comes before it part one; with no closing question,
 * plain words (HerReply.split).
 */
fun replyShaped(parts: Map<String, String>?, said: String): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    // Sundial 0.10.1 (item 10): checked once more as it is shown, whatever way it came: no app line, no keep status, no quote marks wrapping it.
    val hers = { t: String -> com.phenomemental.pocketbeach.bsp.Talking.hers(t) }
    // Sundial 0.10.1: ANSWER, her answer as she gave it, plain.
    parts?.get("THE ANSWER")?.let(hers)?.takeIf { it.isNotBlank() }?.let { append(it.trim()); return@buildAnnotatedString }
    val whole = parts?.filterKeys { it != com.phenomemental.pocketbeach.bsp.HerReply.PLACES }?.mapValues { hers(it.value) }?.takeIf { p -> p.isNotEmpty() && p.values.all { it.isNotBlank() } }
    // Sundial 0.10.2: of what is shown, a closing sentence ending in "?" is her question, in italic, and what comes before it part one; no closing question, plain words.
    val (reflection, question) = if (whole != null) (whole["THE REFLECTION"].orEmpty() to whole["THE QUESTION"].orEmpty()) else com.phenomemental.pocketbeach.bsp.HerReply.split(hers(said))
    if (reflection.isNotBlank()) append(reflection.trim())
    if (question.isNotBlank()) {
        if (reflection.isNotBlank()) append("\n")
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(question.trim()) }
    }
}

/** Where he stands, for the sun: asked once with his permission; declined or unknown, a town chosen in Settings. */
@Composable
private fun WhereAsk(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    val context = LocalContext.current
    var note by remember { mutableStateOf<String?>(null) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        app.where.asked = true
        if (granted && !roughly(context, app.where)) note = "The phone hasn't a fix yet. Choose your town in Settings, or come back in a moment."
    }
    if (app.where.place != null) return
    Column(Modifier.fillMaxWidth().testTag("where-ask"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("The dial follows the real sun where you are. Roughly where is enough: a town.", style = Type.small, color = ink.soft)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!app.where.asked) TextLink("Use this phone's rough location", {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    app.where.asked = true
                    if (!roughly(context, app.where)) note = "The phone hasn't a fix yet. Choose your town in Settings."
                } else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }, tag = "where-phone")
            TextLink("Choose a town", { nav.go(Screen.Settings) }, tag = "where-town")
        }
        note?.let { Text(it, style = Type.small, color = ink.crimson) }
    }
}

/**
 * The phone's last known rough fix, rounded to a tenth of a degree, named by
 * the nearest town on the list; kept on the phone, never sent. False when the
 * phone has none to give.
 */
fun roughly(context: android.content.Context, where: WhereHeStands): Boolean {
    if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
    val lm = context.getSystemService(LocationManager::class.java) ?: return false
    val fix: Location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER)
        .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }.maxByOrNull { it.time } ?: return false
    val (lat, lon) = WhereHeStands.rounded(fix.latitude, fix.longitude)
    val near = runCatching { Towns.all(context).take(20_000).minByOrNull { t -> (t.lat - lat) * (t.lat - lat) + (t.lon - lon) * (t.lon - lon) * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lat)) } }.getOrNull()
    where.set(WhereHeStands.Place(near?.let { "near ${it.name}" } ?: "where the phone said", lat, lon, "phone"))
    return true
}
