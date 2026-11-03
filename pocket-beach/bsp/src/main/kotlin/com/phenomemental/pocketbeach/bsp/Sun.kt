package com.phenomemental.pocketbeach.bsp

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * The real sun where he stands, by the standard solar arithmetic (NOAA's, after Meeus), with no network.
 *
 * The formulas are those of NOAA's Solar Calculator spreadsheet (gml.noaa.gov/grad/solcalc), good to
 * about a minute for sunrise and sunset between 1901 and 2099, less so near the polar circles where
 * the sun skims the horizon. Elevation is geometric: NOAA's refraction correction is left out, since
 * [HORIZON] already allows for refraction at the horizon, so [Position.up] and [day] agree.
 */
object Sun {
    /** Below this elevation (degrees) the sun has set: the standard -0.833, refraction and the sun's half-width. */
    const val HORIZON = -0.833

    /** Where the sun stands: elevation above the horizon and azimuth from north, clockwise, both in degrees (azimuth 0 until 360). */
    data class Position(val elevation: Double, val azimuth: Double) {
        val up: Boolean get() = elevation > HORIZON
    }

    /** The sun at the instant [epochMs] (UTC ms) seen from latitude [lat] and longitude [lon] (degrees, east positive). */
    fun position(epochMs: Long, lat: Double, lon: Double): Position {
        val s = solar(epochMs)
        val ha = hourAngle(epochMs, s, lon)
        val phi = rad(lat)
        val dec = rad(s.declination)
        val zenith = acos((sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(rad(ha))).coerceIn(-1.0, 1.0))
        // NOAA's azimuth: the angle from south, turned to run clockwise from north on either side of noon.
        val below = cos(phi) * sin(zenith)
        val azimuth = if (abs(below) < 1e-12) {
            if (lat >= s.declination) 180.0 else 0.0 // at a pole, or the sun overhead: no bearing to speak of
        } else {
            val a = deg(acos(((sin(phi) * cos(zenith) - sin(dec)) / below).coerceIn(-1.0, 1.0)))
            if (ha > 0) (a + 180) % 360 else (540 - a) % 360
        }
        return Position(90 - deg(zenith), azimuth)
    }

    /**
     * The sun's day for a calendar [date] in [zone] at [lat],[lon]: sunrise and sunset as UTC epoch ms
     * (null when it doesn't rise, or doesn't set, that day), solar noon as UTC epoch ms, and whether it
     * stays up all day or down all day.
     *
     * Noon is the transit nearest the middle of that local day; sunrise is the rising before it and
     * sunset the setting after it, so near the polar circles a sunset can fall after local midnight.
     */
    data class Day(val sunrise: Long?, val sunset: Long?, val noon: Long, val alwaysUp: Boolean, val alwaysDown: Boolean)

    fun day(date: LocalDate, zone: ZoneId, lat: Double, lon: Double): Day {
        val noon = noon(date, zone, lon)
        val c = cosRiseHourAngle(solar(noon).declination, lat)
        return when {
            c < -1 -> Day(null, null, noon, alwaysUp = true, alwaysDown = false)
            c > 1 -> Day(null, null, noon, alwaysUp = false, alwaysDown = true)
            else -> Day(event(noon, lat, lon, -1), event(noon, lat, lon, +1), noon, alwaysUp = false, alwaysDown = false)
        }
    }

    /** Whether [epochMs] is before that day's solar noon where he stands (the morning light, apricot, against the evening's amber). */
    fun beforeNoon(epochMs: Long, zone: ZoneId, lat: Double, lon: Double): Boolean =
        epochMs < day(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate(), zone, lat, lon).noon

    /** The sun's declination (degrees) and the equation of time (minutes) at one instant. */
    private class Solar(val declination: Double, val equationOfTime: Double)

    private fun solar(epochMs: Long): Solar {
        val t = (epochMs / 86_400_000.0 + 2_440_587.5 - 2_451_545.0) / 36_525.0 // Julian century from J2000
        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0) // geometric mean longitude
        val m = 357.52911 + t * (35999.05029 - 0.0001537 * t) // geometric mean anomaly
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t) // eccentricity of the earth's orbit
        val centre = sin(rad(m)) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(rad(2 * m)) * (0.019993 - 0.000101 * t) + sin(rad(3 * m)) * 0.000289
        val omega = rad(125.04 - 1934.136 * t)
        val apparent = l0 + centre - 0.00569 - 0.00478 * sin(omega) // apparent longitude
        val meanObliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
        val obliquity = meanObliquity + 0.00256 * cos(omega) // obliquity corrected
        val declination = deg(asin(sin(rad(obliquity)) * sin(rad(apparent))))
        val y = tan(rad(obliquity / 2)).let { it * it }
        val l = rad(l0)
        val mr = rad(m)
        val eqTime = 4 * deg(
            y * sin(2 * l) - 2 * e * sin(mr) + 4 * e * y * sin(mr) * cos(2 * l) -
                0.5 * y * y * sin(4 * l) - 1.25 * e * e * sin(2 * mr),
        )
        return Solar(declination, eqTime)
    }

    /** The hour angle (degrees, -180 until 180, 0 at solar noon) from true solar time. */
    private fun hourAngle(epochMs: Long, s: Solar, lon: Double): Double {
        val minutesUtc = Math.floorMod(epochMs, 86_400_000L) / 60_000.0
        val trueSolar = (minutesUtc + s.equationOfTime + 4 * lon).mod(1440.0)
        return trueSolar / 4 - 180
    }

    /** cos of the hour angle at which the sun stands at [HORIZON]; beyond ±1 it never gets there. NOAA's cos(90.833)/(cos φ cos δ) − tan φ tan δ. */
    private fun cosRiseHourAngle(declination: Double, lat: Double): Double =
        (sin(rad(HORIZON)) - sin(rad(lat)) * sin(rad(declination))) / (cos(rad(lat)) * cos(rad(declination)))

    /** The transit nearest the middle of [date] in [zone]: step by the hour angle, four minutes a degree, and again for the drift in the equation of time. */
    private fun noon(date: LocalDate, zone: ZoneId, lon: Double): Long {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var t = start + (end - start) / 2
        repeat(3) { t -= (hourAngle(t, solar(t), lon) * MS_PER_DEGREE).roundToLong() }
        return t
    }

    /** Sunrise ([side] -1) or sunset (+1) about [noon], the declination taken afresh at each guess; null if the sun never reaches the horizon there. */
    private fun event(noon: Long, lat: Double, lon: Double, side: Int): Long? {
        var t = noon
        repeat(4) {
            val s = solar(t)
            val c = cosRiseHourAngle(s.declination, lat)
            if (c !in -1.0..1.0) return null
            val step = (side * deg(acos(c)) - hourAngle(t, s, lon) + 180).mod(360.0) - 180
            t += (step * MS_PER_DEGREE).roundToLong()
        }
        return t
    }

    private const val MS_PER_DEGREE = 240_000.0 // four minutes of time
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
}
