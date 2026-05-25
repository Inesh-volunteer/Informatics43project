package com.yolojj333.heythere.models

/**
 * Represents a user profile in the HeyThere app.
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
    var publicLatitude: Double = 0.0,
    var publicLongitude: Double = 0.0,
    var lastUpdatedTimestamp: Long = 0L
)

/**
 * Represents a specific geographic area where the user's location is hidden.
 */
data class BlackoutZone(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var radiusMeters: Double = 200.0 // Default 200m radius
)

/**
 * Manages the user's privacy toggles.
 */
data class PrivacySettings(
    var isGlobalLocationOn: Boolean = true,
    var usePreciseLocation: Boolean = false,
    var isBackgroundLocationEnabled: Boolean = false,
    var backgroundUpdateIntervalSeconds: Int = 60,
    var activeBlackoutZones: List<BlackoutZone> = emptyList() // Changed from List<String>
)