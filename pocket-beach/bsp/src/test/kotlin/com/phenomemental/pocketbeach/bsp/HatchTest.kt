package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 0.8, the hatch (spine:pocket-beach 1.111178713): his shell on the phone
 * as the beach's four templates with his handle filled, born or adopted;
 * his blocks kept and listed in the manifest by meaning; her shell retired
 * into his; the app's own blocks in a hidden region; her window as nine
 * places of his shell, his three lines exactly as they stand on the spine;
 * "Keep that" a write to his stash; and the close, one history entry.
 */
class HatchTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val london = ZoneId.of("Europe/London")
    private val now = ZonedDateTime.of(2026, 11, 1, 8, 40, 0, 0, london)
    private val nowMs = now.toInstant().toEpochMilli()
    private val h = Hatch.MATTHEW_HANDLE

    private val passport = JsObj.of(
        "_" to str("Matthew Bushell — digital social innovator, aspiring writer."),
        "1" to str("The heart. bi-polar · one axis, two ends."),
        "5" to str("How an AI works with me: in conversation."),
    )

    /** A 0.7 pocket: his blocks, her shell named, her heart and ways, the app's own blocks; the hidden region shut. */
    private fun pocket(): Triple<RoutedStorage, Pages, MemoryStorage> {
        val (mem, _) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        val hidden = MemoryStorage()
        val routed = RoutedStorage(mem, hidden, Hidden::routes, openNow = false)
        val pages = Pages(routed, shelf, PocketBsp(routed, shelf) { nowMs }) { nowMs }
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        HerWindow.ensure(pages)
        check(pages.carryOut(pages.planNewBlock(Asks.LOG)).ok)
        return Triple(routed, pages, hidden)
    }

    private fun hatch(storage: BlockStorage, pages: Pages, three: Hatch.Three = Hatch.MATTHEW): Hatch.Hatched {
        val planned = Hatch.plan(h, passport, three, pages, storage, nowMs, "Adopted on this phone from passport-$h.json, read at ${isoString(nowMs)}.")
        assertTrue(planned.ok, planned.refusal)
        return Hatch.carryOut(planned, pages, storage)
    }

    private fun block(storage: BlockStorage, name: String): JsObj = parseJson(storage.read(name)!!) as JsObj

    @Test
    fun theTemplatesAreTheBeachsOwnByteForByte() {
        val dir = File("../conformance/upstream/pscale-beach/seeds/templates")
        if (!dir.isDirectory) return // fetched by conformance/fetch-upstream.sh; CI fetches it
        for ((n, t) in listOf("passport" to Templates.PASSPORT, "shell" to Templates.SHELL, "history" to Templates.HISTORY, "stash" to Templates.STASH))
            assertEquals(File(dir, "$n.template.json").readText(), t, n)
    }

    @Test
    fun theFourAreTheTemplatesWithHisHandleFilled() {
        val (storage, pages, _) = pocket()
        val done = hatch(storage, pages)
        assertNull(done.problem)
        assertEquals(Hatch.fourOf(h), done.written)
        assertEquals(h, Hatch.hatched(storage))
        assertEquals(stringify(passport), stringify(block(storage, "passport:$h")), "adopted as it stands")
        val history = block(storage, "history:$h")
        assertTrue(collectUnderscore(history)!!.startsWith("Phenomemental's history at the pocket on this phone — the automatic lossless record"), collectUnderscore(history))
        assertTrue(collectUnderscore(history)!!.contains("Initialised at ${isoString(nowMs)}."))
        assertEquals(setOf("_"), history.keys().toSet())
        assertTrue(collectUnderscore(block(storage, "stash:$h"))!!.startsWith("Phenomemental's stash at the pocket on this phone — deliberate notes kept by choice"))
        val shell = block(storage, "shell:$h")
        assertTrue(collectUnderscore(shell)!!.startsWith("Phenomemental's shell at the pocket on this phone."))
        val manifest = shell["3"] as JsObj
        assertEquals(str("passport:$h"), manifest["1"])
        assertEquals(str("shell:$h"), manifest["2"])
        assertTrue((manifest["3"] as JsStr).value.startsWith("history:$h — automatic"))
        assertTrue((manifest["4"] as JsStr).value.startsWith("stash:$h — deliberate"))
        assertFalse(stringify(shell)!!.contains("{{"), "every placeholder filled")
        assertEquals(str("v1 — initialised at ${isoString(nowMs)}"), (shell["9"] as JsObj)["1"])
        assertTrue((((shell["9"] as JsObj)["2"]) as JsStr).value.startsWith("Adopted on this phone"))
    }

    @Test
    fun hisBlocksAreAllKeptAndListedByMeaning() {
        val (storage, pages, _) = pocket()
        val before = storage.names().filter { !Hidden.routes(it) }.sorted()
        hatch(storage, pages)
        val manifest = block(storage, "shell:$h")["3"] as JsObj
        val listed = (5..9).flatMap { k -> (manifest["$k"] as? JsObj)?.let { g -> digitKeys(g).map { (g[it] as JsStr).value } }.orEmpty() }
        assertEquals(before, listed.sorted(), "every block of his, once, nested beneath the four")
        if (Landing.NOW in before) assertEquals(str(Landing.NOW), (manifest["5"] as JsObj)["1"])
        assertEquals(str("His own blocks."), (manifest["7"] as JsObj)["_"])
        for (name in before) assertNotNull(storage.read(name), "$name kept")
    }

    @Test
    fun herShellRetiresIntoHisAndTheAppsOwnBlocksAreHidden() {
        val (storage, pages, hidden) = pocket()
        val done = hatch(storage, pages)
        val her = block(storage, "shell:$h")[Hatch.HER] as JsObj
        assertEquals(str("Matthew calls me Gemma."), her["1"], "her name, carried from her shell")
        assertEquals(str(Hatch.WAY), her["2"])
        assertEquals("Gemma", HerWindow.herName(h, pages))
        for (n in listOf(HelloWorld.SHELL, HerWindow.HEART, HerWindow.WAYS, Asks.LOG)) {
            assertTrue(n in done.hidden, "$n moved")
            assertNotNull(hidden.read(n), "$n in the hidden region")
            assertNotNull(storage.read(n), "$n still read by name")
        }
        val shore = pages.shore()
        assertTrue(shore.mine.none { Hidden.routes(it.name) }, "never in his map")
        assertTrue(shore.hidden.map { it.name }.containsAll(listOf(HelloWorld.SHELL, Asks.LOG)))
        assertTrue(HerWindow.his(pages).none { it.startsWith("shell:") || it.startsWith("passport:") || it.startsWith("history:") || Hidden.routes(it) })
    }

    @Test
    fun aSecondHatchAndATakenNameAreRefused() {
        val (storage, pages, _) = pocket()
        hatch(storage, pages)
        assertTrue(Hatch.plan("Another", passport, Hatch.MATTHEW, pages, storage, nowMs, "").refusal!!.contains("already holds a shell"))
        val (s2, p2, _) = pocket()
        check(p2.carryOut(p2.planNewBlock("stash:$h")).ok)
        assertTrue(Hatch.plan(h, passport, Hatch.MATTHEW, p2, s2, nowMs, "").refusal!!.contains("stash:$h already stands"))
        assertNotNull(Hatch.handleRefusal("two words"))
        assertNull(Hatch.handleRefusal("Phenomemental"))
    }

    @Test
    fun aPassportBornAtTheWelcomeIsTheTemplateWithHisWords() {
        val p = Hatch.bornPassport("ana", "Ana — a gardener in Leeds.", nowMs)
        assertEquals("ana — Ana — a gardener in Leeds.", collectUnderscore(p))
        assertTrue(collectUnderscore(p["1"])!!.startsWith("Offers — what ana provides at the pocket on this phone."))
        assertEquals("Ana", Hatch.hisName("Ana — a gardener in Leeds."))
        assertEquals("Matthew", Hatch.hisName(Hatch.MATTHEW.who))
    }

    @Test
    fun aPassportFromAFileIsReadAsItStands() {
        val keyed = Hatch.readPassport(stringify(JsObj.of("passport:$h" to passport))!!)
        assertEquals(h, keyed.handle)
        assertEquals(stringify(passport), stringify(keyed.block))
        assertEquals("Matthew Bushell — digital social innovator, aspiring writer.", keyed.places.first().second)
        assertEquals(h, Hatch.readPassport(stringify(passport)!!, "passport-$h.json").handle)
        assertNull(Hatch.readPassport(stringify(passport)!!, "mine.json").handle)
        assertTrue(runCatching { Hatch.readPassport("not json") }.exceptionOrNull() is Copies.Unreadable)
        assertTrue(runCatching { Hatch.readPassport("""{"_word": "x"}""") }.exceptionOrNull() is Copies.Unreadable)
    }

    @Test
    fun herWindowIsNinePlacesHisThreeLinesExactlyAsTheyStand() {
        val (storage, pages, _) = pocket()
        hatch(storage, pages)
        val w = HerWindow.window(h, pages, storage, shelf, now)
        val lines = w.lines()
        assertEquals("Matthew calls you Gemma: that is your name, just Gemma. You wear his shell on his phone for this conversation; it is his, and these are its places.", lines[0])
        assertEquals("1. His heart: “One axis with two ends, and nowhere outside it to be sent.”", lines[1])
        assertEquals("2. Who he is: “Matthew — philosopher, peer and digital tinkerer, working at the edge of what community could become. Underneath it all: managing the environment so my gifts, and other people's, can flow.”", lines[2])
        assertEquals("3. How he'd be met: “I think in conversation, so speak to me in your own words, and come as yourself. Don't send me a volley of questions; bring your best reading and let me correct it.”", lines[3])
        assertEquals("8. ${Hatch.WAY}", lines[lines.size - 2])
        assertEquals("9. ${HerWindow.CAN_DO}", lines.last())
        for (never in listOf("garden", "shell:", "passport:", "stash:", "pocket-companion", "{{")) assertFalse(w.contains(never), "never “$never” in her window")
    }

    @Test
    fun herWindowCarriesHisDayHandPeopleAndWhatHeKeeps() {
        val (storage, pages, _) = pocket()
        var clock: JsObj = Landing.emptyClock()
        var node: JsObj = clock
        for (d in Landing.dayAddress(nowMs).take(8)) {
            val key = if (d == '0') "_" else "$d"
            node = (node[key] as? JsObj) ?: JsObj().also { node[key] = it }
        }
        node["_"] = JsStr("")
        node["4"] = JsStr("Ring Jo about the music for the pilot")
        check(pages.carryOut(pages.planWhole(Landing.NOW, clock)).ok)
        check(pages.carryOut(pages.planNewBlock("people")).ok)
        check(pages.carryOut(pages.planAdd("people", "", "David: happyseaurchin on the beach")).ok)
        hatch(storage, pages)
        assertEquals("people", (block(storage, "shell:$h")[Hatch.WINDOW] as JsObj)["6"]?.let { (it as JsStr).value })
        check(pages.carryOut(pages.planAdd("stash:$h", "", "I've been making huge progress with my film making.")).ok)
        val w = HerWindow.window(h, pages, storage, shelf, now)
        assertTrue(w.contains("\n4. Today on his clock: morning, “Ring Jo about the music for the pilot”."), w)
        assertTrue(w.contains("\n6. His people: “David: happyseaurchin on the beach”."), w)
        assertTrue(w.contains("\n7. What he keeps: “I've been making huge progress with my film making.”."), w)
        assertFalse(w.contains("\n5. "), "no hand yet: the place is left out")
    }

    @Test
    fun keepThatIsAWriteToHisStashAndTheAppSaysItIsBelow() {
        val (storage, pages, _) = pocket()
        hatch(storage, pages)
        val way = Keeping.way("Keep that for me please", listOf("I've been making huge progress with my film making."), pages, Hatch.shellOf(h), Hatch.HER)
        assertTrue(way is Keeping.Way.Where)
        val line = HerWindow.appLine8(way, "Keep that for me please")
        assertTrue(line.contains("for his yes. Nothing is kept, made or changed until he says yes, so never say it is") && line.contains("\"It's below, for your yes.\""), line)
        val plan = Acts.propose("", listOf(Acts.Action.Add(Hatch.stashOf(h), null, (way as Keeping.Way.Where).words)), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), nowMs)
        assertTrue(plan.proposal?.asks == true, plan.problem ?: plan.proposal?.refusal)
    }

    @Test
    fun aNameAlreadyHeldInHisShellIsTalkAndANewOneIsAName() {
        val (storage, pages, _) = pocket()
        hatch(storage, pages)
        assertTrue(Keeping.way("I'd like to call you Gemma", emptyList(), pages, Hatch.shellOf(h), Hatch.HER) is Keeping.Way.Talk)
        val robin = Keeping.way("I'd like to call you Robin", emptyList(), pages, Hatch.shellOf(h), Hatch.HER)
        assertTrue(robin is Keeping.Way.Name)
        assertTrue(HerWindow.appLine8(robin, "I'd like to call you Robin").contains("(“Robin it is.”), then talk with him."))
    }

    @Test
    fun theCloseAppendsOneHistoryEntryItsOpeningAPlainConcernLine() {
        val (storage, pages, _) = pocket()
        hatch(storage, pages)
        val talk = Close.Talk(
            listOf("Hi Gemma I've been making huge progress with my film making and am eagerly awaiting my first pilot to be rendered.", "Keep that for me please"),
            read = listOf("her window"),
            written = listOf("stash:$h: “I've been making huge progress”"),
        )
        val concern = Close.concern(talk)
        assertTrue(concern.startsWith("Talked with him: 2 messages, from \u201cHi Gemma I've been making huge progress") && concern.endsWith("\u2026\u201d; one thing written on his yes."), concern)
        val plan = Close.plan(talk, pages, storage)!!
        assertTrue(pages.carryOut(plan).ok)
        val history = block(storage, "history:$h")
        val entry = history["1"] as JsObj
        assertEquals(str(Close.concern(talk)), entry["_"])
        assertEquals(str("What the app read for her."), (entry["1"] as JsObj)["_"])
        assertEquals(str("stash:$h: “I've been making huge progress”"), (entry["2"] as JsObj)["1"])
        assertTrue((entry["3"] as JsStr).value.startsWith("2026-11-01T08:40:00"), "the beach's arrival stamp at 3")
        assertNull(Close.plan(Close.Talk(emptyList()), pages, storage), "nothing said, nothing written")
    }
}
