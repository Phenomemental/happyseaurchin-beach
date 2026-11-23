package com.phenomemental.pocketbeach.ui

import com.phenomemental.pocketbeach.model.ModelState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.phenomemental.pocketbeach.model.FetchService
import com.phenomemental.pocketbeach.model.ModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobSettings
import java.util.Locale

/**
 * The app's settings: for each of the companion's jobs, Edge's settings for
 * a model (0.5's Stage 1): the model, the processor, the context, then how
 * her replies are drawn: temperature, top-k, top-p and the longest reply.
 * 0.6: Gemma 4 E4B is fetched here, and is then a choice for each job.
 */
@Composable
fun SettingsScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    Page(onBack = { nav.back() }, top = { Text("Settings", style = Type.name, color = ink.ink, maxLines = 1) }) {
        WhereYouAre(model)
        NameAndRoom(model)
        SaveAndRestore(nav)
        WhereKept(model)
        Workings(model)
        Rule()
        Text("Her voice", style = Type.heading, color = ink.ink, modifier = Modifier.testTag("her-voice"))
        SmallerModel(model)
        Text(
            "She is Gemma, running on this phone. Each of the companion's jobs has Edge's settings of its own. The phone keeps one engine, hers, on its GPU, " +
                "and every conversation opens on it. A job set apart onto another model or context runs on the CPU: the engine is made again for it, " +
                "and again for her after, which is slower. A change is used from the next thing you say in Ask.",
            style = Type.small, color = ink.soft,
        )
        val e4b by produceState<ModelState?>(null) {
            while (true) {
                val st = withContext(Dispatchers.IO) { app.e4b.state() }
                value = st
                delay(if (st is ModelState.Fetching) 1000 else 3000)
            }
        }
        // The models a job can be set to: those on this phone.
        val ready = JobSettings.MODELS.filter { it == JobSettings.GEMMA_E2B || it == JobSettings.GEMMA_E4B && e4b is ModelState.Ready }
        BiggerModel(app, e4b)
        for (job in Job.values()) JobPart(job, app.settings(job), ready, app.cpuWhy(job), follows = if (app.followsTalk(job)) app.settings(Job.TALK).model else null) { app.set(job, it) }

        AskLog(model)
        ReachTheBeach(model, nav)
        WalkCheckPart(model)
        Rule()
        TextLink("Run the bench", { nav.go(Screen.Bench) }, tag = "settings-bench")
        Text("The bench asks fixed things on a scratch pocket and keeps what she said and how fast, to set beside Edge.", style = Type.small, color = ink.soft)
    }
}

/**
 * Sundial 0.10, where you are (spine:pocket-beach 1.111178719914): roughly,
 * for the sun. The phone's rough location, asked once and rounded to a tenth
 * of a degree; or a town from the list shipped with the app, which asks
 * nothing. Kept on this phone; the passport isn't consulted.
 */
@Composable
private fun WhereYouAre(model: Model) {
    val ink = LocalInk.current
    val app = model.app
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typed by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<com.phenomemental.pocketbeach.Towns.Town>>(emptyList()) }
    var note by remember { mutableStateOf<String?>(null) }
    val noFix = "The phone hasn't a rough location to give yet. Choose your town below, or try again in a moment."
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        app.where.asked = true
        note = if (!granted) "The phone's location stays unasked. Choose your town below." else if (roughly(context, app.where)) null else noFix
    }
    Column(Modifier.fillMaxWidth().testTag("settings-where"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Where you are", style = Type.heading, color = ink.ink)
        val p = app.where.place
        Text(
            if (p == null) "Not set yet. The dial follows the real sun where you are; roughly where is enough."
            else "${p.name.replaceFirstChar { it.uppercase() }}, ${if (p.from == "phone") "from this phone's rough location" else "chosen from the town list"}. The dial follows the sun there.",
            style = Type.body, color = ink.ink, modifier = Modifier.testTag("where-now"),
        )
        TextLink("Use this phone's rough location", {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                app.where.asked = true
                note = if (roughly(context, app.where)) null else noFix
            } else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }, tag = "where-use-phone")
        PaperField(typed, { t ->
            typed = t
            scope.launch { found = withContext(Dispatchers.IO) { runCatching { com.phenomemental.pocketbeach.Towns.find(context, t) }.getOrDefault(emptyList()) } }
        }, "Or type your town", singleLine = true, tag = "where-search")
        for (t in found) TextLink("${t.name}, ${t.country}", {
            app.where.set(com.phenomemental.pocketbeach.WhereHeStands.Place(t.name, t.lat, t.lon, "town"))
            typed = ""; found = emptyList(); note = null
        }, style = Type.body, tag = "where-town:${t.name}, ${t.country}")
        if (p != null) TextLink("Forget where I am", { app.where.set(null); note = null }, tag = "where-forget")
        note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("where-note")) }
        Text(
            "Where you are is kept on this phone and never sent. The phone's location service may ask its maker to work out where the phone is; " +
                "the town list is on this phone and asks nothing. Towns from GeoNames, under CC BY 4.0.",
            style = Type.small, color = ink.soft,
        )
    }
}

/** Sundial 0.10: his name and his room (1.111178719918), read from his shell and passport; the box is on Your room. */
@Composable
private fun NameAndRoom(model: Model) {
    val ink = LocalInk.current
    val app = model.app
    val room by produceState<com.phenomemental.pocketbeach.bsp.Room?>(null, model.version) {
        value = withContext(Dispatchers.IO) { runCatching { com.phenomemental.pocketbeach.bsp.PoolPost.room(app.storage) }.getOrNull() }
    }
    Column(Modifier.fillMaxWidth().testTag("settings-room"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Your name and your room", style = Type.heading, color = ink.ink)
        val r = room
        if (r == null || com.phenomemental.pocketbeach.bsp.Hatch.hatched(app.storage) == null) {
            Text(Words.sundial(app, "5.4", "Your room waits for your shell: once you have a name and a room, the box is here."), style = Type.body, color = ink.ink)
        } else {
            Text("Your name is ${r.author}, and your room is pool:${r.pool}.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("room-said"))
            Text("Notes you post go there, under your name. Your room is the first pool in your passport, else the one your shell names, else your own.", style = Type.small, color = ink.soft)
        }
    }
}

/** Sundial 0.10: save a whole copy, or restore one, on Your pocket. */
@Composable
private fun SaveAndRestore(nav: Nav) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().testTag("settings-save"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Save and restore", style = Type.heading, color = ink.ink)
        Text("A whole copy of your pocket, as one file you keep where you choose; and a copy put back.", style = Type.small, color = ink.soft)
        TextLink("Save or restore, on Your pocket", { nav.toPlace(Screen.Shore) }, tag = "settings-to-pocket")
    }
}

/**
 * Sundial 0.10: Show the workings. On, Ask shows its workings beneath the
 * conversation; and here, today's words not yet kept (1.111178719915 (c)).
 */
@Composable
private fun Workings(model: Model) {
    val ink = LocalInk.current
    val app = model.app
    Column(Modifier.fillMaxWidth().testTag("settings-workings"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show the workings", style = Type.heading, color = ink.ink, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = app.showDetails,
                onCheckedChange = { app.showDetails = it },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("workings-switch"),
            )
        }
        Text("Her instruction, the times, and what each thing you said came to, beneath the conversation; and here, your words waiting to be kept.", style = Type.small, color = ink.soft)
        if (app.showDetails) {
            Text(com.phenomemental.pocketbeach.bsp.Waiting.SAID, style = Type.body, color = ink.ink, modifier = Modifier.testTag("waiting-said"))
            val waiting = app.waiting.all
            if (waiting.isEmpty()) Text("None: what you said is kept, or you haven't spoken today.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("waiting-none"))
            for (m in waiting) Column(Modifier.testTag("waiting-moment")) {
                Text(Words.quoted(m.words, 600), style = Type.body, color = ink.ink)
                Text(Words.at(m.atMs).replaceFirstChar { it.uppercase() }, style = Type.small, color = ink.soft)
            }
            if (waiting.isNotEmpty()) Text("They are kept in the app's own file, moments.json, till you tap Keep today; a whole copy carries them.", style = Type.small, color = ink.soft)
        }
    }
}


/**
 * Sundial 0.12.1, the check on his phone (spine:pocket-beach 1.1111787199993):
 * twenty walk calls from her on the GPU, each asked for a known address, and
 * how many arrived intact. He reports the number; since 0.13.2 it moves
 * nothing, her walks staying on the GPU. Sundial 0.13.1, widened: then the
 * same short conversation on the GPU and on the CPU, side by side, so he
 * sees whether her stutters are the GPU's; a reply that loops is cut and
 * marked so. Sundial 0.13.2: the CPU's runs first and the GPU's last, and
 * when the check ends, however it ends, her talk engine is loaded again, so
 * his next message isn't left on the CPU's engine.
 */
@Composable
private fun WalkCheckPart(model: Model) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf<Int?>(null) }
    var outcomes by remember { mutableStateOf<List<com.phenomemental.pocketbeach.bsp.WalkCheck.Outcome>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var side by remember { mutableStateOf<String?>(null) }
    var conversed by remember { mutableStateOf<List<Pair<String, com.phenomemental.pocketbeach.model.Mind.Conversed?>>>(emptyList()) }
    val busy = running != null || side != null
    Column(Modifier.fillMaxWidth().testTag("walk-check-part"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Her on the GPU, checked", style = Type.heading, color = ink.ink)
        Text(
            "Twenty times, she is asked what's at a known address, and calls the walker with it on the phone's GPU. Nothing is walked or written: " +
                "the check reads only whether each address arrived as she sent it. Then the same short conversation, three messages, runs on the GPU " +
                "and on the CPU, side by side, so you can see whether stutters (garbled dates, doubled letters, a word over and over) are the GPU's. " +
                "It takes a few minutes.",
            style = Type.small, color = ink.soft,
        )
        Action(
            running?.let { "Checking her walk… $it of ${com.phenomemental.pocketbeach.bsp.WalkCheck.ADDRESSES.size}" }
                ?: side?.let { "Her conversation on the $it…" } ?: "Check her on this phone",
            {
                if (busy) return@Action
                problem = null
                outcomes = emptyList()
                conversed = emptyList()
                running = 0
                scope.launch {
                    try {
                        val talk = app.running(com.phenomemental.pocketbeach.model.Job.TALK)
                        val settings = talk.copy(processor = "GPU")
                        val got = mutableListOf<com.phenomemental.pocketbeach.bsp.WalkCheck.Outcome>()
                        runCatching {
                            for (a in com.phenomemental.pocketbeach.bsp.WalkCheck.ADDRESSES) {
                                val called = withContext(Dispatchers.IO) {
                                    app.mind.callTool(com.phenomemental.pocketbeach.bsp.WalkCheck.window(java.time.ZonedDateTime.now()), com.phenomemental.pocketbeach.bsp.WalkCheck.message(a), com.phenomemental.pocketbeach.bsp.WalkCheck.TOOL, settings)
                                }
                                got += com.phenomemental.pocketbeach.bsp.WalkCheck.outcome(a, called?.arguments)
                                running = got.size
                                outcomes = got.toList()
                            }
                        }.onFailure { problem = "The check stopped: ${it.message ?: it::class.simpleName}." }
                        running = null
                        if (got.size == com.phenomemental.pocketbeach.bsp.WalkCheck.ADDRESSES.size) {
                            app.walkCheck = com.phenomemental.pocketbeach.PocketApp.WalkChecked(got.count { it.intact }, got.size, got.count { it.altered }, System.currentTimeMillis(), settings.model)
                        }
                        // The same conversation, his Talk settings, on the CPU and then on the GPU, so the GPU's engine is the last made.
                        val now = java.time.ZonedDateTime.now()
                        val handle = withContext(Dispatchers.IO) { app.asking.hatched() } ?: "Sundial"
                        val messages = com.phenomemental.pocketbeach.bsp.SideBySide.messages(now, handle)
                        for (processor in listOf("CPU", "GPU")) {
                            side = processor
                            val c = runCatching {
                                withContext(Dispatchers.IO) {
                                    app.mind.converse(com.phenomemental.pocketbeach.bsp.SideBySide.window(now), messages, talk.copy(processor = processor), com.phenomemental.pocketbeach.bsp.SideBySide.LONGEST)
                                }
                            }.onFailure { problem = "Her conversation on the $processor stopped: ${it.message ?: it::class.simpleName}." }.getOrNull()
                            conversed = conversed + (processor to c)
                        }
                    } finally {
                        running = null
                        side = null
                        // Her talk engine back, as her talk runs (the GPU), whether the check finished, stopped, or he left Settings.
                        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                            runCatching { app.mind.warm(app.running(com.phenomemental.pocketbeach.model.Job.TALK)) }
                        }
                    }
                }
            },
            Modifier.fillMaxWidth(), enabled = !busy, tag = "walk-check",
        )
        problem?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("walk-check-problem")) }
        if (outcomes.isNotEmpty() && running == null) {
            Text(com.phenomemental.pocketbeach.bsp.WalkCheck.said(outcomes), style = Type.body, color = ink.ink, modifier = Modifier.testTag("walk-check-result"))
            for (o in outcomes.filter { !it.intact }) Text(o.said, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("walk-check-altered"))
        } else if (running == null) app.walkCheck?.let { c ->
            Text("Last checked ${Words.at(c.atMs)}, on ${c.model}: ${c.intact} of ${c.of} arrived intact.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("walk-check-last"))
        }
        if (conversed.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((processor, c) in conversed.sortedBy { if (it.first == "GPU") 0 else 1 }) Column(Modifier.weight(1f).testTag("side-${processor.lowercase()}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("On the $processor", style = Type.name, color = ink.ink)
                when {
                    c == null -> Text("It couldn't run here.", style = Type.small, color = ink.crimson)
                    else -> {
                        if (c.processor != processor) Text("The $processor couldn't run her${c.whyNotGpu?.let { ": $it" } ?: ""}; this is the ${c.processor}.", style = Type.small, color = ink.crimson)
                        for (reply in c.replies) {
                            val loop = com.phenomemental.pocketbeach.bsp.Looping.of(reply)
                            Text(loop?.let { com.phenomemental.pocketbeach.bsp.Looping.cut(reply, it) } ?: reply.trim(), style = Type.small, color = ink.ink)
                            if (loop != null) Text("↺ It looped here: \"${loop.unit}\" ${loop.times} times running.", style = Type.small, color = ink.crimson, modifier = Modifier.testTag("side-looped"))
                        }
                    }
                }
            }
        }
    }
}

/** Sundial 0.12 (spine:pocket-beach 1.111178719995): whether the walker may read the beach's blocks, read only; off unless he turns it on. */
@Composable
private fun ReachTheBeach(model: Model, nav: Nav) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().testTag("reach-part"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reach the beach", style = Type.heading, color = ink.ink, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = model.app.reachBeach,
                onCheckedChange = { model.app.reachBeach = it },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("reach-beach"),
            )
        }
        Text(
            "When on, the walker can read the blocks at beach.happyseaurchin.com, read only: it asks for the block you name, and sends nothing of yours. " +
                "A block you want is copied into your pocket on your yes. Off, nothing is asked of the beach.",
            style = Type.small, color = ink.soft,
        )
        // Sundial 0.13.5: his blocks brought across, ticked and copied in on one yes.
        if (model.app.reachBeach) TextLink("Bring my blocks across", { nav.go(Screen.BringAcross) }, tag = "to-bring-across")
    }
}

/** 0.6.4: whether each exchange in Ask is kept in the ask log; off unless he turns it on. The saved conversation keeps everything either way. */
@Composable
private fun AskLog(model: Model) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().testTag("ask-log-part"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Keep an ask log", style = Type.heading, color = ink.ink, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = model.app.keepAskLog,
                onCheckedChange = { model.app.keepAskLog = it },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("ask-log"),
            )
        }
        Text(
            "When on, each exchange in Ask is written to your ask-log block: what you said, what was proposed, and how it ended. Off, nothing is written " +
                "unless you ask; \u201cSave this conversation\u201d keeps everything either way.",
            style = Type.small, color = ink.soft,
        )
    }
}

@Composable
private fun JobPart(job: Job, s: JobSettings, models: List<String>, cpuWhy: String?, follows: String? = null, set: (JobSettings) -> Unit) {
    val ink = LocalInk.current
    val tag = "job-${job.name.lowercase()}"
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text(job.title, style = Type.heading, color = ink.ink)
        Text(job.about, style = Type.small, color = ink.soft)
        // Sundial 0.10.1: a job never set apart runs on her talk's model, with its own drawing, so no second model loads.
        follows?.let { Text("Runs on her talk's model, $it, with its own settings below, so no second model loads. Set a model here to set it apart.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("$tag-follows")) }
        val choices = (models + s.model).distinct().sortedBy { JobSettings.MODELS.indexOf(it) }
        Setting(
            "Model", s.model, tag = "$tag-model",
            hint = if (choices.size == 1) "The one on this phone. Get Gemma 4 E4B above to choose it." else "E2B, the quicker; E4B, the bigger.",
            less = step(choices, s.model, -1)?.let { m -> { set(s.copy(model = m)) } },
            more = step(choices, s.model, 1)?.let { m -> { set(s.copy(model = m)) } },
        )
        Setting(
            "Processor", if (s.processor == "GPU") "GPU, else the CPU" else "CPU", tag = "$tag-processor",
            // 0.6.2: said plainly when it runs on the CPU though set to the GPU.
            hint = cpuWhy?.let { "Runs on the CPU: $it." },
            less = step(JobSettings.PROCESSORS, s.processor, -1)?.let { p -> { set(s.copy(processor = p)) } },
            more = step(JobSettings.PROCESSORS, s.processor, 1)?.let { p -> { set(s.copy(processor = p)) } },
        )
        Setting(
            "Context", "${count(s.context)} tokens", tag = "$tag-context", hint = "Her instruction, the conversation and her reply, together.",
            less = step(JobSettings.CONTEXTS, s.context, -1)?.let { c -> { set(s.copy(context = c)) } },
            more = step(JobSettings.CONTEXTS, s.context, 1)?.let { c -> { set(s.copy(context = c)) } },
        )
        Setting(
            "Temperature", two(s.temperature), tag = "$tag-temperature", hint = if (s.greedy) "With top-k 1 or temperature 0, the likeliest word each time." else null,
            less = (s.temperature > 0.0).then { set(s.copy(temperature = JobSettings.round(maxOf(0.0, s.temperature - 0.1)))) },
            more = (s.temperature < JobSettings.TEMPERATURE_MAX).then { set(s.copy(temperature = JobSettings.round(minOf(JobSettings.TEMPERATURE_MAX, s.temperature + 0.1)))) },
        )
        Setting(
            "Top-k", "${s.topK}", tag = "$tag-topk",
            less = (s.topK > 1).then { set(s.copy(topK = TOP_KS.lastOrNull { it < s.topK } ?: 1)) },
            more = (s.topK < JobSettings.TOP_K_MAX).then { set(s.copy(topK = TOP_KS.firstOrNull { it > s.topK } ?: JobSettings.TOP_K_MAX)) },
        )
        Setting(
            "Top-p", two(s.topP), tag = "$tag-topp",
            less = (s.topP > 0.05).then { set(s.copy(topP = JobSettings.round(maxOf(0.05, s.topP - 0.05)))) },
            more = (s.topP < 1.0).then { set(s.copy(topP = JobSettings.round(minOf(1.0, s.topP + 0.05)))) },
        )
        Setting(
            "Longest reply", "${count(s.longestReply)} tokens", tag = "$tag-reply",
            less = step(JobSettings.REPLIES, s.longestReply, -1)?.let { r -> { set(s.copy(longestReply = r)) } },
            more = step(JobSettings.REPLIES, s.longestReply, 1)?.let { r -> { set(s.copy(longestReply = r)) } },
        )
        val first = JobSettings.of(job)
        val firstName = when (job) {
            Job.TALK -> "Edge's settings"
            Job.WRITE -> "0.4's settings"
            Job.SORT -> "the laptop's settings"
        }
        if (s != first) TextLink("Back to $firstName", { set(first) }, tag = "$tag-reset")
        else Text(
            when (job) {
                Job.TALK -> "Edge's settings for Gemma 4 E2B."
                Job.WRITE -> "0.4's settings: the same words propose the same lines."
                Job.SORT -> "The laptop's settings for Stage 4: the likeliest word, a word or two a piece."
            },
            style = Type.small, color = ink.soft,
        )
    }
}

/** Fetch Gemma 4 E4B, asking first to show its progress as a notification where Android asks for that. */
@Composable
fun rememberE4bFetch(): () -> Unit {
    val context = LocalContext.current
    val asked = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { FetchService.start(context, ModelStore.E4B) }
    return {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            asked.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FetchService.start(context, ModelStore.E4B)
        }
    }
}

/** Gemma 4 E4B's state on this phone, looked at every few seconds (each second while it comes). */
@Composable
fun e4bState(app: com.phenomemental.pocketbeach.PocketApp): ModelState? {
    val st by produceState<ModelState?>(null) {
        while (true) {
            val now = withContext(Dispatchers.IO) { runCatching { app.e4b.state() }.getOrNull() }
            value = now
            delay(if (now is ModelState.Fetching) 1000 else 3000)
        }
    }
    return st
}

/**
 * Sundial 0.10.1: when she talks on the smaller model, E2B, said plainly,
 * and why, with the way to the larger one right there: get it, or use it
 * for her talk when it is on the phone. Nothing when she talks on E4B.
 */
@Composable
fun SmallerModel(model: Model) {
    val ink = LocalInk.current
    val app = model.app
    val talk = app.settings(Job.TALK)
    if (talk.model != JobSettings.GEMMA_E2B) return
    val state = e4bState(app)
    val fetch = rememberE4bFetch()
    val scope = rememberCoroutineScope()
    val size = ModelStore.gb(ModelStore.E4B.size)
    Column(Modifier.fillMaxWidth().testTag("smaller-model"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (state) {
            null -> {}
            ModelState.Ready -> {
                Text("She's using the smaller model, as her talk is set below. The larger one is on your phone.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("smaller-model-why"))
                Action("Use the larger model for her talk", { app.set(Job.TALK, talk.copy(model = JobSettings.GEMMA_E4B)) }, Modifier.fillMaxWidth(), tag = "smaller-model-use")
            }
            is ModelState.TooLittleMemory -> Text(
                "She's using the smaller model because this phone hasn't the memory for the larger one: it has ${ModelStore.gb(state.has)}, and the larger one needs a phone with 12 GB.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("smaller-model-why"),
            )
            is ModelState.Fetching -> Text(
                "She's using the smaller model because the larger one isn't on your phone yet. It's coming: ${ModelStore.gb(state.done)} of $size.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("smaller-model-why"),
            )
            ModelState.Checking -> Text(
                "She's using the smaller model because the larger one isn't on your phone yet. It has come, and is being checked.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("smaller-model-why"),
            )
            is ModelState.TooLittleRoom -> Text(
                "She's using the smaller model because the larger one isn't on your phone yet. It needs ${ModelStore.gb(state.needs)} of room, and the phone has ${ModelStore.gb(state.free)} free.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("smaller-model-why"),
            )
            else -> {
                Text("She's using the smaller model because the larger one isn't on your phone yet.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("smaller-model-why"))
                Action(
                    if (state is ModelState.Paused) "Carry on getting the larger model" else "Get the larger model, $size over Wi-Fi",
                    { if (state is ModelState.Failed) scope.launch(Dispatchers.IO) { app.e4b.dismissFailure() }; fetch() },
                    Modifier.fillMaxWidth(), tag = "smaller-model-get",
                )
                Text("Once it's on your phone, set her talk to it here.", style = Type.small, color = ink.soft)
            }
        }
    }
}

/** Gemma 4 E4B: offered when the phone has the memory, fetched on a tap, checked byte for byte, then a choice for each job. */
@Composable
private fun BiggerModel(app: com.phenomemental.pocketbeach.PocketApp, state: ModelState?) {
    val ink = LocalInk.current
    val scope = rememberCoroutineScope()
    val size = ModelStore.gb(ModelStore.E4B.size)
    fun act(block: () -> Unit) { scope.launch(Dispatchers.IO) { block() } }
    val fetch = rememberE4bFetch()
    Column(Modifier.fillMaxWidth().testTag("e4b"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Gemma 4 E4B", style = Type.heading, color = ink.ink)
        when (state) {
            null -> {}
            ModelState.Ready -> Text("On this phone, checked: a choice for each job below.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("e4b-ready"))
            is ModelState.TooLittleMemory -> Text(
                "This phone has ${ModelStore.gb(state.has)} of memory, and E4B needs a phone with 12 GB, so it isn't offered here.",
                style = Type.small, color = ink.soft, modifier = Modifier.testTag("e4b-too-little-memory"),
            )
            is ModelState.TooLittleRoom -> Text(
                "E4B needs ${ModelStore.gb(state.needs)} of room and this phone has ${ModelStore.gb(state.free)} free.",
                style = Type.small, color = ink.soft, modifier = Modifier.testTag("e4b-too-little-room"),
            )
            ModelState.Absent -> {
                Text(
                    "The bigger Gemma: slower, and on the laptop it sorted your day a little better. One download of $size from Hugging Face, " +
                        "over Wi-Fi only, checked byte for byte before it is used. Gemma 4 E2B stays as it is.",
                    style = Type.small, color = ink.soft,
                )
                Action("Get Gemma 4 E4B, $size over Wi-Fi", { fetch() }, Modifier.fillMaxWidth(), tag = "get-e4b")
            }
            is ModelState.Fetching -> {
                Text(
                    "${ModelStore.gb(state.done)} of $size (${state.done * 100 / ModelStore.E4B.size}%)" +
                        if (state.bytesPerSecond > 0) ", at %.1f MB/s".format(state.bytesPerSecond / 1e6) else "",
                    style = Type.body, color = ink.ink, modifier = Modifier.testTag("e4b-fetching"),
                )
                state.note?.let { Text(it, style = Type.small, color = ink.crimson) }
                TextLink("Stop fetching", { act { app.e4b.stop() } }, tag = "e4b-stop")
            }
            is ModelState.Paused -> {
                Text("Paused at ${ModelStore.gb(state.done)} of $size. It carries on from there.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("e4b-paused"))
                Action("Carry on", { fetch() }, Modifier.fillMaxWidth(), tag = "e4b-carry-on")
                TextLink("Discard what has come", { act { app.e4b.discard() } }, tag = "e4b-discard")
            }
            ModelState.Checking -> Text("Checking the download, byte for byte…", style = Type.aside, color = ink.soft)
            is ModelState.Failed -> {
                Notice(state.why, tag = "e4b-failed")
                Action(if (state.done > 0) "Carry on from ${ModelStore.gb(state.done)}" else "Try again", { act { app.e4b.dismissFailure() }; fetch() }, Modifier.fillMaxWidth(), tag = "e4b-try-again")
            }
        }
    }
}

/** A setting and its value, a step less or more on either side (none where it can go no further). */
@Composable
private fun Setting(name: String, value: String, tag: String, hint: String? = null, less: (() -> Unit)?, more: (() -> Unit)?) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, style = Type.body, color = ink.ink)
            if (hint != null) Text(hint, style = Type.small, color = ink.soft, modifier = Modifier.testTag("$tag-hint"))
        }
        Action("−", { less?.invoke() }, enabled = less != null, tag = "$tag-less")
        Text(value, style = Type.small, color = ink.ink, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 84.dp).testTag(tag))
        Action("+", { more?.invoke() }, enabled = more != null, tag = "$tag-more")
    }
}

/** The steps top-k is moved through. */
private val TOP_KS = listOf(1, 2, 5, 10, 20, 32, 40, 64, 100, 128)

/** The value [by] steps along [values] from [now], or null at either end. */
private fun <T> step(values: List<T>, now: T, by: Int): T? = values.getOrNull(values.indexOf(now) + by)

private fun Boolean.then(action: () -> Unit): (() -> Unit)? = if (this) action else null

private fun two(x: Double) = String.format(Locale.UK, "%.2f", x)

private fun count(n: Int) = String.format(Locale.UK, "%,d", n)
