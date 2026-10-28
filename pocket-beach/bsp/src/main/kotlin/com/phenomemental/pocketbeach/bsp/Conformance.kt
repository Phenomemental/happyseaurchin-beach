package com.phenomemental.pocketbeach.bsp

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

    /** Replay every case, each on a fresh storage from [newStorage]; [release] is handed each one when its case is done. */
    fun replay(golden: String, shelf: Shelf, newStorage: () -> BlockStorage, release: (BlockStorage) -> Unit = {}): Report {
        val doc = parseJson(golden) as JsObj
        val cases = (doc["cases"] as JsArr).items.map { it as JsObj }
        val failures = mutableListOf<String>()
        var calls = 0
        for (case in cases) {
            val storage = newStorage()
            try {
                calls += replayCase(case, storage, shelf, failures)
            } finally {
                release(storage)
            }
        }
        return Report(cases.size, calls, failures)
    }

    private fun replayCase(case: JsObj, store: BlockStorage, shelf: Shelf, failures: MutableList<String>): Int {
        val name = jsString(case["name"])
        val seedState = case["seedState"] as JsObj
        val expected = LinkedHashMap<String, String>()
        val seeded = seedState["blocks"] as JsObj
        for (k in seeded.keys()) {
            val json = jsString(seeded[k])
            store.write(k, json)
            expected[k] = json
        }
        store.setStamps(Stamps(stringMap(seedState["touched"] as JsObj), stringMap(seedState["born"] as JsObj)))
        val pocket = PocketBsp(store, shelf)

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
            val names = store.names().sorted()
            if (names.toSet() != expected.keys) failures += "$where\n  blocks stored: $names, expected ${expected.keys.sorted()}"
            for ((k, json) in expected) {
                val got = store.read(k)
                if (got != json) failures += "$where\n  block \"$k\" differs ${firstDifference(json, got ?: "<absent>")}"
            }
            val stamps = store.stamps()
            if (stamps.touched != stringMap(call["touched"] as JsObj)) failures += "$where\n  touched ${stamps.touched} vs ${stringify(call["touched"])}"
            if (stamps.born != stringMap(call["born"] as JsObj)) failures += "$where\n  born ${stamps.born} vs ${stringify(call["born"])}"
        }
        return calls.size
    }

    private fun stringMap(o: JsObj): Map<String, String> = o.keys().associateWith { jsString(o[it]) }

    private fun firstDifference(want: String, got: String): String {
        var i = 0
        while (i < want.length && i < got.length && want[i] == got[i]) i++
        fun around(s: String) = stringify(str(s.substring(maxOf(0, i - 60), minOf(s.length, i + 80))))
        return "at character $i\n    expected: ${around(want)}\n    got:      ${around(got)}"
    }
}
