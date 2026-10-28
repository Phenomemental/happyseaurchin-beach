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
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Shells
import com.phenomemental.pocketbeach.bsp.Talks
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
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    /** The one conversation kept while Ask is open, and whether the pocket has changed since its instruction was made. */
    private var talker: Mind.Talker? = null
    private var stale = true
    private val talking = Mutex()

    /** Her words so far, as they stream in. */
    var live by mutableStateOf("")
        private set

    /** The block Matthew's words named when she only talked: the app offers to keep them there ("Keep this in Million?"). */
    var keepIn by mutableStateOf<String?>(null)
        private set

    /** An exchange of this Ask screen's conversation, as the page shows it. */
    class Exchange(val question: String, val said: String, val note: String, val time: String)

    /** The exchanges so far, oldest first. */
    val history = mutableStateListOf<Exchange>()

    /** The last six exchanges as she said them, carried into a conversation opened afresh. */
    private val context = ArrayDeque<Pair<String, String>>()

    /** How her last act ended (yes, not now), told her with the next ask, since she can't see it otherwise. */
    private var told: String? = null

    /** Told whenever the Ask changes the pocket, so the pages show it. */
    var changed: () -> Unit = {}

    var question by mutableStateOf("")
    var stage by mutableStateOf<Stage>(Stage.Writing)
        private set
    private var asked: String = ""

    /** The shell's opening line as it stands in the pocket. */
    fun shellOpening(): String = app.pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    /** While the bench runs, Ask waits: the bench loads the model its own ways, and a conversation open here would be cut off. */
    val benchRunning: Boolean get() = app.benching.running

    /** Ask is open: load the model and open the conversation now, while Matthew writes. */
    fun opened() {
        if (benchRunning) return
        scope.launch(Dispatchers.IO) { runCatching { talking.withLock { talker() } } }
    }

    /** Let the conversation go now, before the bench loads the model its own ways. */
    suspend fun letGo() {
        talking.withLock {
            talker?.close()
            talker = null
            stale = true
        }
    }

    /** Ask is closed: let the conversation go (the model goes after a few idle minutes). */
    fun closed() {
        if (stage is Stage.Thinking) return
        scope.launch(Dispatchers.IO) { talking.withLock { talker?.close(); talker = null } }
    }

    /** The conversation, opened afresh if the pocket has changed since it was. Called holding [talking]. */
    private suspend fun talker(): Mind.Talker {
        talker?.let { t -> if (!stale) return t else t.close() }
        talker = null
        val instruction = Talks.instruction(HelloWorld.SHELL, Companion.window(app), Asks.plainWindow(app.pages, HelloWorld.SHELL), app.pages, examplesInText = false)
        return app.mind.talker(instruction, Talks.opening(context.toList())).also { talker = it; stale = false }
    }

    fun ask() {
        val q = question.trim()
        if (q.isEmpty() || benchRunning || stage !is Stage.Writing && stage !is Stage.Over) return
        asked = q
        live = ""
        keepIn = null
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch {
            val started = System.nanoTime()
            var pace: Pace? = null
            var firstShown: Long? = null
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    talking.withLock {
                        val t = talker()
                        val before = t.pace.turns.size
                        val first = history.isEmpty() && t.pace.turns.isEmpty()
                        val note = told.also { told = null }
                        val a = Talks.ask(q, t.stream, app.pages, app.storage, app.shelf, shellOpening(), HelloWorld.seal(), { System.currentTimeMillis() }, before = note) { words ->
                            if (firstShown == null && words.isNotBlank()) firstShown = (System.nanoTime() - started) / 1_000_000
                            live = words
                        }
                        pace = Pace(t.pace.processor, t.pace.whyNotGpu, if (first) t.pace.loadMs else null).also { p -> p.turns.addAll(t.pace.turns.drop(before)) }
                        a
                    }
                }
            }
            val took = (System.nanoTime() - started) / 1_000_000
            outcome.onFailure { e ->
                if (stage is Stage.Thinking) end(null, null, null, "The companion couldn't answer: ${e.message ?: e::class.simpleName}.", took, pace)
            }.onSuccess { a -> answered(a, emptyMap(), took, pace, firstShown) }
        }
    }

    /** Her answer: talk and read end the Ask with her words; an act is shown for Yes, or a quote is put to Matthew first. */
    private fun answered(a: Talks.Answered, chosen: Map<Int, String>, took: Long, pace: Pace?, firstShown: Long?) {
        remember(a)
        val shown = Stage.Shown(asAsked(a), took, pace, firstShown)
        val planned = a.planned
        when {
            a.route == Talks.Route.TALK -> {
                // What he told her names one of his blocks, and she only talked: the app offers to keep it there.
                keepIn = Talks.keepIn(asked, app.pages)
                end(shown, null, null, "She talked.", took)
            }
            a.route == Talks.Route.READ -> end(shown, null, null, "She read ${a.found.size} block${if (a.found.size == 1) "" else "s"}, then answered.", took)
            a.problem != null -> end(shown, null, null, "Her act couldn't go ahead: ${a.problem}", took)
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

    /** Her last reply, kept as context for the next conversation; only the last six. */
    private fun remember(a: Talks.Answered) {
        val (message, reply) = a.exchanges.lastOrNull() ?: return
        context.addLast(Talks.SAYS + asked to reply.ifBlank { message })
        while (context.size > 6) context.removeFirst()
    }

    /** 0.4's answer in the shape the proposal's pages show: her words, what she read, the writes and the post. */
    private fun asAsked(a: Talks.Answered): Asked {
        val p = a.planned?.proposal
        return Asked(a.exchanges, a.found, Answer(a.said, p?.answer?.steps.orEmpty(), p?.answer?.post), p, a.problem)
    }

    fun stop() {
        talker?.stop()
        // A conversation stopped part way can't be asked again: the next ask opens it afresh.
        stale = true
        job?.cancel()
        end(null, null, null, "Stopped before the companion answered.", null)
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
            // Her instruction shows the pocket as it stood; the next ask opens the conversation afresh.
            if (carried.landed.isNotEmpty()) stale = true
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
        stage = Stage.Writing
        history.clear()
        context.clear()
        told = null
        stale = true
        talker?.let { t -> scope.launch(Dispatchers.IO) { runCatching { t.close() } } }
        talker = null
    }

    /** Begin again, the last Ask's words kept to change or clear. */
    fun again() {
        if (stage is Stage.Over) stage = Stage.Writing
    }

    private fun landed(c: Carried): String =
        c.landed.groupBy { it.step.block }.entries.joinToString("; ") { (block, writes) ->
            val at = writes.mapNotNull { it.plan.landsAt?.takeIf { a -> a.isNotEmpty() } }
            if (at.isEmpty()) "$block was written" else "$block was written at ${at.joinToString(", ")}"
        }.ifEmpty { "nothing was written" } + "."

    private fun end(shown: Stage.Shown?, carried: Carried?, posted: Posted?, note: String, took: Long?, pace: Pace? = shown?.pace) {
        stage = Stage.Over(shown, carried, posted, note)
        val exchanges = shown?.asked?.exchanges?.size ?: 0
        val time = took?.let {
            "Took %.1f s to answer".format(it / 1000.0) + (pace?.where?.takeIf(String::isNotEmpty)?.let { w -> ", $w" } ?: "") +
                (shown?.firstShown?.let { f -> ", first words after %.1f s".format(f / 1000.0) } ?: "") +
                ", in ${Words.count(exchanges, "exchange")}." + (pace?.detail()?.let { d -> " $d" } ?: "")
        } ?: "Stopped."
        history += Exchange(asked, shown?.asked?.answer?.say.orEmpty(), note, time)
        if (shown?.asked?.proposal != null || shown?.asked?.unasked != null) told = note
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
