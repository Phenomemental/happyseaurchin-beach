package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.Planned
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Proposal
import com.phenomemental.pocketbeach.bsp.Region
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
    var shellChecked by remember { mutableStateOf(false) }
    LaunchedEffect(shell, model.version) { shellChecked = true }
    val modelState by produceState<ModelStore.State?>(null) {
        while (true) {
            val s = withContext(Dispatchers.IO) { model.app.models.state() }
            value = s
            if (s is ModelStore.State.Fetched) value = withContext(Dispatchers.IO) { model.app.models.check() }
            delay(if (s is ModelStore.State.Fetching) 1000 else 3000)
        }
    }
    val stage = asking.stage

    Page(
        onBack = { nav.back() },
        top = { Text("Ask", style = Type.name, color = ink.ink, maxLines = 1) },
        bottom = bottom(asking, stage, ready = modelState is ModelStore.State.Ready && shell != null),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("The companion")
            Text(
                if (shell != null) "Wearing ${HelloWorld.SHELL}: ${Words.quoted(shell, 120)}" else "The companion wears a shell, a block of yours that says how it behaves.",
                style = Type.heading.copy(fontSize = Type.body.fontSize, lineHeight = Type.body.lineHeight), color = ink.ink,
                modifier = Modifier.testTag("wearing"),
            )
            Stroke()
        }
        if (shell == null && shellChecked) {
            Notice(
                "Its shell, ${HelloWorld.SHELL}, isn't in the pocket yet. Making it writes one new block: its opening line, " +
                    "${Words.quoted(HelloWorld.SHELL_OPENING, 200)}, and beneath it the line that says how to do the hello world.",
                link = "Make the companion's shell" to {
                    scope.launch {
                        val made = withContext(Dispatchers.IO) { runCatching { HelloWorld.makeShell(model.pages) } }
                        model.changed()
                        made.onSuccess { model.say(Screen.Ask, "Made ${HelloWorld.SHELL}.") }
                            .onFailure { model.say(Screen.Ask, it.message ?: "The shell wasn't made.") }
                    }
                },
                tag = "make-shell",
            )
        }

        ModelPart(model, modelState)

        if (modelState is ModelStore.State.Ready && shell != null) when (stage) {
            Asking.Stage.Writing, is Asking.Stage.Over -> {
                if (stage is Asking.Stage.Over) Over(model, stage)
                Label("Your ask")
                PaperField(asking.question, { asking.question = it }, "Ask the companion something, or for the hello world with your secret", tag = "question")
                Speak { words -> asking.question = if (asking.question.isBlank()) words else asking.question.trimEnd() + " " + words }
            }
            is Asking.Stage.Thinking -> {
                Asked(asking.question)
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(stage) { while (true) { delay(1000); now = System.currentTimeMillis() } }
                val secs = ((now - stage.since) / 1000).coerceAtLeast(0)
                Text(
                    if (model.app.gemma.loaded) "Thinking… ${secs} s" else "Loading the companion, then thinking… ${secs} s",
                    style = Type.aside, color = ink.soft, modifier = Modifier.testTag("thinking"),
                )
            }
            is Asking.Stage.Shown -> Proposed(asking.question, stage)
            is Asking.Stage.Declining -> {
                Proposed(asking.question, stage.shown)
                Label("Why not now?")
                Text("One tap says why, so the log keeps it.", style = Type.small, color = ink.soft)
            }
            is Asking.Stage.Carrying -> {
                Proposed(asking.question, stage.shown)
                Text("Writing…", style = Type.aside, color = ink.soft)
            }
            is Asking.Stage.Written -> {
                Written(model, stage.shown.asked.proposal!!.seal, stage.carried, asking.shellOpening())
                stage.carried.post?.let { PostCard(it) }
            }
            is Asking.Stage.Posting -> {
                Written(model, stage.written.shown.asked.proposal!!.seal, stage.written.carried, asking.shellOpening())
                PostCard(stage.message)
                Text("Posting…", style = Type.aside, color = ink.soft)
            }
        }
    }
}

/** The actions at the foot, by stage. */
private fun bottom(asking: Asking, stage: Asking.Stage, ready: Boolean): (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = when (stage) {
    Asking.Stage.Writing, is Asking.Stage.Over -> if (!ready) null else ({
        Action("Ask", { asking.ask() }, Modifier.weight(1f), enabled = asking.question.isNotBlank(), strong = true, tag = "ask")
    })
    is Asking.Stage.Thinking -> ({ Action("Stop", { asking.stop() }, Modifier.weight(1f), tag = "stop") })
    is Asking.Stage.Shown -> ({
        Action("Not now", { asking.notNow() }, Modifier.weight(1f), tag = "not-now")
        Action("Yes, write it", { asking.yes() }, Modifier.weight(1f), strong = true, tag = "yes")
    })
    is Asking.Stage.Declining -> ({
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("Wrong place", { asking.because("wrong place") }, Modifier.weight(1f), tag = "wrong-place")
                Action("Wrong block", { asking.because("wrong block") }, Modifier.weight(1f), tag = "wrong-block")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("Misunderstood", { asking.because("misunderstood") }, Modifier.weight(1f), tag = "misunderstood")
                Action("Just not now", { asking.because(null) }, Modifier.weight(1f), tag = "just-not-now")
            }
            TextLink("Back to the proposal", { asking.back() }, tag = "back-to-proposal")
        }
    })
    is Asking.Stage.Carrying, is Asking.Stage.Posting -> null
    is Asking.Stage.Written -> if (stage.carried.post == null) null else ({
        Action("Not now", { asking.dontPost() }, Modifier.weight(1f), tag = "dont-post")
        Action("Post it", { asking.post() }, Modifier.weight(1f), strong = true, tag = "post")
    })
}

@Composable
private fun Asked(question: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label("You asked")
        Text(question, style = Type.body, color = LocalInk.current.ink)
    }
}

/** What the companion said and proposes, before anything is written. */
@Composable
private fun Proposed(question: String, shown: Asking.Stage.Shown) {
    val ink = LocalInk.current
    Asked(question)
    val answer = shown.asked.answer
    if (answer != null && answer.say.isNotEmpty()) Text(answer.say, style = Type.aside, color = ink.ink, modifier = Modifier.testTag("say"))
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
    Text(
        "Took %.1f s".format(shown.took / 1000.0) + ". Nothing is written until you say yes.",
        style = Type.small, color = ink.soft,
    )
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
    Column(Modifier.fillMaxWidth().testTag("write"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(Words.landing(w.plan.kind, w.plan.block, w.plan.address, w.plan.landsAt, w.plan.wraps, w.standing), style = Type.small, color = ink.ink)
        w.shown?.let { Text(Words.quoted(it.replace("{{fingerprint}}", PRINT_SHOWN), 400), style = Type.body, color = ink.ink) }
        SelectionContainer { Text(Words.call(w.plan).let { c -> w.print?.let { c.replace(it, "{{fingerprint}}") } ?: c }, style = Type.call, color = ink.soft) }
    }
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
                style = Type.small, color = ink.ink,
            )
        }
        if (carried.landed.isEmpty()) Text("Nothing.", style = Type.aside, color = ink.soft)
    }
    carried.problem?.let { Notice(it, tag = "problem") }
    if (carried.made == HelloWorld.BLOCK) {
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
private fun PostCard(message: PoolMessage) {
    val ink = LocalInk.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label("Post to the beach?")
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
private fun Over(model: Model, over: Asking.Stage.Over) {
    val ink = LocalInk.current
    val context = LocalContext.current
    over.shown?.let { shown ->
        val answer = shown.asked.answer
        Asked(model.app.asking.question)
        if (answer != null && answer.say.isNotEmpty()) Text(answer.say, style = Type.aside, color = ink.ink, modifier = Modifier.testTag("say"))
        if (over.carried == null) shown.asked.proposal?.let { if (it.refusal != null) Writes(it) }
    }
    over.carried?.let { c -> Written(model, over.shown?.asked?.proposal?.seal ?: "", c, model.app.asking.shellOpening()) }
    when (val p = over.posted) {
        is Posted.Landed -> {
            Text("Posted. It landed at slot ${p.slot} of pool:${p.pool}.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("posted"))
            TextLink("Open the beach", {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BEACH_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                    model.say(Screen.Ask, "There's no browser on this phone to open $BEACH_URL.")
                }
            }, tag = "open-beach")
        }
        is Posted.Refused -> Notice(p.why, tag = "not-posted")
        null -> {}
    }
    Text(over.note, style = Type.small, color = ink.soft, modifier = Modifier.testTag("over"))
    Text("This Ask is in your ask-log block.", style = Type.small, color = ink.soft)
    Rule()
}

/** The model: offered only when the phone can run it, fetched once over Wi-Fi, checked before it is used. */
@Composable
private fun ModelPart(model: Model, state: ModelStore.State?) {
    val ink = LocalInk.current
    val models = model.app.models
    val scope = rememberCoroutineScope()
    fun act(block: () -> Unit) { scope.launch(Dispatchers.IO) { block() } }
    val size = ModelStore.gb(ModelStore.SIZE)
    when (state) {
        null, ModelStore.State.Ready -> {}
        is ModelStore.State.TooLittleMemory -> Notice(
            "This phone has ${ModelStore.gb(state.has)} of memory. The companion's model needs a phone with 8 GB, so it isn't offered here. " +
                "The hello world can still be done by hand, under About, Try a call by hand.",
            tag = "too-little-memory",
        )
        is ModelStore.State.TooLittleRoom -> Notice(
            "The companion's model needs $size of room and this phone has ${ModelStore.gb(state.free)} free. Free some room, then come back.",
            tag = "too-little-room",
        )
        ModelStore.State.Absent -> {
            Text(
                "The companion is Gemma 4 E2B, a model that runs on this phone. It comes as one download of $size from Hugging Face, " +
                    "over Wi-Fi only, checked byte for byte before it is used. After that it needs no connection.",
                style = Type.body, color = ink.ink,
            )
            Action("Get the model, $size over Wi-Fi", { act { models.fetch() } }, Modifier.fillMaxWidth(), tag = "get-model")
        }
        is ModelStore.State.Fetching -> {
            Text(
                if (state.waitingForWifi) "Waiting for Wi-Fi to fetch the model. ${ModelStore.gb(state.done)} of $size so far."
                else "Fetching the model: ${ModelStore.gb(state.done)} of $size.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("fetching"),
            )
            TextLink("Stop fetching", { act { models.stop() } }, tag = "stop-fetching")
        }
        ModelStore.State.Fetched, ModelStore.State.Checking -> Text("Checking the download, byte for byte…", style = Type.aside, color = ink.soft)
        is ModelStore.State.Failed -> Notice(state.why, link = "Try again" to { act { models.dismissFailure() } }, tag = "model-failed")
    }
}
