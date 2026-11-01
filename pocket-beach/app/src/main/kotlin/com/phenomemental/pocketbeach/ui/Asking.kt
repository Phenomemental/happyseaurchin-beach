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
import com.phenomemental.pocketbeach.bsp.Close
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.Js
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keeping
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.Whens
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
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
 * 0.6, Stage 4: his day, sorted by the Sort job a piece at a time, each
 * piece in a conversation of its own, and the day, the hand and the rungs
 * shown as one proposal ([Stage.Day]): any line unticked, moved or a misheard
 * word corrected before Yes, each move kept as a worked example she is shown
 * next time. 0.6.1: only when he asks ("Sort as my day", offered when his
 * words could be a day); her conversation is opened afresh for each message,
 * the date, the time and her shell in her instruction and his turn his words
 * alone; and a line of her reply that plays her turn back is never shown.
 *
 * 0.6.3, a plain chat: [stage] is the turn in hand only, his next line
 * being written or her reply (or his day) coming. Her reply shows and the
 * box is ready at once; the Write job runs after it, behind the chat, one
 * exchange at a time, and gives way when he says the next thing (it runs
 * again once she has answered). What waits on his answer (a proposal, which
 * line she meant, his day, a post) waits on its own exchange in the thread
 * ([Exchange.waiting]) while he carries on.
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
        /** The exchange is over and logged: [note] says how it ended. */
        class Over(val shown: Shown?, val carried: Carried?, val posted: Posted?, val note: String) : Stage
        /** His day, sorted: her reply, and every line of the landing, to untick or move before Yes. [pieces], how many she placed. */
        class Day(val shown: Shown, val said: String, val sorting: Landing.Sorting, val lines: List<Landing.Line>, val pieces: Int) : Stage
        class DayWriting(val day: Day) : Stage
        /** 0.6.4: he asked to keep [words] and named no block of his: where it goes is his to say, among [blocks] and his day. */
        class Where(val words: String, val blocks: List<String>) : Stage
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

    /** Whether a job's conversation is in use now: for the device tests to say, when a wait times out. */
    val lockedForTests: Boolean get() = talking.isLocked

    /** Her words so far, as they stream in. */
    var live by mutableStateOf("")
        private set

    /**
     * An exchange of the conversation, as the thread shows it: his words, her
     * reply, and what came of it. 0.6.3: what waits on his answer waits here
     * ([waiting]) while he carries on, and the Write job's look at his words
     * comes after her reply ([checking]).
     */
    class Exchange(val question: String, val at: Long = System.currentTimeMillis()) {
        /** Her reply, as shown: never a line playing her turn back. */
        var said by mutableStateOf("")
            internal set
        /** How it ended, once it has; empty while it hasn't. */
        var note by mutableStateOf("")
            internal set
        /** 0.6.3: the safety note shown beneath her reply when his safety words match, whatever she says; null otherwise. */
        var safety by mutableStateOf<String?>(null)
            internal set
        /** Her answer's time and chip, for Details and the log. */
        var time by mutableStateOf("")
            internal set
        /** What came of a proposal; null when there was none. */
        var outcome by mutableStateOf<String?>(null)
            internal set
        /** What waits on his answer beneath her reply: a proposal, which line she meant, his day, a post. */
        var waiting by mutableStateOf<Stage?>(null)
            internal set
        /** How it ended, with what was written and posted, once it has. */
        var over by mutableStateOf<Stage.Over?>(null)
            internal set
        /** The Write job is seeing whether his words ask for something to be kept, or waits to. */
        var checking by mutableStateOf(false)
            internal set
        /** His day being placed, a piece at a time: placed, of how many. */
        var sorting by mutableStateOf<Pair<Int, Int>?>(null)
            internal set
        /** His words could be a day ([Landing.isTelling]): "Sort as my day" is offered under her reply while it's the last thing said. */
        var sortable by mutableStateOf(false)
            internal set
        /** The block his words named when she only talked: the app offers to keep them there ("Keep this in Million?"). */
        var keepIn by mutableStateOf<String?>(null)
            internal set
        /** For the saved conversation: when "Sort as my day" was offered, and when he tapped it. */
        var sortOffered: Long? = null
            internal set
        var sortTapped: Long? = null
            internal set
        /** 0.6.1, for the saved conversation: which model and settings answered each job. */
        var jobs = ""
            internal set
        /** The proposal whole, as he saw it; null when there was none. */
        var proposal: String? = null
            internal set
        /** The lines of her reply left out, as her turn played back. */
        var copied: List<String> = emptyList()
            internal set
        /** How long her reply took, where it ran, when her first words showed; her answer as shown. */
        internal var took: Long? = null
        internal var pace: Pace? = null
        internal var firstShown: Long? = null
        internal var shown: Stage.Shown? = null
        /** 0.7: the app's line to her on this turn, for the saved conversation. */
        internal var appLine: String? = null
        /** Her turn as the engine had it: his words and hers. */
        internal var talk: List<Pair<String, String>> = emptyList()
        /** How his last proposal ended, when he said this: told her, and the Write job. */
        internal var told: String? = null
        /** He sorted it as his day: a Write proposal still to come is left, and one waiting is kept as left. */
        internal var dropped = false
        internal var writeLeft: String? = null
        internal val answeredBy = linkedSetOf<MindJob>()
        internal val jobMs = java.util.concurrent.ConcurrentHashMap<MindJob, Long>()
    }

    /** The Write job is on an exchange, or one waits for it. */
    val checking: Boolean get() = history.any { it.checking }

    /** His day being placed: pieces placed, of how many; null when it isn't. */
    val sorting: Pair<Int, Int>? get() = history.firstNotNullOfOrNull { it.sorting }

    /** "Sort as my day" is offered under her last reply. */
    val sortable: Boolean get() = history.lastOrNull()?.sortable == true

    /** The block his last words named, when she only talked: "Keep this in …?". */
    val keepIn: String? get() = history.lastOrNull()?.keepIn

    /** What waits on his answer, the newest. */
    val waiting: Stage? get() = history.lastOrNull { it.waiting != null }?.waiting

    /** Nothing in hand: her last reply is in, the Write job has looked, and it is over or waits on his answer. For the device tests. */
    val settled: Boolean get() = stage !is Stage.Thinking && history.none { it.checking } && history.lastOrNull()?.let { it.over != null || it.waiting != null } == true

    /** Her reply coming, or something he said yes to being written: the bench waits. */
    val busy: Boolean get() = stage is Stage.Thinking || history.any { it.waiting is Stage.Carrying || it.waiting is Stage.Posting || it.waiting is Stage.DayWriting }

    /**
     * 0.7: her conversation is kept while Ask is open, her window read once
     * (spine:pocket-beach 1.1111781). It is opened afresh on a new day, after
     * [TURNS] messages (so it never outgrows her 4,096 tokens), and whenever
     * the pocket changes by a write.
     */
    private var windowDay: java.time.LocalDate? = null
    private var talkTurns = 0

    /**
     * 0.8, the close (spine:pocket-beach 1.1111787133): what this conversation
     * was, for the one history entry the app appends when it closes (a new
     * day, six messages, a write, as 0.7 reopens it, or Ask left).
     */
    private class Conversation {
        val said = mutableListOf<String>()
        val read = mutableListOf("her window")
        val written = mutableListOf<String>()
    }
    private var conversation = Conversation()

    /** 0.8: the handle his shell on this phone is of, once he has hatched; null before. */
    fun hatched(): String? = runCatching { Hatch.hatched(app.storage) }.getOrNull()

    /**
     * 0.8: the conversation closes: one entry in his history, its opening a
     * plain concern line, what was read and written beneath, written by the
     * app. Nothing when nothing was said, or before the hatch.
     */
    private fun closeConversation() {
        val c = conversation
        conversation = Conversation()
        if (c.said.isEmpty()) return
        runCatching {
            Close.plan(Close.Talk(c.said.toList(), c.read.toList(), c.written.toList()), app.pages, app.storage)?.let { plan ->
                val done = app.pages.carryOut(plan)
                if (!done.ok) android.util.Log.w("Asking", "The close's history entry didn't land: ${done.problem}")
            }
        }.onFailure { android.util.Log.w("Asking", "The close's history entry couldn't be written.", it) }
    }

    /** 0.8: he has just hatched: both conversations are opened afresh, hers wearing his shell. */
    fun hatchedNow() {
        conversation = Conversation()
        stale += MindJob.values()
    }

    /** The app's copies of his blocks as she last saw them: what tells her what's changed since. The app's own file, never a block. */
    private val copies get() = java.io.File(app.filesDir, COPIES)

    /** The exchanges so far, oldest first. */
    val history = mutableStateListOf<Exchange>()

    /** Each job's last six exchanges as they went, carried into its conversation opened afresh. */
    private val context = MindJob.values().associateWith { ArrayDeque<Pair<String, String>>() }

    /** Her talk since the Write job was last asked: told it the next time it is, since it doesn't see turns it sat out. */
    private val unseen = mutableListOf<Exchange>()

    /** How her last act ended (yes, not now), told her with the next ask, since she can't see it otherwise. */
    private var told: String? = null

    /** Told whenever the Ask changes the pocket, so the pages show it. */
    var changed: () -> Unit = {}

    var question by mutableStateOf("")
    var stage by mutableStateOf<Stage>(Stage.Writing)
        private set

    /** The exchanges waiting for the Write job, oldest first; the one it is on; whether it gave way to her reply. */
    private val writes = ArrayDeque<Exchange>()
    private var writingNow: Exchange? = null
    private var yielded = false
    private var writer: Job? = null

    /** The shell's opening line as it stands in the pocket. */
    fun shellOpening(): String = app.pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    /** While the bench runs, Ask waits: the bench loads the model its own ways, and a conversation open here would be cut off. */
    val benchRunning: Boolean get() = app.benching.running

    /** How many times Ask has been opened: a close asked for before the latest opening lets nothing go. */
    @Volatile private var openings = 0

    /**
     * Ask is open: load the model, and open the Write job's conversation, now,
     * while Matthew writes. 0.6.1: hers is opened afresh for each message,
     * its instruction carrying the time then and the blocks his words name,
     * so only her model is loaded here.
     */
    fun opened() {
        openings++
        if (benchRunning) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                talking.withLock {
                    app.mind.warm(app.running(MindJob.TALK))
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
                // 0.8: Ask left: the conversation closes into his history.
                closeConversation()
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
        val settings = app.running(job)
        kept[job]?.let { k ->
            if (job !in stale && k.settings == settings) return k.talker
            runCatching { k.talker.close() }
            kept.remove(job)
        }
        if (!together) kept.keys.filter { it != job }.forEach { other -> kept.remove(other)?.let { runCatching { it.talker.close() } } }
        val open = suspend {
            when (job) {
                MindJob.SORT -> error("The Sort job asks a piece at a time, in conversations of its own.")
                // 0.6.1: the date, the time and her shell in her instruction, opened afresh for each message; the earlier exchanges his words and hers.
                // 0.6.2, part three: when his words say he isn't safe, the safety line is the turn's only line; how his last proposal ended waits.
                // 0.7: her window, read once an Ask; the day, the time, what's changed and the app's line ride in each turn.
                // 0.8: once he has hatched, her window is the nine places of his shell; the conversation before it closes into his history.
                MindJob.TALK -> {
                    closeConversation()
                    val now = ZonedDateTime.now()
                    windowDay = now.toLocalDate()
                    talkTurns = 0
                    val h = hatched()
                    val window = if (h != null) HerWindow.window(h, app.pages, app.storage, app.shelf, now)
                    else {
                        runCatching { HerWindow.ensure(app.pages) }
                        HerWindow.instruction(HelloWorld.SHELL, app.pages, app.storage, app.shelf, now)
                    }
                    app.mind.talker(window, context.getValue(job).toList(), settings = settings)
                }
                // 0.6.2: what the Write job needs, about 1,000 tokens; her name in its examples as her shell gives it.
                MindJob.WRITE -> app.mind.talker(
                    Talks.lean(HelloWorld.SHELL, app.pages),
                    Talks.opening(context.getValue(job).toList(), Talking.name(HelloWorld.SHELL, app.pages)), settings = settings,
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

    /**
     * He says the next thing: the box empties, her reply streams in, and the
     * box is ready again as soon as it has. What was offered under her last
     * reply lapses; a proposal waiting stays, for his answer; and the Write
     * job, if it is on something, gives way to her reply.
     */
    fun ask() {
        val q = question.trim()
        if (q.isEmpty() || benchRunning || stage is Stage.Thinking) return
        // 0.8: the welcome asks in the chat's own box; his answer is the hatch's, not hers.
        if (app.hatching.takesTheBox) {
            if (app.hatching.answer(q)) question = ""
            return
        }
        question = ""
        history.lastOrNull()?.let { it.sortable = false; it.keepIn = null }
        yieldWrite()
        val e = Exchange(q)
        e.safety = safetyNote(q)
        history += e
        live = ""
        e.answeredBy += MindJob.TALK
        e.told = told.also { told = null }
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch {
            val started = System.nanoTime()
            var pace: Pace? = null
            var firstShown: Long? = null
            // 0.7, the gate before her reply: what the app does with his words, told her in her turn.
            val earlier = history.takeWhile { it !== e }.map { it.question }
            val h = withContext(Dispatchers.IO) { hatched() }
            // 0.8: her name is a line at her place in his shell.
            val gate = withContext(Dispatchers.IO) { runCatching { if (h != null) Keeping.way(q, earlier, app.pages, Hatch.shellOf(h), Hatch.HER) else Keeping.way(q, earlier, app.pages) } }
            gate.exceptionOrNull()?.let { t -> failed("the gate", t) }
            val way = gate.getOrDefault(Keeping.Way.Talk)
            // Her reply, streamed as she writes it.
            val talked = withContext(Dispatchers.IO) {
                runCatching {
                    talking.withLock {
                        val now = ZonedDateTime.now()
                        // 0.7: her conversation is kept; opened afresh on a new day, or when it has run long.
                        if (windowDay != now.toLocalDate() || talkTurns >= TURNS) stale += MindJob.TALK
                        val t = talker(MindJob.TALK)
                        val before = t.pace.turns.size
                        val first = history.size == 1
                        // Her turn: the day and the time, what's changed since she last looked, how his last proposal ended, the app's line, his words.
                        val seen = runCatching { copies.takeIf { it.isFile }?.readText() }.getOrNull()
                        val changes = runCatching { HerWindow.changes(app.pages, app.storage, seen) }.getOrNull()
                        e.appLine = if (h != null) HerWindow.appLine8(way, q) else HerWindow.appLine(way, q)
                        conversation.said += q
                        runCatching { Talking.named(q, app.pages, all = true) }.getOrNull()?.let { names -> conversation.read += names.map { "his $it" } }
                        val message = HerWindow.turn(q, now, app.pages, app.storage, e.appLine!!, changes, e.told)
                        val echo = HerWindow.echoLines(message)
                        val opening = shellOpening()
                        val words = StringBuilder()
                        val reply = t.stream.say(message) { piece ->
                            words.append(piece)
                            // Never shown: a line of hers that plays her turn back.
                            val shown = Talking.unechoed(words.toString(), q, opening, echo)
                            if (firstShown == null && shown.isNotBlank()) firstShown = (System.nanoTime() - started) / 1_000_000
                            live = shown
                        }
                        pace = t.pace.copy(if (first) t.pace.loadMs else null).also { p ->
                            p.turns.addAll(t.pace.turns.drop(before))
                            app.cpuWhy(MindJob.TALK)?.takeIf { Pace.GPU_FAILED !in p.notes }?.let { p.notes += "Talk ran on the CPU: $it." }
                        }
                        // 0.6.2: the GPU failed her answer and that engine was closed: every conversation on it is opened again, on the CPU.
                        if (Pace.GPU_FAILED in t.pace.notes) stale += MindJob.values()
                        talkTurns++
                        // She has now seen his blocks as they stand.
                        runCatching { copies.writeText(HerWindow.copies(app.pages, app.storage)) }
                        e.copied = Talking.copied(reply, q, opening, echo)
                        // 0.6.1 holds in a kept conversation: her turn played back is never given her again, so it is opened afresh with what was shown.
                        if (e.copied.isNotEmpty()) stale += MindJob.TALK
                        e.jobMs[MindJob.TALK] = (System.nanoTime() - started) / 1_000_000
                        message to Talking.unechoed(reply, q, opening, echo)
                    }
                }
            }
            if (stage !is Stage.Thinking) return@launch
            e.took = (System.nanoTime() - started) / 1_000_000
            e.pace = pace
            e.firstShown = firstShown
            val (message, said) = talked.getOrElse { ex ->
                live = ""
                stage = Stage.Writing
                finish(e, null, "The companion couldn't answer: ${ex.message ?: ex::class.simpleName}.")
                runWrites()
                return@launch
            }
            e.said = said
            e.talk = listOf(message to said)
            e.time = timeLine(e, pace, e.talk.size)
            live = ""
            // What she said, never her turn played back, is what she is given again: his words and hers.
            remember(MindJob.TALK, Talking.said(q), said.ifBlank { "…" })
            // 0.6.1: his words could be a day: "Sort as my day" is offered under her reply. Nothing is sorted unless he asks.
            e.sortable = withContext(Dispatchers.IO) { runCatching { Landing.isTelling(q, Telling.cards(handNow()), Landing.names(app.pages)) }.getOrDefault(false) }
            if (e.sortable) e.sortOffered = System.currentTimeMillis()
            unseen += e
            // The box is ready. 0.6.4, the gate: the writing check runs only when his words ask to keep, change, make a block or post;
            // a name for her is one line on her shell, by code; asked to keep with no block of his named, the app asks him where.
            stage = Stage.Writing
            when (way) {
                is Keeping.Way.Write -> {
                    e.checking = true
                    writes.addLast(e)
                }
                is Keeping.Way.Name ->
                    if (h != null) byApp(e, listOf(Hatch.nameAction(h, app.pages, way.name)), "Her name couldn't be kept")
                    else byApp(e, listOf(Acts.Action.Add(HelloWorld.SHELL, null, way.line)), "Her name couldn't be kept")
                // 0.8: "Keep that" with no block named is a write to his stash, set out for his yes.
                is Keeping.Way.Where ->
                    if (h != null) byApp(e, listOf(Acts.Action.Add(Hatch.stashOf(h), null, way.words)), "It couldn't be kept in your stash")
                    else e.waiting = Stage.Where(way.words, hisBlocks())
                Keeping.Way.Talk -> answered(e, talkOnly(e), emptyMap())
            }
            runWrites()
        }
    }

    /** Her turn alone, as an answer: she talked. */
    private fun talkOnly(e: Exchange) = Talks.Answered(Talks.Route.TALK, e.said, emptyList(), null, null, e.talk)

    /**
     * The Write job, behind the chat: one exchange at a time, oldest first,
     * and never while her reply or his day is coming. Its proposal appears
     * beneath her reply when it's ready.
     */
    private fun runWrites() {
        if (stage is Stage.Thinking || writingNow != null || benchRunning) return
        val e = writes.removeFirstOrNull() ?: return
        writingNow = e
        yielded = false
        writer = scope.launch {
            val started = System.nanoTime()
            val since = unseen.takeWhile { it !== e }.takeLast(3)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    talking.withLock {
                        val w = talker(MindJob.WRITE)
                        val before = w.pace.turns.size
                        // 0.6.4: his words only; hers are never his to keep.
                        val sinceLast = since.joinToString(" ") { x -> "Matthew said: ${Words.quoted(x.question, 200)}." }
                        val a = Talks.ask(
                            e.question, w.stream, app.pages, app.storage, app.shelf, shellOpening(), HelloWorld.seal(), { System.currentTimeMillis() },
                            before = listOfNotNull(sinceLast.takeIf(String::isNotEmpty)?.let { "Since you were last asked: $it" }, e.told, Talks.forMessage(HelloWorld.SHELL, app.pages, e.question)).joinToString(" ").ifEmpty { null },
                            answerReads = false,
                            guard = guard(e),
                        )
                        e.pace?.turns?.addAll(w.pace.turns.drop(before))
                        if (Pace.GPU_FAILED in w.pace.notes) { e.pace?.gpuFailed(); stale += MindJob.values() }
                        app.cpuWhy(MindJob.WRITE)?.let { e.pace?.notes?.add("Write ran on the CPU: $it.") }
                        a
                    }
                }
            }
            writingNow = null
            when {
                // He sorted it as his day: what the Write job had to say is let go.
                e.dropped -> {}
                // It gave way to her reply: it runs again after.
                yielded -> writes.addFirst(e)
                else -> {
                    e.checking = false
                    e.answeredBy += MindJob.WRITE
                    e.jobMs[MindJob.WRITE] = (System.nanoTime() - started) / 1_000_000
                    // Her talk up to this exchange has now been told the Write job.
                    val i = unseen.indexOf(e)
                    if (i >= 0) repeat(i + 1) { unseen.removeAt(0) }
                    val a = result.getOrNull()?.let { w ->
                        remember(MindJob.WRITE, Talks.SAYS + e.question, w.exchanges.lastOrNull()?.second.orEmpty())
                        if (w.route == Talks.Route.ACT || w.planned?.nothing == true) Talks.Answered(w.route, e.said, w.found, w.planned, w.problem, e.talk + w.exchanges, w.actions) else null
                    } ?: talkOnly(e)
                    answered(e, a, emptyMap(), writeFailed = result.exceptionOrNull())
                }
            }
            runWrites()
        }
    }

    /** The Write job gives way: what it is on is stopped, and asked again once her reply is in. */
    private fun yieldWrite() {
        if (writingNow == null) return
        yielded = true
        kept[MindJob.WRITE]?.talker?.stop()
        // A conversation stopped part way can't be asked again: it is opened afresh.
        stale += MindJob.WRITE
    }

    /** His structure block as it stands: the six named, the hand and the rungs; his structure as first given when there is none yet. */
    private fun handNow(): JsObj = (app.storage.read(Landing.TELLING)?.let(::parseJson) as? JsObj) ?: Telling.shape()

    private fun stored(name: String): Js? = app.storage.read(name)?.let(::parseJson)

    /**
     * Sort [e]'s words as his day, after her reply: each piece placed by the
     * Sort job in a conversation of its own, its when by his time words, a
     * piece near a card put to her yes or no, then the rungs; the lines shown
     * as one proposal beneath her reply.
     */
    private suspend fun day(e: Exchange, talked: Stage.Shown, started: Long, said: String = e.question) {
        e.answeredBy += MindJob.SORT
        val names = withContext(Dispatchers.IO) { runCatching { Landing.names(app.pages) }.getOrDefault(setOf("gemma")) }
        e.sorting = 0 to Telling.cut(said, names).size
        val outcome = withContext(Dispatchers.IO) {
            runCatching {
                talking.withLock {
                    val settings = app.running(MindJob.SORT)
                    val today = java.time.LocalDate.now()
                    val state = handNow()
                    val words = stored(Whens.BLOCK) as? JsObj ?: Whens.first()
                    val instruction = Telling.sortInstruction(today, Landing.examples(stored(Landing.EXAMPLES)))
                    // On a phone that keeps one conversation at a time, hers are let go while the pieces are placed.
                    if (!together) closeAll()
                    val pace = talked.pace?.let { p -> p.copy().also { it.turns.addAll(p.turns) } }
                    app.cpuWhy(MindJob.SORT)?.let { pace?.notes?.add("Sort ran on the CPU: $it.") }
                    fun ask(system: String?, message: String, s: com.phenomemental.pocketbeach.model.JobSettings): String = kotlinx.coroutines.runBlocking {
                        val a = try {
                            app.mind.ask(system, message, s)
                        } catch (ex: Throwable) {
                            if (ex is kotlinx.coroutines.CancellationException || kept.isEmpty()) throw ex
                            android.util.Log.w("Asking", "A conversation of its own couldn't be opened beside hers; hers are let go while the day is sorted.", ex)
                            together = false
                            closeAll()
                            app.mind.ask(system, message, s)
                        }
                        a.turn?.let { pace?.turns?.add(it) }
                        if (a.gpuFailed) { pace?.gpuFailed(); stale += MindJob.values() }
                        a.text
                    }
                    var placed = 0
                    val (pieces, steps) = Telling.sorted(
                        said, today, words, state,
                        judge = { piece -> ask(instruction, Telling.aloneAsk(piece, said), settings).also { e.sorting = ++placed to (e.sorting?.second ?: placed) } },
                        about = { piece, card -> Telling.yes(ask(null, Telling.cardAsk(piece, said, card), settings)) },
                        names = names,
                    )
                    // A rung his words bear on may be given one of his pieces, or left; none is asked about otherwise.
                    val rungs = Telling.rungsFor(state, steps) { a -> ask(null, a, settings.copy(longestReply = maxOf(64, settings.longestReply))) }
                    Triple(Landing.sorting(said, pieces + rungs), steps.size, pace)
                }
            }
        }
        e.sorting = null
        if (stage !is Stage.Thinking) return
        val took = (System.nanoTime() - started) / 1_000_000
        e.jobMs[MindJob.SORT] = took
        stage = Stage.Writing
        outcome.onFailure { ex ->
            finish(e, talked, "She talked. Your day couldn't be sorted: ${ex.message ?: ex::class.simpleName}.")
        }.onSuccess { (sorted, pieces, pace) ->
            e.waiting = Stage.Day(Stage.Shown(talked.asked, took, pace, talked.firstShown), said, sorted, sorted.lines, pieces)
        }
        runWrites()
    }

    /** The exchange whose day waits on his answer, the newest. */
    private fun dayOf(): Exchange? = history.lastOrNull { it.waiting is Stage.Day }

    /** Untick or tick the [i]th line of the day. */
    fun tick(i: Int, e: Exchange? = dayOf()) {
        val d = e?.waiting as? Stage.Day ?: return
        val l = d.lines.getOrNull(i) ?: return
        if (l.place == null) return
        e.waiting = Stage.Day(d.shown, d.said, d.sorting, d.lines.toMutableList().also { it[i] = l.copy(ticked = !l.ticked) }, d.pieces)
    }

    /** He corrects a word the phone misheard in the [i]th line ("torque" to "talk"): his correction is his words. */
    fun correct(i: Int, words: String, e: Exchange? = dayOf()) {
        val d = e?.waiting as? Stage.Day ?: return
        val l = d.lines.getOrNull(i) ?: return
        e.waiting = Stage.Day(d.shown, d.said, d.sorting, d.lines.toMutableList().also { it[i] = l.correctedTo(words) }, d.pieces)
    }

    /** Move the [i]th line of the day to [place], ticked: kept as a worked example once he says yes. */
    fun move(i: Int, place: String, e: Exchange? = dayOf()) {
        val d = e?.waiting as? Stage.Day ?: return
        val l = d.lines.getOrNull(i) ?: return
        e.waiting = Stage.Day(d.shown, d.said, d.sorting, d.lines.toMutableList().also { it[i] = l.movedTo(place) }, d.pieces)
    }

    /** Matthew said yes to his day: each block whole, in order, each only if it still stands as planned; his moves kept as examples. */
    fun yesToDay(e: Exchange? = dayOf()) {
        val d = e?.waiting as? Stage.Day ?: return
        e.waiting = Stage.DayWriting(d)
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
            finish(e, d.shown, note, proposed = true, proposal = Landing.describe(d.lines, d.sorting.invented) + "\n  His answer: Yes")
        }
    }

    /** Matthew said not now to his day: nothing is written. */
    fun notNowToDay(e: Exchange? = dayOf()) {
        val d = e?.waiting as? Stage.Day ?: return
        finish(e, d.shown, "Matthew said not now to his day.", proposed = true, proposal = Landing.describe(d.lines, d.sorting.invented) + "\n  His answer: Not now")
    }

    /**
     * 0.6.1: sort what he said as his day, on his tap of "Sort as my day"
     * under her reply, and only then. A Write proposal waiting, or still to
     * come, is left as not taken, and the saved conversation says so.
     */
    fun sortAsDay(e: Exchange? = history.lastOrNull()) {
        if (e == null || benchRunning || !e.sortable || stage is Stage.Thinking) return
        e.sortable = false
        e.sortTapped = System.currentTimeMillis()
        if (e.checking) {
            e.checking = false
            e.dropped = true
            writes.remove(e)
            if (writingNow === e) { kept[MindJob.WRITE]?.talker?.stop(); stale += MindJob.WRITE }
        }
        (e.waiting as? Stage.Shown)?.let { s -> e.writeLeft = describe(s.asked.proposal) }
        if (e.waiting is Stage.Shown || e.waiting is Stage.Choosing || e.waiting is Stage.Declining) e.waiting = null
        val talked = e.shown ?: Stage.Shown(asAsked(talkOnly(e)), e.took ?: 0, e.pace, e.firstShown)
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch { day(e, talked, System.nanoTime()) }
    }

    /** Her answer, once the Write job has looked: talk ends the exchange with her words; a proposal waits beneath them for Yes, or a quote is put to Matthew first. */
    private fun answered(e: Exchange, a: Talks.Answered, chosen: Map<Int, String>, writeFailed: Throwable? = null) {
        val shown = Stage.Shown(asAsked(a), e.took ?: 0, e.pace, e.firstShown)
        e.shown = shown
        val planned = a.planned
        when {
            writeFailed != null -> finish(e, shown, "She talked. The Write job couldn't answer: ${writeFailed.message ?: writeFailed::class.simpleName}.")
            // 0.6.4: he asked, and the guards let nothing by: why, under her reply.
            a.route == Talks.Route.TALK && planned?.dropped?.isNotEmpty() == true -> finish(e, shown, "Nothing proposed: ${planned.dropped.joinToString("; ")}.", proposed = true)
            a.route == Talks.Route.TALK -> {
                // What he told her names one of his blocks, and nothing was proposed: the app offers to keep it there, while it's the last thing said.
                if (e === history.lastOrNull()) e.keepIn = Talks.keepIn(e.question, app.pages)
                finish(e, shown, "She talked.")
            }
            a.route == Talks.Route.READ -> finish(e, shown, "She read ${a.found.size} block${if (a.found.size == 1) "" else "s"}, then answered.")
            a.problem != null -> finish(e, shown, "Her proposal couldn't go ahead: ${a.problem}")
            planned?.choose != null -> e.waiting = Stage.Choosing(a, chosen, e.took ?: 0, e.pace)
            planned?.proposal?.asks != true -> finish(e, shown, planned?.proposal?.refusal?.let { "It couldn't go ahead: $it" } ?: "Nothing to say yes to.")
            else -> e.waiting = shown
        }
    }

    /** Matthew tapped "Keep this in …?": his words, as he said them, planned as a line added to that block, for Yes. */
    fun keep(e: Exchange? = history.lastOrNull()) {
        val block = e?.keepIn ?: return
        e.keepIn = null
        byApp(e, listOf(Acts.Action.Add(block, null, e.question)), "It couldn't be kept in $block")
    }

    /** 0.6.4: the guard on what the writing check proposes for [e]: his words in this conversation, up to and with this message. */
    private fun guard(e: Exchange) = Acts.Guard(history.takeWhile { it !== e }.map { it.question } + e.question, e.question)

    /** 0.6.4: his blocks, for where something goes: never the app's own, her shell, the hello world or a bench run. */
    private fun hisBlocks(): List<String> = runCatching {
        app.pages.shore().mine.map { it.name }.filter { !Keeping.own(it, app.pages) && it != HelloWorld.SHELL && it != HelloWorld.BLOCK && !it.startsWith("bench-") }
    }.getOrDefault(emptyList())

    /**
     * A proposal the app makes itself, from his words or a name he gives her
     * (0.6.4), planned on a copy, for his Yes. Shown as her talk was timed:
     * the app's offer is made in no time.
     */
    private fun byApp(e: Exchange, actions: List<Acts.Action>, couldnt: String) {
        scope.launch {
            val planned = withContext(Dispatchers.IO) {
                Acts.propose("", actions, app.storage, app.shelf, shellOpening(), HelloWorld.seal(), System.currentTimeMillis())
            }
            val a = Talks.Answered(Talks.Route.ACT, "", emptyList(), planned, planned.problem, e.talk, actions)
            val shown = Stage.Shown(asAsked(a), e.took ?: 0, e.pace, e.firstShown)
            when {
                planned.problem != null -> finish(e, shown, "$couldnt: ${planned.problem}")
                planned.proposal?.asks != true -> finish(e, shown, planned.proposal?.refusal?.let { "$couldnt: $it" } ?: "Nothing to say yes to.")
                else -> e.waiting = shown
            }
        }
    }

    /** 0.6.4: he picked [block] for what he asked to keep: his words, a line added to it, for Yes. */
    fun putIn(block: String, e: Exchange? = waitingOn<Stage.Where>()) {
        val w = e?.waiting as? Stage.Where ?: return
        e.waiting = null
        byApp(e, listOf(Acts.Action.Add(block, null, w.words)), "It couldn't be kept in $block")
    }

    /** 0.6.4: he picked his day: what he asked to keep is sorted as his day, as "Sort as my day" sorts it. */
    fun whereDay(e: Exchange? = waitingOn<Stage.Where>()) {
        val w = e?.waiting as? Stage.Where ?: return
        if (benchRunning || stage is Stage.Thinking) return
        e.waiting = null
        e.sortable = false
        if (e.sortOffered == null) e.sortOffered = System.currentTimeMillis()
        e.sortTapped = System.currentTimeMillis()
        val talked = e.shown ?: Stage.Shown(asAsked(talkOnly(e)), e.took ?: 0, e.pace, e.firstShown)
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch { day(e, talked, System.nanoTime(), w.words) }
    }

    /** 0.6.4: not now, to where it goes: nothing is kept. */
    fun notNowWhere(e: Exchange? = waitingOn<Stage.Where>()) {
        if (e?.waiting !is Stage.Where) return
        finish(e, Stage.Shown(asAsked(talkOnly(e)), e.took ?: 0, e.pace, e.firstShown), "Matthew said not now to where it goes.")
    }

    /** 0.6.4: the blocks his day's Yes will write, by name, before he taps it. */
    fun dayWrites(d: Stage.Day): List<String> = runCatching {
        Landing.writes(Landing.landed(d.lines, d.said, System.currentTimeMillis()) { stored(it) })
    }.getOrElse { t -> failed("the day's blocks", t); emptyList() }

    /** What last went wrong where the app reads his words for itself, for Details and the device tests; never silent. */
    var failure: String? = null
        private set

    private fun failed(where: String, t: Throwable) {
        android.util.Log.w("Asking", "$where failed", t)
        failure = "$where: $t" + (t.cause?.let { " (cause: $it)" } ?: "") + " at " + t.stackTrace.take(4).joinToString(" < ")
    }

    /** The exchange whose [waiting] is of [kind], the newest. */
    private inline fun <reified T : Stage> waitingOn(): Exchange? = history.lastOrNull { it.waiting is T }

    /** Matthew picked which line a quote means; the act is planned again with it. */
    fun choose(address: String, e: Exchange? = waitingOn<Stage.Choosing>()) {
        val c = e?.waiting as? Stage.Choosing ?: return
        val choice = c.answered.planned?.choose ?: return
        val chosen = c.chosen + (choice.action to address)
        scope.launch {
            val planned = withContext(Dispatchers.IO) {
                Acts.propose(c.answered.said, c.answered.actions, app.storage, app.shelf, shellOpening(), c.answered.planned!!.proposal?.seal ?: HelloWorld.seal(), System.currentTimeMillis(), chosen, Talks.secretIn(e.question), guard(e))
            }
            answered(e, Talks.Answered(c.answered.route, c.answered.said, c.answered.found, planned, planned.problem, c.answered.exchanges, c.answered.actions), chosen)
        }
    }

    /** Matthew won't say which line now; nothing is planned. */
    fun dropChoice(e: Exchange? = waitingOn<Stage.Choosing>()) {
        val c = e?.waiting as? Stage.Choosing ?: return
        finish(e, Stage.Shown(asAsked(c.answered), c.took, c.pace), "Matthew said not now to which line she meant.")
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

    /** Stop her reply, or the placing of his day; the Write job, if it was on something, asks again after. */
    fun stop() {
        if (stage !is Stage.Thinking) return
        kept.values.forEach { it.talker.stop() }
        // A conversation stopped part way can't be asked again: the next ask opens it afresh.
        stale += MindJob.values()
        if (writingNow != null) yielded = true
        job?.cancel()
        live = ""
        stage = Stage.Writing
        history.lastOrNull()?.let { e ->
            when {
                e.sorting != null -> { e.sorting = null; finish(e, e.shown, "Stopped while his day was being placed.") }
                e.over == null && e.waiting == null && e.said.isEmpty() -> finish(e, null, "Stopped before the companion answered.")
            }
        }
        runWrites()
    }

    fun notNow(e: Exchange? = waitingOn<Stage.Shown>()) {
        val s = e?.waiting as? Stage.Shown ?: return
        e.waiting = Stage.Declining(s)
    }

    fun because(reason: String?, e: Exchange? = waitingOn<Stage.Declining>()) {
        val d = e?.waiting as? Stage.Declining ?: return
        finish(e, d.shown, if (reason == null) "Matthew said not now." else "Matthew said not now: $reason.")
    }

    fun back(e: Exchange? = waitingOn<Stage.Declining>()) {
        val d = e?.waiting as? Stage.Declining ?: return
        e.waiting = d.shown
    }

    fun yes(e: Exchange? = waitingOn<Stage.Shown>()) {
        val s = e?.waiting as? Stage.Shown ?: return
        val proposal = s.asked.proposal ?: return
        e.waiting = Stage.Carrying(s)
        scope.launch {
            val carried = withContext(Dispatchers.IO) { Asks.carryOut(proposal, app.pages, app.storage, shellOpening()) }
            // Her instructions show the pocket as it stood; the next ask opens the conversations afresh.
            if (carried.landed.isNotEmpty()) stale += MindJob.values()
            for (w in carried.landed) conversation.written += "${w.plan.block}${w.plan.landsAt?.takeIf { it.isNotEmpty() }?.let { " at $it" }.orEmpty()}: \u201c${(w.text ?: w.plan.text).take(120)}\u201d"
            changed()
            when {
                carried.problem != null -> finish(e, s, "Matthew said yes. ${carried.problem}", carried)
                carried.post == null -> finish(e, s, "Matthew said yes; " + landed(carried) + (carried.postRefusal?.let { " The post couldn't be offered: $it" } ?: ""), carried)
                else -> e.waiting = Stage.Written(s, carried)
            }
        }
    }

    fun post(e: Exchange? = waitingOn<Stage.Written>()) {
        val w = e?.waiting as? Stage.Written ?: return
        val message = w.carried.post ?: return
        e.waiting = Stage.Posting(w, message)
        scope.launch {
            val posted = withContext(Dispatchers.IO) { app.post(message) }
            val said = when (posted) {
                is Posted.Landed -> "He said yes to the post: it landed at entry ${posted.slot} of pool:${posted.pool}."
                is Posted.Refused -> "He said yes to the post, but it didn't go: ${posted.why}"
                is Posted.Unreached -> "He said yes to the post, but it didn't go: ${posted.why} It is kept, to be posted only when he taps Post now."
            }
            finish(e, w.shown, "Matthew said yes; " + landed(w.carried) + " " + said, w.carried, posted)
        }
    }

    fun dontPost(e: Exchange? = waitingOn<Stage.Written>()) {
        val w = e?.waiting as? Stage.Written ?: return
        finish(e, w.shown, "Matthew said yes; " + landed(w.carried) + " He said not now to the post.", w.carried)
    }

    /** Forget the Ask in hand and the conversation without logging them, as the device tests begin. */
    fun reset() {
        job?.cancel()
        writer?.cancel()
        writes.clear()
        writingNow = null
        yielded = false
        question = ""
        live = ""
        stage = Stage.Writing
        history.clear()
        context.values.forEach { it.clear() }
        unseen.clear()
        told = null
        conversation = Conversation()
        together = true
        val all = kept.values.map { it.talker }
        kept.clear()
        stale += MindJob.values()
        scope.launch(Dispatchers.IO) { all.forEach { runCatching { it.close() } } }
    }

    /**
     * The conversation so far as a file of plain text, to keep or send: each
     * exchange, when he said it, her reply, and how it ended and how long it
     * took; first, the settings her jobs ran on. 0.6.3: when "Sort as my day"
     * was offered under her reply, and whether he tapped it; and what still
     * waits on his answer.
     */
    fun transcript(): String = buildString {
        val day = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.UK)
        val time = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.UK)
        fun at(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
        appendLine("Pocket beach: a conversation in Ask, ${day.format(at(history.firstOrNull()?.at ?: System.currentTimeMillis()))}")
        val h = hatched()
        if (h != null) appendLine("Wearing ${Hatch.shellOf(h)}: her window is its nine places.") else appendLine("Wearing ${HelloWorld.SHELL}: \"${shellOpening()}\"")
        for (j in MindJob.values()) appendLine("${j.title}: " + settingsLine(app.running(j)) + app.cpuWhy(j)?.let { " On the CPU: $it." }.orEmpty())
        for (e in history) {
            appendLine()
            appendLine("${time.format(at(e.at))} Matthew: ${e.question}")
            appendLine("Companion: ${e.said.ifBlank { "(nothing of hers shown)" }}")
            e.appLine?.let { appendLine("  The app's line to her: $it") }
            e.safety?.let { appendLine("  The safety note, shown beneath her reply: \"${it.replace("\n", " / ")}\"") }
            if (e.copied.isNotEmpty()) appendLine("  Left out of her reply, as her turn played back: " + e.copied.joinToString(" / "))
            if (e.jobs.isNotEmpty()) appendLine("  Answered by: ${e.jobs}")
            if (e.time.isNotEmpty()) appendLine("  Times: ${e.time}")
            e.sortOffered?.let { offered ->
                appendLine("  At ${time.format(at(offered))}, \"Sort as my day\" was offered under her reply; " +
                    (e.sortTapped?.let { "he tapped it at ${time.format(at(it))}." } ?: "he didn't tap it."))
            }
            e.proposal?.let { appendLine("  Proposed:"); appendLine(it.prependIndent("  ")) }
            e.waiting?.let { w -> appendLine("  Waiting on his answer: " + waitingLine(w)) }
            if (e.checking) appendLine("  The Write job hadn't looked at it yet.")
            if (e.note.isNotEmpty()) appendLine("  How it ended: ${e.note}")
        }
    }

    /** What waits on his answer, in a line for the saved conversation. */
    private fun waitingLine(w: Stage): String = when (w) {
        is Stage.Shown -> "a proposal.\n" + (describe(w.asked.proposal) ?: "")
        is Stage.Declining -> "why not now.\n" + (describe(w.shown.asked.proposal) ?: "")
        is Stage.Choosing -> "which line she meant."
        is Stage.Where -> "where it goes: \u201c${w.words}\u201d, among ${w.blocks.joinToString(", ").ifEmpty { "none of his blocks" }}, or his day."
        is Stage.Day -> "his day, sorted.\n" + Landing.describe(w.lines, w.sorting.invented)
        is Stage.Written -> "the post, after the writes: " + landed(w.carried)
        else -> "it was being carried out."
    }.trimEnd()

    /**
     * 0.6.3: the note shown beneath her reply when his words hold one of his
     * safety words; his own note when he keeps one. It never depends on her
     * words; should his pocket not be read, the first words and note stand.
     */
    private fun safetyNote(said: String): String? = try {
        Talking.note(said, app.pages)
    } catch (e: Exception) {
        if (Talking.unsafe(said, Talking.SAFETY_WORDS)) Talking.SAFETY_NOTE else null
    }

    /** The name the conversation's file is offered under. */
    fun fileName(): String =
        "ask-" + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", java.util.Locale.UK).format(java.time.LocalDateTime.now()) + ".txt"

    private fun settingsLine(s: JobSettings): String =
        "${s.model}, ${if (s.processor == "GPU") "the GPU, else the CPU" else "the CPU"}, a context of ${s.context} tokens, " +
            (if (s.greedy) "the likeliest word each time" else "temperature %.2f, top-k %d, top-p %.2f".format(java.util.Locale.UK, s.temperature, s.topK, s.topP)) +
            ", replies of up to ${s.longestReply} tokens."

    private fun landed(c: Carried): String =
        c.landed.groupBy { it.step.block }.entries.joinToString("; ") { (block, writes) ->
            // 0.6.3: where, in words; never by a line's number.
            val beneath = writes.mapNotNull { w -> w.standing?.takeIf { w.plan.kind == com.phenomemental.pocketbeach.bsp.Kind.ADD && w.plan.address.isNotEmpty() } }
            if (beneath.isEmpty()) "$block was written" else "$block was written, beneath " + beneath.joinToString(" and ") { Words.quoted(it) }
        }.ifEmpty { "nothing was written" } + "."

    /** A proposal's writes and post, a line each, as the saved conversation and the log keep them. */
    private fun describe(p: com.phenomemental.pocketbeach.bsp.Proposal?): String? = p?.let {
        it.writes.joinToString("\n") { w -> "  " + Words.landing(w.plan.kind, w.plan.block, w.plan.address, w.plan.landsAt, w.plan.wraps, w.standing) + (w.shown ?: w.plan.text).takeIf { t -> t.isNotBlank() }?.let { t -> " “$t”" }.orEmpty() } +
            (it.post?.let { post -> "\n  Then a post to pool:${post.pool}" }.orEmpty()) + (it.refusal?.let { r -> "\n  It couldn't go ahead: $r" }.orEmpty())
    }

    /** Her answer's time and chip: how long her reply took, where it ran, when her first words showed, and the engine's counts. */
    private fun timeLine(e: Exchange, pace: Pace?, exchanges: Int): String = e.took?.let {
        "Took %.1f s to answer".format(it / 1000.0) + (pace?.where?.takeIf(String::isNotEmpty)?.let { w -> ", $w" } ?: "") +
            (e.firstShown?.let { f -> ", first words after %.1f s".format(f / 1000.0) } ?: "") +
            ", in ${Words.count(exchanges, "exchange")}." + (pace?.detail()?.let { d -> " $d" } ?: "")
    } ?: "Stopped."

    /** [e] is over: how it ended, what was written and posted, its times; logged; how a proposal ended is told her with the next thing he says. */
    private fun finish(e: Exchange, shown: Stage.Shown?, note: String, carried: Carried? = null, posted: Posted? = null, proposed: Boolean = false, proposal: String? = null) {
        e.waiting = null
        e.over = Stage.Over(shown, carried, posted, note)
        e.note = note
        val time = timeLine(e, shown?.pace ?: e.pace, shown?.asked?.exchanges?.size ?: e.talk.size)
        e.time = time
        val proposedHere = proposed || shown?.asked?.proposal != null || shown?.asked?.unasked != null
        e.outcome = if (proposedHere) note else null
        e.jobs = e.answeredBy.joinToString("; ") { j -> "${j.title}" + (e.jobMs[j]?.let { " (%.1f s)".format(java.util.Locale.UK, it / 1000.0) }.orEmpty()) + ": " + settingsLine(app.running(j)) + app.cpuWhy(j)?.let { " On the CPU: $it." }.orEmpty() }
        // A day sorted on his tap: the Write job's proposal he left for it comes first.
        e.proposal = listOfNotNull(e.writeLeft?.let { "The Write job's proposal, left as he sorted it as his day:\n$it" }, proposal ?: describe(shown?.asked?.proposal)).joinToString("\n").ifEmpty { null }
        if (proposedHere) told = note
        scope.launch {
            // 0.6.4: the ask log only when he has turned it on.
            if (app.keepAskLog) withContext(Dispatchers.IO) { runCatching { Asks.log(app.pages, Asks.entry(e.question, Asks.proposed(shown?.asked?.proposal, shown?.asked?.unasked), note + (if (e.safety != null) " The safety note was shown beneath her reply." else ""), time)) } }
            changed()
        }
    }

    /** What the companion is shown of the shell it wears: the shell put on as the play door puts it on, offline. */
    companion object {
        /** 0.7: how many messages her kept conversation carries before it is opened afresh with the last six exchanges. */
        const val TURNS = 6

        /** The app's copies of his blocks as she last saw them, beside the pocket. */
        const val COPIES = "her-copies.json"
    }
}
