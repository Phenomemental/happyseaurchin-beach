package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
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

/** Ask the companion: Gemma, on this phone, wearing the shell pocket-companion. */
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
    val stage = asking.stage
    // The model stays loaded, and the conversation open, while Ask is.
    val ready = modelState is ModelStore.State.Ready && shell != null
    DisposableEffect(ready) {
        if (ready) asking.opened()
        // Only a visit that opened the conversation lets it go: not the wait for the model and the shell, ended as they come ready.
        onDispose { if (ready) asking.closed() }
    }
    // A message posted already is not sent again unless Matthew asks for that on purpose.
    val postedBefore = (stage as? Asking.Stage.Written)?.carried?.post?.let(model.app.posts::landed)
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

    Page(
        onBack = { nav.back() },
        top = { Text("Ask", style = Type.name, color = ink.ink, maxLines = 1) },
        bottom = bottom(asking, stage, ready = modelState is ModelStore.State.Ready && shell != null, postedBefore = postedBefore),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (details) "How she answers: her shell, what she proposes and the time each reply took." else "The workings: her shell, what she proposes, the times.",
                style = Type.small, color = ink.soft, modifier = Modifier.weight(1f),
            )
            Text("Details", style = Type.small, color = ink.ink)
            Switch(
                checked = details,
                onCheckedChange = { model.app.showDetails = it },
                colors = SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("details"),
            )
        }
        if (details) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("The companion")
            Text(
                if (shell != null) "Wearing ${HelloWorld.SHELL}: ${Words.quoted(shell, 120)}" else "The companion wears a shell, a block of yours that says how it behaves.",
                style = Type.heading.copy(fontSize = Type.body.fontSize, lineHeight = Type.body.lineHeight), color = ink.ink,
                modifier = Modifier.testTag("wearing"),
            )
            Text(
                "She talks as Settings' Talk job says, then, when your words may ask for a write, the Write job sets out the lines.",
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

        // The conversation so far; the last, once over, is shown in full below.
        val earlier = if (stage is Asking.Stage.Over) asking.history.dropLast(1) else asking.history.toList()
        earlier.forEach { Earlier(it, details) }

        if (modelState is ModelStore.State.Ready && shell != null) when (stage) {
            Asking.Stage.Writing, is Asking.Stage.Over -> {
                if (stage is Asking.Stage.Over) Over(model, stage, details)
                PaperField(asking.question, { asking.question = it }, "Say something to the companion, or ask for the hello world with your secret", tag = "question")
                Speak { words -> asking.question = if (asking.question.isBlank()) words else asking.question.trimEnd() + " " + words }
                if (asking.benchRunning) Notice("The bench is running. Ask can be used once it's over.", tag = "ask-bench-running")
                if (asking.history.isNotEmpty()) TextLink("Save the conversation as a file", { saveTo.launch(asking.fileName()) }, tag = "save-conversation")
                if (details || asking.benchRunning) TextLink(if (asking.benchRunning) "See the bench" else "Run the bench", { nav.go(Screen.Bench) }, tag = "open-bench")
            }
            is Asking.Stage.Thinking -> {
                His(asking.question)
                // Her words as she writes them.
                if (asking.live.isNotBlank()) Text(asking.live, style = Type.body, color = ink.ink, modifier = Modifier.testTag("live"))
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(stage) { while (true) { delay(1000); now = System.currentTimeMillis() } }
                val secs = ((now - stage.since) / 1000).coerceAtLeast(0)
                Text(
                    when {
                        asking.checking -> "Seeing whether there's anything to write…" + if (details) " ${secs} s" else ""
                        asking.live.isNotBlank() -> if (details) "${secs} s" else ""
                        !model.app.mind.loaded(model.app.settings(com.phenomemental.pocketbeach.model.Job.TALK)) -> "Loading the companion…" + if (details) " ${secs} s" else ""
                        else -> "Thinking…" + if (details) " ${secs} s" else ""
                    },
                    style = Type.aside, color = ink.soft, modifier = Modifier.testTag("thinking"),
                )
            }
            is Asking.Stage.Choosing -> Choosing(asking, stage)
            is Asking.Stage.Shown -> Proposed(asking.question, stage, details)
            is Asking.Stage.Declining -> {
                Proposed(asking.question, stage.shown, details)
                Label("Why not now?")
                Text("One tap says why, so the log keeps it.", style = Type.small, color = ink.soft)
            }
            is Asking.Stage.Carrying -> {
                Proposed(asking.question, stage.shown, details)
                Text("Writing…", style = Type.aside, color = ink.soft)
            }
            is Asking.Stage.Written -> {
                Written(model, stage.shown.asked.proposal!!.seal, stage.carried, asking.shellOpening())
                stage.carried.post?.let { PostCard(it, postedBefore) }
            }
            is Asking.Stage.Posting -> {
                Written(model, stage.written.shown.asked.proposal!!.seal, stage.written.carried, asking.shellOpening())
                PostCard(stage.message, null)
                Text("Posting…", style = Type.aside, color = ink.soft)
            }
        }
    }
}

/** The actions at the foot, by stage. */
private fun bottom(asking: Asking, stage: Asking.Stage, ready: Boolean, postedBefore: String?): (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = when (stage) {
    Asking.Stage.Writing, is Asking.Stage.Over -> if (!ready) null else ({
        Action("Ask", { asking.ask() }, Modifier.weight(1f), enabled = asking.question.isNotBlank() && !asking.benchRunning, strong = true, tag = "ask")
    })
    is Asking.Stage.Thinking -> ({ Action("Stop", { asking.stop() }, Modifier.weight(1f), tag = "stop") })
    is Asking.Stage.Choosing -> ({ Action("Not now", { asking.dropChoice() }, Modifier.weight(1f), tag = "not-now") })
    is Asking.Stage.Shown -> ({
        Action("Not now", { asking.notNow() }, Modifier.weight(1f), tag = "not-now")
        Action("Yes, write it", { asking.yes() }, Modifier.weight(1f), strong = true, tag = "yes")
    })
    is Asking.Stage.Declining -> ({
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Action("Wrong place", { asking.because("wrong place") }, Modifier.weight(1f), tag = "wrong-place")
                Action("Wrong block", { asking.because("wrong block") }, Modifier.weight(1f), tag = "wrong-block")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Action("Misunderstood", { asking.because("misunderstood") }, Modifier.weight(1f), tag = "misunderstood")
                Action("Just not now", { asking.because(null) }, Modifier.weight(1f), tag = "just-not-now")
            }
            TextLink("Back to the proposal", { asking.back() }, tag = "back-to-proposal")
        }
    })
    is Asking.Stage.Carrying, is Asking.Stage.Posting -> null
    is Asking.Stage.Written -> if (stage.carried.post == null) null else ({
        Action("Not now", { asking.dontPost() }, Modifier.weight(1f), tag = "dont-post")
        if (postedBefore == null) Action("Post it", { asking.post() }, Modifier.weight(1f), strong = true, tag = "post")
        else Action("Post again?", { asking.post() }, Modifier.weight(1f), tag = "post-again")
    })
}

/** An earlier exchange of the conversation: what Matthew said, what she said, what came of a proposal, and, in Details, how it ended and how long it took. */
@Composable
private fun Earlier(e: Asking.Exchange, details: Boolean) {
    val ink = LocalInk.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("earlier")) {
        His(e.question)
        if (e.said.isNotBlank()) Text(e.said, style = Type.body, color = ink.ink)
        when {
            details -> Text(e.note + " " + e.time, style = Type.small, color = ink.soft)
            e.outcome != null -> Text(e.outcome, style = Type.small, color = ink.soft)
        }
    }
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
private fun Choosing(asking: Asking, stage: Asking.Stage.Choosing) {
    val ink = LocalInk.current
    val choice = stage.answered.planned?.choose ?: return
    His(asking.question)
    if (stage.answered.said.isNotBlank()) Text(stage.answered.said, style = Type.body, color = ink.ink, modifier = Modifier.testTag("say"))
    Text(
        "She means the line “${choice.quote}” in ${choice.block}, and ${choice.lines.size} lines say that. Which one?",
        style = Type.body, color = ink.ink, modifier = Modifier.testTag("which-line"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        choice.lines.forEach { line ->
            Action(Words.quoted(line.text ?: "", 80), { asking.choose(line.address) }, Modifier.fillMaxWidth(), tag = "choose:${line.address}")
        }
    }
}

/** What the companion said and proposes, before anything is written. */
@Composable
private fun Proposed(question: String, shown: Asking.Stage.Shown, details: Boolean) {
    val ink = LocalInk.current
    His(question)
    val answer = shown.asked.answer
    if (answer != null && answer.say.isNotEmpty()) Text(answer.say, style = Type.body, color = ink.ink, modifier = Modifier.testTag("say"))
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // The time and the chip, in Details; that nothing is written yet, always.
        if (details) {
            Text(
                "Took %.1f s".format(shown.took / 1000.0) + (shown.pace?.where?.takeIf(String::isNotEmpty)?.let { " $it" } ?: "") +
                    (shown.firstShown?.let { f -> ", first words after %.1f s".format(f / 1000.0) } ?: "") +
                    ", in ${Words.count(shown.asked.exchanges.size, "exchange")}.",
                style = Type.small, color = ink.soft, modifier = Modifier.testTag("took"),
            )
            shown.pace?.detail()?.let { Text(it, style = Type.small, color = ink.soft, modifier = Modifier.testTag("pace")) }
        }
        Text("Nothing is written until you say yes.", style = Type.small, color = ink.soft)
    }
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

/** How the Ask ended. */
@Composable
private fun Over(model: Model, over: Asking.Stage.Over, details: Boolean) {
    val ink = LocalInk.current
    val context = LocalContext.current
    His(model.app.asking.question)
    over.shown?.let { shown ->
        val answer = shown.asked.answer
        if (answer != null && answer.say.isNotEmpty()) Text(answer.say, style = Type.body, color = ink.ink, modifier = Modifier.testTag("say"))
        if (over.carried == null) shown.asked.proposal?.let { if (it.refusal != null) Writes(it) }
    }
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
    // She only talked, and his words named one of his blocks: the app offers to keep them there, for Yes as ever.
    model.app.asking.keepIn?.let { block ->
        TextLink("Keep this in $block?", { model.app.asking.keep() }, tag = "keep-in")
    }
    // How it ended: in Details always; else only what came of a proposal, or that she couldn't answer.
    val last = model.app.asking.history.lastOrNull()
    if (details || last?.outcome != null || over.shown == null) Text(over.note, style = Type.small, color = ink.soft, modifier = Modifier.testTag("over"))
    // Every answer's time and chip, in Details.
    if (details) {
        last?.let { Text(it.time, style = Type.small, color = ink.soft, modifier = Modifier.testTag("took")) }
        Text("This Ask is in your ask-log block.", style = Type.small, color = ink.soft)
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
