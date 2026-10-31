package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.Planned
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Proposal
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.model.FetchService
import com.phenomemental.pocketbeach.model.ModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PRINT_SHOWN = "‹its fingerprint, made as it is written›"

/**
 * Ask the companion: Gemma, on this phone, wearing the shell pocket-companion.
 * 0.6.3, a plain chat: the thread of his words and hers, newest at the foot;
 * the box pinned beneath it, with his voice and Ask beside it, empty again
 * when he sends. What waits on his answer waits in the thread, beneath her
 * reply, while he carries on. The workings stay behind Details; saving the
 * conversation and the bench are at the top.
 */
@Composable
fun AskScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val asking = model.app.asking
    val scope = rememberCoroutineScope()
    val shell by produceState<String?>(null, model.version) {
        value = withContext(Dispatchers.IO) { model.pages.position(Region.POCKET, HelloWorld.SHELL)?.text }
    }
    val recipe by produceState<String?>(null, model.version) {
        value = withContext(Dispatchers.IO) { model.pages.position(Region.POCKET, HelloWorld.SHELL, "1")?.text }
    }
    var shellChecked by remember { mutableStateOf(false) }
    LaunchedEffect(shell, model.version) { shellChecked = true }
    val modelState by produceState<ModelStore.State?>(null) {
        while (true) {
            val s = withContext(Dispatchers.IO) { model.app.mind.state() }
            value = s
            delay(if (s is ModelStore.State.Fetching) 1000 else 3000)
        }
    }
    // The model stays loaded, and the conversation open, while Ask is.
    val ready = modelState is ModelStore.State.Ready && shell != null
    DisposableEffect(ready) {
        if (ready) asking.opened()
        // Only a visit that opened the conversation lets it go: not the wait for the model and the shell, ended as they come ready.
        onDispose { if (ready) asking.closed() }
    }
    // 0.5: the conversation, and its workings only behind the Details switch.
    val details = model.app.showDetails
    val context = LocalContext.current
    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = asking.transcript()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) } }
            }
            outcome.onSuccess { model.say(Screen.Ask, "Saved the conversation as a file.") }
                .onFailure { model.say(Screen.Ask, "The conversation wasn't saved: ${it.message ?: "the file couldn't be written"}.") }
        }
    }
    // The thread keeps its foot in view: her reply as it streams, what comes beneath it.
    val scroll = rememberScrollState()
    val last = asking.history.lastOrNull()
    LaunchedEffect(asking.history.size, asking.live, last?.said, last?.waiting, last?.checking, last?.over, asking.stage) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Page(
        onBack = { nav.back() },
        top = { Text("Ask", style = Type.name, color = ink.ink, maxLines = 1) },
        end = {
            if (asking.history.isNotEmpty()) TextLink("Save", { saveTo.launch(asking.fileName()) }, tag = "save-conversation")
            TextLink(if (asking.benchRunning) "See the bench" else "Bench", { nav.go(Screen.Bench) }, tag = "open-bench")
            Text("Details", style = Type.small, color = ink.ink)
            Switch(
                checked = details,
                onCheckedChange = { model.app.showDetails = it },
                colors = SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("details"),
            )
        },
        bottom = if (!ready) null else ({ Composer(asking) }),
        scroll = scroll,
    ) {
        if (details) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("The companion")
            Text(
                if (shell != null) "Wearing ${HelloWorld.SHELL}: ${Words.quoted(shell, 120)}" else "The companion wears a shell, a block of yours that says how it behaves.",
                style = Type.heading.copy(fontSize = Type.body.fontSize, lineHeight = Type.body.lineHeight), color = ink.ink,
                modifier = Modifier.testTag("wearing"),
            )
            Text(
                "She talks as Settings' Talk job says. Then, behind the chat, when your words may ask for a write, the Write job sets out the lines beneath her reply.",
                style = Type.small, color = ink.soft,
            )
            Stroke()
        }
        if (shell == null && shellChecked) {
            Notice(
                "Its shell, ${HelloWorld.SHELL}, isn't in the pocket yet. Making it writes one new block, which opens " +
                    "${Words.quoted(HelloWorld.SHELL_OPENING, 60)} and holds the line that says how to do the hello world.",
                link = "Make the companion's shell" to {
                    scope.launch {
                        val made = withContext(Dispatchers.IO) { runCatching { HelloWorld.makeShell(model.pages, Talks.SHELL_RECIPE) } }
                        model.changed()
                        made.onSuccess { model.say(Screen.Ask, "Made ${HelloWorld.SHELL}.") }
                            .onFailure { model.say(Screen.Ask, it.message ?: "The shell wasn't made.") }
                    }
                },
                tag = "make-shell",
            )
        }
        if (HelloWorld.recipeIsOlder(recipe, Talks.SHELL_RECIPE)) {
            Notice(
                "The companion's shell has an older hello world line. The new one is in the companion's short actions: she writes " +
                    "only the block, your secret and the post, and the app writes the seal, the fingerprint and the message. " +
                    "Updating it changes that one line of ${HelloWorld.SHELL}.",
                link = "Update the hello world line" to {
                    scope.launch {
                        val done = withContext(Dispatchers.IO) { model.pages.carryOut(model.pages.planChange(HelloWorld.SHELL, "1", Talks.SHELL_RECIPE)) }
                        model.changed()
                        model.say(Screen.Ask, if (done.ok) "Updated the hello world line of ${HelloWorld.SHELL}." else done.problem ?: "It wasn't updated.")
                    }
                },
                tag = "update-shell",
            )
        }

        ModelPart(model, modelState)

        // The thread: each thing he said, her reply, and what waits on his answer beneath it.
        asking.history.forEach { e -> Turn(model, e, latest = e === asking.history.lastOrNull(), details = details) }
        if (asking.benchRunning) Notice("The bench is running. Ask can be used once it's over.", tag = "ask-bench-running")
    }
}

/** The box, pinned at the foot: his words, his voice, and Ask (Stop while her reply comes). It empties when he sends. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Composer(asking: Asking) {
    val ink = LocalInk.current
    val voice = rememberVoice { words -> asking.question = if (asking.question.isBlank()) words else asking.question.trimEnd() + " " + words }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (voice.heard.isNotEmpty()) Text(voice.heard, style = Type.aside, color = ink.soft, modifier = Modifier.testTag("speech-heard"))
        voice.note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("speech-note")) }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaperField(
                asking.question, { asking.question = it }, "Say something to the companion",
                modifier = Modifier.weight(1f), tag = "question", minLines = 3, maxLines = 6,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                Action(if (voice.listening) "Listening…" else "Speak", { voice.tap() }, Modifier.width(96.dp), tag = "speak")
                if (asking.stage is Asking.Stage.Thinking) Action("Stop", { asking.stop() }, Modifier.width(96.dp), tag = "stop")
                else Action("Ask", { asking.ask() }, Modifier.width(96.dp), enabled = asking.question.isNotBlank() && !asking.benchRunning, strong = true, tag = "ask")
            }
        }
    }
}

/** One exchange of the thread: his words, her reply, what waits on his answer beneath it, and how it ended. */
@Composable
private fun Turn(model: Model, e: Asking.Exchange, latest: Boolean, details: Boolean) {
    val ink = LocalInk.current
    val asking = model.app.asking
    val thinking = latest && asking.stage is Asking.Stage.Thinking
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("turn")) {
        His(e.question)
        when {
            e.said.isNotEmpty() -> Text(e.said, style = Type.body, color = ink.ink, modifier = Modifier.testTag("say"))
            // Her words as she writes them.
            thinking && asking.live.isNotBlank() -> Text(asking.live, style = Type.body, color = ink.ink, modifier = Modifier.testTag("live"))
        }
        if (thinking) {
            val since = (asking.stage as Asking.Stage.Thinking).since
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(since) { while (true) { delay(1000); now = System.currentTimeMillis() } }
            val secs = ((now - since) / 1000).coerceAtLeast(0)
            val line = when {
                e.sorting != null -> e.sorting!!.let { (done, of) -> "Placing your day, a piece at a time: $done of $of…" }
                asking.live.isNotBlank() -> ""
                !model.app.mind.loaded(model.app.settings(com.phenomemental.pocketbeach.model.Job.TALK)) -> "Loading the companion…"
                else -> "Thinking…"
            } + if (details) " ${secs} s" else ""
            if (line.isNotBlank()) Text(line.trim(), style = Type.aside, color = ink.soft, modifier = Modifier.testTag("thinking"))
        }
        // Behind the chat, the Write job looks at what he said; what it sets out comes here.
        if (e.checking) Text("Seeing whether there's anything to keep…", style = Type.small, color = ink.soft, modifier = Modifier.testTag("checking"))
        when (val w = e.waiting) {
            is Asking.Stage.Shown -> {
                ProposalParts(w, details)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Action("Not now", { asking.notNow(e) }, Modifier.weight(1f), tag = "not-now")
                    Action("Yes, write it", { asking.yes(e) }, Modifier.weight(1f), strong = true, tag = "yes")
                }
            }
            is Asking.Stage.Declining -> {
                ProposalParts(w.shown, details)
                Label("Why not now?")
                Text("One tap says why, so the log keeps it.", style = Type.small, color = ink.soft)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Action("Wrong place", { asking.because("wrong place", e) }, Modifier.weight(1f), tag = "wrong-place")
                        Action("Wrong block", { asking.because("wrong block", e) }, Modifier.weight(1f), tag = "wrong-block")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Action("Misunderstood", { asking.because("misunderstood", e) }, Modifier.weight(1f), tag = "misunderstood")
                        Action("Just not now", { asking.because(null, e) }, Modifier.weight(1f), tag = "just-not-now")
                    }
                    TextLink("Back to the proposal", { asking.back(e) }, tag = "back-to-proposal")
                }
            }
            is Asking.Stage.Choosing -> {
                Choosing(asking, e, w)
                Action("Not now", { asking.dropChoice(e) }, Modifier.fillMaxWidth(), tag = "not-now")
            }
            is Asking.Stage.Carrying -> {
                ProposalParts(w.shown, details)
                Text("Writing…", style = Type.aside, color = ink.soft)
            }
            is Asking.Stage.Written -> {
                val postedBefore = w.carried.post?.let(model.app.posts::landed)
                Written(model, w.shown.asked.proposal!!.seal, w.carried, asking.shellOpening())
                w.carried.post?.let { post ->
                    PostCard(post, postedBefore)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Action("Not now", { asking.dontPost(e) }, Modifier.weight(1f), tag = "dont-post")
                        if (postedBefore == null) Action("Post it", { asking.post(e) }, Modifier.weight(1f), strong = true, tag = "post")
                        else Action("Post again?", { asking.post(e) }, Modifier.weight(1f), tag = "post-again")
                    }
                }
            }
            is Asking.Stage.Posting -> {
                Written(model, w.written.shown.asked.proposal!!.seal, w.written.carried, asking.shellOpening())
                PostCard(w.message, null)
                Text("Posting…", style = Type.aside, color = ink.soft)
            }
            is Asking.Stage.Day -> {
                DayProposed(asking, e, w, details)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Action("Not now", { asking.notNowToDay(e) }, Modifier.weight(1f), tag = "day-not-now")
                    Action("Yes, keep my day", { asking.yesToDay(e) }, Modifier.weight(1f), strong = true, tag = "day-yes")
                }
            }
            is Asking.Stage.DayWriting -> {
                DayProposed(asking, e, w.day, details, moving = false)
                Text("Writing your day…", style = Type.aside, color = ink.soft)
            }
            else -> {}
        }
        e.over?.let { over -> Ended(model, over) }
        // Offered under her reply while it's the last thing said: keeping his words in the block they name, and sorting them as his day.
        if (latest && !thinking) {
            e.keepIn?.let { block -> TextLink("Keep this in $block?", { asking.keep(e) }, tag = "keep-in") }
            if (e.sortable) TextLink("Sort as my day", { asking.sortAsDay(e) }, tag = "sort-as-day")
        }
        // How it ended: in Details always; else only what came of a proposal, or that she couldn't answer.
        if (e.note.isNotEmpty() && (details || e.outcome != null || e.over?.shown == null)) Text(e.note, style = Type.small, color = ink.soft, modifier = Modifier.testTag("over"))
        // Every answer's time and chip, in Details; a GPU that failed, always.
        if (details) {
            if (e.time.isNotEmpty()) Text(e.time, style = Type.small, color = ink.soft, modifier = Modifier.testTag("took"))
        } else if (e.waiting !is Asking.Stage.Day && (e.over?.shown?.pace ?: (e.waiting as? Asking.Stage.Shown)?.pace)?.notes?.contains(com.phenomemental.pocketbeach.model.Pace.GPU_FAILED) == true) {
            Text(com.phenomemental.pocketbeach.model.Pace.GPU_FAILED, style = Type.small, color = ink.soft, modifier = Modifier.testTag("gpu-failed"))
        }
    }
}

/** What saying yes wrote and posted, or the proposal that couldn't go ahead. */
@Composable
private fun Ended(model: Model, over: Asking.Stage.Over) {
    val context = LocalContext.current
    if (over.carried == null) over.shown?.asked?.proposal?.let { if (it.refusal != null) Writes(it) }
    when (val p = over.posted) {
        is Posted.Landed -> {
            Done("Posted to your pool at entry ${p.slot}.", detail = "pool:${p.pool} on beach.happyseaurchin.com", tag = "posted")
            TextLink("Open the beach", {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BEACH_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                    model.say(Screen.Ask, "There's no browser on this phone to open $BEACH_URL.")
                }
            }, tag = "open-beach")
        }
        is Posted.Refused -> Notice(p.why, tag = "not-posted")
        is Posted.Unreached -> {
            Notice("${p.why} It's kept below, and on the Shore, to post when you tap Post now.", tag = "not-posted")
            Waiting(model)
        }
        null -> {}
    }
    over.carried?.let { c -> Written(model, over.shown?.asked?.proposal?.seal ?: "", c, model.app.asking.shellOpening()) }
}

/** What Matthew said, set to the right in a softer hand, as in a conversation. */
@Composable
private fun His(words: String) {
    val ink = LocalInk.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            words, style = Type.body, color = ink.soft, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(0.85f).background(ink.sheet).padding(horizontal = 12.dp, vertical = 6.dp).testTag("his"),
        )
    }
}

/** A quote in her act matches more than one line: each line, to tap the one meant. */
@Composable
private fun Choosing(asking: Asking, e: Asking.Exchange, stage: Asking.Stage.Choosing) {
    val ink = LocalInk.current
    val choice = stage.answered.planned?.choose ?: return
    Text(
        "She means the line “${choice.quote}” in ${choice.block}, and ${choice.lines.size} lines say that. Which one?",
        style = Type.body, color = ink.ink, modifier = Modifier.testTag("which-line"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        choice.lines.forEach { line ->
            Action(Words.quoted(line.text ?: "", 80), { asking.choose(line.address, e) }, Modifier.fillMaxWidth(), tag = "choose:${line.address}")
        }
    }
}

/** His day, sorted: every line in his words, where it lands, to untick or move before Yes. */
@Composable
private fun DayProposed(asking: Asking, e: Asking.Exchange, d: Asking.Stage.Day, details: Boolean, moving: Boolean = true) {
    val ink = LocalInk.current
    Label("Your day, sorted")
    Text(
        "Every line is your words. Untick a line to leave it out, or move it to another place: each move is kept, " +
            "to show her next time how you'd place it. Tap a line to put a misheard word right.",
        style = Type.small, color = ink.soft,
    )
    var choosing by remember { mutableStateOf<Int?>(null) }
    var correcting by remember { mutableStateOf<Int?>(null) }
    val groups = listOf(
        "Today, on your clock" to Telling.DAY,
        "Your hand" to Telling.HAND,
        "The rungs above" to Telling.RUNGS,
    )
    for ((title, places) in groups) {
        val here = d.lines.withIndex().filter { it.value.place in places }
        if (here.isEmpty()) continue
        Sheet(Modifier.testTag("day-group")) {
            Label(title, Modifier.padding(top = 8.dp))
            for (place in places) {
                val at = here.filter { it.value.place == place }
                if (at.isEmpty()) continue
                Text(Landing.name(place), style = Type.name, color = ink.ink, modifier = Modifier.padding(top = 8.dp))
                for ((i, line) in at) DayLine(i, line, moving && place !in Telling.RUNGS, moving, { asking.tick(i, e) }, { choosing = i }, { correcting = i })
            }
        }
    }
    val unplaced = d.lines.withIndex().filter { it.value.place == null }
    if (unplaced.isNotEmpty()) {
        Label("Not placed")
        Text("Sentences of yours she put nowhere. Place one, and it lands there.", style = Type.small, color = ink.soft)
        for ((i, line) in unplaced) DayLine(i, line, moving, moving, {}, { choosing = i }, { correcting = i })
    }
    if (d.sorting.invented.isNotEmpty() && details) {
        Text("Left out, as not your words: " + d.sorting.invented.joinToString("; ") { Words.quoted(it.words, 80) }, style = Type.small, color = ink.soft, modifier = Modifier.testTag("day-invented"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (details) Text(
            "Took %.1f s, %d pieces placed".format(d.shown.took / 1000.0, d.pieces) + (d.shown.pace?.where?.takeIf(String::isNotEmpty)?.let { ", $it" } ?: "") + "." +
                d.shown.pace?.notes?.takeIf { it.isNotEmpty() }?.joinToString(" ", prefix = " ").orEmpty(),
            style = Type.small, color = ink.soft, modifier = Modifier.testTag("took"),
        ) else if (d.shown.pace?.notes?.contains(com.phenomemental.pocketbeach.model.Pace.GPU_FAILED) == true) {
            Text(com.phenomemental.pocketbeach.model.Pace.GPU_FAILED, style = Type.small, color = ink.soft, modifier = Modifier.testTag("gpu-failed"))
        }
        Text("Nothing is written until you say yes.", style = Type.small, color = ink.soft)
    }
    // A word the phone misheard, put right: his correction counts as his words.
    correcting?.let { i ->
        var words by remember(i) { mutableStateOf(d.lines[i].words) }
        PaperDialog(
            "Put a word right", onDismiss = { correcting = null },
            actions = {
                TextLink("Not now", { correcting = null }, tag = "day-correct-not-now")
                TextLink("Keep it", { asking.correct(i, words, e); correcting = null }, tag = "day-correct-keep")
            },
        ) {
            Text("As the phone heard it: " + Words.quoted(d.lines[i].heard ?: d.lines[i].words, 300), style = Type.small, color = ink.soft)
            PaperField(words, { words = it }, "Your words", tag = "day-correct-field", focus = true)
        }
    }
    choosing?.let { i ->
        PaperDialog("Where does it go?", onDismiss = { choosing = null }, actions = { TextLink("Not now", { choosing = null }, tag = "day-move-not-now") }) {
            Text(Words.quoted(d.lines[i].words, 200), style = Type.body, color = ink.ink)
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                for (place in Landing.PLACES) {
                    TextLink(
                        (if (place in Telling.DAY) "Today: " else "Hand: ") + Landing.name(place),
                        { asking.move(i, place, e); choosing = null }, style = Type.body, tag = "day-to:$place",
                    )
                }
            }
        }
    }
}

/** One line of the day: ticked or not, his words, and, if he moved it, from where. */
@Composable
private fun DayLine(i: Int, line: Landing.Line, movable: Boolean, tickable: Boolean, tick: () -> Unit, move: () -> Unit, correct: () -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().testTag("day-line"), verticalAlignment = Alignment.Top) {
        if (line.place != null) Checkbox(
            checked = line.ticked, onCheckedChange = { tick() }, enabled = tickable,
            colors = CheckboxDefaults.colors(checkedColor = ink.crimson, uncheckedColor = ink.soft),
            modifier = Modifier.testTag("day-tick:$i"),
        )
        Column(Modifier.weight(1f).padding(top = if (line.place == null) 0.dp else 10.dp, start = if (line.place == null) 0.dp else 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Tapped, his words open to put a misheard word right.
            Text(
                line.words, style = Type.body, color = if (line.ticked || line.place == null) ink.ink else ink.soft,
                modifier = Modifier.then(if (tickable) Modifier.clickable(onClickLabel = "Put a word right", onClick = correct) else Modifier).testTag("day-words:$i"),
            )
            if (line.corrected) Text("You put it right; the phone heard " + Words.quoted(line.heard ?: "", 200) + ".", style = Type.small, color = ink.crimson)
            if (line.card != null) Text("Updates the card already on this shelf.", style = Type.small, color = ink.soft)
            if (line.moved) Text("You moved it here" + (line.from?.takeIf { it.isNotEmpty() }?.let { " from ${Landing.name(it)}" } ?: "") + ".", style = Type.small, color = ink.crimson)
            if (movable) TextLink(if (line.place == null) "Place it" else "Move", move, tag = "day-move:$i")
        }
    }
}

/** What the Write job proposes beneath her reply, before anything is written: what it read, the writes, the post it offers. */
@Composable
private fun ProposalParts(shown: Asking.Stage.Shown, details: Boolean) {
    val ink = LocalInk.current
    if (shown.asked.found.isNotEmpty()) {
        Label("What it read")
        SelectionContainer { Text(shown.asked.found.joinToString("\n\n"), style = Type.call, color = ink.soft) }
    }
    val p = shown.asked.proposal ?: return
    Writes(p)
    p.post?.let { post ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label("Then it offers to post")
            Text(Words.quoted(masked(post.message, p.writes), 1000), style = Type.body, color = ink.ink, modifier = Modifier.testTag("offered-post"))
            Text("to pool:${post.pool} on the beach, as ${post.author}. You'll be asked again before it goes.", style = Type.small, color = ink.soft)
        }
    }
    p.postRefusal?.let { Notice("It offered a post the pocket can't send: $it", tag = "post-refusal") }
    Text("Nothing is written until you say yes.", style = Type.small, color = ink.soft)
}

@Composable
private fun Writes(p: Proposal) {
    val ink = LocalInk.current
    if (p.writes.isEmpty() && p.refusal == null) return
    Label(if (p.refusal == null) "What it proposes" else "What it proposed")
    Sheet(Modifier.testTag("writes")) {
        p.writes.forEachIndexed { i, w ->
            if (i > 0) Rule()
            WriteRow(w)
        }
        if (p.writes.isEmpty()) Text("Nothing could be planned.", style = Type.aside, color = ink.soft)
    }
    p.refusal?.let { Notice("It can't go ahead. $it", tag = "refusal") }
}

@Composable
private fun WriteRow(w: Planned) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("write"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(Words.landing(w.plan.kind, w.plan.block, w.plan.address, w.plan.landsAt, w.plan.wraps, w.standing), style = Type.small, color = ink.ink)
        if (w.step.byApp) Text(byApp(w), style = Type.small, color = ink.crimson, modifier = Modifier.testTag("by-app"))
        w.shown?.let { Text(Words.quoted(it.replace("{{fingerprint}}", PRINT_SHOWN), 400), style = Type.body, color = ink.ink) }
        SelectionContainer { Text(Words.call(w.plan).let { c -> w.print?.let { c.replace(it, "{{fingerprint}}") } ?: c }, style = Type.call, color = ink.soft) }
    }
}

/** Why the app adds a write of its own to the companion's. */
private fun byApp(w: Planned): String = when {
    w.step.text?.startsWith("Seal: ") == true -> "The app's own write: a new seal, made for this Ask."
    w.step.text?.startsWith("Fingerprint: ") == true -> "The app's own write: the block's fingerprint, made as this line is written, after every other."
    w.step.text == HelloWorld.SHELL_LINES.first() -> "The app's own write: the shell's opening line, which a hello world begins with."
    else -> "The app's own write."
}

private fun masked(message: String, writes: List<Planned>): String =
    writes.mapNotNull { it.print }.fold(message) { m, print -> m.replace(print, PRINT_SHOWN) }

/** What saying yes wrote, and whether the hello world checks out. */
@Composable
private fun Written(model: Model, seal: String, carried: com.phenomemental.pocketbeach.bsp.Carried, shellOpening: String) {
    val ink = LocalInk.current
    Label("Written")
    Sheet {
        carried.landed.forEachIndexed { i, w ->
            if (i > 0) Rule()
            Text(
                (if (w.plan.landsAt.isNullOrEmpty()) w.plan.block else "${w.plan.block} at ${w.plan.landsAt}") + ": " + Words.quoted(w.text ?: w.plan.text, 200),
                style = Type.small, color = ink.ink, modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        if (carried.landed.isEmpty()) Text("Nothing.", style = Type.aside, color = ink.soft)
    }
    carried.problem?.let { Notice(it, tag = "problem") }
    if (carried.landed.any { it.step.block == HelloWorld.BLOCK }) {
        val problems by produceState<List<String>?>(null, carried) {
            value = withContext(Dispatchers.IO) { HelloWorld.check(model.app.storage.read(HelloWorld.BLOCK), shellOpening, seal) }
        }
        problems?.let { list ->
            if (list.isEmpty()) Text(
                "The hello world checks out: it holds the shell's opening line, your secret, the seal the app made, and a fingerprint that matches the block.",
                style = Type.small, color = ink.ink, modifier = Modifier.testTag("hello-checks"),
            ) else Notice("The hello world doesn't check out: " + list.joinToString(" "), tag = "hello-checks")
        }
    }
}

@Composable
private fun PostCard(message: PoolMessage, postedBefore: String?) {
    val ink = LocalInk.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label("Post to the beach?")
        if (postedBefore != null) Text(
            "This message was posted already, at entry $postedBefore. Posting it again adds a second, identical entry.",
            style = Type.body, color = ink.crimson, modifier = Modifier.testTag("posted-before"),
        )
        Sheet(Modifier.testTag("post-card")) {
            SelectionContainer { Text(message.message, style = Type.body, color = ink.ink, modifier = Modifier.testTag("post-message")) }
        }
        Text(
            "It goes to pool:${message.pool} at beach.happyseaurchin.com, under the name ${message.author}, and can't be taken back. " +
                "It is the only thing the pocket sends.",
            style = Type.small, color = ink.soft,
        )
    }
}

/** The model: offered only when the phone can run it, fetched once over Wi-Fi, checked before it is used. */
@Composable
private fun ModelPart(model: Model, state: ModelStore.State?) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val models = model.app.models
    val scope = rememberCoroutineScope()
    fun act(block: () -> Unit) { scope.launch(Dispatchers.IO) { block() } }
    // The download shows in a notification while Matthew is away; Android asks once whether it may.
    val asked = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { FetchService.start(context) }
    fun fetch() {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            asked.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FetchService.start(context)
        }
    }
    val size = ModelStore.gb(ModelStore.SIZE)
    when (state) {
        null, ModelStore.State.Ready -> {}
        is ModelStore.State.TooLittleMemory -> Notice(
            "This phone has ${ModelStore.gb(state.has)} of memory. The companion's model needs a phone with 8 GB, so it isn't offered here. " +
                "The hello world can still be done by hand, under About, Try a call by hand.",
            tag = "too-little-memory",
        )
        is ModelStore.State.TooLittleRoom -> Notice(
            "The companion's model needs ${ModelStore.gb(state.needs)} of room and this phone has ${ModelStore.gb(state.free)} free. Free some room, then come back.",
            tag = "too-little-room",
        )
        ModelStore.State.Absent -> {
            Text(
                "The companion is Gemma 4 E2B, a model that runs on this phone. It comes as one download of $size from Hugging Face, " +
                    "over Wi-Fi only, checked byte for byte before it is used. After that it needs no connection.",
                style = Type.body, color = ink.ink,
            )
            Action("Get the model, $size over Wi-Fi", { fetch() }, Modifier.fillMaxWidth(), tag = "get-model")
        }
        is ModelStore.State.Fetching -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("Fetching the model")
            Text(
                "${ModelStore.gb(state.done)} of $size (${state.done * 100 / ModelStore.SIZE}%)" +
                    if (state.bytesPerSecond > 0) ", at %.1f MB/s".format(state.bytesPerSecond / 1e6) else "",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("fetching"),
            )
            Bar(state.done.toFloat() / ModelStore.SIZE)
            state.note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("fetch-note")) }
            Text("It carries on while you're away from this screen, and shows in a notification.", style = Type.small, color = ink.soft)
            TextLink("Stop fetching", { act { models.stop() } }, tag = "stop-fetching")
        }
        is ModelStore.State.Paused -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Paused at ${ModelStore.gb(state.done)} of $size. It carries on from there.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("paused"),
            )
            Bar(state.done.toFloat() / ModelStore.SIZE)
            Action("Carry on", { fetch() }, Modifier.fillMaxWidth(), strong = true, tag = "carry-on")
            TextLink("Discard what has come, and begin again", { act { models.discard() } }, tag = "discard")
        }
        ModelStore.State.Checking -> Text("Checking the download, byte for byte…", style = Type.aside, color = ink.soft, modifier = Modifier.testTag("checking"))
        is ModelStore.State.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Notice(state.why, tag = "model-failed")
            if (state.done > 0) Text("${ModelStore.gb(state.done)} of $size is kept, to carry on from.", style = Type.small, color = ink.soft)
            Action(
                if (state.done > 0) "Carry on from ${ModelStore.gb(state.done)}" else "Try again",
                { act { models.dismissFailure() }; fetch() },
                Modifier.fillMaxWidth(), strong = true, tag = "try-again",
            )
            if (state.done > 0) TextLink("Discard what has come, and begin again", { act { models.discard() } }, tag = "discard")
        }
    }
}

/** How far a download has come, as a ruled line filling with crimson. */
@Composable
private fun Bar(fraction: Float) {
    val ink = LocalInk.current
    Box(Modifier.fillMaxWidth().height(6.dp).background(ink.rule)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(ink.crimson))
    }
}
