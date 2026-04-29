package com.yolojj333.heythere.models

/**
 * Represents a user profile in the Beacon app.
 */
data class User(
    var userId: String = "",
    var displayName: String = "",
    var age: Int = 18,
    var gender: String = "Not Specified",
    var bio: String = "",
    var profileImageUrls: List<String> = emptyList(),
    var subscribedTags: List<String> = emptyList(),
    var locationData: LocationData = LocationData(),
    var privacySettings: PrivacySettings = PrivacySettings()
)

/**
 * Holds the user's geographic state.
 */
data class LocationData(
    var preciseLatitude: Double = 0.0,
    var preciseLongitude: Double = 0.0,
    var noiseLatitude: Double = 0.0,
    var noiseLongitude: Double = 0.0,
    var lastUpdatedTimestamp: Long = 0L
)

/**
 * Manages the user's privacy toggles.
 */
data class PrivacySettings(
    var isGlobalLocationOn: Boolean = true,
    var usePreciseLocation: Boolean = false,
    var activeBlackoutZones: List<String> = emptyList()
)