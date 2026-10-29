package com.phenomemental.pocketbeach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Acts
import com.phenomemental.pocketbeach.bsp.Answer
import com.phenomemental.pocketbeach.bsp.Asked
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Carried
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.Js
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.Whens
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Shells
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.model.Job as MindJob
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.Pace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/**
 * One Ask at a time, from Matthew's words to the entry it leaves in the log.
 * Held by the app, not the screen, so an Ask goes on while he looks
 * elsewhere and is still there when he comes back.
 *
 * 0.4: Ask is a conversation. One conversation with the companion is kept
 * while the Ask screen is open, its instruction processed once, opened
 * afresh only when the pocket has changed, and carrying the last six
 * exchanges. Her answer begins with talk, read or act ([Talks]); talk
 * streams into the page as she writes it, and an act is planned for Yes.
 *
 * 0.5, let her talk: two jobs, a conversation each, kept side by side. She
 * talks first ([Talking], drawn as the Talk job's settings say), her words
 * streaming in; then, if his words may ask for a write, the Write job is
 * asked the 0.4 way, and what it proposes is shown beneath her words for
 * Yes. Should the phone not keep the two side by side, each is opened in
 * turn, carrying its last six exchanges.
 *
 * 0.6, Stage 4: when his words are a day told ([Landing.isTelling]), after
 * her reply the Sort job places it a piece at a time, each piece in a
 * conversation of its own, and the day, the hand and the rungs are shown as
 * one proposal ([Stage.Day]): any line unticked or moved before Yes, each
 * move kept as a worked example she is shown next time.
 */
class Asking(private val app: PocketApp) {
    sealed interface Stage {
        data object Writing : Stage
        class Thinking(val since: Long) : Stage
        /** A quote in her act matches more than one line: which Matthew means, before anything is planned. */
        class Choosing(val answered: Talks.Answered, val chosen: Map<Int, String>, val took: Long, val pace: Pace?) : Stage
        /** The companion's answer, and what it proposes, waiting on Yes or Not now; [pace], where and how fast it answered; [firstShown], when her first words showed. */
        class Shown(val asked: Asked, val took: Long, val pace: Pace?, val firstShown: Long? = null) : Stage
        class Declining(val shown: Shown) : Stage
        class Carrying(val shown: Shown) : Stage
        /** The writes landed (or one didn't); the post, if one was offered, waits on its own Yes. */
        class Written(val shown: Shown, val carried: Carried) : Stage
        class Posting(val written: Written, val message: PoolMessage) : Stage
        /** The Ask is over and logged: [note] says how it ended. */
        class Over(val shown: Shown?, val carried: Carried?, val posted: Posted?, val note: String) : Stage
        /** His day, sorted: her reply, and every line of the landing, to untick or move before Yes. [pieces], how many she placed. */
        class Day(val shown: Shown, val said: String, val sorting: Landing.Sorting, val lines: List<Landing.Line>, val pieces: Int) : Stage
        class DayWriting(val day: Day) : Stage
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    /** A job's conversation, kept while Ask is open, and the settings it was opened with. */
    private class Kept(val talker: Mind.Talker, val settings: JobSettings)

    /** Each job's conversation; the jobs whose pocket has changed since their instruction was made. */
    private val kept = java.util.concurrent.ConcurrentHashMap<MindJob, Kept>()
    private val stale: MutableSet<MindJob> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** Whether the two conversations can be kept side by side on this phone; once they can't, each is opened in turn. */
    @Volatile private var together = true
    private val talking = Mutex()

    /** Her reply is in, and the Write job is seeing whether his words ask for a write. */
    var checking by mutableStateOf(false)
        private set

    /** Her reply is in, and the Sort job is placing his day: pieces placed, of how many; null when it isn't. */
    var sorting by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    /** Her words so far, as they stream in. */
    var live by mutableStateOf("")
        private set

    /** The block Matthew's words named when she only talked: the app offers to keep them there ("Keep this in Million?"). */
    var keepIn by mutableStateOf<String?>(null)
        private set

    /**
     * An exchange of this Ask screen's conversation, as the page shows it:
     * his words, her reply, how it ended and how long it took, when he said
     * it ([at]), and what came of a proposal ([outcome], null when there
     * was none).
     */
    class Exchange(val question: String, val said: String, val note: String, val time: String, val at: Long = System.currentTimeMillis(), val outcome: String? = null)

    /** The exchanges so far, oldest first. */
    val history = mutableStateListOf<Exchange>()

    /** Each job's last six exchanges as they went, carried into its conversation opened afresh. */
    private val context = MindJob.values().associateWith { ArrayDeque<Pair<String, String>>() }

    /** Her talk since the Write job was last asked: told it the next time it is, since it doesn't see turns it sat out. */
    private val unseen = mutableListOf<Pair<String, String>>()

    /** How her last act ended (yes, not now), told her with the next ask, since she can't see it otherwise. */
    private var told: String? = null

    /** Told whenever the Ask changes the pocket, so the pages show it. */
    var changed: () -> Unit = {}

    var question by mutableStateOf("")
    var stage by mutableStateOf<Stage>(Stage.Writing)
        private set
    private var asked: String = ""
    private var askedAt = 0L

    /** The shell's opening line as it stands in the pocket. */
    fun shellOpening(): String = app.pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    /** While the bench runs, Ask waits: the bench loads the model its own ways, and a conversation open here would be cut off. */
    val benchRunning: Boolean get() = app.benching.running

    /** How many times Ask has been opened: a close asked for before the latest opening lets nothing go. */
    @Volatile private var openings = 0

    /** Ask is open: load the model and open the conversations now, while Matthew writes. */
    fun opened() {
        openings++
        if (benchRunning) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                talking.withLock {
                    talker(MindJob.TALK)
                    if (together) talker(MindJob.WRITE)
                }
            }
        }
    }

    /** Let the conversations go now, before the bench loads the model its own ways. */
    suspend fun letGo() {
        talking.withLock { closeAll() }
    }

    /** Close both jobs' conversations. Called holding [talking], or as Ask is reset. */
    private fun closeAll() {
        kept.values.forEach { runCatching { it.talker.close() } }
        kept.clear()
    }

    /**
     * Ask is closed: let the conversation go (the model goes after a few idle
     * minutes). A close and an opening run on other threads in either order
     * (as when Ask is left and opened again at once): a close is let go once
     * Ask has been opened since, or it would shut the conversation just
     * opened, to be opened again at the first ask.
     */
    fun closed() {
        if (stage is Stage.Thinking) return
        val at = openings
        scope.launch(Dispatchers.IO) {
            talking.withLock {
                if (openings != at) return@withLock
                closeAll()
            }
        }
    }

    /**
     * [job]'s conversation, opened afresh if the pocket or the job's settings
     * have changed since it was. Should the phone not open it beside the
     * other's, the other is let go, and from then on each is opened in turn.
     * Called holding [talking].
     */
    private suspend fun talker(job: MindJob): Mind.Talker {
        val settings = app.settings(job)
        kept[job]?.let { k ->
            if (job !in stale && k.settings == settings) return k.talker
            runCatching { k.talker.close() }
            kept.remove(job)
        }
        if (!together) kept.keys.filter { it != job }.forEach { other -> kept.remove(other)?.let { runCatching { it.talker.close() } } }
        val open = suspend {
            when (job) {
                MindJob.SORT -> error("The Sort job asks a piece at a time, in conversations of its own.")
                MindJob.TALK -> app.mind.talker(Talking.instruction(HelloWorld.SHELL, app.pages), context.getValue(job).toList(), settings = settings)
                MindJob.WRITE -> app.mind.talker(
                    Talks.instruction(HelloWorld.SHELL, Companion.window(app), Asks.plainWindow(app.pages, HelloWorld.SHELL), app.pages, examplesInText = false),
                    Talks.opening(context.getValue(job).toList()), settings = settings,
                )
            }
        }
        val t = try {
            open()
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException || kept.isEmpty()) throw e
            android.util.Log.w("Asking", "The two conversations couldn't be kept side by side; each is opened in turn from now on.", e)
            together = false
            closeAll()
            open()
        }
        kept[job] = Kept(t, settings)
        stale.remove(job)
        return t
    }

    fun ask() {
        val q = question.trim()
        if (q.isEmpty() || benchRunning || stage !is Stage.Writing && stage !is Stage.Over) return
        asked = q
        askedAt = System.currentTimeMillis()
        live = ""
        keepIn = null
        checking = false
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch {
            val started = System.nanoTime()
            var pace: Pace? = null
            var firstShown: Long? = null
            val note = told.also { told = null }
            // Her reply, streamed as she writes it.
            val talked = withContext(Dispatchers.IO) {
                runCatching {
                    talking.withLock {
                        val t = talker(MindJob.TALK)
                        val before = t.pace.turns.size
                        val first = history.isEmpty() && t.pace.turns.isEmpty()
                        val message = Talking.turn(q, ZonedDateTime.now(), HelloWorld.SHELL, app.pages, note)
                        val words = StringBuilder()
                        val reply = t.stream.say(message) { piece ->
                            words.append(piece)
                            if (firstShown == null && words.isNotBlank()) firstShown = (System.nanoTime() - started) / 1_000_000
                            live = words.toString().trim()
                        }
                        pace = Pace(t.pace.processor, t.pace.whyNotGpu, if (first) t.pace.loadMs else null).also { p -> p.turns.addAll(t.pace.turns.drop(before)) }
                        message to reply.trim()
                    }
                }
            }
            val (message, said) = talked.getOrElse { e ->
                if (stage is Stage.Thinking) end(null, null, null, "The companion couldn't answer: ${e.message ?: e::class.simpleName}.", (System.nanoTime() - started) / 1_000_000, pace)
                return@launch
            }
            live = said
            remember(MindJob.TALK, Talking.kept(q), said)
            // A day told: the Sort job places it, a piece at a time, and the landing is one proposal.
            if (withContext(Dispatchers.IO) { Landing.isTelling(q, Telling.cards(handNow())) }) {
                val talk = Talks.Answered(Talks.Route.TALK, said, emptyList(), null, null, listOf(message to said))
                day(q, Stage.Shown(asAsked(talk), 0, pace, firstShown), started)
                return@launch
            }
            // Then, if his words may ask for a write, what the Write job proposes.
            val written = if (!Talking.mayWrite(q)) null else {
                checking = true
                withContext(Dispatchers.IO) {
                    runCatching {
                        talking.withLock {
                            val w = talker(MindJob.WRITE)
                            val before = w.pace.turns.size
                            val sinceLast = unseen.takeLast(3).joinToString(" ") { (his, hers) -> "Matthew said: ${Words.quoted(his, 200)}; the companion said: ${Words.quoted(hers, 200)}." }
                            val a = Talks.ask(
                                q, w.stream, app.pages, app.storage, app.shelf, shellOpening(), HelloWorld.seal(), { System.currentTimeMillis() },
                                before = listOfNotNull(sinceLast.takeIf(String::isNotEmpty)?.let { "Since you were last asked: $it" }, note).joinToString(" ").ifEmpty { null },
                                answerReads = false,
                            )
                            pace?.turns?.addAll(w.pace.turns.drop(before))
                            a
                        }
                    }
                }.also { checking = false }
            }
            unseen += q to said
            if (written != null) unseen.clear()
            val took = (System.nanoTime() - started) / 1_000_000
            val talk = Talks.Answered(Talks.Route.TALK, said, emptyList(), null, null, listOf(message to said))
            val a = written?.getOrNull()?.let { w ->
                remember(MindJob.WRITE, Talks.SAYS + asked, w.exchanges.lastOrNull()?.second.orEmpty())
                if (w.route == Talks.Route.ACT) Talks.Answered(w.route, said, w.found, w.planned, w.problem, talk.exchanges + w.exchanges, w.actions) else null
            } ?: talk
            answered(a, emptyMap(), took, pace, firstShown, writeFailed = written?.exceptionOrNull())
        }
    }

    /** His structure block as it stands: the six named, the hand and the rungs; his structure as first given when there is none yet. */
    private fun handNow(): JsObj = (app.storage.read(Landing.TELLING)?.let(::parseJson) as? JsObj) ?: Telling.shape()

    private fun stored(name: String): Js? = app.storage.read(name)?.let(::parseJson)

    /**
     * Sort [said] as his day, after her reply [talked]: each piece placed by
     * the Sort job in a conversation of its own, its when by his time words,
     * a piece near a card put to her yes or no, then the rungs; the lines
     * shown as one proposal.
     */
    private suspend fun day(said: String, talked: Stage.Shown, started: Long) {
        sorting = 0 to Telling.cut(said).size
        val outcome = withContext(Dispatchers.IO) {
            runCatching {
                talking.withLock {
                    val settings = app.settings(MindJob.SORT)
                    val today = java.time.LocalDate.now()
                    val state = handNow()
                    val words = stored(Whens.BLOCK) as? JsObj ?: Whens.first()
                    val instruction = Telling.sortInstruction(today, Landing.examples(stored(Landing.EXAMPLES)))
                    // On a phone that keeps one conversation at a time, hers are let go while the pieces are placed.
                    if (!together) closeAll()
                    val pace = talked.pace?.let { p -> Pace(p.processor, p.whyNotGpu, p.loadMs).also { it.turns.addAll(p.turns) } }
                    fun ask(system: String?, message: String, s: com.phenomemental.pocketbeach.model.JobSettings): String = kotlinx.coroutines.runBlocking {
                        val a = try {
                            app.mind.ask(system, message, s)
                        } catch (e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException || kept.isEmpty()) throw e
                            android.util.Log.w("Asking", "A conversation of its own couldn't be opened beside hers; hers are let go while the day is sorted.", e)
                            together = false
                            closeAll()
                            app.mind.ask(system, message, s)
                        }
                        a.turn?.let { pace?.turns?.add(it) }
                        a.text
                    }
                    var placed = 0
                    val (pieces, steps) = Telling.sorted(
                        said, today, words, state,
                        judge = { piece -> ask(instruction, Telling.aloneAsk(piece, said), settings).also { sorting = ++placed to (sorting?.second ?: placed) } },
                        about = { piece, card -> Telling.yes(ask(null, Telling.cardAsk(piece, said, card), settings)) },
                    )
                    val rungs = Telling.placed(ask(null, Telling.rungsAsk(state, today, said), settings.copy(longestReply = maxOf(256, settings.longestReply))))
                        .filter { it.place in Telling.RUNGS }
                    Triple(Landing.sorting(said, pieces + rungs), steps.size, pace)
                }
            }
        }
        sorting = null
        val took = (System.nanoTime() - started) / 1_000_000
        outcome.onFailure { e ->
            if (stage is Stage.Thinking) end(Stage.Shown(talked.asked, took, talked.pace, talked.firstShown), null, null, "She talked. Your day couldn't be sorted: ${e.message ?: e::class.simpleName}.", took)
        }.onSuccess { (sorted, pieces, pace) ->
            if (stage !is Stage.Thinking) return@onSuccess
            stage = Stage.Day(Stage.Shown(talked.asked, took, pace, talked.firstShown), said, sorted, sorted.lines, pieces)
        }
    }

    /** Untick or tick the [i]th line of the day. */
    fun tick(i: Int) {
        val d = stage as? Stage.Day ?: return
        val l = d.lines.getOrNull(i) ?: return
        if (l.place == null) return
        stage = Stage.Day(d.shown, d.said, d.sorting, d.lines.toMutableList().also { it[i] = l.copy(ticked = !l.ticked) }, d.pieces)
    }

    /** Move the [i]th line of the day to [place], ticked: kept as a worked example once he says yes. */
    fun move(i: Int, place: String) {
        val d = stage as? Stage.Day ?: return
        val l = d.lines.getOrNull(i) ?: return
        stage = Stage.Day(d.shown, d.said, d.sorting, d.lines.toMutableList().also { it[i] = l.movedTo(place) }, d.pieces)
    }

    /** Matthew said yes to his day: each block whole, in order, each only if it still stands as planned; his moves kept as examples. */
    fun yesToDay() {
        val d = stage as? Stage.Day ?: return
        stage = Stage.DayWriting(d)
        scope.launch {
            val done = withContext(Dispatchers.IO) {
                runCatching {
                    val landed = Landing.landed(d.lines, d.said, System.currentTimeMillis(), ::stored)
                    val problems = mutableListOf<String>()
                    for (plan in Landing.plans(landed, app.pages)) {
                        val r = app.pages.carryOut(plan)
                        if (!r.ok) {
                            problems += "${plan.block}: ${r.problem ?: plan.refusal}"
                            break
                        }
                    }
                    landed to problems
                }
            }
            stale += MindJob.values()
            changed()
            val kept = d.lines.filter { it.ticked && it.place != null }
            val note = done.fold(
                onSuccess = { (landed, problems) ->
                    if (problems.isNotEmpty()) "Matthew said yes to his day, but it didn't all land: ${problems.joinToString("; ")}"
                    else "Matthew said yes to his day: ${kept.count { it.place in Telling.DAY }} lines in today on his clock, " +
                        "${kept.count { it.place in Telling.HAND }} on his hand, ${kept.count { it.place in Telling.RUNGS }} rungs" +
                        (if (landed.examples.isEmpty()) "." else "; ${Words.count(landed.examples.size, "move")} kept as ${if (landed.examples.size == 1) "an example" else "examples"} for her next time.") +
                        (if (landed.over.isEmpty()) "" else " ${Words.count(landed.over.size, "line")} didn't fit: a place keeps nine.")
                },
                onFailure = { "Matthew said yes to his day, but it couldn't be written: ${it.message ?: it::class.simpleName}." },
            )
            end(d.shown, null, null, note, d.shown.took, proposed = true)
        }
    }

    /** Matthew said not now to his day: nothing is written. */
    fun notNowToDay() {
        val d = stage as? Stage.Day ?: return
        end(d.shown, null, null, "Matthew said not now to his day.", d.shown.took, proposed = true)
    }

    /**
     * Sort the last thing he said as his day, though it didn't read as one:
     * on his tap, after the Ask is over.
     */
    fun sortAsDay() {
        val over = stage as? Stage.Over ?: return
        val talked = over.shown ?: return
        if (benchRunning) return
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch { day(asked, talked, System.nanoTime()) }
    }

    /** Her answer: talk ends the Ask with her words; a proposal is shown for Yes, or a quote is put to Matthew first. */
    private fun answered(a: Talks.Answered, chosen: Map<Int, String>, took: Long, pace: Pace?, firstShown: Long?, writeFailed: Throwable? = null) {
        val shown = Stage.Shown(asAsked(a), took, pace, firstShown)
        val planned = a.planned
        when {
            writeFailed != null -> end(shown, null, null, "She talked. The Write job couldn't answer: ${writeFailed.message ?: writeFailed::class.simpleName}.", took)
            a.route == Talks.Route.TALK -> {
                // What he told her names one of his blocks, and nothing was proposed: the app offers to keep it there.
                keepIn = Talks.keepIn(asked, app.pages)
                end(shown, null, null, "She talked.", took)
            }
            a.route == Talks.Route.READ -> end(shown, null, null, "She read ${a.found.size} block${if (a.found.size == 1) "" else "s"}, then answered.", took)
            a.problem != null -> end(shown, null, null, "Her proposal couldn't go ahead: ${a.problem}", took)
            planned?.choose != null -> stage = Stage.Choosing(a, chosen, took, pace)
            planned?.proposal?.asks != true -> end(shown, null, null, planned?.proposal?.refusal?.let { "It couldn't go ahead: $it" } ?: "Nothing to say yes to.", took)
            else -> stage = shown
        }
    }

    /** Matthew tapped "Keep this in …?": his words, as he said them, planned as a line added to that block, for Yes. */
    fun keep() {
        val block = keepIn ?: return
        val over = stage as? Stage.Over ?: return
        keepIn = null
        scope.launch {
            val actions = listOf(Acts.Action.Add(block, null, asked))
            val planned = withContext(Dispatchers.IO) {
                Acts.propose("", actions, app.storage, app.shelf, shellOpening(), HelloWorld.seal(), System.currentTimeMillis())
            }
            // Shown as her talk was timed: the offer is the app's, made in no time.
            val a = Talks.Answered(Talks.Route.ACT, "", emptyList(), planned, planned.problem, over.shown?.asked?.exchanges.orEmpty(), actions)
            val shown = Stage.Shown(asAsked(a), over.shown?.took ?: 0, over.shown?.pace, over.shown?.firstShown)
            when {
                planned.problem != null -> end(shown, null, null, "It couldn't be kept in $block: ${planned.problem}", shown.took)
                planned.proposal?.asks != true -> end(shown, null, null, planned.proposal?.refusal?.let { "It couldn't be kept in $block: $it" } ?: "Nothing to say yes to.", shown.took)
                else -> stage = shown
            }
        }
    }

    /** Matthew picked which line a quote means; the act is planned again with it. */
    fun choose(address: String) {
        val c = stage as? Stage.Choosing ?: return
        val choice = c.answered.planned?.choose ?: return
        val chosen = c.chosen + (choice.action to address)
        scope.launch {
            val planned = withContext(Dispatchers.IO) {
                Acts.propose(c.answered.said, c.answered.actions, app.storage, app.shelf, shellOpening(), c.answered.planned!!.proposal?.seal ?: HelloWorld.seal(), System.currentTimeMillis(), chosen, Talks.secretIn(asked))
            }
            answered(Talks.Answered(c.answered.route, c.answered.said, c.answered.found, planned, planned.problem, c.answered.exchanges, c.answered.actions), chosen, c.took, c.pace, null)
        }
    }

    /** Matthew won't say which line now; nothing is planned. */
    fun dropChoice() {
        val c = stage as? Stage.Choosing ?: return
        end(Stage.Shown(asAsked(c.answered), c.took, c.pace), null, null, "Matthew said not now to which line she meant.", c.took)
    }

    /** A job's exchange, kept as context for its next conversation; only the last six. */
    private fun remember(job: MindJob, message: String, reply: String) {
        if (reply.isBlank()) return
        val c = context.getValue(job)
        c.addLast(message to reply)
        while (c.size > 6) c.removeFirst()
    }

    /** 0.4's answer in the shape the proposal's pages show: her words, what she read, the writes and the post. */
    private fun asAsked(a: Talks.Answered): Asked {
        val p = a.planned?.proposal
        return Asked(a.exchanges, a.found, Answer(a.said, p?.answer?.steps.orEmpty(), p?.answer?.post), p, a.problem)
    }

    fun stop() {
        kept.values.forEach { it.talker.stop() }
        // A conversation stopped part way can't be asked again: the next ask opens it afresh.
        stale += MindJob.values()
        val writing = checking
        checking = false
        sorting = null
        job?.cancel()
        end(null, null, null, if (writing) "Stopped while the Write job was seeing whether there was anything to write." else "Stopped before the companion answered.", null)
    }

    fun notNow() {
        val s = stage as? Stage.Shown ?: return
        stage = Stage.Declining(s)
    }

    fun because(reason: String?) {
        val d = stage as? Stage.Declining ?: return
        end(d.shown, null, null, if (reason == null) "Matthew said not now." else "Matthew said not now: $reason.", d.shown.took)
    }

    fun back() {
        val d = stage as? Stage.Declining ?: return
        stage = d.shown
    }

    fun yes() {
        val s = stage as? Stage.Shown ?: return
        val proposal = s.asked.proposal ?: return
        stage = Stage.Carrying(s)
        scope.launch {
            val carried = withContext(Dispatchers.IO) { Asks.carryOut(proposal, app.pages, app.storage, shellOpening()) }
            // Her instructions show the pocket as it stood; the next ask opens the conversations afresh.
            if (carried.landed.isNotEmpty()) stale += MindJob.values()
            changed()
            val written = Stage.Written(s, carried)
            when {
                carried.problem != null -> end(s, carried, null, "Matthew said yes. ${carried.problem}", s.took)
                carried.post == null -> end(s, carried, null, "Matthew said yes; " + landed(carried) + (carried.postRefusal?.let { " The post couldn't be offered: $it" } ?: ""), s.took)
                else -> stage = written
            }
        }
    }

    fun post() {
        val w = stage as? Stage.Written ?: return
        val message = w.carried.post ?: return
        stage = Stage.Posting(w, message)
        scope.launch {
            val posted = withContext(Dispatchers.IO) { app.post(message) }
            val said = when (posted) {
                is Posted.Landed -> "He said yes to the post: it landed at entry ${posted.slot} of pool:${posted.pool}."
                is Posted.Refused -> "He said yes to the post, but it didn't go: ${posted.why}"
                is Posted.Unreached -> "He said yes to the post, but it didn't go: ${posted.why} It is kept, to be posted only when he taps Post now."
            }
            end(w.shown, w.carried, posted, "Matthew said yes; " + landed(w.carried) + " " + said, w.shown.took)
        }
    }

    fun dontPost() {
        val w = stage as? Stage.Written ?: return
        end(w.shown, w.carried, null, "Matthew said yes; " + landed(w.carried) + " He said not now to the post.", w.shown.took)
    }

    /** Forget the Ask in hand and the conversation without logging them, as the device tests begin. */
    fun reset() {
        job?.cancel()
        question = ""
        live = ""
        keepIn = null
        checking = false
        sorting = null
        stage = Stage.Writing
        history.clear()
        context.values.forEach { it.clear() }
        unseen.clear()
        told = null
        together = true
        val all = kept.values.map { it.talker }
        kept.clear()
        stale += MindJob.values()
        scope.launch(Dispatchers.IO) { all.forEach { runCatching { it.close() } } }
    }

    /**
     * The conversation so far as a file of plain text, to keep or send: each
     * exchange, when he said it, her reply, and how it ended and how long it
     * took; first, the settings her jobs ran on.
     */
    fun transcript(): String = buildString {
        val day = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.UK)
        val time = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.UK)
        fun at(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
        appendLine("Pocket beach: a conversation in Ask, ${day.format(at(history.firstOrNull()?.at ?: System.currentTimeMillis()))}")
        appendLine("Wearing ${HelloWorld.SHELL}: \"${shellOpening()}\"")
        for (j in MindJob.values()) appendLine("${j.title}: " + settingsLine(app.settings(j)))
        for (e in history) {
            appendLine()
            appendLine("${time.format(at(e.at))} Matthew: ${e.question}")
            if (e.said.isNotBlank()) appendLine("Companion: ${e.said}")
            appendLine("  (${e.note} ${e.time})")
        }
    }

    /** The name the conversation's file is offered under. */
    fun fileName(): String =
        "ask-" + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", java.util.Locale.UK).format(java.time.LocalDateTime.now()) + ".txt"

    private fun settingsLine(s: JobSettings): String =
        "${s.model}, ${if (s.processor == "GPU") "the GPU, else the CPU" else "the CPU"}, a context of ${s.context} tokens, " +
            (if (s.greedy) "the likeliest word each time" else "temperature %.2f, top-k %d, top-p %.2f".format(java.util.Locale.UK, s.temperature, s.topK, s.topP)) +
            ", replies of up to ${s.longestReply} tokens."

    /** Begin again, the last Ask's words kept to change or clear. */
    fun again() {
        if (stage is Stage.Over) stage = Stage.Writing
    }

    private fun landed(c: Carried): String =
        c.landed.groupBy { it.step.block }.entries.joinToString("; ") { (block, writes) ->
            val at = writes.mapNotNull { it.plan.landsAt?.takeIf { a -> a.isNotEmpty() } }
            if (at.isEmpty()) "$block was written" else "$block was written at ${at.joinToString(", ")}"
        }.ifEmpty { "nothing was written" } + "."

    private fun end(shown: Stage.Shown?, carried: Carried?, posted: Posted?, note: String, took: Long?, pace: Pace? = shown?.pace, proposed: Boolean = false) {
        stage = Stage.Over(shown, carried, posted, note)
        val exchanges = shown?.asked?.exchanges?.size ?: 0
        val time = took?.let {
            "Took %.1f s to answer".format(it / 1000.0) + (pace?.where?.takeIf(String::isNotEmpty)?.let { w -> ", $w" } ?: "") +
                (shown?.firstShown?.let { f -> ", first words after %.1f s".format(f / 1000.0) } ?: "") +
                ", in ${Words.count(exchanges, "exchange")}." + (pace?.detail()?.let { d -> " $d" } ?: "")
        } ?: "Stopped."
        val proposed = proposed || shown?.asked?.proposal != null || shown?.asked?.unasked != null
        history += Exchange(asked, shown?.asked?.answer?.say.orEmpty(), note, time, at = askedAt.takeIf { it > 0 } ?: System.currentTimeMillis(), outcome = if (proposed) note else null)
        if (proposed) told = note
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { Asks.log(app.pages, Asks.entry(asked, Asks.proposed(shown?.asked?.proposal, shown?.asked?.unasked), note, time)) } }
            changed()
        }
    }

    /** What the companion is shown of the shell it wears: the shell put on as the play door puts it on, offline. */
    object Companion {
        fun window(app: PocketApp): String = Shells(app.storage, app.shelf).wear(HelloWorld.SHELL, System.currentTimeMillis()).window
    }
}
