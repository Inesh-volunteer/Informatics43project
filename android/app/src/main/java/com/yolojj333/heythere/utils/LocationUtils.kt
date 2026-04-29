// File: app/src/main/java/com/yolojj333/beacon/utils/LocationUtils.kt
package com.yolojj333.heythere.utils

import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object LocationUtils {

    /**
     * Generates a randomized location within a specified radius (in meters) 
     * of the exact location to protect user privacy.
     */
    fun applyLocationNoise(exactLocation: LatLng, radiusInMeters: Double = 500.0): LatLng {
        val radiusInDegrees = radiusInMeters / 111320.0 // Roughly 111.32km per degree of latitude

        // Generate random offset using polar coordinates
        val u = Random.nextDouble()
        val v = Random.nextDouble()

        // Square root of 'u' ensures an even distribution within the circle
        val w = radiusInDegrees * sqrt(u)
        val t = 2 * Math.PI * v

        val offsetLat = w * sin(t)
        // Adjust longitude offset based on latitude (longitude lines converge at poles)
        val offsetLng = w * cos(t) / cos(Math.toRadians(exactLocation.latitude))

        val noiseLat = exactLocation.latitude + offsetLat
        val noiseLng = exactLocation.longitude + offsetLng

        return LatLng(noiseLat, noiseLng)
    }
}