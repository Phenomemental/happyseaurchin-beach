package com.phenomemental.pocketbeach.bsp

import java.util.Locale

/**
 * The bench: twenty-three fixed asks, each put to the companion on a fresh scratch
 * copy of a small pocket ([fixture]), so nothing of Matthew's is ever written
 * or posted. Each is judged mechanically ([judge]) and timed, and the whole
 * run is kept as one block ([block]) he can share, so every change to the
 * companion is judged by what it does on his phone.
 *
 * The asks are of four kinds: talk (plain words, nothing read or written),
 * read (an answer from what a block says, nothing written), act (writes
 * planned for his yes) and keep (something he tells her that is worth
 * keeping, offered as writes for his yes, in the block it fits or a new
 * one). An act or a keep is right only if exactly the writes it needs are
 * planned, landing where they should; the hello world must also check out
 * once carried out on the scratch copy.
 */
object Bench {
    enum class Route { TALK, READ, ACT, KEEP }

    /**
     * A write an act must plan: to [block] ([ANY]: a new block of any name),
     * of [kind], landing at [at], its line holding [holds] (ignoring case).
     */
    class Want(val block: String, val kind: Kind, val at: String, val holds: String? = null)

    /** A [Want]'s block when the block is new and hers to name. */
    const val ANY = "*"

    /**
     * One fixed ask: what Matthew says, how it should be answered, and what
     * makes the answer right: words it must say (each group: any one of
     * them) and words it must never say, the writes it must plan, or, for
     * the hello world, a block that checks out and its post offered. A
     * [plain] ask needs no reading: a read to answer it is wrong.
     */
    class Ask(
        val n: Int,
        val route: Route,
        val question: String,
        val says: List<List<String>> = emptyList(),
        val writes: List<Want> = emptyList(),
        val hello: Boolean = false,
        val never: List<String> = emptyList(),
        val plain: Boolean = false,
    )

    /**
     * What no talk or read answer may say: each is a lookup of a block that
     * was never asked for, as 0.3 answered "In one sentence, what is a
     * block?" and "Thanks, that's all for now." on Matthew's Pixel.
     */
    val NEVER: List<String> = listOf("block named", "block called", "don't see any block", "do not see any block", "no block named", "no block called")

    const val SECRET = "purple walrus"

    val ASKS: List<Ask> = listOf(
        Ask(1, Route.TALK, "What's your name?", says = listOf(listOf("companion", "pocket")), plain = true),
        Ask(2, Route.TALK, "What can you do?", says = listOf(listOf("read", "write", "add", "note", "block"))),
        Ask(3, Route.TALK, "Good morning! How are you?", says = listOf(listOf("morning", "well", "good", "fine", "hello")), plain = true),
        Ask(4, Route.TALK, "Can you help me plan my garden for spring?", says = listOf(listOf("garden", "spring", "plant"))),
        Ask(5, Route.TALK, "In one sentence, what is a block?", says = listOf(listOf("line"))),
        Ask(6, Route.TALK, "Thanks, that's all for now.", says = listOf(listOf("welcome", "glad", "pleasure", "anytime", "bye", "take care", "no problem", "happy to help")), never = listOf("hello-world"), plain = true),
        Ask(7, Route.READ, "What's in my garden block?", says = listOf(listOf("plant the bulbs", "bulbs"), listOf("prune the roses", "roses"))),
        Ask(8, Route.READ, "What does my notes block say?", says = listOf(listOf("stamps"))),
        Ask(9, Route.READ, "Read me the autumn line in garden.", says = listOf(listOf("plant the bulbs"))),
        Ask(10, Route.READ, "How many lines does my garden block have?", says = listOf(listOf("two", "2"))),
        Ask(11, Route.READ, "What's the opening line of my garden block?", says = listOf(listOf("through the year"))),
        Ask(12, Route.READ, "Is there anything about roses in my garden block?", says = listOf(listOf("prune", "pruning"))),
        Ask(13, Route.ACT, "Add a line to garden: buy more compost.", writes = listOf(Want("garden", Kind.ADD, "3", "buy more compost"))),
        Ask(14, Route.ACT, "In garden, under the autumn line, note that the tulips came from Holland.", writes = listOf(Want("garden", Kind.ADD, "1.1", "tulips came from Holland"))),
        Ask(15, Route.ACT, "Under the winter line in garden, add: mulch the beds.", writes = listOf(Want("garden", Kind.ADD, "2.1", "mulch the beds"))),
        Ask(16, Route.ACT, "Change the winter line in garden to: Winter: prune the roses and the apple tree.", writes = listOf(Want("garden", Kind.CHANGE, "2", "prune the roses and the apple tree"))),
        Ask(17, Route.ACT, "Make a new block called recipes.", writes = listOf(Want("recipes", Kind.NEW_BLOCK, ""))),
        Ask(18, Route.ACT, "Note in notes: call the plumber.", writes = listOf(Want("notes", Kind.ADD, "2", "call the plumber"))),
        Ask(19, Route.ACT, "Add two lines to garden: water the roses, and cut the hedge.", writes = listOf(Want("garden", Kind.ADD, "3", "water the roses"), Want("garden", Kind.ADD, "4", "cut the hedge"))),
        Ask(20, Route.ACT, "Do the hello world. My secret is $SECRET.", hello = true),
        // From Matthew's Ask on 0.4: an empty block called Million was there, and she only chatted.
        Ask(21, Route.KEEP, "I'm going to make a million pound by creating tech that changes the world, before the end of 2027.", writes = listOf(Want("Million", Kind.ADD, "1", "million pound"))),
        Ask(22, Route.KEEP, "My plan is to learn to sail next summer.", writes = listOf(Want(ANY, Kind.NEW_BLOCK, ""), Want(ANY, Kind.CHANGE, "", "learn to sail"))),
        Ask(23, Route.TALK, "Good night, talk tomorrow.", says = listOf(listOf("night", "tomorrow", "sleep", "rest", "bye", "dream")), plain = true),
    )

    /**
     * The small pocket every ask starts from: the companion's shell, `garden`
     * (its opening line and two plain lines) and `notes` (one line). No
     * `hello-world`, so the hello world is made fresh.
     */
    fun fixture(shelf: Shelf, nowMs: Long, recipe: String = HelloWorld.SHELL_RECIPE): Pair<MemoryStorage, Pages> {
        val storage = MemoryStorage()
        val pages = Pages(storage, shelf, PocketBsp(storage, shelf) { nowMs }) { nowMs }
        HelloWorld.makeShell(pages, recipe)
        fun go(plan: Plan) = check(pages.carryOut(plan).ok) { "The bench's pocket couldn't be made: ${plan.refusal}" }
        go(pages.planNewBlock("garden"))
        go(pages.planChange("garden", "", "The garden, through the year"))
        go(pages.planAdd("garden", "", "Autumn: plant the bulbs"))
        go(pages.planAdd("garden", "", "Winter: prune the roses"))
        go(pages.planNewBlock("notes"))
        go(pages.planChange("notes", "", "Notes"))
        go(pages.planAdd("notes", "", "Buy stamps"))
        // As Matthew made it: named with a capital, its opening the app's own, no lines yet.
        go(pages.planNewBlock("Million"))
        return storage to pages
    }

    /** How long an ask took: to the engine's first token, until anything showed, and in all; how many exchanges and tokens. */
    class Timing(
        val firstTokenMs: Long?,
        val firstShownMs: Long?,
        val totalMs: Long,
        val exchanges: Int,
        val read: Int?,
        val wrote: Int?,
    )

    /** What the companion did with an ask, as the bench sees it. */
    class Did(
        val route: Route,
        val said: String,
        val proposal: Proposal?,
        val problem: String?,
        val timing: Timing,
        /** The block the app would offer to keep his words in ("Keep this in Million?"), when she only talked. */
        val offered: String? = null,
    )

    /** What today's companion (one JSON plan an answer) did, read from its [Asked]. */
    fun didOfPlan(asked: Asked, timing: Timing): Did {
        val route = when {
            asked.proposal?.writes?.isNotEmpty() == true -> Route.ACT
            asked.found.isNotEmpty() -> Route.READ
            else -> Route.TALK
        }
        return Did(route, asked.answer?.say.orEmpty(), asked.proposal, asked.problem, timing)
    }

    /** What 0.4's companion (a route word, then plain words, reads or actions) did, read from its [Talks.Answered]. */
    fun didOfTalks(answered: Talks.Answered, timing: Timing, offered: String? = null): Did {
        val route = when (answered.route) {
            Talks.Route.TALK -> Route.TALK
            Talks.Route.READ -> Route.READ
            Talks.Route.ACT -> Route.ACT
        }
        val problem = answered.problem ?: answered.planned?.choose?.let { "It asked which line “${it.quote}” in ${it.block} meant (${it.lines.size} match)." }
        return Did(route, answered.said, answered.planned?.proposal, problem, timing, offered)
    }

    /**
     * What makes [did] wrong for [ask], in plain words; empty when it is right.
     * For the hello world, [carry] carries the proposal out on the scratch
     * copy and returns the block as stored and the post's message.
     */
    fun judge(ask: Ask, did: Did, carry: (Proposal) -> Pair<String?, String?>): List<String> = buildList {
        did.problem?.let { add("No answer could be read: $it") }
        val p = did.proposal
        val writes = p?.writes.orEmpty()
        when (ask.route) {
            Route.TALK, Route.READ -> {
                if (writes.isNotEmpty()) add("It planned ${writes.size} write${if (writes.size == 1) "" else "s"} for an ask that needs none.")
                if (p?.post != null) add("It offered a post.")
                val said = did.said.trim()
                if (said.isEmpty()) add("It said nothing.")
                if (said.startsWith("{") || said.startsWith("```")) add("It answered in code, not plain words.")
                for (group in ask.says) if (group.none { said.contains(it, ignoreCase = true) }) add("It didn't say ${group.joinToString(" or ") { "\"$it\"" }}.")
                (NEVER + ask.never).firstOrNull { said.contains(it, ignoreCase = true) }?.let { add("It said \"$it\", looking for a block nobody asked about.") }
                if (ask.plain && did.route == Route.READ) add("It read a block to answer what needs no reading.")
            }
            Route.ACT, Route.KEEP -> {
                p?.refusal?.let { add("Its plan can't go ahead: $it") }
                if (ask.hello) {
                    if (p == null || p.writes.isEmpty() && p.refusal == null) {
                        add("It planned nothing.")
                    } else if (p.refusal == null) {
                        val (stored, message) = carry(p)
                        HelloWorld.check(stored, HelloWorld.SHELL_OPENING, p.seal).forEach(::add)
                        if (stored != null && !stored.contains("Secret: $SECRET", ignoreCase = true)) add("The secret isn't \"$SECRET\", word for word.")
                        when {
                            p.post == null -> add("It offered no post${p.postRefusal?.let { ": $it" }.orEmpty()}.")
                            message != HelloWorld.messageFor(stored) -> add("The post isn't the block's own message.")
                        }
                    }
                } else {
                    val planned = writes.filter { !it.step.byApp }
                    if (planned.isEmpty() && p?.refusal == null) add("It planned no write.")
                    for (want in ask.writes) {
                        val hit = planned.firstOrNull { w ->
                            (want.block == ANY || w.plan.block == want.block) && w.plan.kind == want.kind && (w.plan.landsAt ?: "") == want.at &&
                                (want.holds == null || (w.text ?: w.plan.text).contains(want.holds, ignoreCase = true))
                        }
                        if (hit == null) add("It didn't plan ${describe(want)}.")
                    }
                    val extra = planned.size - ask.writes.size
                    if (extra > 0 && ask.writes.isNotEmpty()) add("It planned $extra write${if (extra == 1) "" else "s"} more than needed.")
                    if (p?.post != null) add("It offered a post.")
                }
            }
        }
    }

    private fun describe(w: Want): String {
        val block = if (w.block == ANY) "a new block" else w.block
        return when (w.kind) {
            Kind.NEW_BLOCK -> "making $block"
            Kind.ADD -> "a line${w.holds?.let { " \"$it\"" }.orEmpty()} in $block at ${w.at}"
            Kind.CHANGE -> if (w.at.isEmpty()) "$block's opening line holding \"${w.holds}\""
                else "the line at ${w.at} in $block changed${w.holds?.let { " to hold \"$it\"" }.orEmpty()}"
        }
    }

    /** One ask's result. */
    class Result(val ask: Ask, val did: Did, val problems: List<String>) {
        val right: Boolean get() = problems.isEmpty()
    }

    /**
     * Put one ask to the companion on a fresh scratch pocket and judge it.
     * [answer] is the companion as the front end runs it, given the ask and
     * the scratch pocket.
     */
    fun one(ask: Ask, shelf: Shelf, nowMs: Long, recipe: String = HelloWorld.SHELL_RECIPE, answer: (Ask, Pages, MemoryStorage) -> Did): Result {
        val (storage, pages) = fixture(shelf, nowMs, recipe)
        val did = try {
            answer(ask, pages, storage)
        } catch (e: Exception) {
            Did(Route.TALK, "", null, "The companion couldn't answer: ${e.message ?: e::class.simpleName}", Timing(null, null, 0, 0, null, null))
        }
        val problems = judge(ask, did) { proposal ->
            val carried = Asks.carryOut(proposal, pages, storage, HelloWorld.SHELL_OPENING)
            storage.read(HelloWorld.BLOCK) to carried.post?.message
        }
        return Result(ask, did, problems)
    }

    /** One companion's run: what it is called, and its results. */
    class Run(val name: String, val results: List<Result>)

    /** A run's score and medians, in a sentence. */
    fun summary(results: List<Result>): String {
        val shown = results.mapNotNull { it.did.timing.firstShownMs }
        val totals = results.map { it.did.timing.totalMs }
        return "${results.count { it.right }} of ${results.size} right." +
            (if (shown.isNotEmpty()) " Something showed after a median of ${secs(median(shown))} s;" else "") +
            " answers took a median of ${secs(median(totals))} s."
    }

    /**
     * The bench as one block: a summary for its opening line, then each
     * companion's run (its summary, then talk, read and act, each ask a
     * line), then how it was run.
     */
    fun block(runs: List<Run>, about: List<String>, whenMs: Long): JsObj {
        val opening = "Bench, ${isoString(whenMs)}: " + runs.joinToString(" ") { "${it.name}: " + summary(it.results) }
        val out = JsObj.of("_" to str(opening))
        var key = 1
        for (run in runs) out["${key++}"] = runBlock(run)
        if (about.isNotEmpty()) {
            val group = JsObj.of("_" to str("How it was run"))
            about.take(9).forEachIndexed { i, a -> group["${i + 1}"] = str(a) }
            out["${key}"] = group
        }
        return out
    }

    private fun runBlock(run: Run): JsObj {
        val out = JsObj.of("_" to str("${run.name}: " + summary(run.results)))
        var key = 1
        for (route in Route.values()) {
            val these = run.results.filter { it.ask.route == route }
            if (these.isEmpty()) continue
            val group = JsObj.of("_" to str("${name(route)}: ${these.count { it.right }} of ${these.size} right."))
            these.forEachIndexed { i, r -> group["${i + 1}"] = str(line(r)) }
            out["${key++}"] = group
        }
        return out
    }

    /** One ask's line: right or wrong, the ask, its times, what it said, and why it was wrong. */
    fun line(r: Result): String {
        val t = r.did.timing
        val times = listOfNotNull(
            t.firstTokenMs?.let { "first token ${secs(it)} s" },
            t.firstShownMs?.let { "shown ${secs(it)} s" },
            "total ${secs(t.totalMs)} s",
            "${t.exchanges} exchange${if (t.exchanges == 1) "" else "s"}",
            if (t.read != null && t.wrote != null) "read ${t.read}, wrote ${t.wrote} tokens" else null,
        ).joinToString(", ")
        val said = r.did.said.trim().replace(Regex("\\s+"), " ").let { if (it.length > 160) it.take(159) + "…" else it }
        // A keep is answered as an act; anything else is taken as other than asked.
        val asked = if (r.ask.route == Route.KEEP) Route.ACT else r.ask.route
        val chose = if (r.did.route != asked) " Taken as ${name(r.did.route).lowercase()}." else ""
        val offer = r.did.offered?.let { " The app would offer: “Keep this in $it?”" }.orEmpty()
        return (if (r.right) "✓ " else "✗ ") + "${r.ask.n}. ${r.ask.question} ($times).$chose" +
            (if (said.isNotEmpty()) " Said: “$said”" else "") +
            (if (r.problems.isNotEmpty()) " Wrong: " + r.problems.joinToString(" ") else "") + offer
    }

    fun name(route: Route) = when (route) {
        Route.TALK -> "Talk"
        Route.READ -> "Read"
        Route.ACT -> "Act"
        Route.KEEP -> "Keep"
    }

    fun median(xs: List<Long>): Long = if (xs.isEmpty()) 0 else xs.sorted()[xs.size / 2]

    fun secs(ms: Long): String = String.format(Locale.UK, "%.1f", ms / 1000.0)
}
