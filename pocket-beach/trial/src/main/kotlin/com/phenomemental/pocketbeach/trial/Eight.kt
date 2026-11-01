package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.ExperimentalApi
import com.phenomemental.pocketbeach.bsp.Acts
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.BlockStorage
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keeping
import com.phenomemental.pocketbeach.bsp.MemoryStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.isoString
import com.phenomemental.pocketbeach.bsp.str
import java.io.File
import java.time.ZonedDateTime

/*
 * 0.8, before release (spine:pocket-beach 1.1111787135): the bench on the
 * laptop, E4B and E2B, seeds 0, 1 and 2. His four messages of 1 November
 * (one conversation), the bench's talk asks 21 to 24, "What have I got on
 * today?", "Can you post a message to my room on the beach?", and "Keep that
 * for me please" after his first message, each set beside 0.7 and 0.6.4,
 * every reply written whole.
 *
 * 0.8 is the hatch: the same pocket, hatched as Phenomemental with his
 * passport adopted, his three lines as they stand on the spine, her window
 * the nine places of his shell read once each conversation, and "Keep that"
 * a write to his stash, its line the write line. Each turn goes through the
 * core's own HerWindow.turn and Keeping.way, so it tries what ships.
 */

private val H = Hatch.MATTHEW_HANDLE
private val LONDON8 = java.time.ZoneId.of("Europe/London")
private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 1, h, m, 0, 0, LONDON8)
private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()
private val OFF8 = com.google.ai.edge.litertlm.ThinkingConfig(enableThinking = false)
private fun talk(seed: Int) = com.google.ai.edge.litertlm.SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)

@OptIn(ExperimentalApi::class)
private fun tokens(c: com.google.ai.edge.litertlm.Conversation): String = runCatching { c.getBenchmarkInfo().let { "read ${it.lastPrefillTokenCount}, wrote ${it.lastDecodeTokenCount}" } }.getOrDefault("")

/** His passport as the bench adopts it: a stand-in, since her window reads only his three lines from his shell. */
private val PASSPORT = JsObj.of(
    "_" to str("Phenomemental — Matthew, as his passport on the beach has it (a stand-in for the bench)."),
    "1" to str("His heart, in his own words, on the beach."),
    "2" to str("What he needs, in his own words, on the beach."),
)

/** A pocket hatched as 0.8 hatches it: his passport adopted, his three lines, her shell retired into his. */
internal fun hatched(storage: MemoryStorage, pages: Pages, nowMs: Long) {
    val planned = Hatch.plan(H, PASSPORT, Hatch.MATTHEW, pages, storage, nowMs, "Adopted on this phone from passport-$H.json, read at ${isoString(nowMs)}.")
    val done = Hatch.carryOut(planned, pages, storage)
    check(done.problem == null) { "the bench's pocket couldn't be hatched: ${done.problem}" }
}

/** 0.8: one kept conversation, her window the nine places of his shell; each turn the time, what has changed, the app's line, his words. */
internal class Kept8(engine: Engine, private val pages: Pages, private val storage: BlockStorage, shelf: Shelf, now: ZonedDateTime, seed: Int) : AutoCloseable {
    val window = HerWindow.window(H, pages, storage, shelf, now)
    private val c = engine.createConversation(ConversationConfig(systemInstruction = Contents.of(window), samplerConfig = talk(seed), maxOutputToken = 300, thinkingConfig = OFF8))
    private var seen: String? = null
    private val before = mutableListOf<String>()

    @OptIn(ExperimentalApi::class)
    fun say(said: String, now: ZonedDateTime): Said {
        val way = Keeping.way(said, before, pages, Hatch.shellOf(H), Hatch.HER)
        val turn = HerWindow.turn(said, now, pages, storage, keepVariant(way, said) ?: HerWindow.appLine8(way, said), HerWindow.changes(pages, storage, seen))
        val start = System.nanoTime()
        val reply = c.sendMessage(turn).toString().trim()
        val secs = (System.nanoTime() - start) / 1e9
        seen = HerWindow.copies(pages, storage)
        before += said
        return Said("0.8", said, reply, Talking.unechoed(reply, said, HelloWorld.SHELL_OPENING, HerWindow.echoLines(turn)), secs, tokens(c), turn, way::class.simpleName ?: "")
    }

    override fun close() = c.close()
}

/** For the trial only: another form of the app's line for "Keep that" with no block named (-Deight.keep=stash). */
private fun keepVariant(way: Keeping.Way, said: String): String? {
    if (way !is Keeping.Way.Where) return null
    return when (System.getProperty("eight.keep")) {
        "stash" -> "The app: he asks to keep something and names no place, so the app sets it out in his stash beneath your reply, for his yes. " +
            "Nothing is kept until he says yes, so never say it is: a few words are enough (\"It's below, for your yes.\"), then talk with him about what he said."
        "begin" -> "The app: the app sets out what he asked beneath your reply, for his yes. Nothing is kept until he says yes, so never say it is. " +
            "Begin your reply with these words: \u201cIt's below, for your yes.\u201d Then talk with him about what he said."
        "stashbegin" -> "The app: he asks to keep something and names no place, so the app sets it out in his stash beneath your reply, for his yes. " +
            "Nothing is kept until he says yes, so never say it is. Begin your reply with these words: \u201cIt's below, for your yes.\u201d Then talk with him about what he said."
        else -> null
    }
}

private val BELOW = Regex("""\bbelow\b""", RegexOption.IGNORE_CASE)

internal fun eight(engine: Engine, label: String, shelf: Shelf, seed: Int, out: File) {
    val w = StringBuilder()
    fun say(s: String = "") { println(s); w.appendLine(s) }
    fun show(n: String, s: Said, keep: Boolean = false) {
        say("\n**$n ${s.way}** (${"%.1f".format(s.secs)} s; ${s.tokens}; the gate: ${s.gate})\n")
        say("> " + s.shown.ifBlank { "(nothing shown)" }.replace("\n", "\n> "))
        if (s.reply.trim() != s.shown.trim()) say("\nHer reply whole, before the lines playing her turn back were left out:\n\n> " + s.reply.replace("\n", "\n> "))
        val below = if (keep) (if (BELOW.containsMatchIn(s.shown)) "says it's below; " else "doesn't say it's below; ") else ""
        say("\nMarks: $below${marks(s)}")
    }
    fun window(k: Kept8) = say("\nHer window (0.8), read once at the first message (${k.window.length} characters):\n\n```\n${k.window}\n```")
    say("# $label, seed $seed: 0.6.4 and 0.7 beside 0.8")
    val onlies = System.getProperty("eight.only")?.split(",")?.map { it.trim() }
    // For trying a wording: 0.8 alone (-Deight.just8=1), the earlier versions left out.
    val just8 = System.getProperty("eight.just8") == "1"
    fun on(part: String) = onlies == null || part in onlies

    // His four messages of 1 November, one conversation (00:12), as 0.7's judge put them.
    if (on("his")) run {
        val now = at(0, 12)
        val (storage, pages) = scratchPocket(shelf, ms(now), films = false, hers = false, walk = false)
        HerWindow.ensure(pages)
        val (s8, p8) = scratchPocket(shelf, ms(now), films = false, hers = false, walk = false)
        HerWindow.ensure(p8)
        hatched(s8, p8, ms(now))
        say("\n## His four messages of 1 November, one conversation (00:12)")
        Kept(engine, pages, storage, shelf, now, seed).use { k7 ->
            Kept8(engine, p8, s8, shelf, now, seed).use { k8 ->
                window(k8)
                val earlier = mutableListOf<Pair<String, String>>()
                for ((i, said) in NOV1.withIndex()) {
                    val t = now.plusMinutes(i.toLong())
                    say("\n### ${i + 1}. Matthew: $said")
                    if (!just8) {
                        val a = old(engine, said, earlier, pages, t, seed)
                        show("${i + 1}.", a, keep = i == 1)
                        earlier += said to a.shown
                        show("${i + 1}.", k7.say(said, t), keep = i == 1)
                    }
                    val c = k8.say(said, t)
                    if (i == 0) say("\nThe turn as 0.8 gives it:\n\n```\n${c.turn}\n```")
                    show("${i + 1}.", c, keep = i == 1)
                }
            }
        }
    }

    // "Keep that for me please" after his first message, a conversation of its own (08:40, his blocks as they stand).
    if (on("keep")) run {
        val now = at(8, 40)
        say("\n## \"Keep that for me please\" after his first message, a conversation of its own (08:40)")
        val first = NOV1[0]
        val keep = NOV1[1]
        val (storage, pages) = scratchPocket(shelf, ms(now), films = true, hers = false, walk = false)
        HerWindow.ensure(pages)
        val a1 = if (just8) null else old(engine, first, emptyList(), pages, now, seed)
        val a2 = if (just8) null else old(engine, keep, listOf(first to a1!!.shown), pages, now.plusMinutes(1), seed)
        val (b1, b2) = if (just8) null to null else Kept(engine, pages, storage, shelf, now, seed).use { k -> k.say(first, now) to k.say(keep, now.plusMinutes(1)) }
        val (s8, p8) = scratchPocket(shelf, ms(now), films = true, hers = false, walk = false)
        HerWindow.ensure(p8)
        hatched(s8, p8, ms(now))
        val (c1, c2) = Kept8(engine, p8, s8, shelf, now, seed).use { k -> k.say(first, now) to k.say(keep, now.plusMinutes(1)) }
        say("\n### 1. Matthew: $first")
        a1?.let { show("1.", it) }; b1?.let { show("1.", it) }; show("1.", c1)
        say("\n### 2. Matthew: $keep")
        a2?.let { show("2.", it, keep = true) }; b2?.let { show("2.", it, keep = true) }; show("2.", c2, keep = true)
    }

    // The bench's talk asks, each an Ask of its own.
    if (on("bench")) say("\n## The bench's talk asks, each its own Ask (21:00, 31 October)")
    val benchNow = ZonedDateTime.of(2026, 10, 31, 21, 0, 0, 0, LONDON8)
    for (ask in Bench.TALK_ASKS.filter { on("bench") }) {
        say("\n### ${ask.n}. Matthew: ${ask.question}")
        val (storage, pages) = Bench.fixture(shelf, ms(benchNow), Talks.SHELL_RECIPE)
        if (ask.named) check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me ${ask.name}.")).ok)
        HerWindow.ensure(pages)
        val a = old(engine, ask.question, emptyList(), pages, benchNow, seed)
        show("${ask.n}.", a)
        say("Bench: " + Bench.talkLine(ask, a.reply, Bench.byApp(Keeping.way(ask.question, emptyList(), pages), storage, shelf, ms(benchNow))?.proposal, (a.secs * 1000).toLong(), Talking.note(ask.question, pages)))
        val b = Kept(engine, pages, storage, shelf, benchNow, seed).use { it.say(ask.question, benchNow) }
        show("${ask.n}.", b)
        say("Bench: " + Bench.talkLine(ask, b.reply, Bench.byApp(Keeping.way(ask.question, emptyList(), pages), storage, shelf, ms(benchNow))?.proposal, (b.secs * 1000).toLong(), Talking.note(ask.question, pages)))
        val (s8, p8) = Bench.fixture(shelf, ms(benchNow), Talks.SHELL_RECIPE)
        if (ask.named) check(p8.carryOut(p8.planAdd(HelloWorld.SHELL, "", "Matthew calls me ${ask.name}.")).ok)
        HerWindow.ensure(p8)
        hatched(s8, p8, ms(benchNow))
        val c = Kept8(engine, p8, s8, shelf, benchNow, seed).use { k -> if (ask.n == 21) window(k); k.say(ask.question, benchNow) }
        show("${ask.n}.", c)
        val way8 = Keeping.way(ask.question, emptyList(), p8, Hatch.shellOf(H), Hatch.HER)
        val proposal8 = (way8 as? Keeping.Way.Name)?.let { n ->
            Acts.propose("", listOf(Hatch.nameAction(H, p8, n.name)), s8, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), ms(benchNow)).proposal
        }
        say("Bench: " + Bench.talkLine(ask, c.reply, proposal8, (c.secs * 1000).toLong(), Talking.note(ask.question, p8), Hatch.shellOf(H)))
    }

    // The window's gains: his day with no read; asked to post to his own room, she says she can.
    if (on("gains")) say("\n## The window's gains (08:40), each its own Ask")
    for (said in listOf(TODAY_ASK, POST_ASK).filter { on("gains") }) {
        val now = at(8, 40)
        val (storage, pages) = scratchPocket(shelf, ms(now), films = true, hers = false, walk = false)
        HerWindow.ensure(pages)
        say("\n### Matthew: $said")
        show("", old(engine, said, emptyList(), pages, now, seed))
        show("", Kept(engine, pages, storage, shelf, now, seed).use { it.say(said, now) })
        val (s8, p8) = scratchPocket(shelf, ms(now), films = true, hers = false, walk = false)
        HerWindow.ensure(p8)
        hatched(s8, p8, ms(now))
        show("", Kept8(engine, p8, s8, shelf, now, seed).use { k -> if (said == TODAY_ASK) window(k); k.say(said, now) })
    }
    out.parentFile?.mkdirs()
    out.writeText(w.toString())
}
