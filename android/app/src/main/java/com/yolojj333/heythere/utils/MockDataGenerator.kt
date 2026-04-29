package com.yolojj333.heythere.utils

import com.google.android.gms.maps.model.LatLng
import com.yolojj333.heythere.models.LocationData
import com.yolojj333.heythere.models.User
import kotlin.random.Random

object MockDataGenerator {

    private val sampleNames = listOf("Alex", "Jordan", "Taylor", "Casey", "Morgan", "Riley")
    private val sampleTags = listOf("Hiking", "Coding", "Coffee", "Photography", "Volleyball")

    /**
     * Generates a list of mock users scattered around a center point.
     */
    fun generateMockUsers(centerPoint: LatLng, count: Int = 10): List<User> {
        val mockUsers = mutableListOf<User>()

        for (i in 1..count) {
            // Scatter them within roughly 5km of the center point
            val offsetLat = (Random.nextDouble() - 0.5) * 0.05
            val offsetLng = (Random.nextDouble() - 0.5) * 0.05

            val exactLocation = LatLng(centerPoint.latitude + offsetLat, centerPoint.longitude + offsetLng)

            // Apply our Noise Algorithm so some users show exact, some show noise
            val isPrecise = Random.nextBoolean()
            val noiseLocation = LocationUtils.applyLocationNoise(exactLocation)

            val locationData = LocationData(
                preciseLatitude = exactLocation.latitude,
                preciseLongitude = exactLocation.longitude,
                noiseLatitude = noiseLocation.latitude,
                noiseLongitude = noiseLocation.longitude
            )

            // Pick 1 to 3 random tags for this user
            val shuffledTags = sampleTags.shuffled()
            val numTags = Random.nextInt(1, 4)
            val userTags = shuffledTags.take(numTags)

            val user = User(
                userId = "mock_$i",
                displayName = sampleNames.random(),
                age = Random.nextInt(18, 35),
                subscribedTags = userTags,
                locationData = locationData,
                privacySettings = com.yolojj333.heythere.models.PrivacySettings(
                    usePreciseLocation = isPrecise
                )
            )
            mockUsers.add(user)
        }
        return mockUsers
    }
}