package com.phenomemental.pocketbeach.ui

import android.app.ActivityManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.Shells
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.Whens
import com.phenomemental.pocketbeach.model.Gemma
import com.phenomemental.pocketbeach.model.Job as MindJob
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.ModelStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * One run of the bench at a time: the twenty fixed asks, each on a fresh
 * scratch pocket (nothing of Matthew's is written, nothing posted), then the
 * engine's probes, then the whole run kept as one new block he can share.
 * Held by the app, so it goes on while he looks elsewhere.
 */
class Benching(private val app: PocketApp) {
    sealed interface Stage {
        data object Ready : Stage
        class Loading(val since: Long) : Stage
        class Asking(val run: String, val ask: Bench.Ask) : Stage
        class Probing(val what: String) : Stage
        /** The run kept as [name]: how many of the twenty each way got right, in order. */
        class Saved(val name: String, val right: List<Pair<String, Int>>, val of: Int) : Stage
        class Failed(val why: String) : Stage
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var chat: Mind.Chat? = null
    private var talker: Mind.Talker? = null

    /** How 0.4's first word was chosen on this run: held by the engine, or free and why. */
    private var route = "not asked"

    companion object {
        const val BEFORE = "0.3 (one JSON plan, held)"
        const val HELD = "0.4, her first word held (route word, actions, streamed)"
        const val FREE = "0.4, her first word free (route word, actions, streamed)"
        const val CPU = "0.4 on the CPU, her first word free (route word, actions, streamed)"
    }

    var stage by mutableStateOf<Stage>(Stage.Ready)
        private set
    val results = mutableStateListOf<Bench.Result>()

    /** Told when the run is kept, so the pages show the new block. */
    var changed: () -> Unit = {}

    val running: Boolean get() = stage is Stage.Loading || stage is Stage.Asking || stage is Stage.Probing

    fun start() {
        if (running) return
        results.clear()
        stage = Stage.Loading(System.currentTimeMillis())
        job = scope.launch {
            try {
                val mind = app.mind
                // Ask's conversation goes first: the bench loads the model both ways, and would cut it off.
                withContext(Dispatchers.IO) { app.asking.letGo() }
                val about = mutableListOf(phone(), "The companion: ${companion(mind)}.")
                val loadMs = withContext(Dispatchers.IO) { mind.warm() }
                about += "Loaded " + (loadMs?.let { "in ${Bench.secs(it)} s" } ?: "already") + (mind.processor?.let { ", on the $it" } ?: "") + "."
                // Before: 0.3's companion, one JSON plan an answer, held to its form.
                for (ask in Bench.ASKS) {
                    stage = Stage.Asking(BEFORE, ask)
                    results += withContext(Dispatchers.IO) {
                        Bench.one(ask, app.shelf, System.currentTimeMillis()) { a, pages, storage -> runBlocking { plan(mind, a, pages, storage) } }
                    }
                }
                val before = results.toList()
                // After: 0.4's, a route word then plain words, reads or actions, streamed; her first word held by the engine, then free.
                val after = listOf(HELD to true, FREE to false).map { (name, held) ->
                    val from = results.size
                    for (ask in Bench.ASKS) {
                        stage = Stage.Asking(name, ask)
                        results += withContext(Dispatchers.IO) {
                            Bench.one(ask, app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE) { a, pages, storage -> runBlocking { talks(mind, a, pages, storage, held) } }
                        }
                    }
                    if (held) about += "0.4's first word, asked to be held: $route."
                    Bench.Run(name, results.drop(from))
                }
                stage = Stage.Probing("Keeping one conversation for three asks")
                val keptGpu = withContext(Dispatchers.IO) { kept(mind) }
                // Then on the CPU alone, beside the GPU: the same asks, free as in Ask, and one conversation kept, where its slower reading falls on the first ask.
                stage = Stage.Probing("Loading the model on the CPU")
                val cpu = try {
                    withContext(Dispatchers.IO) { mind.only("CPU") }
                    val cpuLoad = withContext(Dispatchers.IO) { mind.warm() }
                    about[2] = about[2] + " On the CPU alone: loaded " + (cpuLoad?.let { "in ${Bench.secs(it)} s" } ?: "already") + (mind.processor?.let { ", on the $it" } ?: "") + "."
                    val from = results.size
                    for (ask in Bench.ASKS) {
                        stage = Stage.Asking(CPU, ask)
                        results += withContext(Dispatchers.IO) {
                            Bench.one(ask, app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE) { a, pages, storage -> runBlocking { talks(mind, a, pages, storage, false) } }
                        }
                    }
                    stage = Stage.Probing("Keeping one conversation for three asks, on the CPU")
                    val keptCpu = withContext(Dispatchers.IO) { kept(mind) }
                    about += "One conversation kept for three asks, her first word free as in Ask, her first words after: on the GPU $keptGpu; on the CPU $keptCpu."
                    Bench.Run(CPU, results.drop(from))
                } finally {
                    withContext(Dispatchers.IO + NonCancellable) { runCatching { mind.only(null) } }
                }
                (mind as? Gemma)?.let { gemma ->
                    stage = Stage.Probing("Letting the model go, to try the engine")
                    about += withContext(Dispatchers.IO) { gemma.probes { what -> scope.launch { stage = Stage.Probing(what) } } }
                }
                val models = withContext(Dispatchers.IO) { models(mind) }
                val now = System.currentTimeMillis()
                val name = "bench-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.UK))
                val runs = listOf(Bench.Run(BEFORE, before)) + after + cpu
                val block = Bench.block(runs, about, now, models)
                val done = withContext(Dispatchers.IO) { app.pages.carryOut(app.pages.planKeep(name, block)) }
                changed()
                stage = if (done.ok) Stage.Saved(name, runs.map { it.name to it.results.count { r -> r.right } }, Bench.ASKS.size) else Stage.Failed("The run wasn't kept: ${done.problem}")
            } catch (e: CancellationException) {
                stage = Stage.Ready
                throw e
            } catch (e: Exception) {
                stage = Stage.Failed(e.message ?: e::class.java.simpleName)
            }
        }
    }

    /**
     * 0.6, the quicker run: only each model on this phone, its speed and the
     * telling test, kept as a block of its own (bench-models- and the date).
     */
    fun startModels() {
        if (running) return
        results.clear()
        stage = Stage.Loading(System.currentTimeMillis())
        job = scope.launch {
            try {
                val mind = app.mind
                withContext(Dispatchers.IO) { app.asking.letGo() }
                val about = listOf(phone(), "The companion: ${companion(mind)}.", "The Sort job's settings, each model in turn: " + settingsLine(app.settings(MindJob.SORT)) + ".",
                    "The Talk job's settings, each model in turn: " + settingsLine(app.settings(MindJob.TALK)) + "; the Write job's: " + settingsLine(app.settings(MindJob.WRITE)) + ".")
                val models = withContext(Dispatchers.IO) { models(mind) }
                val name = "bench-models-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.UK))
                val block = Bench.block(emptyList(), about, System.currentTimeMillis(), models)
                val done = withContext(Dispatchers.IO) { app.pages.carryOut(app.pages.planKeep(name, block)) }
                changed()
                stage = if (done.ok) Stage.Saved(name, emptyList(), 0) else Stage.Failed("The run wasn't kept: ${done.problem}")
            } catch (e: CancellationException) {
                stage = Stage.Ready
                throw e
            } catch (e: Exception) {
                stage = Stage.Failed(e.message ?: e::class.java.simpleName)
            }
        }
    }

    /**
     * Each model on this phone, with the Sort job's settings but its own
     * model: how long it takes to load, a plain answer timed, and the telling
     * test, written then spoken, each followed by the second telling, sorted a
     * piece at a time as Ask sorts a day, on a scratch hand, with its marks.
     */
    private suspend fun models(mind: Mind): List<String> = buildList {
        for (spec in ModelStore.SPECS) {
            if (mind.state(spec.name) !is ModelStore.State.Ready) {
                add("${spec.name}: not on this phone" + if (spec == ModelStore.E4B) " (get it in Settings)." else ".")
                continue
            }
            val s = app.settings(MindJob.SORT).copy(model = spec.name)
            stage = Stage.Probing("${spec.name}: loading")
            val load = runCatching { mind.warm(s) }
            stage = Stage.Probing("${spec.name}: a plain answer")
            val plain = runCatching {
                mind.ask("You are a companion on a phone. Answer in plain words.", "Tell me about the sea at night, in one plain paragraph of about 120 words.", s.copy(longestReply = 300))
            }
            add(
                "${spec.name}" + (plain.getOrNull()?.processor?.let { ", on the $it" } ?: "") + ": " +
                    load.fold({ ms -> "loaded " + (ms?.let { "in ${Bench.secs(it)} s" } ?: "already") }, { "it wouldn't load (${it.message})" }) + "; " +
                    plain.fold({ a -> "a plain answer, " + (a.turn?.let { t -> "first token ${t.firstTokenSeconds?.let { "%.1f".format(Locale.UK, it) } ?: "?"} s, ${t.wrote} tokens at ${"%.1f".format(Locale.UK, t.writePerSecond)} a second, " } ?: "") + "${Bench.secs(a.ms)} s in all." }, { "a plain answer failed (${it.message})." }),
            )
            if (load.isFailure) continue
            stage = Stage.Probing("${spec.name}: the Talk job, his message of 29 October")
            addAll(runCatching { talkAsks(mind, spec.name) }.getOrElse { listOf("${spec.name}, the Talk job: it failed (${it.message}).") })
            for ((told, said) in listOf("written" to Telling.WRITTEN, "spoken" to Telling.SPOKEN)) {
                stage = Stage.Probing("${spec.name}: the telling test, $told")
                add(runCatching { tellingTest(mind, s, said) }.getOrElse { "the telling test failed (${it.message})" }.let { "${spec.name}, the telling test, $told: $it" })
            }
        }
    }

    /**
     * 0.6.1, with [model] and the Talk job's settings: what makes her play her
     * turn back, his message of 29 October put to her each way tried on the
     * laptop (one reply each, as the app draws them), then the Talk job's two
     * fixed asks as Ask puts them now, each with the Write job's proposal
     * where his words may ask for a write. A scratch pocket; nothing written.
     */
    private suspend fun talkAsks(mind: Mind, model: String): List<String> = buildList {
        val talk = app.settings(MindJob.TALK).copy(model = model)
        val write = app.settings(MindJob.WRITE).copy(model = model)
        val nowMs = System.currentTimeMillis()
        val now = java.time.ZonedDateTime.now()
        val (_, fixture) = Bench.fixture(app.shelf, nowMs, Talks.SHELL_RECIPE)
        val q = Bench.GEMMA_ASK
        val ways = listOf(
            "his message alone" to (null to q),
            "her 0.6 instruction" to (Talking.instruction(HelloWorld.SHELL, fixture) to q),
            "0.6, the date and her shell in his turn" to (Talking.instruction(HelloWorld.SHELL, fixture) to Talking.turn(q, now, HelloWorld.SHELL, fixture)),
            "0.6.1, the time and her shell in her instruction, his words alone" to (Talking.instruction(HelloWorld.SHELL, fixture, now, q) to Talking.said(q)),
        )
        for ((name, way) in ways) {
            val a = mind.ask(way.first, way.second, talk.copy(longestReply = 300))
            val copied = Talking.copied(a.text, q, HelloWorld.SHELL_OPENING)
            val shown = Talking.unechoed(a.text, q, HelloWorld.SHELL_OPENING)
            val answers = Bench.answers(Bench.TALK_ASKS.first(), shown)
            add("$model, the Talk job, $name: " + (if (copied.isEmpty()) "plays nothing back" else "PLAYS HER TURN BACK") + ", " + (if (answers) "answers him" else "doesn't answer him") +
                " (${Bench.secs(a.ms)} s): “${a.text.replace("\n", " / ").take(240)}”")
        }
        for (ask in Bench.TALK_ASKS) {
            val (storage, pages) = Bench.fixture(app.shelf, nowMs, Talks.SHELL_RECIPE)
            val a = mind.ask(Talking.instruction(HelloWorld.SHELL, pages, now, ask.question), Talking.said(ask.question), talk)
            val proposal = if (!Talking.mayWrite(ask.question)) null else {
                val window = Shells(storage, app.shelf).wear(HelloWorld.SHELL, nowMs).window
                mind.talker(Talks.instruction(HelloWorld.SHELL, window, Asks.plainWindow(pages, HelloWorld.SHELL), pages, examplesInText = false), Talks.opening(emptyList()), settings = write).use { t ->
                    Talks.ask(ask.question, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { nowMs }, answerReads = false).planned?.proposal
                }
            }
            add("$model, the Talk job's fixed ask " + Bench.talkLine(ask, a.text, proposal, a.ms))
        }
    }

    /** The telling test once: sorted, landed on a scratch hand, the second telling after it, and the marks. */
    private suspend fun tellingTest(mind: Mind, s: JobSettings, said: String): String {
        val firsts = mutableListOf<Double>()
        suspend fun sort(state: JsObj, telling: String): Pair<List<Telling.Placed>, String> {
            val started = System.nanoTime()
            val instruction = Telling.sortInstruction(Telling.BENCH_DAY)
            fun ask(system: String?, message: String, settings: JobSettings) = runBlocking { mind.ask(system, message, settings) }.also { a -> a.turn?.firstTokenSeconds?.let(firsts::add) }.text
            val (placed, steps) = Telling.sorted(
                telling, Telling.BENCH_DAY, Whens.first(), state,
                judge = { piece -> ask(instruction, Telling.aloneAsk(piece, telling), s) },
                about = { piece, card -> Telling.yes(ask(null, Telling.cardAsk(piece, telling, card), s)) },
            )
            val sorting = (System.nanoTime() - started) / 1_000_000
            val rungStarted = System.nanoTime()
            // 0.6.1: a rung is asked about only when his words bear on it, and takes one of his pieces or none.
            var rungsAsked = false
            val rungs = Telling.rungsFor(state, steps) { a -> rungsAsked = true; ask(null, a, s.copy(longestReply = maxOf(64, s.longestReply))) }
            return (placed + rungs) to "${steps.size} pieces in ${Bench.secs(sorting)} s, " +
                if (rungsAsked) "the rungs in ${Bench.secs((System.nanoTime() - rungStarted) / 1_000_000)} s" else "no rung asked about"
        }
        val (placed, timing) = sort(Telling.shape(), said)
        val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
        val (placed2, timing2) = sort(state, Telling.SECOND)
        val after = Telling.land(state, Telling.check(Telling.SECOND, placed2).kept)
        val marks = Telling.marks(said, placed) + Telling.secondMarks(after)
        val median = firsts.sorted().getOrNull(firsts.size / 2)
        return "${marks.count { it.met }} of ${marks.size} marks; $timing" + (median?.let { ", first tokens a median of ${"%.1f".format(Locale.UK, it)} s" } ?: "") +
            "; the second telling $timing2." + marks.filter { !it.met }.takeIf { it.isNotEmpty() }?.let { missed -> " Missed: " + missed.joinToString("; ") { "${it.name} (${it.note.take(140)})" } + "." }.orEmpty()
    }

    private fun settingsLine(s: JobSettings): String =
        "${if (s.processor == "GPU") "the GPU, else the CPU" else "the CPU"}, a context of ${s.context} tokens, " +
            (if (s.greedy) "the likeliest word each time" else "temperature %.2f, top-k %d, top-p %.2f".format(Locale.UK, s.temperature, s.topK, s.topP)) +
            ", replies of up to ${s.longestReply} tokens"

    /** Forget the last run, as the device tests begin. */
    fun reset() {
        job?.cancel()
        results.clear()
        stage = Stage.Ready
    }

    /** Stop the run; nothing is kept. */
    fun stop() {
        chat?.stop()
        talker?.stop()
        job?.cancel()
        stage = Stage.Ready
    }

    /** Today's companion: one JSON plan an answer, its writes planned on the scratch pocket, timed. */
    private suspend fun plan(mind: Mind, ask: Bench.Ask, pages: com.phenomemental.pocketbeach.bsp.Pages, storage: com.phenomemental.pocketbeach.bsp.MemoryStorage): Bench.Did {
        val now = System.currentTimeMillis()
        val instruction = Asks.instruction(HelloWorld.SHELL, Shells(storage, app.shelf).wear(HelloWorld.SHELL, now).window, Asks.plainWindow(pages, HelloWorld.SHELL), pages)
        val started = System.nanoTime()
        return mind.chat(instruction).use { c ->
            chat = c
            try {
                val asked = Asks.ask(ask.question, c.talk, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal()) { System.currentTimeMillis() }
                val total = (System.nanoTime() - started) / 1_000_000
                val turns = c.pace.turns
                val first = turns.firstOrNull()?.firstTokenSeconds?.let { (it * 1000).toLong() }
                // Today's companion shows nothing until its whole plan is written.
                Bench.didOfPlan(asked, Bench.Timing(first, total, total, asked.exchanges.size, turns.sumOf { it.read }.takeIf { turns.isNotEmpty() }, turns.sumOf { it.wrote }.takeIf { turns.isNotEmpty() }))
            } finally {
                chat = null
            }
        }
    }

    /** 0.4's companion: one conversation per ask (the bench's pocket is fresh each time), her reply streamed, timed to its first token and to her first words shown. */
    private suspend fun talks(mind: Mind, ask: Bench.Ask, pages: com.phenomemental.pocketbeach.bsp.Pages, storage: com.phenomemental.pocketbeach.bsp.MemoryStorage, held: Boolean): Bench.Did {
        val now = System.currentTimeMillis()
        val instruction = Talks.instruction(HelloWorld.SHELL, Shells(storage, app.shelf).wear(HelloWorld.SHELL, now).window, Asks.plainWindow(pages, HelloWorld.SHELL), pages, examplesInText = false)
        val started = System.nanoTime()
        return mind.talker(instruction, Talks.opening(emptyList()), held).use { t ->
            talker = t
            try {
                if (held) route = t.route
                var shown: Long? = null
                val answered = Talks.ask(ask.question, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { System.currentTimeMillis() }) { words ->
                    if (shown == null && words.isNotBlank()) shown = (System.nanoTime() - started) / 1_000_000
                }
                val total = (System.nanoTime() - started) / 1_000_000
                val turns = t.pace.turns
                val first = turns.firstOrNull()?.firstTokenSeconds?.let { (it * 1000).toLong() }
                Bench.didOfTalks(
                    answered,
                    Bench.Timing(first, shown ?: total, total, answered.exchanges.size, turns.sumOf { it.read }.takeIf { turns.isNotEmpty() }, turns.sumOf { it.wrote }.takeIf { turns.isNotEmpty() }),
                    offered = if (answered.route == Talks.Route.TALK) Talks.keepIn(ask.question, pages) else null,
                )
            } finally {
                talker = null
            }
        }
    }

    /** One conversation kept for three asks, as Ask keeps it: how soon her first words come once the instruction has been read. */
    private suspend fun kept(mind: Mind): String {
        val (storage, pages) = Bench.fixture(app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE)
        val instruction = Talks.instruction(HelloWorld.SHELL, Shells(storage, app.shelf).wear(HelloWorld.SHELL, System.currentTimeMillis()).window, Asks.plainWindow(pages, HelloWorld.SHELL), pages, examplesInText = false)
        return runCatching {
            mind.talker(instruction, Talks.opening(emptyList())).use { t ->
                val times = listOf("What's your name?", "What can you do?", "Thanks, that's all for now.").map { q ->
                    val started = System.nanoTime()
                    var first: Long? = null
                    Talks.ask(q, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { System.currentTimeMillis() }) { words ->
                        if (first == null && words.isNotBlank()) first = (System.nanoTime() - started) / 1_000_000
                    }
                    Bench.secs(first ?: ((System.nanoTime() - started) / 1_000_000)) + " s"
                }
                times.joinToString(", then ")
            }
        }.getOrElse { "(it failed: ${it.message})" }
    }

    private fun phone(): String {
        val memory = ActivityManager.MemoryInfo().also { app.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem
        return "The phone: ${Build.MANUFACTURER} ${Build.MODEL}, ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}, " +
            String.format(Locale.UK, "%.1f", memory / 1e9) + " GB, Android ${Build.VERSION.RELEASE}; Pocket beach ${app.packageManager.getPackageInfo(app.packageName, 0).versionName}."
    }

    private fun companion(mind: Mind): String = when (mind) {
        is Gemma -> "Gemma 4 E2B (revision 6e5c4f1e) on LiteRT-LM 0.18.0, a context of ${Asks.Tuning.MAX_TOKENS} tokens, speculative decoding " +
            (if (Gemma.SPECULATIVE) "on" else "off")
        else -> "a stand-in, as the device tests run it"
    }
}
