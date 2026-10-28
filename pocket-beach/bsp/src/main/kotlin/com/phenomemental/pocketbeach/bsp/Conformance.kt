package com.phenomemental.pocketbeach.bsp

import java.io.File

/**
 * The golden replay, shared by the JVM test and the on-device test.
 *
 * conformance/golden/golden.json holds every call of the corpus as the
 * original bsp-mcp-server and pscale-beach answered it. Each case starts from
 * the stored state the reference had after seeding; each call runs at the
 * instant the reference ran it; the reply text and every stored block must
 * come back identical, byte for byte, with the same touched and born stamps.
 */
object Conformance {
    class Report(val cases: Int, val calls: Int, val failures: List<String>)

    fun replay(golden: String, shelf: Shelf, newRoot: () -> File): Report {
        val doc = parseJson(golden) as JsObj
        val cases = (doc["cases"] as JsArr).items.map { it as JsObj }
        val failures = mutableListOf<String>()
        var calls = 0
        for (case in cases) {
            val root = newRoot()
            try {
                calls += replayCase(case, root, shelf, failures)
            } finally {
                root.deleteRecursively()
            }
        }
        return Report(cases.size, calls, failures)
    }

    private fun replayCase(case: JsObj, root: File, shelf: Shelf, failures: MutableList<String>): Int {
        val name = jsString(case["name"])
        val store = BlockStore(root)
        val seedState = case["seedState"] as JsObj
        val expected = LinkedHashMap<String, String>()
        val seeded = seedState["blocks"] as JsObj
        for (k in seeded.keys()) {
            val json = jsString(seeded[k])
            store.write(k, json)
            expected[k] = json
        }
        val touched = seedState["touched"] as JsObj
        val born = seedState["born"] as JsObj
        for (k in touched.keys()) store.stamp(k, jsString(touched[k]), false)
        for (k in born.keys()) store.stampBorn(k, jsString(born[k]))
        val pocket = PocketBsp(root, shelf)

        val calls = (case["calls"] as JsArr).items.map { it as JsObj }
        calls.forEachIndexed { i, call ->
            val params = call["params"] as JsObj
            val nowMs = parseIsoMs(jsString(call["now"]))!!
            val text = pocket.call(params, nowMs)
            val where = "case \"$name\" call ${i + 1}: ${stringify(params)}"
            val want = jsString(call["text"])
            if (text != want) failures += "$where\n  reply differs ${firstDifference(want, text)}"
            val changed = call["changed"] as JsObj
            for (k in changed.keys()) expected[k] = jsString(changed[k])
            val names = store.names()
            if (names.toSet() != expected.keys) failures += "$where\n  blocks stored: $names, expected ${expected.keys.sorted()}"
            for ((k, json) in expected) {
                val got = store.read(k)
                if (got != json) failures += "$where\n  block \"$k\" differs ${firstDifference(json, got ?: "<absent>")}"
            }
            if (!sameMap(store.touched(), call["touched"] as JsObj)) failures += "$where\n  touched ${stringify(store.touched())} vs ${stringify(call["touched"])}"
            if (!sameMap(store.born(), call["born"] as JsObj)) failures += "$where\n  born ${stringify(store.born())} vs ${stringify(call["born"])}"
        }
        return calls.size
    }

    private fun sameMap(a: JsObj, b: JsObj): Boolean = a.keys().toSet() == b.keys().toSet() && a.keys().all { a[it] == b[it] }

    private fun firstDifference(want: String, got: String): String {
        var i = 0
        while (i < want.length && i < got.length && want[i] == got[i]) i++
        fun around(s: String) = stringify(str(s.substring(maxOf(0, i - 60), minOf(s.length, i + 80))))
        return "at character $i\n    expected: ${around(want)}\n    got:      ${around(got)}"
    }
}
