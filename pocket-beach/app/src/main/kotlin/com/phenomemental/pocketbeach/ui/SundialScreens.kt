package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.DialMath
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Mark
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.collectUnderscore
import com.phenomemental.pocketbeach.bsp.parseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Sundial 0.10 (spine:pocket-beach 1.1111787197): Sundial's own pages, native.
 * The three lines at the top of every screen open Your places, in Sundial's
 * own form (a name with its line beneath, as drawPlaces makes them); Your
 * days, the strandline and, held, where they have come to rest; Your room, a
 * box and Post; Your hand; Your recipes. And the keep card, shared by Keep
 * today on the dial and "Sort as my day" beneath her reply.
 */

private val DAY_SAID = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)

/** Go to [screen] from Your places: Today and Your pocket are the two at the foot of every way back. */
fun Nav.toPlace(screen: Screen) {
    while (stack.size > 1) stack.removeAt(stack.lastIndex)
    when (screen) {
        Screen.Shore -> {}
        Screen.Ask -> go(Screen.Ask)
        else -> { go(Screen.Ask); go(screen) }
    }
}

/** Your places: Sundial's menu (sundial.html:6011-6250), YOURS and then Settings and About. */
@Composable
fun PlacesSheet(model: Model, nav: Nav, close: () -> Unit) {
    val ink = LocalInk.current
    val app = model.app
    fun item(at: String, fallback: String) = Words.sundial(app, "3.$at", fallback).split(" — ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    val yours = listOf(
        Triple(item("1", "Today — the sundial, as it is now"), Screen.Ask, "places-today"),
        Triple(item("2", "Your days — every day you have kept, the newest first"), Screen.Days, "places-days"),
        Triple(item("3", "Your room — where people leave you a note"), Screen.Room, "places-room"),
        Triple(item("4", "Your hand — what you’re holding"), Screen.Hand, "places-hand"),
        Triple(item("5", "Your pocket — your blocks, in plain groups"), Screen.Shore, "menu-blocks"),
        Triple(item("6", "Your recipes — your cook and kernel"), Screen.Recipes, "places-recipes"),
    )
    val more = listOf(
        Triple(item("7", "Settings — her voice, where you are, your name and your room"), Screen.Settings, "menu-settings"),
        Triple(item("8", "About — what this is, and who made it"), Screen.About, "menu-about"),
    )
    Column(Modifier.fillMaxSize().background(ink.paper).windowInsetsPadding(WindowInsets.safeDrawing).testTag("places")) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 18.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundMark("Close", close, tag = "places-close") { c -> drawNew(c) }
            Spacer(Modifier.weight(1f))
            Text("SUNDIAL", style = Type.label.copy(letterSpacing = 0.5.sp), color = ink.soft)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Label("Yours")
            for ((words, screen, tag) in yours) PlaceRow(words.first, words.second, tag) { close(); nav.toPlace(screen) }
            Rule()
            for ((words, screen, tag) in more) PlaceRow(words.first, words.second, tag) { close(); if (screen == Screen.Settings || screen == Screen.About) nav.go(screen) else nav.toPlace(screen) }
            // The workings, quietly at the foot: the teaching blocks, the bench, the conversation saved as a file, and the workings shown.
            Rule()
            Label("The workings")
            val asking = app.asking
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextLink("Teaching blocks", { close(); nav.toPlace(Screen.Shore) }, tag = "menu-teaching")
                TextLink(if (asking.benchRunning) "See the bench" else "Bench", { close(); nav.go(Screen.Bench) }, tag = "open-bench")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (asking.history.isNotEmpty()) TextLink("Save this conversation", { close(); nav.toPlace(Screen.Ask); model.saveConversation++ }, tag = "save-conversation")
                TextLink(if (app.showDetails) "Show the workings: on" else "Show the workings: off", { app.showDetails = !app.showDetails }, tag = "details")
            }
        }
    }
}

@Composable
private fun PlaceRow(name: String, line: String, tag: String, onClick: () -> Unit) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp).testTag(tag)) {
        Text(name, style = Type.heading.copy(fontStyle = FontStyle.Italic, fontSize = 25.sp), color = ink.ink)
        if (line.isNotEmpty()) Text(line, style = Type.small, color = ink.soft)
    }
}

/** His days kept, read from his now and his P6, up to the beach's today. */
fun keptDays(model: Model): List<Keep.KeptDay> {
    val app = model.app
    val h = Hatch.hatched(app.storage)
    val clock = app.storage.read(Days.NOW)?.let { runCatching { parseJson(it) }.getOrNull() }
    val p6 = h?.let { app.storage.read(Days.p6Of(it)) }?.let { runCatching { parseJson(it) }.getOrNull() }
    return Keep.days(clock, p6, Keep.dayOf(System.currentTimeMillis()))
}

/** Your days: the strandline, newest first; and where they have come to rest. */
@Composable
fun DaysScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val days by produceState<List<Keep.KeptDay>>(emptyList(), model.version) { value = withContext(Dispatchers.IO) { runCatching { keptDays(model) }.getOrDefault(emptyList()) } }
    var resting by rememberSaveable { mutableStateOf(false) }
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Page(onBack = { nav.back() }, top = { Text("Your days", style = Type.name, color = ink.ink) }) {
        Text(Words.sundial(model.app, "1.7", "{n} days", "n" to "${days.size}").let { if (days.size == 1) it.replace(" days", " day") else it }.uppercase(), style = Type.label, color = ink.soft, modifier = Modifier.testTag("days-count"))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TextLink("Newest first", { resting = false }, tag = "days-strand")
            TextLink(Words.sundial(model.app, "4.2", "where your days have come to rest"), { resting = true }, tag = "days-resting")
        }
        if (days.isEmpty()) Text(Words.sundial(model.app, "4.3", "No days kept yet. Keep today, and the first comes to rest here."), style = Type.aside, color = ink.soft)
        if (!resting) {
            for (d in days.asReversed()) StrandRow(d, open == d.address) { open = if (open == d.address) null else d.address }
        } else {
            Pools(model, days, picked) { picked = it }
        }
    }
}

/** One day on the strandline: its shell in its colours, its date, its title, the times he spoke (sundial.html:3212-3254). */
@Composable
private fun StrandRow(d: Keep.KeptDay, open: Boolean, onClick: () -> Unit) {
    val ink = LocalInk.current
    val zone = ZoneId.systemDefault()
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("strand:${d.address}"), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Coil(d.places, Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text((d.date?.format(DAY_SAID) ?: d.address).uppercase(), style = Type.label, color = ink.soft)
            Text(d.title, style = Type.body.copy(fontStyle = FontStyle.Italic, fontSize = 22.sp), color = ink.ink)
            for (m in d.moments.take(if (open) 9 else 3)) {
                val time = m.atMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalTime().let { t -> "%02d:%02d".format(t.hour, t.minute) } } ?: ""
                Text("$time  ${Words.quoted(m.words, if (open) 2000 else 80)}", style = Type.small, color = ink.soft)
            }
            if (open) {
                d.text?.let { Text(it, style = Type.body, color = ink.ink, modifier = Modifier.padding(top = 6.dp)) }
                if (d.pieces.isNotEmpty()) Text(d.pieces.joinToString("\n") { (p, w) -> "${Mark.POOLS[p - 1].phrase}: $w" }, style = Type.small, color = ink.soft)
                if (d.places.isNotEmpty()) Text(Mark.placesWords(d.places).replaceFirstChar { it.uppercase() } + ".", style = Type.aside, color = ink.soft)
            }
        }
    }
}

/** A day's shell, seen from above: a flat spiral in ink, the stretch of each place it touched banded in its hue (mark.js:943-976). */
@Composable
fun Coil(places: List<Int>, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val pts = remember { DialMath.coilPoints() }
    Canvas(modifier.semantics { contentDescription = if (places.isEmpty()) "a day" else "a day of " + Mark.placesSaid(places) }) {
        val s = size.minDimension / 64f
        val c = Offset(size.width / 2, size.height / 2)
        fun path(from: Double, to: Double): Path {
            val p = Path()
            var first = true
            for (q in pts) if (q.at >= from - 1e-9 && q.at <= to + 1e-9) {
                val o = Offset(c.x + q.x.toFloat() * s, c.y + q.y.toFloat() * s)
                if (first) { p.moveTo(o.x, o.y); first = false } else p.lineTo(o.x, o.y)
            }
            return p
        }
        for (b in DialMath.coilBands(places)) drawPath(path(b.from, b.to), Color(Mark.POOLS[b.n - 1].hue.toInt()), alpha = 0.55f, style = Stroke(5f * s, cap = StrokeCap.Round))
        drawPath(path(0.0, 1.0), ink.ink, style = Stroke(1.2f * s, cap = StrokeCap.Round))
        drawCircle(ink.crimson, radius = 1.6f * s, center = c)
    }
}

/** Where his days have come to rest: the six places, each with how many days rest there; a place picked shows them (read only, Sundial 0.10). */
@Composable
private fun Pools(model: Model, days: List<Keep.KeptDay>, picked: Int?, pick: (Int?) -> Unit) {
    val ink = LocalInk.current
    val counts = Keep.counts(days)
    val total = counts.sum().coerceAtLeast(1)
    for (n in Mark.P6) {
        val i = n - 1
        val pool = Mark.POOLS[i]
        Row(Modifier.fillMaxWidth().clickable { pick(if (picked == i) null else i) }.testTag("pool:$i"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val r = if (counts[i] == 0) 7.dp else (8 + 16 * kotlin.math.sqrt(counts[i].toDouble() / total)).dp
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(r * 2).then(if (counts[i] == 0) Modifier.border(1.5.dp, ink.rule, CircleShape) else Modifier.background(Color(pool.hue.toInt()).copy(alpha = 0.9f), CircleShape)))
            }
            Column(Modifier.weight(1f)) {
                Text(pool.phrase, style = Type.body.copy(fontStyle = FontStyle.Italic), color = ink.ink)
                Text(if (counts[i] == 1) "one day" else "${Words.number(counts[i])} days", style = Type.small, color = ink.soft)
            }
        }
        if (picked == i) {
            val resting = Keep.restingIn(days, n, n = 81)
            if (resting.isEmpty()) {
                Text(Words.sundial(model.app, "4.1", "Nothing has come to rest at {phrase}.", "phrase" to pool.phrase), style = Type.aside, color = ink.soft, modifier = Modifier.testTag("pool-empty"))
                Text(pool.question, style = Type.body, color = ink.ink)
            } else for ((d, body) in resting.asReversed()) {
                Sheet(Modifier.testTag("resting:${d.address}")) {
                    Text((d.date?.format(DAY_SAID) ?: d.address).uppercase(), style = Type.label, color = ink.soft, modifier = Modifier.padding(top = 6.dp))
                    Text(body, style = Type.body, color = ink.ink, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

/** Your room: a box and Post; the notes sent from the phone; and what reaching the beach will add (spine:pocket-beach 1.111178719918). */
@Composable
fun RoomScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    val room by produceState<com.phenomemental.pocketbeach.bsp.Room?>(null, model.version) { value = withContext(Dispatchers.IO) { runCatching { PoolPost.room(app.storage) }.getOrNull() } }
    var words by rememberSaveable { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    var posting by remember { mutableStateOf(false) }
    Page(onBack = { nav.back() }, top = { Text("Your room", style = Type.name, color = ink.ink) }) {
        val r = room
        if (r == null) {
            Text(Words.sundial(app, "5.4", "Your room waits for your shell: once you have a name and a room, the box is here."), style = Type.body, color = ink.ink, modifier = Modifier.testTag("room-waits"))
        } else {
            Text("pool:${r.pool}, as ${r.author}", style = Type.small, color = ink.soft, modifier = Modifier.testTag("room-name"))
            PaperField(words, { words = it }, "A note to your room", modifier = Modifier.fillMaxWidth(), tag = "room-box")
            Action("Post", {
                val text = words.trim()
                if (text.isEmpty() || posting) return@Action
                posting = true
                scope.launch {
                    val posted = withContext(Dispatchers.IO) { runCatching { app.post(PoolPost.prepare(r.pool, text, r)) } }
                    posting = false
                    note = posted.fold({ p ->
                        when (p) {
                            is Posted.Landed -> {
                                app.notesSent.add(text, System.currentTimeMillis(), p.slot)
                                words = ""
                                Words.sundial(app, "5.2", "Posted to your room, entry {n}.", "n" to p.slot)
                            }
                            is Posted.Refused -> p.why
                            is Posted.Unreached -> "${p.why} It's kept, to post when you tap Post now on Your pocket."
                        }
                    }, { it.message ?: "The note couldn't be made ready." })
                }
            }, Modifier.fillMaxWidth(), enabled = words.isNotBlank() && !posting, strong = true, tag = "room-post")
            note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("room-note")) }
            Text(Words.sundial(app, "5.1", "Write a note and tap Post. Or tell her any time, 'post this to my room', and she'll set it ready for you to send."), style = Type.small, color = ink.soft)
            val sent = app.notesSent.all
            if (sent.isNotEmpty()) {
                Label(Words.sundial(app, "5.5", "Notes you have sent"))
                for (n in sent) Column(Modifier.testTag("sent-note")) {
                    Text(Words.quoted(n.words, 600), style = Type.body, color = ink.ink)
                    Text("${Words.at(n.atMs).replaceFirstChar { it.uppercase() }}, entry ${n.entry}", style = Type.small, color = ink.soft)
                }
            }
        }
        Rule()
        Text(Words.sundial(app, "5.3", "Soon, the notes others leave you will be here too, with a dot when one is new."), style = Type.aside, color = ink.soft)
    }
}

/** Your hand: what he's holding, from his ahead, pile by pile. */
@Composable
fun HandScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val h = Hatch.hatched(model.app.storage)
    val piles by produceState<List<Pair<String, List<String>>>>(emptyList(), model.version) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val a = h?.let { model.app.storage.read(Days.aheadOf(it)) }?.let { parseJson(it) as? JsObj } ?: return@runCatching emptyList()
                (1..9).mapNotNull { k ->
                    val pile = a["$k"] as? JsObj ?: return@mapNotNull null
                    val cards = (1..9).mapNotNull { c -> pile["$c"]?.let { (it as? com.phenomemental.pocketbeach.bsp.JsStr)?.value ?: collectUnderscore(it) }?.trim()?.takeIf { it.isNotEmpty() } }
                    if (cards.isEmpty()) null else (collectUnderscore(pile)?.replaceFirstChar { it.uppercase() } ?: "Pile $k") to cards
                }
            }.getOrDefault(emptyList())
        }
    }
    Page(onBack = { nav.back() }, top = { Text("Your hand", style = Type.name, color = ink.ink) }) {
        if (piles.isEmpty()) Text("Nothing on your hand yet. When you tell her what you mean to do, it comes here.", style = Type.aside, color = ink.soft)
        for ((pile, cards) in piles) {
            Label(pile)
            for (c in cards) Text(c, style = Type.body, color = ink.ink, modifier = Modifier.testTag("hand-card"))
        }
        if (h != null) TextLink("Open it in your pocket", { nav.go(Screen.Block(Region.POCKET, Days.aheadOf(h), "")) }, tag = "hand-open")
    }
}

/** Your recipes: his cook and kernel, his to change; change a line and she behaves differently. */
@Composable
fun RecipesScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val h = Hatch.hatched(model.app.storage)
    Page(onBack = { nav.back() }, top = { Text("Your recipes", style = Type.name, color = ink.ink) }) {
        Text("Your cook holds what she does with your words: each step a sentence. Your kernel says which of them each moment runs. Change a line, and she does what it now says.", style = Type.body, color = ink.ink)
        if (h == null) Text("They come with your shell.", style = Type.aside, color = ink.soft)
        else {
            TextLink("Your cook", { nav.go(Screen.Block(Region.POCKET, com.phenomemental.pocketbeach.bsp.Cook.cookOf(h), "")) }, style = Type.body, tag = "recipes-cook")
            TextLink("Your kernel", { nav.go(Screen.Block(Region.POCKET, com.phenomemental.pocketbeach.bsp.Cook.kernelOf(h), "")) }, style = Type.body, tag = "recipes-kernel")
            Text("Sundial's ways are at 6 of your cook: keeping today (6.1), placing (6.2), the greeting (6.3) and listening (6.4).", style = Type.small, color = ink.soft)
        }
    }
}

/**
 * The keep card (spine:pocket-beach 1.1111787197, sundial.html:2309-2325): a
 * short title from his words, two to four lines in the second person, tomorrow
 * if he voiced one, the places as coloured dots he can tap off or on, his
 * moments kept whole, and his hand. Only Keep it writes it.
 */
@Composable
fun KeepCardView(
    model: Model,
    card: Days.DayCard,
    writing: Boolean,
    onPlace: (Int) -> Unit,
    onTick: (Int) -> Unit,
    onKeep: () -> Unit,
    onNotNow: () -> Unit,
) {
    val ink = LocalInk.current
    val app = model.app
    val today = Keep.dayOf(System.currentTimeMillis())
    Column(Modifier.fillMaxWidth().testTag("keep-card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (card.yesterday) Text(Words.sundial(app, "2.5", "You spoke yesterday and didn't keep it."), style = Type.aside, color = ink.crimson, modifier = Modifier.testTag("keep-card-yesterday"))
        Text(((if (card.day == today) "Today, " else if (card.day == today.minusDays(1)) "Yesterday, " else "") + card.day.format(DAY_SAID)).uppercase(), style = Type.label, color = ink.soft)
        card.sheet?.let { s ->
            if (card.his == null) {
                Text(s.title, style = Type.title.copy(fontSize = 30.sp), color = ink.ink, modifier = Modifier.testTag("keep-title"))
                for (l in s.lines) Text(l, style = Type.body, color = ink.ink, modifier = Modifier.testTag("keep-line"))
                if (s.tomorrow.isNotBlank()) Text("Tomorrow: ${s.tomorrow}", style = Type.aside, color = ink.ink)
            }
        }
        card.his?.let { Text(Words.sundial(app, "2.4", "Today already holds your own words; Keep it adds your moments."), style = Type.aside, color = ink.soft, modifier = Modifier.testTag("keep-his")) }
        // The places as coloured dots, his to tap off or on.
        if (card.his == null && card.sheet != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                for (n in Mark.P6) {
                    val on = n in card.places
                    val hue = Color(Mark.POOLS[n - 1].hue.toInt())
                    Box(
                        Modifier.size(30.dp).clickable(enabled = !writing) { onPlace(n) }.testTag("keep-place:$n")
                            .semantics { contentDescription = Mark.POOLS[n - 1].phrase + if (on) ", on" else ", off" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(18.dp).then(if (on) Modifier.background(hue, CircleShape) else Modifier.border(1.5.dp, hue.copy(alpha = 0.6f), CircleShape)))
                    }
                }
            }
            Text(
                if (card.places.isEmpty()) Mark.OWN else Mark.placesWords(card.places).replaceFirstChar { it.uppercase() } + ".",
                style = Type.aside, color = ink.soft, modifier = Modifier.testTag("keep-places"),
            )
            Text(Words.sundial(app, "2.7", "Tap a colour to take it off, or on."), style = Type.small, color = ink.soft)
        }
        if (card.moments.isNotEmpty()) Text(
            "${if (card.moments.size == 1) "One moment" else "${Words.number(card.moments.size).replaceFirstChar { it.uppercase() }} moments"} in your own words, each kept at its minute.",
            style = Type.small, color = ink.soft, modifier = Modifier.testTag("keep-moments"),
        )
        if (card.lines.isNotEmpty()) {
            Label("Your hand, and what's booked")
            card.lines.forEachIndexed { i, l ->
                Row(Modifier.fillMaxWidth().testTag("day-line"), verticalAlignment = Alignment.Top) {
                    Checkbox(checked = l.ticked, onCheckedChange = { onTick(i) }, enabled = !writing, colors = CheckboxDefaults.colors(checkedColor = ink.crimson, uncheckedColor = ink.soft), modifier = Modifier.testTag("day-tick:$i"))
                    Column(Modifier.weight(1f).padding(top = 10.dp, start = 4.dp)) {
                        Text(l.words, style = Type.body, color = if (l.ticked) ink.ink else ink.soft, modifier = Modifier.testTag("day-words:$i"))
                        Text(Days.name(l.where), style = Type.small, color = ink.soft)
                    }
                }
            }
        }
        Text(Words.sundial(app, "2.8", "Nothing is kept until you tap Keep it."), style = Type.small, color = ink.soft)
        if (writing) Text("Keeping your day…", style = Type.aside, color = ink.soft)
        else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Action(Words.sundial(app, "2.3", "Not now"), onNotNow, Modifier.weight(1f), tag = "day-not-now")
            Action(Words.sundial(app, "2.2", "Keep it"), onKeep, Modifier.weight(1f), strong = true, tag = "day-yes")
        }
    }
}

/** Sundial's small heart, as on Keep today and the mark (sundial.html:2321): ring and line stroked in ink, the crimson centre. */
@Composable
fun SmallHeart(modifier: Modifier = Modifier, ink: Color = LocalInk.current.ink, crimson: Color = LocalInk.current.crimson) {
    val ring = remember { androidx.compose.ui.graphics.vector.PathParser().parsePathString(Heart.SMALL_RING).toPath() }
    val line = remember { androidx.compose.ui.graphics.vector.PathParser().parsePathString(Heart.SMALL_LINE).toPath() }
    Canvas(modifier) {
        val s = minOf(size.width / 390f, size.height / 325f)
        val ox = (size.width - 390f * s) / 2f
        val oy = (size.height - 325f * s) / 2f - 10f * s
        drawContext.transform.translate(ox, oy)
        drawContext.transform.scale(s, s, Offset.Zero)
        drawPath(ring, ink, style = Stroke(24f, cap = StrokeCap.Round))
        drawPath(line, ink, style = Stroke(16f, cap = StrokeCap.Round))
        drawContext.transform.rotate(Heart.CENTRE_TURN, Offset(Heart.CENTRE_X, Heart.CENTRE_Y))
        drawOval(crimson, topLeft = Offset(Heart.CENTRE_X - Heart.CENTRE_RX, Heart.CENTRE_Y - Heart.CENTRE_RY), size = androidx.compose.ui.geometry.Size(2 * Heart.CENTRE_RX, 2 * Heart.CENTRE_RY))
    }
}

/** Sundial's mark (spine:pocket-beach 1.1111787191, 1.111178719913): the small heart with nine ink stones round it, on paper. */
@Composable
fun SundialMark(modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val w = listOf(4.4f, 3.8f, 5.0f, 4.1f, 3.6f, 4.7f, 3.9f, 4.5f, 4.2f)
    val h = listOf(3.2f, 2.9f, 3.6f, 3.0f, 2.8f, 3.4f, 3.1f, 3.5f, 2.95f)
    Box(modifier.background(Color(0xFFEEE7D5), androidx.compose.foundation.shape.RoundedCornerShape(22)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.minDimension / 108f
            val c = Offset(54 * u, 54 * u)
            for (k in 0 until 9) {
                val th = Math.toRadians(-90.0 + 40 * k)
                val at = Offset(c.x + 29.5f * u * Math.cos(th).toFloat(), c.y + 29.5f * u * Math.sin(th).toFloat())
                drawContext.canvas.save()
                drawContext.transform.rotate((Math.toDegrees(th) + 90).toFloat(), at)
                drawOval(Color(0xFF1D1A17), topLeft = Offset(at.x - w[k] * 1.15f / 2 * u, at.y - h[k] * 1.15f / 2 * u), size = androidx.compose.ui.geometry.Size(w[k] * 1.15f * u, h[k] * 1.15f * u))
                drawContext.canvas.restore()
            }
        }
        SmallHeart(Modifier.fillMaxSize(0.38f), ink = Color(0xFF1D1A17), crimson = Color(0xFFC21D4E))
    }
}
