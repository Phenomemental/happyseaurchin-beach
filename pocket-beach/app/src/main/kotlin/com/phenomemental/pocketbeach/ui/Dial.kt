package com.phenomemental.pocketbeach.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.phenomemental.pocketbeach.bsp.DialMath
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Mark
import com.phenomemental.pocketbeach.bsp.Sun
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Sundial 0.10, the dial (spine:pocket-beach 1.1111787192): drawn natively
 * from Sundial's own paths and colours (sundial.html:2244-2253, mark.js), and
 * live on the real sun where he stands (spine:pocket-beach 1.111178719914):
 * between sunrise and sunset the heart casts its shadow away from the sun,
 * north at the top, long and faint when the sun is low; behind it the
 * sunlight, its brightest point towards the sun; between sunset and sunrise
 * no shadow, and the dial glows. Today's moments on their ring by the phone's
 * clock; the spiral of days kept, each full dot in the light of the hour it
 * was kept; the colour inside the ink by each place's share; the crimson
 * centre by how evenly his days rest. Everything is reckoned in the core
 * (Sun, DialMath, Mark, Keep); this only draws.
 */
class DialState(
    /** His days kept, up to today (Keep.days). */
    val days: List<Keep.KeptDay>,
    /** Today's moments, each its instant (kept, and waiting for Keep today). */
    val moments: List<Long>,
    /** Where he stands; null before he has said, and then the dial glows as at night. */
    val lat: Double?,
    val lon: Double?,
    val zone: ZoneId,
    /** The instant the light and shadow are drawn for (the tick, every 30 s). */
    val nowMs: Long,
    /** The day kept just now, which blooms in its colours; null when none. */
    val bloom: String? = null,
) {
    val counts: List<Int> = Keep.counts(days)
    val shares: List<Double> = Mark.shares(counts)
    val even: Double = Mark.evenness(shares)

    /** The sun as the drawing wants it: the shadow's screen angle, the height 0..1 (the true one, sin of the elevation), and which side of noon; null when it is below the horizon or he hasn't said where he is. */
    fun sun(ms: Long = nowMs): Triple<Double, Double, Boolean>? {
        val la = lat ?: return null
        val lo = lon ?: return null
        val p = Sun.position(ms, la, lo)
        if (!p.up) return null
        val s = max(0.0, sin(Math.toRadians(p.elevation)))
        return Triple(p.azimuth + 90.0, s, Sun.beforeNoon(ms, zone, la, lo))
    }

    /** The light a day was kept in, against that day's real sun where he stands: night's glow, else the sunlight's colour then; null (ink) when the instant isn't known. */
    fun lightOfKeep(ms: Long?, glow: Color): Color? {
        ms ?: return null
        val sun = sun(ms) ?: return glow
        return Color(DialMath.lightOf(sun.first, sun.second, sun.third, 27).colour.toInt())
    }
}

@Composable
fun Dial(
    state: DialState,
    answering: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
    pooled: Boolean = false,
    picked: Int? = null,
    onPool: (Int) -> Unit = {},
) {
    val ink = LocalInk.current
    val ring = remember { PathParser().parsePathString(Heart.RING_INK).toPath() }
    val line = remember { PathParser().parsePathString(Heart.LINE_INK).toPath() }
    val flow = remember { PathParser().parsePathString(Heart.FLOW).toPath() }
    val flowMeasure = remember(flow) { PathMeasure().apply { setPath(flow, false) } }

    // The light and the shadow, eased from where they were to where the sun now puts them.
    val sun = state.sun()
    val cast = sun?.let { DialMath.castOf(it.first, it.second) }
    val light = sun?.let { DialMath.lightOf(it.first, it.second, it.third, state.days.size) }
    val dx by animateFloatAsState(cast?.dx?.toFloat() ?: 0f, tween(3400), label = "dx")
    val dy by animateFloatAsState(cast?.dy?.toFloat() ?: 0f, tween(3400), label = "dy")
    val castAlpha by animateFloatAsState(cast?.opacity?.toFloat() ?: 0f, tween(1500), label = "cast")
    val lx by animateFloatAsState(light?.x?.toFloat() ?: DialMath.CX.toFloat(), tween(3400), label = "lx")
    val ly by animateFloatAsState(light?.y?.toFloat() ?: DialMath.CY.toFloat(), tween(3400), label = "ly")
    val washAlpha by animateFloatAsState(light?.strength?.toFloat() ?: 0f, tween(1500), label = "wash")
    val glowAlpha by animateFloatAsState(if (sun == null) 1f else 0f, tween(1500), label = "glow")
    val lightColour = light?.let { Color(it.colour.toInt()) } ?: ink.glow

    // The heart breathes (sundial.html:466-474); while she writes, the slow pulse along the line (476-499).
    val t = rememberInfiniteTransition(label = "dial")
    val breathe by t.animateFloat(1f, 1.03f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "breathe")
    val travel by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "travel")
    val pulse by t.animateFloat(0.25f, 0.8f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")

    // A day kept just now blooms in its colours, then settles to its dot (mark.js:514-529).
    val bloomed = remember { Animatable(0f) }
    LaunchedEffect(state.bloom) {
        if (state.bloom != null) {
            bloomed.snapTo(0f)
            bloomed.animateTo(1f, tween(2400))
        }
    }

    val n = state.days.size
    val described = if (n == 0) "A sundial: a heart drawn in ink, casting its shadow at this hour, no days kept yet"
        else "A sundial: a heart drawn in ink, casting its shadow at this hour, $n ${if (n == 1) "day" else "days"} kept along a spiral"

    Canvas(
        modifier
            .semantics { contentDescription = described }
            .testTag("dial")
            .pointerInput(pooled) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val up = withTimeoutOrNull(700) { waitForUpOrCancellation() }
                    if (up == null) {
                        onHold()
                        waitForUpOrCancellation()
                    } else if (pooled) {
                        // A tap on a pool picks it; a tap elsewhere gathers them back.
                        val sc = size.width.coerceAtMost(size.height) / 430f
                        val ox = (size.width - 430f * sc) / 2f
                        val oy = (size.height - 430f * sc) / 2f
                        val x = (up.position.x - ox) / sc + 17f
                        val y = (up.position.y - oy) / sc - 75f
                        val hit = DialMath.poolsAt(state.counts).withIndex().firstOrNull { (_, p) ->
                            val r = max(p.r, 22.0)
                            (x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) <= r * r
                        }
                        if (hit != null) onPool(hit.index) else onTap()
                    } else onTap()
                    down.consume()
                }
            },
    ) {
        val sc = size.minDimension / 430f
        translate((size.width - 430f * sc) / 2f, (size.height - 430f * sc) / 2f) {
            scale(sc, sc, pivot = Offset.Zero) {
                translate(-17f, 75f) {
                    val c = Offset(DialMath.CX.toFloat(), DialMath.CY.toFloat())
                    scale(breathe, breathe, pivot = c) {
                        // The night glow (mark.js:305-317).
                        if (glowAlpha > 0f) drawCircle(
                            Brush.radialGradient(listOf(ink.glow.copy(alpha = 0.3f), ink.glow.copy(alpha = 0f)), center = c, radius = 120f),
                            radius = 120f, center = c, alpha = glowAlpha,
                        )
                        // The sunlight, its brightest point towards the sun (mark.js:198-219, 318-324).
                        if (washAlpha > 0f) drawOval(
                            Brush.radialGradient(
                                0f to lightColour, 0.5f to lightColour.copy(alpha = 0.7f), 1f to lightColour.copy(alpha = 0f),
                                center = Offset(lx, ly), radius = DialMath.LIGHT_R.toFloat(),
                            ),
                            topLeft = Offset(c.x - 124f, c.y - 124f), size = Size(248f, 248f), alpha = washAlpha,
                        )
                        // The shadow: the heart's own ink, softened, thrown away from the sun (mark.js:189-197, 327-334).
                        if (castAlpha > 0.001f) translate(dx, dy) {
                            drawIntoCanvasBlurred(4f * sc) { canvas, paint ->
                                paint.color = android.graphics.Color.argb((castAlpha * 255).toInt(), (ink.ink.red * 255).toInt(), (ink.ink.green * 255).toInt(), (ink.ink.blue * 255).toInt())
                                canvas.drawPath(ring.asAndroid(), paint)
                                canvas.drawPath(line.asAndroid(), paint)
                            }
                        }
                        // The heart in ink.
                        drawPath(ring, ink.ink)
                        drawPath(line, ink.ink)
                        // The colour inside the ink: six stretches along the line, in P6's order from Facts (mark.js:345-352).
                        val len = flowMeasure.length
                        Mark.P6.forEachIndexed { k, pool ->
                            val a = DialMath.tint(state.shares[pool - 1]).toFloat()
                            if (a <= 0f) return@forEachIndexed
                            val seg = Path()
                            flowMeasure.getSegment(len * k * 0.16f, len * (k + 1) * 0.16f, seg, true)
                            drawPath(seg, Color(Mark.POOLS[pool - 1].hue.toInt()), alpha = a, style = Stroke(7f, cap = StrokeCap.Butt))
                        }
                        // While she writes, the slow pulse of colour along the line.
                        if (answering) {
                            val seg = Path()
                            val from = len * (travel * 1.15f - 0.15f)
                            flowMeasure.getSegment(from.coerceAtLeast(0f), (from + len * 0.15f).coerceAtMost(len), seg, true)
                            drawPath(seg, ink.crimson, alpha = pulse, style = Stroke(7f, cap = StrokeCap.Round))
                        }
                        // The crimson centre, sized by how evenly his days rest; its halo while they don't (mark.js:221-226, 336-340, 410-416).
                        val cen = DialMath.centre(n, state.even, true)
                        rotate(Heart.CENTRE_TURN, pivot = Offset(Heart.CENTRE_X, Heart.CENTRE_Y)) {
                            if (n > 0) drawOval(ink.crimson, topLeft = Offset(Heart.CENTRE_X - 64f, Heart.CENTRE_Y - 40f), size = Size(128f, 80f), alpha = (0.18 * (1 - state.even)).toFloat())
                            val rx = Heart.CENTRE_RX * cen.scale.toFloat()
                            val ry = Heart.CENTRE_RY * cen.scale.toFloat()
                            drawOval(ink.crimson, topLeft = Offset(Heart.CENTRE_X - rx, Heart.CENTRE_Y - ry), size = Size(2 * rx, 2 * ry), alpha = cen.opacity.toFloat())
                        }
                    }
                    if (pooled) drawPools(state, picked, ink) else {
                        drawMoments(state, ink)
                        drawSpiral(state, bloomed.value, ink)
                    }
                }
            }
        }
    }
}

/** Today's moments, each a small ink dot on its own ring at the hour and minute it was spoken, noon up (mark.js:473-492). */
private fun DrawScope.drawMoments(state: DialState, inks: Ink) {
    if (state.moments.isEmpty()) return
    val ink = inks.ink
    val angles = state.moments.map { ms ->
        val t = java.time.Instant.ofEpochMilli(ms).atZone(state.zone).toLocalTime()
        DialMath.momentAngle(t.hour + t.minute / 60.0)
    }
    val a0 = angles.min() - 8
    val a1 = angles.max() + 8
    val arc = Path()
    var a = a0
    val p0 = DialMath.at(a0, DialMath.MOMENTS)
    arc.moveTo(p0.x.toFloat(), p0.y.toFloat())
    while (a < a1) {
        a += 2
        val p = DialMath.at(a.coerceAtMost(a1), DialMath.MOMENTS)
        arc.lineTo(p.x.toFloat(), p.y.toFloat())
    }
    drawPath(arc, ink, alpha = 0.12f, style = Stroke(1f))
    for (ang in angles) {
        val p = DialMath.at(ang, DialMath.MOMENTS)
        drawCircle(ink, radius = 4.5f, center = Offset(p.x.toFloat(), p.y.toFloat()), alpha = 0.6f)
    }
}

/** The spiral of days kept: day n the nth day kept, nine to a turn; each full dot in the light of its hour (mark.js:255-283, 386-409). */
private fun DrawScope.drawSpiral(state: DialState, bloom: Float, inks: Ink) {
    val ink = inks.ink
    val n = state.days.size
    if (n == 0) {
        val o = DialMath.dayAt(1)
        drawCircle(ink, radius = 1.8f, center = Offset(o.x.toFloat(), o.y.toFloat()), alpha = 0.3f)
        return
    }
    val pts = DialMath.spiralPoints(n)
    if (pts.size > 1) {
        val p = Path()
        p.moveTo(pts[0].x.toFloat(), pts[0].y.toFloat())
        for (q in pts.drop(1)) p.lineTo(q.x.toFloat(), q.y.toFloat())
        drawPath(p, ink, alpha = 0.15f, style = Stroke(1.5f, cap = StrokeCap.Round))
    }
    state.days.forEachIndexed { i, day ->
        val k = i + 1
        val at = DialMath.dayAt(k)
        val c = Offset(at.x.toFloat(), at.y.toFloat())
        val dot = DialMath.dotOf(k, n)
        when (dot.kind) {
            "ninth" -> drawCircle(ink, radius = dot.r.toFloat(), center = c, style = Stroke(1.5f))
            "full" -> {
                val lit = state.lightOfKeep(day.keptMs, inks.glow)
                drawCircle(lit ?: ink, radius = dot.r.toFloat(), center = c)
                // The ink's edge keeps the dot a mark on paper, whatever its light.
                drawCircle(ink, radius = dot.r.toFloat(), center = c, style = Stroke(0.9f), alpha = if (lit == null) 0f else 0.55f)
            }
            else -> drawCircle(ink, radius = dot.r.toFloat(), center = c, alpha = dot.opacity.toFloat())
        }
        // The day just kept blooms in its colours, like wet ink, then settles.
        if (state.bloom == day.address && bloom in 0.001f..0.999f) {
            val hues = day.places.map { Color(Mark.POOLS[it - 1].hue.toInt()) }.ifEmpty { listOf(ink) }
            hues.forEachIndexed { j, hue ->
                val ang = j.toDouble() / hues.size * 2 * Math.PI - Math.PI / 2
                val off = if (hues.size > 1) 6f else 0f
                val r = 20f * (0.1f + 1.15f * bloom)
                val alpha = if (bloom < 0.3f) bloom / 0.3f * 0.85f else 0.85f * (1f - (bloom - 0.3f) / 0.7f)
                drawCircle(hue, radius = r, center = Offset(c.x + off * cos(ang).toFloat(), c.y + off * sin(ang).toFloat()), alpha = alpha)
            }
        }
    }
}

/** Hold: where his days have come to rest, six pools round the centre in P6's order, each sized by its share (mark.js:428-457, 924-934). */
private fun DrawScope.drawPools(state: DialState, picked: Int?, inks: Ink) {
    val frame = inks.rule
    DialMath.poolsAt(state.counts).forEachIndexed { i, p ->
        val c = Offset(p.x.toFloat(), p.y.toFloat())
        val hue = Color(Mark.POOLS[i].hue.toInt())
        if (p.days > 0) drawCircle(hue, radius = p.r.toFloat(), center = c, alpha = if (picked == null || picked == i) 0.9f else 0.35f)
        else drawCircle(frame, radius = p.r.toFloat(), center = c, style = Stroke(2f))
        val words = DialMath.captionLines(Mark.POOLS[i].phrase)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 19f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
            color = android.graphics.Color.argb(255, (inks.soft.red * 255).toInt(), (inks.soft.green * 255).toInt(), (inks.soft.blue * 255).toInt())
        }
        words.forEachIndexed { k, w -> drawContext.canvas.nativeCanvas.drawText(w, c.x, c.y + p.r.toFloat() + 20f + 21f * k, paint) }
    }
}

/** A blurred draw on the native canvas, for the heart's shadow. */
private fun DrawScope.drawIntoCanvasBlurred(radius: Float, draw: (android.graphics.Canvas, android.graphics.Paint) -> Unit) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = android.graphics.BlurMaskFilter(radius.coerceAtLeast(0.5f), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    draw(drawContext.canvas.nativeCanvas, paint)
}

private fun Path.asAndroid(): android.graphics.Path = (this as androidx.compose.ui.graphics.AndroidPath).internalPath
