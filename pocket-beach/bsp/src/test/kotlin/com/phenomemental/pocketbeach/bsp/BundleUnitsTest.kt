package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The unit goldens: conformance/golden/golden.json's "units", made by calling
 * the original's bundle, clock and seat functions directly (conformance/units.mts).
 * Each input runs through the port here and must give the same output.
 */
class BundleUnitsTest {
    private val units: JsObj = run {
        val golden = File("../conformance/golden/golden.json")
        assertTrue(golden.isFile, "no golden file at $golden — run conformance/generate.sh")
        (parseJson(golden.readText(Charsets.UTF_8)) as JsObj)["units"] as? JsObj ?: error("golden.json has no unit goldens")
    }

    private fun rows(key: String): List<JsArr> = (units[key] as JsArr).items.map { it as JsArr }
    private fun JsArr.at(i: Int): Js? = items[i]
    private fun JsArr.text(i: Int): String = (items[i] as JsStr).value
    private fun JsArr.textOrNull(i: Int): String? = (items[i] as? JsStr)?.value
    private fun num(v: Js?): Long = (v as JsNum).value.toLong()
    private fun textOf(v: Js?): String? = (v as? JsStr)?.value
    private fun intOrNull(v: Js?): Int? = (v as? JsNum)?.value?.toInt()

    private fun threw(f: () -> String): String = try { f() } catch (e: Exception) { "[threw] ${e.message}" }

    private class Mismatches(val what: String) {
        val list = mutableListOf<String>()
        var checked = 0
        fun check(where: String, want: Any?, got: Any?) {
            checked++
            if (want != got) list += "$where\n    expected: $want\n    got:      $got"
        }
        fun report() {
            println("units ($what): $checked checked, ${list.size} mismatches")
            if (list.isNotEmpty()) fail("$what: ${list.size} of $checked differ from the original:\n\n" + list.take(10).joinToString("\n\n"))
            assertTrue(checked > 0, "$what: nothing was checked")
        }
    }

    @Test
    fun momentsAddressesAndVoices() {
        val m = Mismatches("the clock")
        for (r in rows("moments")) {
            val ms = num(r.at(0))
            for (f in 0..4) m.check("momentToAddress($ms, $f)", r.text(f + 1), threw { momentToAddress(ms, f) })
        }
        for (r in rows("voices")) m.check("voiceAddress(${r.text(0)})", r.text(1), threw { voiceAddress(r.text(0)) })
        for (r in rows("spans")) {
            val want = r.at(1).let { v -> if (v is JsArr) v.items.joinToString(",") { jsString(it) } else textOf(v) }
            m.check("addressToSpan(${r.text(0)})", want, threw { addressToSpan(r.text(0)).let { "${it.startMs},${it.endMs},${it.pscale}" } })
        }
        for (r in rows("divergence")) m.check("divergencePscale(${r.text(0)}, ${r.text(1)})", intOrNull(r.at(2)), divergencePscale(r.text(0), r.text(1)))
        for (r in rows("clockStamps")) m.check("clockStamp(${r.text(0)})", r.text(1), clockStamp(r.text(0)))
        for (r in rows("rungs")) m.check("rungName(${num(r.at(0))})", r.text(1), rungName(num(r.at(0)).toInt()))
        m.report()
    }

    @Test
    fun referencesAndForms() {
        val m = Mismatches("references and forms")
        for (r in rows("refs")) {
            val want = (r.at(1) as? JsObj)?.let { o ->
                listOf(textOf(o["ref"]), textOf(o["name"]), textOf(o["address"]), textOf(o["word"]), (o["attention"] as? JsNum)?.value?.toLong())
            }
            val got = parseBundleRef(r.text(0))?.let { listOf(it.ref, it.name, it.address, it.word, it.attention) }
            m.check("parseBundleRef(${stringify(r.at(0))})", want, got)
        }
        for (r in rows("names")) m.check("handleOfBlock(${r.text(0)})", r.textOrNull(1), handleOfBlock(r.text(0)))
        for (r in rows("looks")) {
            val node = r.at(0)
            m.check("looksLikeBundle(${stringify(node)})", r.at(1), JsBool(looksLikeBundle(node)))
            m.check("isEntryNode(${stringify(node)})", r.at(2), JsBool(isEntryNode(node)))
            val want = (r.at(3) as JsArr).items.map { s -> (s as JsArr).items.map(::textOf) }
            val got = bundleSlots(node).map { listOf(it.slot, it.leaf, it.ref?.ref) }
            m.check("bundleSlots(${stringify(node)})", want, got)
        }
        for (r in rows("heads")) m.check("headAddress(${stringify(r.at(0))})", r.textOrNull(1), headAddress(r.at(0)!!))
        m.report()
    }

    @Test
    fun theClockRange() {
        val m = Mismatches("the clock range")
        val clock = units["clock"]!!
        val ranges = rows("ranges")
        fun lines(v: Js?) = (v as JsArr).items.map { l -> l as JsObj; listOf(textOf(l["address"]), intOrNull(l["pscale"]), textOf(l["text"]), textOf(l["stamp"])) }
        for (r in ranges) {
            val got = clockRange(clock, r.text(0), r.text(1)).map { listOf(it.address, it.pscale, it.text, it.stamp) }
            m.check("clockRange(${r.text(0)}, ${r.text(1)})", lines(r.at(2)), got)
        }
        for (r in rows("clockLines")) {
            val i = num(r.at(0)).toInt()
            val range = clockRange(clock, ranges[i].text(0), ranges[i].text(1))
            m.check("renderClockLines(range $i, ${num(r.at(1))})", r.text(2), renderClockLines(range, num(r.at(1)).toInt()))
        }
        val beat = (units["beat"] as JsStr).value
        val walk = bspRead(clock, beat, null).walkEntries ?: emptyList()
        for (r in rows("cuts")) {
            val c = cutClockWalk(walk, r.text(0), r.text(1), (r.at(2) as JsBool).value)
            val want = listOf((r.at(3) as JsArr).items.map(::textOf), intOrNull(r.at(4)), r.at(5), r.at(6))
            m.check("cutClockWalk(${r.text(0)}, ${r.text(1)}, ${r.at(2)})", want, listOf(c.entries.map { it.address }, c.from, JsBool(c.same), JsBool(c.within)))
        }
        m.report()
    }

    @Test
    fun bundlesCompiledAndRendered() {
        val m = Mismatches("bundles")
        val surface = units["surface"] as JsObj
        for ((n, b) in (units["bundles"] as JsArr).items.withIndex()) {
            b as JsObj
            val node = b["node"]
            val handle = textOf(b["handle"])
            val now = num(b["now"])
            val since = textOf(b["since"])
            val loads = mutableListOf<String>()
            val load: (String) -> Js? = { name ->
                loads += name
                if (name == "broken") throw IllegalStateException("the loader refused")
                surface[name]
            }
            val members = compileBundle(node, BundleContext(load, handle, now, since))
            val where = "bundle $n (${stringify(node)}, handle $handle, now $now, since $since)"
            m.check("$where loads", (b["loads"] as JsArr).items.map(::textOf), loads.sorted())
            val want = (b["members"] as? JsArr)?.items?.map { x -> (x as JsArr).items.map { if (it is JsBool) it.value else textOf(it) } }
            val got = members?.map { listOf(it.slot, it.name, it.address, it.absent, it.note, renderMemberBody(it)) }
            m.check("$where members", want, got)
            val label = "\"pocket/desk\" 2"
            m.check("$where window", textOf(b["window"]), members?.let { renderBundle(it, label, now, since) })
            val clockFree = !stringify(node)!!.lowercase().contains(":now")
            m.check("$where small", textOf(b["small"]), members?.takeIf { since == null && clockFree }?.let { renderBundle(it, label, now, since, cap = 1500) })
            m.check("$where member", textOf(b["member"]), members?.let { renderMember(it[0], "    ") })
        }
        m.report()
    }

    @Test
    fun theSeatRides() {
        val m = Mismatches("seats")
        for (sc in (units["seats"] as JsArr).items) {
            sc as JsObj
            val blocks = HashMap<String, Js?>()
            val start = sc["blocks"] as JsObj
            for (k in start.keys()) blocks[k] = deepCopy(start[k]!!)
            val touched = LinkedHashMap<String, String>()
            val io = object : RiderIO {
                override fun load(origin: String): (String) -> Js? = { name -> blocks[name] }
                override fun touched(origin: String): Map<String, String> = touched.toMap()
            }
            val handle = jsString(sc["handle"])
            val block = jsString(sc["block"])
            val spindle = jsString(sc["spindle"])
            var seat: Seat? = null
            for ((i, step) in (sc["steps"] as JsArr).items.withIndex()) {
                step as JsObj
                val at = num(step["at"])
                when (jsString(step["op"])) {
                    "declare" -> seat = declareSeat("pocket", handle, block, spindle, readAt(blocks[block]!!, spindle), at)
                    "set" -> {
                        val name = jsString(step["block"])
                        blocks[name] = step["content"]
                        touched[name] = isoString(at)
                    }
                    "told" -> seat?.told(at)
                    "ride" -> m.check("seat \"$handle\" step ${i + 1} at $at", jsString(step["text"]), seat?.rider(at, io) ?: "")
                }
            }
        }
        m.report()
    }
}
