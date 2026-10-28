package com.phenomemental.pocketbeach.bsp

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Js.kt — JSON values that behave as JavaScript's do.
 *
 * The walker this port follows was written in TypeScript, and what it returns
 * leans on JavaScript's own rules: an object lists integer-like keys first in
 * ascending order and every other key in the order it was added; JSON.stringify
 * prints numbers in their shortest form and escapes a fixed set of characters;
 * String(x), trim() and \s each have their own definitions. Every one of those
 * lands in a reply or in a stored block, so each is reproduced here exactly.
 *
 * Kotlin null stands for JavaScript undefined; JsNull is JavaScript null.
 */
sealed class Js

object JsNull : Js()

data class JsBool(val value: Boolean) : Js()

data class JsNum(val value: Double) : Js()

data class JsStr(val value: String) : Js()

/**
 * A property assigned the value undefined. It stays a key (`in` sees it, as do
 * Object.keys and the wire's read-back) while JSON leaves it out, as JavaScript does.
 */
internal object JsUndefined : Js()

/** An object, keyed as JavaScript keys one: integer-like keys ascending, then the rest in insertion order. */
class JsObj : Js() {
    private val map = LinkedHashMap<String, Js>()

    operator fun get(key: String): Js? = map[key]?.takeUnless { it === JsUndefined }
    operator fun set(key: String, value: Js?) { map[key] = value ?: JsUndefined }
    fun has(key: String): Boolean = map.containsKey(key)
    internal fun raw(key: String): Js? = map[key]
    fun remove(key: String) { map.remove(key) }
    fun clear() = map.clear()
    val size: Int get() = map.size

    fun keys(): List<String> {
        val index = map.keys.filter(::isArrayIndex).sortedBy { it.toLong() }
        return index + map.keys.filterNot(::isArrayIndex)
    }

    companion object {
        fun of(vararg pairs: Pair<String, Js>): JsObj = JsObj().also { o -> pairs.forEach { o[it.first] = it.second } }
    }
}

/** An array. A hole (never assigned) is null in [items]; other properties ride in [props], as on a JS array. */
class JsArr(val items: MutableList<Js?> = mutableListOf()) : Js() {
    val props = LinkedHashMap<String, Js>()

    fun keys(): List<String> = items.indices.filter { items[it] != null }.map { it.toString() } + props.keys
}

fun isArrayIndex(key: String): Boolean {
    if (key.isEmpty() || key.length > 10) return false
    if (key == "0") return true
    if (key[0] !in '1'..'9' || key.any { it !in '0'..'9' }) return false
    return key.toLong() <= 4294967294L
}

// ── JavaScript operators ──

/** typeof v === 'object' && v !== null */
fun isObject(v: Js?): Boolean = v is JsObj || v is JsArr

/** The JS truthiness of a value. */
fun truthy(v: Js?): Boolean = when (v) {
    null, JsNull, JsUndefined -> false
    is JsBool -> v.value
    is JsNum -> v.value != 0.0 && !v.value.isNaN()
    is JsStr -> v.value.isNotEmpty()
    else -> true
}

/** `key in v` — callers guard primitives exactly as the original does. */
fun jsIn(v: Js?, key: String): Boolean = when (v) {
    is JsObj -> v.has(key)
    is JsArr -> {
        val i = if (isArrayIndex(key)) key.toLong() else -1L
        if (i >= 0) i < v.items.size && v.items[i.toInt()] != null else key == "length" || v.props.containsKey(key)
    }
    is JsStr -> isArrayIndex(key) && key.toLong() < v.value.length
    else -> false
}

/** `v[key]` */
fun jsGet(v: Js?, key: String): Js? = when (v) {
    is JsObj -> v[key]
    is JsArr -> {
        val x = if (isArrayIndex(key)) v.items.getOrNull(key.toInt()) else if (key == "length") JsNum(v.items.size.toDouble()) else v.props[key]
        x?.takeUnless { it === JsUndefined }
    }
    is JsStr -> if (isArrayIndex(key) && key.toLong() < v.value.length) JsStr(v.value[key.toInt()].toString()) else null
    else -> null
}

/** `v[key] = value`; undefined stays a key that JSON leaves out. */
fun jsSet(v: Js?, key: String, value: Js?) {
    when (v) {
        is JsObj -> v[key] = value
        is JsArr -> {
            if (isArrayIndex(key)) {
                val i = key.toInt()
                while (v.items.size <= i) v.items.add(null)
                v.items[i] = value ?: JsUndefined
            } else v.props[key] = value ?: JsUndefined
        }
        else -> {}
    }
}

/** Object.keys(v) */
fun jsKeys(v: Js?): List<String> = when (v) {
    is JsObj -> v.keys()
    is JsArr -> v.keys()
    is JsStr -> v.value.indices.map { it.toString() }
    else -> emptyList()
}

/** The digit positions a node holds, sorted — Object.keys(node).filter(/^[1-9]$/).sort(). */
fun digitKeys(v: Js?): List<String> = jsKeys(v).filter { it.length == 1 && it[0] in '1'..'9' }.sorted()

/** String(v) */
fun jsString(v: Js?): String = when (v) {
    null -> "undefined"
    JsNull -> "null"
    is JsBool -> v.value.toString()
    is JsNum -> jsNumberToString(v.value)
    is JsStr -> v.value
    is JsArr -> v.items.joinToString(",") { if (it == null || it == JsNull || it === JsUndefined) "" else jsString(it) }
    is JsObj -> "[object Object]"
    JsUndefined -> "undefined"
}

/** typeof v, for the messages that name it. */
fun jsTypeof(v: Js?): String = when (v) {
    null, JsUndefined -> "undefined"
    is JsBool -> "boolean"
    is JsNum -> "number"
    is JsStr -> "string"
    else -> "object"
}

fun str(s: String): JsStr = JsStr(s)

/** A deep copy, as JSON.parse(JSON.stringify(v)) would give for JSON-shaped values. */
fun deepCopy(v: Js): Js = when (v) {
    is JsObj -> JsObj().also { o -> v.keys().forEach { k -> o[k] = v.raw(k)?.takeUnless { it === JsUndefined }?.let(::deepCopy) } }
    is JsArr -> JsArr(v.items.map { it?.let(::deepCopy) }.toMutableList()).also { a -> v.props.forEach { (k, x) -> a.props[k] = deepCopy(x) } }
    else -> v
}

// ── Numbers, as Number.prototype.toString prints them ──

fun jsNumberToString(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d == 0.0) return "0"
    if (d.isInfinite()) return if (d > 0) "Infinity" else "-Infinity"
    if (d < 0) return "-" + jsNumberToString(-d)
    var shortest: BigDecimal? = null
    for (p in 1..17) {
        val bd = BigDecimal(d).round(MathContext(p, RoundingMode.HALF_EVEN))
        if (bd.toDouble() == d) { shortest = bd; break }
    }
    val bd = (shortest ?: BigDecimal(d)).stripTrailingZeros()
    val digits = bd.unscaledValue().toString()
    val k = digits.length
    val n = k - bd.scale()
    return when {
        n in k..21 -> digits + "0".repeat(n - k)
        n in 1..21 -> digits.substring(0, n) + "." + digits.substring(n)
        n in -5..0 -> "0." + "0".repeat(-n) + digits
        else -> {
            val e = n - 1
            val sign = if (e >= 0) "+" else "-"
            val mantissa = if (k == 1) digits else digits[0] + "." + digits.substring(1)
            "${mantissa}e$sign${kotlin.math.abs(e)}"
        }
    }
}

// ── Strings, as JavaScript defines whitespace ──

fun isJsSpace(c: Char): Boolean = when (c) {
    '\u0009', '\u000A', '\u000B', '\u000C', '\u000D', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '　', '﻿' -> true
    in ' '..' ' -> true
    else -> false
}

fun jsTrim(s: String): String = s.trim(::isJsSpace)

/** s.replace(/\s+/g, ' ') */
fun collapseSpace(s: String): String {
    val out = StringBuilder()
    var inRun = false
    for (c in s) {
        if (isJsSpace(c)) {
            if (!inRun) out.append(' ')
            inRun = true
        } else {
            out.append(c); inRun = false
        }
    }
    return out.toString()
}

/** The first n UTF-16 units, as String.prototype.slice(0, n) gives them. */
fun jsSlice(s: String, n: Int): String = if (s.length <= n) s else s.substring(0, n)

// ── Time, as Date prints and parses it ──

private val ISO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

/** new Date(ms).toISOString() */
fun isoString(epochMs: Long): String = ISO_MILLIS.format(Instant.ofEpochMilli(epochMs))

private val ISO_PARTS = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?(Z|([+-])(\d{2}):?(\d{2}))?$""")

/**
 * Date.parse for the ISO date-times the beach works with — NaN (null) wherever V8
 * answers NaN. A time with no offset reads as UTC, as on the beach's servers.
 */
fun parseIsoMs(s: String): Long? {
    val m = ISO_PARTS.matchEntire(s) ?: return null
    val g = m.groupValues
    val year = g[1].toInt(); val month = g[2].toInt(); val day = g[3].toInt()
    val hour = g[4].toInt(); val minute = g[5].toInt()
    val second = if (g[6].isEmpty()) 0 else g[6].toInt()
    val fraction = g[7]
    if (month !in 1..12 || day !in 1..31) return null
    if (minute > 59 || second > 59) return null
    if (hour > 24 || (hour == 24 && (minute != 0 || second != 0 || fraction.any { it != '0' }))) return null
    var offsetMin = 0
    if (g[8].isNotEmpty() && g[8] != "Z") {
        val oh = g[10].toInt(); val om = g[11].toInt()
        if (oh > 23 || om > 59) return null
        offsetMin = (oh * 60 + om) * (if (g[9] == "-") -1 else 1)
    }
    val ms = if (fraction.isEmpty()) 0 else (fraction + "00").substring(0, 3).toInt()
    val days = java.time.LocalDate.of(year, month, 1).toEpochDay() + (day - 1)
    return ((days * 24 + hour) * 60 + minute - offsetMin) * 60_000L + second * 1000L + ms
}

// ── JSON.parse ──

class JsonSyntaxError(message: String) : Exception(message)

fun parseJson(text: String): Js = JsonReader(text).run {
    val v = value()
    space()
    if (pos != text.length) throw JsonSyntaxError("Unexpected non-whitespace character after JSON at position $pos")
    v
}

private class JsonReader(val s: String) {
    var pos = 0

    fun space() {
        while (pos < s.length && (s[pos] == ' ' || s[pos] == '\t' || s[pos] == '\n' || s[pos] == '\r')) pos++
    }

    fun fail(): Nothing = throw JsonSyntaxError(if (pos >= s.length) "Unexpected end of JSON input" else "Unexpected token in JSON at position $pos")

    fun value(): Js {
        space()
        if (pos >= s.length) fail()
        return when (s[pos]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> JsStr(string())
            't' -> literal("true", JsBool(true))
            'f' -> literal("false", JsBool(false))
            'n' -> literal("null", JsNull)
            else -> number()
        }
    }

    fun literal(word: String, v: Js): Js {
        if (!s.startsWith(word, pos)) fail()
        pos += word.length
        return v
    }

    fun obj(): JsObj {
        val o = JsObj()
        pos++
        space()
        if (pos < s.length && s[pos] == '}') { pos++; return o }
        while (true) {
            space()
            if (pos >= s.length || s[pos] != '"') fail()
            val k = string()
            space()
            if (pos >= s.length || s[pos] != ':') fail()
            pos++
            o[k] = value()
            space()
            if (pos >= s.length) fail()
            when (s[pos]) {
                ',' -> pos++
                '}' -> { pos++; return o }
                else -> fail()
            }
        }
    }

    fun arr(): JsArr {
        val a = JsArr()
        pos++
        space()
        if (pos < s.length && s[pos] == ']') { pos++; return a }
        while (true) {
            a.items.add(value())
            space()
            if (pos >= s.length) fail()
            when (s[pos]) {
                ',' -> pos++
                ']' -> { pos++; return a }
                else -> fail()
            }
        }
    }

    fun string(): String {
        pos++
        val out = StringBuilder()
        while (true) {
            if (pos >= s.length) fail()
            val c = s[pos++]
            when {
                c == '"' -> return out.toString()
                c == '\\' -> {
                    if (pos >= s.length) fail()
                    when (s[pos++]) {
                        '"' -> out.append('"'); '\\' -> out.append('\\'); '/' -> out.append('/')
                        'b' -> out.append('\b'); 'f' -> out.append('\u000C'); 'n' -> out.append('\n')
                        'r' -> out.append('\r'); 't' -> out.append('\t')
                        'u' -> {
                            if (pos + 4 > s.length) fail()
                            val hex = s.substring(pos, pos + 4)
                            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail()
                            out.append(hex.toInt(16).toChar()); pos += 4
                        }
                        else -> { pos--; fail() }
                    }
                }
                c < ' ' -> { pos--; fail() }
                else -> out.append(c)
            }
        }
    }

    fun number(): Js {
        val start = pos
        if (pos < s.length && s[pos] == '-') pos++
        if (pos >= s.length) fail()
        if (s[pos] == '0') pos++
        else if (s[pos] in '1'..'9') while (pos < s.length && s[pos] in '0'..'9') pos++
        else fail()
        if (pos < s.length && s[pos] == '.') {
            pos++
            if (pos >= s.length || s[pos] !in '0'..'9') fail()
            while (pos < s.length && s[pos] in '0'..'9') pos++
        }
        if (pos < s.length && (s[pos] == 'e' || s[pos] == 'E')) {
            pos++
            if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
            if (pos >= s.length || s[pos] !in '0'..'9') fail()
            while (pos < s.length && s[pos] in '0'..'9') pos++
        }
        return JsNum(s.substring(start, pos).toDouble())
    }
}

// ── JSON.stringify ──

/** JSON.stringify(v) — null when v is undefined. */
fun stringify(v: Js?): String? = v?.let { StringBuilder().also { b -> write(b, it, null, "") }.toString() }

/** JSON.stringify(v, null, 2) */
fun stringifyPretty(v: Js): String = StringBuilder().also { b -> write(b, v, "  ", "") }.toString()

private fun write(b: StringBuilder, v: Js, gap: String?, indent: String) {
    when (v) {
        JsNull -> b.append("null")
        is JsBool -> b.append(v.value)
        is JsNum -> b.append(if (v.value.isNaN() || v.value.isInfinite()) "null" else jsNumberToString(v.value))
        is JsStr -> quote(b, v.value)
        is JsArr -> {
            if (v.items.isEmpty()) { b.append("[]"); return }
            val inner = if (gap == null) "" else indent + gap
            b.append('[')
            v.items.forEachIndexed { i, x ->
                if (i > 0) b.append(',')
                if (gap != null) b.append('\n').append(inner)
                if (x == null || x === JsUndefined) b.append("null") else write(b, x, gap, inner)
            }
            if (gap != null) b.append('\n').append(indent)
            b.append(']')
        }
        JsUndefined -> b.append("null")
        is JsObj -> {
            val keys = v.keys().filter { v.raw(it) !== JsUndefined }
            if (keys.isEmpty()) { b.append("{}"); return }
            val inner = if (gap == null) "" else indent + gap
            b.append('{')
            keys.forEachIndexed { i, k ->
                if (i > 0) b.append(',')
                if (gap != null) b.append('\n').append(inner)
                quote(b, k)
                b.append(if (gap != null) ": " else ":")
                write(b, v[k]!!, gap, inner)
            }
            if (gap != null) b.append('\n').append(indent)
            b.append('}')
        }
    }
}

private fun quote(b: StringBuilder, s: String) {
    b.append('"')
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '"' -> b.append("\\\"")
            c == '\\' -> b.append("\\\\")
            c == '\b' -> b.append("\\b")
            c == '\u000C' -> b.append("\\f")
            c == '\n' -> b.append("\\n")
            c == '\r' -> b.append("\\r")
            c == '\t' -> b.append("\\t")
            c < ' ' -> b.append("\\u").append(c.code.toString(16).padStart(4, '0'))
            Character.isHighSurrogate(c) && i + 1 < s.length && Character.isLowSurrogate(s[i + 1]) -> { b.append(c).append(s[i + 1]); i++ }
            Character.isSurrogate(c) -> b.append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> b.append(c)
        }
        i++
    }
    b.append('"')
}

/** The deepEqual the wire's read-back runs (pscale-wire.ts). */
fun deepEqual(a: Js?, b: Js?): Boolean {
    if (a == null || b == null) return a == null && b == null
    if (a === b) return true
    if (a is JsNum && b is JsNum) return a.value == b.value
    if (a is JsArr || b is JsArr) {
        if (a !is JsArr || b !is JsArr || a.items.size != b.items.size) return false
        return a.items.indices.all { i -> a.items[i] == null || deepEqual(a.items[i], b.items[i]) }
    }
    if (a is JsObj && b is JsObj) {
        val ka = a.keys()
        if (ka.size != b.keys().size) return false
        return ka.all { k -> b.has(k) && deepEqual(a[k], b[k]) }
    }
    return a == b
}
