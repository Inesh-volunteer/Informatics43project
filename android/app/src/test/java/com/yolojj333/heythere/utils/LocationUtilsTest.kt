package com.yolojj333.heythere.utils

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito

/**
 * Integration tests for [LocationUtils.applyLocationNoise]. These exercise the real production
 * function end-to-end: the noise math, the SharedPreferences persistence, and the cross-call
 * "stable offset" behaviour. The Android Context is supplied by Mockito and backed by an
 * in-memory [FakeSharedPreferences], so no emulator is required.
 */
class LocationUtilsTest {

    /** Builds a mock Context whose getSharedPreferences() always returns [prefs]. */
    private fun contextBackedBy(prefs: FakeSharedPreferences): Context {
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        return context
    }

    /** Great-circle distance in meters — used only to assert the privacy radius (test helper). */
    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    @Test
    fun firstCall_persistsOffset_andStaysWithinPrivacyRadius() {
        val prefs = FakeSharedPreferences()
        val context = contextBackedBy(prefs)
        val exact = LatLng(33.6405, -117.8443) // near UCI

        assertFalse("precondition: nothing persisted yet", prefs.contains("noise_offset_lat"))

        val noised = LocationUtils.applyLocationNoise(context, exact, radiusInMeters = 500.0)

        // The randomized offset must be saved to disk so the user never teleports after a RAM purge.
        assertTrue(prefs.contains("noise_offset_lat"))
        assertTrue(prefs.contains("noise_offset_lng"))

        // The broadcast point must stay within the ~500m privacy radius of the true location.
        val distance = distanceMeters(exact.latitude, exact.longitude, noised.latitude, noised.longitude)
        assertTrue("noised point was $distance m from exact (>500m)", distance <= 520.0)
    }

    @Test
    fun repeatedCalls_returnTheSameLocation_noTeleport() {
        val prefs = FakeSharedPreferences()
        val context = contextBackedBy(prefs)
        val exact = LatLng(40.7128, -74.0060) // NYC

        val first = LocationUtils.applyLocationNoise(context, exact)
        val second = LocationUtils.applyLocationNoise(context, exact)
        val third = LocationUtils.applyLocationNoise(context, exact)

        // A regression of the "stable offset" fix would re-randomize and move us hundreds of meters;
        // 1e-6 degrees (~0.1m) tolerance absorbs only the float<->double storage rounding.
        assertEquals(first.latitude, second.latitude, 1e-6)
        assertEquals(first.longitude, second.longitude, 1e-6)
        assertEquals(second.latitude, third.latitude, 1e-6)
        assertEquals(second.longitude, third.longitude, 1e-6)
    }

    @Test
    fun storedOffset_isReusedAcrossDifferentExactLocations() {
        val prefs = FakeSharedPreferences()
        val context = contextBackedBy(prefs)

        val atOrigin = LocationUtils.applyLocationNoise(context, LatLng(0.0, 0.0))
        // The offset is now fixed; a new exact location must shift by the SAME stored lat offset.
        val shifted = LocationUtils.applyLocationNoise(context, LatLng(10.0, 10.0))

        val storedLatOffset = prefs.getFloat("noise_offset_lat", 0f).toDouble()
        assertEquals(0.0 + storedLatOffset, atOrigin.latitude, 1e-6)
        assertEquals(10.0 + storedLatOffset, shifted.latitude, 1e-6)
    }
}
