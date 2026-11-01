package com.phenomemental.pocketbeach.ui

import android.app.ActivityManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Acts
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.Keeping
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
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
 * One run of the bench at a time: 0.6.4, each model on this phone, its
 * speed, the Keep asks, the Talk job's fixed asks and the telling test, each
 * on a fresh scratch pocket (nothing of Matthew's is written, nothing
 * posted), then the whole run kept as one new block he can share. Held by
 * the app, so it goes on while he looks elsewhere.
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

    var stage by mutableStateOf<Stage>(Stage.Ready)
        private set
    val results = mutableStateListOf<Bench.Result>()

    /** Told when the run is kept, so the pages show the new block. */
    var changed: () -> Unit = {}

    val running: Boolean get() = stage is Stage.Loading || stage is Stage.Asking || stage is Stage.Probing

    /**
     * 0.6.4, rewritten to the principles: how it was run, the engine's
     * probes, then each model on this phone (its speed, the Keep asks, the
     * Talk job's fixed asks, the telling test), kept as one block. 0.3's
     * run, 0.4's runs and one conversation kept are gone: Ask no longer
     * works that way.
     */
    fun start() {
        if (running) return
        results.clear()
        stage = Stage.Loading(System.currentTimeMillis())
        job = scope.launch {
            try {
                val mind = app.mind
                // Ask's conversation goes first: the bench loads the model, and would cut it off.
                withContext(Dispatchers.IO) { app.asking.letGo() }
                val about = (listOf(phone(), "The companion: ${companion(mind)}.") + settingsAbout()).toMutableList()
                (mind as? Gemma)?.let { gemma ->
                    stage = Stage.Probing("Letting the model go, to try the engine")
                    about += withContext(Dispatchers.IO) { gemma.probes { what -> scope.launch { stage = Stage.Probing(what) } } }
                }
                keep("bench-", about, withContext(Dispatchers.IO) { models(mind) })
            } catch (e: CancellationException) {
                stage = Stage.Ready
                throw e
            } catch (e: Exception) {
                stage = Stage.Failed(e.message ?: e::class.java.simpleName)
            }
        }
    }

    private fun settingsAbout(): List<String> = listOf(
        "The Sort job's settings, each model in turn: " + settingsLine(app.settings(MindJob.SORT)) + ".",
        "The Talk job's settings, each model in turn: " + settingsLine(app.settings(MindJob.TALK)) + "; the Write job's: " + settingsLine(app.settings(MindJob.WRITE)) + ".",
    )

    /** The run kept as one block: the Keep asks first, then each model. */
    private suspend fun keep(prefix: String, about: List<String>, run: Pair<List<Pair<String, List<String>>>, List<Pair<String, List<String>>>>) {
        val name = prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.UK))
        val block = Bench.block(emptyList(), about, System.currentTimeMillis(), run.first, run.second)
        val done = withContext(Dispatchers.IO) { app.pages.carryOut(app.pages.planKeep(name, block)) }
        changed()
        stage = if (done.ok) Stage.Saved(name, run.second.map { (model, lines) -> model to lines.count { it.contains(". right in ") } }, Bench.KEEP_ASKS.size)
        else Stage.Failed("The run wasn't kept: ${done.problem}")
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
                val about = listOf(phone(), "The companion: ${companion(mind)}.") + settingsAbout()
                keep("bench-models-", about, withContext(Dispatchers.IO) { models(mind) })
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
     * 0.6.1: and the Talk job (see [talkAsks]). Each model's lines are kept
     * beneath its name.
     */
    private suspend fun models(mind: Mind): Pair<List<Pair<String, List<String>>>, List<Pair<String, List<String>>>> {
        val keeps = mutableListOf<Pair<String, List<String>>>()
        val models = ModelStore.SPECS.map { spec -> spec.name to model(mind, spec) { keeps += spec.name to it } }
        return models to keeps
    }

    /** One model's lines: nine at most, a place keeps nine; its Keep asks, to [kept]. */
    private suspend fun model(mind: Mind, spec: ModelStore.Spec, kept: (List<String>) -> Unit): List<String> = buildList {
        run {
            if (mind.state(spec.name) !is ModelStore.State.Ready) {
                add("${spec.name}: not on this phone" + if (spec == ModelStore.E4B) " (get it in Settings)." else ".")
                return@run
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
            if (load.isFailure) return@run
            // 0.6.4: the Keep asks first.
            kept(runCatching { keepAsks(mind, spec.name) }.getOrElse { listOf("Keep asks: they failed (${it.message}).") })
            stage = Stage.Probing("${spec.name}: the Talk job's fixed asks")
            addAll(runCatching { talkAsks(mind, spec.name) }.getOrElse { listOf("${spec.name}, the Talk job: it failed (${it.message}).") })
            for ((told, said) in listOf("written" to Telling.WRITTEN, "spoken" to Telling.SPOKEN)) {
                stage = Stage.Probing("${spec.name}: the telling test, $told")
                add(runCatching { tellingTest(mind, s, said) }.getOrElse { "the telling test failed (${it.message})" }.let { "${spec.name}, the telling test, $told: $it" })
            }
        }
    }

    /**
     * 0.6.4: the Keep asks, each on a fresh scratch pocket, through the gate
     * as Ask runs it: she only talks, or the app asks where, or her name by
     * code, or the writing check with [model] and the Write job's settings,
     * its proposal under the guards. Nothing of his written, nothing posted.
     */
    private suspend fun keepAsks(mind: Mind, model: String): List<String> = buildList {
        val write = app.settings(MindJob.WRITE).copy(model = model)
        for (ask in Bench.KEEP_ASKS) {
            stage = Stage.Probing("$model: Keep ask ${ask.n} of ${Bench.KEEP_ASKS.size}")
            val nowMs = System.currentTimeMillis()
            val (storage, pages) = Bench.keepFixture(ask, app.shelf, nowMs)
            val way = Keeping.way(ask.question, emptyList(), pages)
            val started = System.nanoTime()
            val planned = when (way) {
                is Keeping.Way.Write -> mind.talker(Talks.lean(HelloWorld.SHELL, pages), Talks.opening(emptyList()), settings = write).use { t ->
                    talker = t
                    try {
                        Talks.ask(
                            ask.question, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { nowMs },
                            before = Talks.forMessage(HelloWorld.SHELL, pages, ask.question), answerReads = false, guard = Acts.Guard(listOf(ask.question), ask.question),
                        ).planned
                    } finally {
                        talker = null
                    }
                }
                else -> Bench.byApp(way, storage, app.shelf, nowMs)
            }
            add(Bench.keepLine(ask, way, planned, (System.nanoTime() - started) / 1_000_000))
        }
    }

    /**
     * The Talk job's fixed asks as Ask puts them, with [model] and the Talk
     * job's settings, each with what the gate does with his words (0.6.4):
     * the writing check only where they ask, her name by code. A scratch
     * pocket; nothing written.
     */
    private suspend fun talkAsks(mind: Mind, model: String): List<String> = buildList {
        val talk = app.settings(MindJob.TALK).copy(model = model)
        val write = app.settings(MindJob.WRITE).copy(model = model)
        val nowMs = System.currentTimeMillis()
        val now = java.time.ZonedDateTime.now()
        for (ask in Bench.TALK_ASKS) {
            val (storage, pages) = Bench.fixture(app.shelf, nowMs, Talks.SHELL_RECIPE)
            // 0.6.2: ask 23, her shell already saying the name: nothing is proposed.
            if (ask.named) check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me ${ask.name}.")).ok)
            // 0.7: as Ask asks her now: her window, then the turn with the time and the app's line.
            HerWindow.ensure(pages)
            val way = Keeping.way(ask.question, emptyList(), pages)
            val a = mind.ask(HerWindow.instruction(HelloWorld.SHELL, pages, storage, app.shelf, now), HerWindow.turn(ask.question, now, pages, storage, HerWindow.appLine(way, ask.question)), talk)
            val proposal = when (way) {
                is Keeping.Way.Write -> mind.talker(Talks.lean(HelloWorld.SHELL, pages), Talks.opening(emptyList()), settings = write).use { t ->
                    Talks.ask(ask.question, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { nowMs }, before = Talks.forMessage(HelloWorld.SHELL, pages, ask.question), answerReads = false, guard = Acts.Guard(listOf(ask.question), ask.question)).planned?.proposal
                }
                else -> Bench.byApp(way, storage, app.shelf, nowMs)?.proposal
            }
            // 0.6.3: the safety note Ask would show him beneath her reply, from his own safety words and note (read, never written).
            val note = if (ask.safety) Talking.note(ask.question, app.pages) else null
            add("$model, the Talk job's fixed ask " + Bench.talkLine(ask, a.text, proposal, a.ms, note))
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

    private fun phone(): String {
        val memory = ActivityManager.MemoryInfo().also { app.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem
        return "The phone: ${Build.MANUFACTURER} ${Build.MODEL}, ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}, " +
            String.format(Locale.UK, "%.1f", memory / 1e9) + " GB, Android ${Build.VERSION.RELEASE}; Rockpool ${app.packageManager.getPackageInfo(app.packageName, 0).versionName}."
    }

    private fun companion(mind: Mind): String = when (mind) {
        is Gemma -> "Gemma 4 E2B (revision 6e5c4f1e) on LiteRT-LM 0.18.1, a context of ${Asks.Tuning.MAX_TOKENS} tokens, speculative decoding " +
            (if (Gemma.SPECULATIVE) "on" else "off")
        else -> "a stand-in, as the device tests run it"
    }
}
