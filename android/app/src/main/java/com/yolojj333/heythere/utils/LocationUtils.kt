package com.yolojj333.heythere.utils

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object LocationUtils {

    /**
     * Generates a stable randomized location within a specified radius.
     * CRITICAL FIX: The offset is now permanently saved to disk so the user NEVER teleports
     * when the OS inevitably clears background RAM.
     */
    fun applyLocationNoise(context: Context, exactLocation: LatLng, radiusInMeters: Double = 500.0): LatLng {
        val prefs = context.getSharedPreferences("HeyTherePrefs", Context.MODE_PRIVATE)

        var offsetLat = prefs.getFloat("noise_offset_lat", 0f).toDouble()
        var offsetLng = prefs.getFloat("noise_offset_lng", 0f).toDouble()

        // If the offset doesn't exist on the hard drive yet, generate and save it
        if (!prefs.contains("noise_offset_lat")) {
            val radiusInDegrees = radiusInMeters / 111320.0
            val u = Random.nextDouble()
            val v = Random.nextDouble()

            val w = radiusInDegrees * sqrt(u)
            val t = 2 * Math.PI * v

            offsetLat = w * sin(t)
            offsetLng = w * cos(t)

            prefs.edit()
                .putFloat("noise_offset_lat", offsetLat.toFloat())
                .putFloat("noise_offset_lng", offsetLng.toFloat())
                .apply()
        }

        val noiseLat = exactLocation.latitude + offsetLat
        // Adjust longitude offset based on current latitude
        val noiseLng = exactLocation.longitude + (offsetLng / cos(Math.toRadians(exactLocation.latitude)))

        return LatLng(noiseLat, noiseLng)
    }
}