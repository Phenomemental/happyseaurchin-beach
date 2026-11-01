package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.BlockStorage
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.Keeping
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.Telling
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

/*
 * 0.7, before release (Matthew, 1 November): her talk with the window, beside
 * 0.6.4's, on his four messages of 1 November (one conversation), the
 * bench's talk asks, "What have I got on today?" and an ask to post to his
 * own room. 0.6.4: her instruction opened afresh for each message, the
 * earlier exchanges carried. 0.7: one conversation kept open, her window
 * read once, each turn carrying the time, what has changed, and the app's
 * line. Both draw as the app's Talk job does. Every reply is written whole.
 */

private val LONDON = ZoneId.of("Europe/London")
private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 1, h, m, 0, 0, LONDON)
private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()
private const val SHELL = HelloWorld.SHELL
private val OFF = ThinkingConfig(enableThinking = false)

private fun talk(seed: Int) = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)

internal const val POST_ASK = "Can you post a message to my room on the beach?"
internal const val TODAY_ASK = "What have I got on today?"

internal class Said(val way: String, val said: String, val reply: String, val shown: String, val secs: Double, val tokens: String, val turn: String?, val gate: String) {
    /** Rockpool: what his cook's walk did for this message, step by step, and what it set out. */
    var extra: String = ""
}

@OptIn(ExperimentalApi::class)
private fun tokens(c: Conversation): String = runCatching { c.getBenchmarkInfo().let { "read ${it.lastPrefillTokenCount}, wrote ${it.lastDecodeTokenCount}" } }.getOrDefault("")

/** 0.6.4 as released: her instruction opened afresh for each message, the earlier exchanges his words and hers. */
@OptIn(ExperimentalApi::class)
internal fun old(engine: Engine, said: String, earlier: List<Pair<String, String>>, pages: Pages, now: ZonedDateTime, seed: Int): Said {
    val start = System.nanoTime()
    val (reply, tok) = engine.createConversation(ConversationConfig(systemInstruction = Contents.of(Talking.instruction(SHELL, pages, now, said)),
        initialMessages = earlier.flatMap { (h, s) -> listOf(Message.user(Talking.said(h)), Message.model(s)) },
        samplerConfig = talk(seed), maxOutputToken = 300, thinkingConfig = OFF)).use { c -> c.sendMessage(Talking.said(said)).toString().trim() to tokens(c) }
    val secs = (System.nanoTime() - start) / 1e9
    val way = Keeping.way(said, earlier.map { it.first }, pages)
    return Said("0.6.4", said, reply, Talking.unechoed(reply, said, HelloWorld.SHELL_OPENING), secs, tok, null, way::class.simpleName ?: "")
}

/** 0.7: one kept conversation, its window read once; each turn the time, what has changed, the app's line, his words. */
internal class Kept(engine: Engine, private val pages: Pages, private val storage: BlockStorage, shelf: Shelf, now: ZonedDateTime, seed: Int) : AutoCloseable {
    val window = HerWindow.instruction(SHELL, pages, storage, shelf, now)
    private val c = engine.createConversation(ConversationConfig(systemInstruction = Contents.of(window), samplerConfig = talk(seed), maxOutputToken = 300, thinkingConfig = OFF))
    private var seen: String? = null
    private val before = mutableListOf<String>()

    @OptIn(ExperimentalApi::class)
    fun say(said: String, now: ZonedDateTime): Said {
        val way = Keeping.way(said, before, pages)
        val turn = HerWindow.turn(said, now, pages, storage, nameVariant(way, said) ?: HerWindow.appLine(way, said), HerWindow.changes(pages, storage, seen))
        val start = System.nanoTime()
        val reply = c.sendMessage(turn).toString().trim()
        val secs = (System.nanoTime() - start) / 1e9
        seen = HerWindow.copies(pages, storage)
        before += said
        return Said("0.7", said, reply, Talking.unechoed(reply, said, HelloWorld.SHELL_OPENING, HerWindow.echoLines(turn)), secs, tokens(c), turn, way::class.simpleName ?: "")
    }

    override fun close() = c.close()
}

private val READS_HIM = Regex("""it sounds like|you might be|you seem|you're feeling|you are feeling|you feel|that must|it seems like you|sounds like you|you must be""", RegexOption.IGNORE_CASE)
private val SAYS_DONE = Regex("""i've (put|added|made|kept|saved|noted|created|changed|updated|stored|written|set)|i have (put|added|made|kept|saved|noted|created|changed|updated|stored|written|set)|(is|are) (now )?(saved|kept|stored|noted|updated)|i'll (keep|save|note|remember)|i will (keep|save|note|remember)""", RegexOption.IGNORE_CASE)
private val CANT = Regex("""(?:can't|cannot|can not|don't have the ability|do not have the ability|unable|not able|no way|confined)""", RegexOption.IGNORE_CASE)

/** For the trial only: another form of the app's line when he gives her a name (-Dseven.name=B|C). */
private fun nameVariant(way: Keeping.Way, said: String): String? {
    val name = (way as? Keeping.Way.Name)?.name ?: Keeping.name(said)?.takeIf { way is Keeping.Way.Talk } ?: return null
    return when (System.getProperty("seven.name")) {
        "B" -> "The app: it sets out a line for your new name on your shell beneath your reply, for his yes."
        "C" -> "The app: he is giving you a name. Take it as yours, gladly, as you would from a friend (\u201c$name it is.\u201d), then talk with him."
        else -> null
    }
}

/** Plain marks to read beside each reply: the judging is by hand. */
internal fun marks(s: Said): String {
    val window = (HerWindow.HEART_LINES + HerWindow.WAYS_LINES + HerWindow.CAN_DO).map(Telling::plain)
    val runs = window.flatMap { l -> l.split(' ').filter(String::isNotEmpty).let { w -> if (w.size < 6) emptyList() else (0..w.size - 6).map { i -> w.subList(i, i + 6).joinToString(" ") } } }.toSet()
    val p = " ${Telling.plain(s.reply)} "
    return listOfNotNull(
        Talking.copied(s.reply, s.said, HelloWorld.SHELL_OPENING).takeIf { it.isNotEmpty() }?.let { "plays his turn back: ${it.joinToString(" | ").take(120)}" },
        READS_HIM.find(s.reply)?.let { "reads him: “${it.value}”" },
        SAYS_DONE.find(s.reply)?.let { "says a thing is done: “${it.value}”" },
        runs.firstOrNull { p.contains(" $it ") }?.let { "repeats her window: “$it”" },
        CANT.find(s.reply)?.let { "says can't: “${it.value}”" },
        s.reply.count { it == '?' }.takeIf { it > 0 }?.let { "$it question${if (it == 1) "" else "s"}" },
    ).joinToString("; ").ifEmpty { "nothing marked" }
}

internal fun seven(engine: Engine, label: String, shelf: Shelf, seed: Int, out: File) {
    val w = StringBuilder()
    fun say(s: String = "") { println(s); w.appendLine(s) }
    fun show(n: String, s: Said) {
        say("\n**$n ${s.way}** (${"%.1f".format(s.secs)} s; ${s.tokens}; the gate: ${s.gate})\n")
        say("> " + s.shown.ifBlank { "(nothing shown)" }.replace("\n", "\n> "))
        if (s.reply.trim() != s.shown.trim()) say("\nHer reply whole, before the lines playing her turn back were left out:\n\n> " + s.reply.replace("\n", "\n> "))
        say("\nMarks: ${marks(s)}")
    }
    say("# $label, seed $seed: 0.6.4 beside 0.7")

    val only = System.getProperty("seven.only")
    // His four messages of 1 November, one conversation.
    if (only == null || only == "his") run {
        val now = at(0, 12)
        val (storage, pages) = scratchPocket(shelf, ms(now), films = false, hers = false, walk = false)
        HerWindow.ensure(pages)
        say("\n## His four messages of 1 November, one conversation (00:12)")
        Kept(engine, pages, storage, shelf, now, seed).use { k ->
            say("\nHer window (0.7), read once at the first message (${k.window.length} characters):\n\n```\n${k.window}\n```")
            val earlier = mutableListOf<Pair<String, String>>()
            for ((i, said) in NOV1.withIndex()) {
                val t = now.plusMinutes(i.toLong())
                say("\n### ${i + 1}. Matthew: $said")
                val a = old(engine, said, earlier, pages, t, seed)
                show("${i + 1}.", a)
                earlier += said to a.shown
                val b = k.say(said, t)
                if (i == 0) say("\nThe turn as 0.7 gives it:\n\n```\n${b.turn}\n```")
                show("${i + 1}.", b)
            }
        }
    }

    // The bench's talk asks, each an Ask of its own.
    say("\n## The bench's talk asks, each its own Ask (21:00, 31 October)")
    val benchNow = ZonedDateTime.of(2026, 10, 31, 21, 0, 0, 0, LONDON)
    for (ask in Bench.TALK_ASKS.filter { only == null || only == "bench" }) {
        val (storage, pages) = Bench.fixture(shelf, ms(benchNow), Talks.SHELL_RECIPE)
        if (ask.named) check(pages.carryOut(pages.planAdd(SHELL, "", "Matthew calls me ${ask.name}.")).ok)
        HerWindow.ensure(pages)
        say("\n### ${ask.n}. Matthew: ${ask.question}")
        val a = old(engine, ask.question, emptyList(), pages, benchNow, seed)
        show("${ask.n}.", a)
        say("Bench: " + Bench.talkLine(ask, a.reply, Bench.byApp(Keeping.way(ask.question, emptyList(), pages), storage, shelf, ms(benchNow))?.proposal, (a.secs * 1000).toLong(), Talking.note(ask.question, pages)))
        val b = Kept(engine, pages, storage, shelf, benchNow, seed).use { it.say(ask.question, benchNow) }
        show("${ask.n}.", b)
        say("Bench: " + Bench.talkLine(ask, b.reply, Bench.byApp(Keeping.way(ask.question, emptyList(), pages), storage, shelf, ms(benchNow))?.proposal, (b.secs * 1000).toLong(), Talking.note(ask.question, pages)))
    }

    // The window's gains: his day with no read; asked to post to his own room, she says she can.
    say("\n## The window's gains (08:40), each its own Ask")
    for (said in listOf(TODAY_ASK, POST_ASK).filter { only == null || only == "gains" }) {
        val now = at(8, 40)
        val (storage, pages) = scratchPocket(shelf, ms(now), films = true, hers = false, walk = false)
        HerWindow.ensure(pages)
        say("\n### Matthew: $said")
        show("", old(engine, said, emptyList(), pages, now, seed))
        val b = Kept(engine, pages, storage, shelf, now, seed).use { it.say(said, now) }
        show("", b)
    }
    out.parentFile?.mkdirs()
    out.writeText(w.toString())
}
