package com.yolojj333.heythere.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local (JVM) unit tests for the data models. These lock down the default values and the
 * value-semantics (equality + copy) that the rest of the app relies on — e.g. MainActivity
 * compares LocationData and uses User.copy(...) all over the place, and the privacy defaults
 * are security-relevant.
 */
class UserModelTest {

    @Test
    fun user_hasExpectedDefaults() {
        val user = User()
        assertEquals("", user.userId)
        assertEquals("", user.displayName)
        assertEquals(18, user.age)
        assertEquals("Not Specified", user.gender)
        assertEquals("", user.bio)
        assertTrue(user.profileImageUrls.isEmpty())
        assertTrue(user.subscribedTags.isEmpty())
    }

    @Test
    fun locationData_hasExpectedDefaults() {
        val loc = LocationData()
        assertEquals(0.0, loc.publicLatitude, 0.0)
        assertEquals(0.0, loc.publicLongitude, 0.0)
        assertEquals(0L, loc.lastUpdatedTimestamp)
    }

    @Test
    fun blackoutZone_defaultRadiusIs200Meters() {
        val zone = BlackoutZone()
        assertEquals("", zone.name)
        assertEquals(0.0, zone.latitude, 0.0)
        assertEquals(0.0, zone.longitude, 0.0)
        assertEquals(200.0, zone.radiusMeters, 0.0)
    }

    @Test
    fun privacySettings_defaultsArePrivacySafe() {
        val settings = PrivacySettings()
        // Broadcasting on, but precise location OFF and background OFF by default.
        assertTrue(settings.isGlobalLocationOn)
        assertFalse(settings.usePreciseLocation)
        assertFalse(settings.isBackgroundLocationEnabled)
        assertEquals(60, settings.backgroundUpdateIntervalSeconds)
        assertEquals(100, settings.mapPinSize)
        assertTrue(settings.activeBlackoutZones.isEmpty())
    }

    @Test
    fun user_copyOverridesOnlyTargetedField() {
        val base = User(displayName = "Ann", age = 20)
        val updated = base.copy(displayName = "Annika")

        assertEquals("Annika", updated.displayName)
        assertEquals(20, updated.age)            // untouched field carries over
        assertEquals("Ann", base.displayName)    // original is not mutated
    }

    @Test
    fun user_hasValueEquality() {
        val a = User(userId = "u1", displayName = "Ann", age = 22)
        val b = User(userId = "u1", displayName = "Ann", age = 22)
        assertEquals(a, b)
        assertNotEquals(a, a.copy(age = 23))
    }

    @Test
    fun locationData_hasValueEquality() {
        assertEquals(LocationData(1.0, 2.0, 3L), LocationData(1.0, 2.0, 3L))
        // A changed timestamp must make the objects unequal (drives map recomposition).
        assertNotEquals(LocationData(1.0, 2.0, 3L), LocationData(1.0, 2.0, 4L))
    }

    @Test
    fun privacySettings_copyTogglesSingleFlagOnly() {
        val on = PrivacySettings()
        val off = on.copy(isGlobalLocationOn = false)

        assertFalse(off.isGlobalLocationOn)
        assertTrue(on.isGlobalLocationOn)                       // original unchanged
        assertEquals(on.usePreciseLocation, off.usePreciseLocation)
        assertEquals(on.mapPinSize, off.mapPinSize)
    }
}
