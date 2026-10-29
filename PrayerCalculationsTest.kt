package org.wsb.mescidtv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalculationsTest {

    @Test
    fun testMinuteOffsetCalculation() {
        val originalTime = "12:30"
        val offsetMinutes = 15
        
        val parts = originalTime.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        
        val total = h * 60 + m + offsetMinutes
        val newH = (total / 60) % 24
        val newM = total % 60
        
        val formatted = String.format("%02d:%02d", newH, newM)
        assertEquals("12:45", formatted)
    }

    @Test
    fun testMidnightRollover() {
        val originalTime = "23:55"
        val offsetMinutes = 10
        
        val parts = originalTime.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        
        val total = (h * 60 + m + offsetMinutes) % 1440
        val newH = total / 60
        val newM = total % 60
        
        val formatted = String.format("%02d:%02d", newH, newM)
        assertEquals("00:05", formatted)
    }

    @Test
    fun testHamburgCoordinatesValidity() {
        val lat = 53.5511
        val lon = 9.9937
        assertTrue("Latitude must be within valid Northern Hemisphere range", lat > 50.0 && lat < 60.0)
        assertTrue("Longitude must be within valid Central Europe range", lon > 8.0 && lon < 12.0)
    }
}
