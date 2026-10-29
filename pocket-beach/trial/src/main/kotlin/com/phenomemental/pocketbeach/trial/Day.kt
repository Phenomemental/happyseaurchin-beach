package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Telling
import java.io.File

/**
 * 0.5's item 7 on the laptop: one telling, three landings. Each telling is put
 * to the pocket (the block, today's date and what he said; the pocket's check;
 * the hand's cards landed for the second telling) and, beside it, to the same
 * Gemma with no pocket (his names as plain text, then the paragraph; the
 * second telling in the same conversation). Once as the phone asks (the
 * likeliest word each time), then five times with Gemma's own sampling, each
 * with its seed. Her answers are kept in [out].
 */
@OptIn(ExperimentalApi::class)
internal fun tellings(engine: Engine, tellings: List<Pair<String, String>>, out: File, runs: Int = 5) {
    out.mkdirs()
    val ways = listOf("as the phone asks" to null) + (1..runs).map { "sampled, seed $it" to it }
    for ((name, said) in tellings) {
        for (pocket in listOf(true, false)) {
            val who = if (pocket) "pocket" else "plain"
            println("\n== $name, $who ==")
            for ((way, seed) in ways) {
                val sampler = if (seed == null) SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0)
                else SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)
                val config = ConversationConfig(
                    systemInstruction = if (pocket) Contents.of(Telling.INSTRUCTION) else null,
                    samplerConfig = sampler,
                    maxOutputToken = Asks.Tuning.MAX_OUTPUT,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                )
                val tag = "${name.replace(' ', '-')}-$who-${seed ?: 0}"
                engine.createConversation(config).use { c ->
                    val first = send(c, if (pocket) Telling.ask(Telling.shape(), Telling.BENCH_DATE, said) else Telling.plainAsk(said))
                    File(out, "$tag-1.txt").writeText(first.text)
                    val placed = if (pocket) Telling.placed(first.text) else Telling.fromPlain(first.text)
                    val marks = Telling.marks(said, placed)
                    // The second telling: the pocket's to the block with the hand landed; plain Gemma's in the same conversation.
                    val after: JsObj
                    val second: Reply
                    if (pocket) {
                        val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
                        second = engine.createConversation(config).use { c2 -> send(c2, Telling.ask(state, Telling.BENCH_DATE, Telling.SECOND)) }
                        after = Telling.land(state, Telling.check(Telling.SECOND, Telling.placed(second.text)).kept)
                    } else {
                        second = send(c, Telling.SECOND)
                        val hand = Telling.fromPlain(second.text).filter { it.place in Telling.HAND }.ifEmpty { placed.filter { it.place in Telling.HAND } }
                        after = Telling.land(Telling.shape(), hand)
                    }
                    File(out, "$tag-2.txt").writeText(second.text)
                    val all = marks + Telling.secondMarks(after)
                    println("$way: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines; ${first.timing}; then ${second.timing}")
                    for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(400)}")
                    if (pocket) println("  rungs: " + placed.filter { it.place in Telling.RUNGS }.joinToString(" | ") { "${it.place} ${it.words}" })
                }
            }
        }
    }
}

/**
 * The shape for when the one-pass fill drops pieces or misses the doubles:
 * the phone cuts the telling into pieces, copying and never changing a word,
 * and she labels each piece with its places, one short choice at a time, in
 * one conversation; then the rungs, once. The second telling likewise, to the
 * block with the hand landed.
 */
@OptIn(ExperimentalApi::class)
internal fun pieces(engine: Engine, tellings: List<Pair<String, String>>, out: File, runs: Int = 5) {
    out.mkdirs()
    val ways = listOf("as the phone asks" to null) + (1..runs).map { "sampled, seed $it" to it }
    for ((name, said) in tellings) {
        println("\n== $name, a piece at a time ==")
        for ((way, seed) in ways) {
            val sampler = if (seed == null) SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0)
            else SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)
            fun label(state: JsObj, telling: String, log: StringBuilder): Pair<List<Telling.Placed>, String> {
                val config = ConversationConfig(
                    systemInstruction = Contents.of(Telling.piecesInstruction(state, Telling.BENCH_DAY, telling)),
                    samplerConfig = sampler,
                    maxOutputToken = Telling.PIECE_TOKENS,
                    enableResponseFormat = true,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                )
                val start = System.nanoTime()
                return engine.createConversation(config).use { c ->
                    val firsts = mutableListOf<String>()
                    val labelled = Telling.cut(telling).map { piece ->
                        val r = send(c, Telling.pieceAsk(piece), Telling.PIECE_FORM)
                        firsts += r.timing.substringBefore(",").removePrefix("first token ")
                        log.append("\u201c$piece\u201d -> ${r.text.trim()}\n")
                        piece to Telling.places(r.text)
                    }
                    val rungs = engine.createConversation(ConversationConfig(samplerConfig = sampler, maxOutputToken = 256, thinkingConfig = ThinkingConfig(enableThinking = false))).use { r ->
                        send(r, Telling.rungsAsk(state, Telling.BENCH_DAY, telling))
                    }
                    log.append("rungs -> ${rungs.text.trim()}\n")
                    Telling.fromPieces(telling, labelled, rungs.text) to "${labelled.size} pieces, ${"%.1f".format((System.nanoTime() - start) / 1e9)} s in all, first tokens ${firsts.take(3).joinToString(", ")}, …"
                }
            }
            val log = StringBuilder()
            val (placed, timing) = label(Telling.shape(), said, log)
            val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
            log.append("-- second --\n")
            val (placed2, timing2) = label(state, Telling.SECOND, log)
            val after = Telling.land(state, Telling.check(Telling.SECOND, placed2).kept)
            File(out, "${name.replace(' ', '-')}-pieces-${seed ?: 0}.txt").writeText(log.toString())
            val all = Telling.marks(said, placed) + Telling.secondMarks(after)
            println("$way: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines; $timing; then $timing2")
            for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(400)}")
            println("  rungs: " + placed.filter { it.place in Telling.RUNGS }.joinToString(" | ") { "${it.place} ${it.words}" })
            println("  hand after both: " + Telling.cards(after).entries.joinToString(" | ") { "${it.key} ${it.value}" })
        }
    }
}

/** The one-pass marks again, from her answers kept in [dir] by [tellings]: the same reading, without the model. */
internal fun remark(tellings: List<Pair<String, String>>, dir: File) {
    for ((name, said) in tellings) for (pocket in listOf(true, false)) {
        val who = if (pocket) "pocket" else "plain"
        println("\n== $name, $who, marked again ==")
        for (seed in 0..9) {
            val tag = "${name.replace(' ', '-')}-$who-$seed"
            val first = File(dir, "$tag-1.txt").takeIf { it.exists() }?.readText() ?: continue
            val second = File(dir, "$tag-2.txt").readText()
            val placed = if (pocket) Telling.placed(first) else Telling.fromPlain(first)
            val after = if (pocket) {
                val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
                Telling.land(state, Telling.check(Telling.SECOND, Telling.placed(second)).kept)
            } else {
                Telling.land(Telling.shape(), Telling.fromPlain(second).filter { it.place in Telling.HAND }.ifEmpty { placed.filter { it.place in Telling.HAND } })
            }
            val all = Telling.marks(said, placed) + Telling.secondMarks(after)
            println("${if (seed == 0) "as the phone asks" else "sampled, seed $seed"}: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines")
            for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(300)}")
        }
    }
}

internal class Reply(val text: String, val timing: String)

@OptIn(ExperimentalApi::class)
private fun send(c: Conversation, message: String, form: String? = null): Reply {
    val start = System.nanoTime()
    var first: Long? = null
    val whole = StringBuilder()
    kotlinx.coroutines.runBlocking {
        (if (form != null) c.sendMessageAsync(message, responseFormat = com.google.ai.edge.litertlm.ResponseFormat.regex(form)) else c.sendMessageAsync(message)).collect { m ->
            if (first == null) first = (System.nanoTime() - start) / 1_000_000
            whole.append(m.toString())
        }
    }
    val b = c.getBenchmarkInfo()
    return Reply(whole.toString(), "first token ${first?.let { "%.1f".format(it / 1000.0) }} s, ${"%.1f".format((System.nanoTime() - start) / 1e9)} s in all, read ${b.lastPrefillTokenCount}, wrote ${b.lastDecodeTokenCount}")
}
