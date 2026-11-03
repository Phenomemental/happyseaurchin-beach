package com.phenomemental.pocketbeach.bsp

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The sun where he stands. Expected figures come from two sources that agree to within
 * fifteen seconds: api.sunrise-sunset.org (queried 3 November 2026, times in UTC, here put
 * into local time), and a separate Python script with only the standard library that works
 * NOAA's day spreadsheet column by column. The brief's first guesses (04:44, 21:30, 08:09)
 * were a minute or two out; the figures below are the checked ones.
 */
class SunTest {
    private val london = ZoneId.of("Europe/London")
    private val sydney = ZoneId.of("Australia/Sydney")
    private val oslo = ZoneId.of("Europe/Oslo")

    // Northampton, Tromsø and Sydney.
    private val nLat = 52.2405
    private val nLon = -0.9027
    private val tLat = 69.65
    private val tLon = 18.96
    private val sLat = -33.87
    private val sLon = 151.21

    private val midsummer = LocalDate.of(2026, 6, 21)
    private val midwinter = LocalDate.of(2026, 12, 21)

    private fun at(date: LocalDate, time: LocalTime, zone: ZoneId) = date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    /** Within a minute: the brief allowed three, and both sources and the code agree far closer. */
    private fun assertNear(expected: Long, actual: Long?, what: String) {
        assertNotNull(actual, what)
        assertTrue(abs(actual - expected) <= 60_000, "$what: ${(actual - expected) / 1000}s out")
    }

    /** Degrees apart round the circle, so 359.8 and 0.1 are 0.3 apart. */
    private fun bearingApart(a: Double, b: Double): Double {
        val d = ((a - b) % 360 + 360) % 360
        return min(d, 360 - d)
    }

    @Test
    fun northamptonRisesAndSetsAtTheSolstices() {
        val june = Sun.day(midsummer, london, nLat, nLon)
        // sunrise-sunset.org: 03:41:57 and 20:28:54 UTC; the Python NOAA sheet: 04:41:59 and 21:28:53 BST.
        assertNear(at(midsummer, LocalTime.of(4, 41, 57), london), june.sunrise, "June sunrise")
        assertNear(at(midsummer, LocalTime.of(21, 28, 54), london), june.sunset, "June sunset")
        // sunrise-sunset.org solar noon 12:05:26 UTC; Python 13:05:26 BST.
        assertNear(at(midsummer, LocalTime.of(13, 5, 26), london), june.noon, "June noon")
        assertFalse(june.alwaysUp)
        assertFalse(june.alwaysDown)

        val december = Sun.day(midwinter, london, nLat, nLon)
        // sunrise-sunset.org: 08:10:32 and 15:52:48 UTC (GMT); Python: 08:10:40 and 15:52:43.
        assertNear(at(midwinter, LocalTime.of(8, 10, 32), london), december.sunrise, "December sunrise")
        assertNear(at(midwinter, LocalTime.of(15, 52, 48), london), december.sunset, "December sunset")
        // sunrise-sunset.org solar noon 12:01:40 UTC.
        assertNear(at(midwinter, LocalTime.of(12, 1, 40), london), december.noon, "December noon")
    }

    @Test
    fun northamptonsNoonSunStandsSouthAtItsSolsticeHeights() {
        // 90 − 52.24 ± 23.44: Python NOAA gives 61.198° and 14.322° (geometric, no refraction).
        val june = Sun.position(Sun.day(midsummer, london, nLat, nLon).noon, nLat, nLon)
        assertEquals(61.2, june.elevation, 0.5)
        assertEquals(180.0, june.azimuth, 1.0)
        val december = Sun.position(Sun.day(midwinter, london, nLat, nLon).noon, nLat, nLon)
        assertEquals(14.4, december.elevation, 0.5)
        assertEquals(180.0, december.azimuth, 1.0)

        // Python NOAA, 21 June: 08:00 BST azimuth 85.36°, elevation 26.56°; 17:00 BST azimuth 260.29°, elevation 37.37°.
        val morning = Sun.position(at(midsummer, LocalTime.of(8, 0), london), nLat, nLon)
        val evening = Sun.position(at(midsummer, LocalTime.of(17, 0), london), nLat, nLon)
        assertTrue(morning.azimuth < 180, "morning sun in the east: ${morning.azimuth}")
        assertTrue(evening.azimuth > 180, "evening sun in the west: ${evening.azimuth}")
        assertEquals(85.36, morning.azimuth, 0.2)
        assertEquals(26.56, morning.elevation, 0.2)
        assertEquals(260.29, evening.azimuth, 0.2)
        assertEquals(37.37, evening.elevation, 0.2)
    }

    @Test
    fun sydneysNoonSunStandsNorth() {
        val day = Sun.day(midsummer, sydney, sLat, sLon)
        // sunrise-sunset.org solar noon 01:56:53 UTC, 11:56:53 AEST; Python NOAA noon elevation 32.69°.
        assertNear(at(midsummer, LocalTime.of(11, 56, 53), sydney), day.noon, "Sydney noon")
        val noon = Sun.position(day.noon, sLat, sLon)
        assertTrue(bearingApart(noon.azimuth, 0.0) < 1.0, "Sydney's noon sun to the north: ${noon.azimuth}")
        assertTrue(noon.azimuth >= 0.0 && noon.azimuth < 360.0)
        assertEquals(32.69, noon.elevation, 0.5)
        // Its winter day, from sunrise-sunset.org: 20:59:55 UTC the evening before (06:59:55 AEST) and 06:53:51 UTC (16:53:51 AEST).
        assertNear(at(midsummer, LocalTime.of(6, 59, 55), sydney), day.sunrise, "Sydney sunrise")
        assertNear(at(midsummer, LocalTime.of(16, 53, 51), sydney), day.sunset, "Sydney sunset")
    }

    @Test
    fun tromsoHasTheMidnightSunAndThePolarNight() {
        // sunrise-sunset.org gives no rise or set at 69.65, 18.96 on either solstice; Python NOAA cos H0 −1.21 and +1.12.
        val june = Sun.day(midsummer, oslo, tLat, tLon)
        assertTrue(june.alwaysUp)
        assertFalse(june.alwaysDown)
        assertNull(june.sunrise)
        assertNull(june.sunset)
        assertTrue(Sun.position(at(midsummer, LocalTime.of(0, 0), oslo), tLat, tLon).up, "up at midnight")

        val december = Sun.day(midwinter, oslo, tLat, tLon)
        assertTrue(december.alwaysDown)
        assertFalse(december.alwaysUp)
        assertNull(december.sunrise)
        assertNull(december.sunset)
        assertFalse(Sun.position(december.noon, tLat, tLon).up, "down even at noon")
        // sunrise-sunset.org solar noon 10:42:11 UTC, 11:42:11 CET.
        assertNear(at(midwinter, LocalTime.of(11, 42, 11), oslo), december.noon, "Tromsø noon")
    }

    @Test
    fun upAgreesWithSunriseAndSunset() {
        for (date in listOf(midsummer, midwinter, LocalDate.of(2026, 3, 20), LocalDate.of(2026, 11, 3))) {
            val day = Sun.day(date, london, nLat, nLon)
            val rise = day.sunrise!!
            val set = day.sunset!!
            fun up(t: Long) = Sun.position(t, nLat, nLon).up
            assertFalse(up(rise - 120_000), "$date two minutes before sunrise")
            assertTrue(up(rise + 120_000), "$date two minutes after sunrise")
            assertTrue(up(day.noon), "$date noon")
            assertTrue(up(set - 120_000), "$date two minutes before sunset")
            assertFalse(up(set + 120_000), "$date two minutes after sunset")
            assertFalse(up(set + 3 * 3_600_000), "$date three hours after sunset")
            assertEquals(Sun.HORIZON, Sun.position(rise, nLat, nLon).elevation, 0.02, "$date at sunrise the sun is on the horizon")
            assertEquals(Sun.HORIZON, Sun.position(set, nLat, nLon).elevation, 0.02, "$date at sunset the sun is on the horizon")
        }
    }

    @Test
    fun morningIsBeforeNoon() {
        assertTrue(Sun.beforeNoon(at(midsummer, LocalTime.of(9, 0), london), london, nLat, nLon))
        assertTrue(Sun.beforeNoon(at(midsummer, LocalTime.of(13, 0), london), london, nLat, nLon), "13:00 BST is before 13:05 solar noon")
        assertFalse(Sun.beforeNoon(at(midsummer, LocalTime.of(13, 10), london), london, nLat, nLon))
        assertFalse(Sun.beforeNoon(at(midwinter, LocalTime.of(18, 0), london), london, nLat, nLon))
        assertTrue(Sun.beforeNoon(at(midwinter, LocalTime.of(0, 30), london), london, nLat, nLon))
    }
}
