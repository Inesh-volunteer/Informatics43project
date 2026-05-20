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
 * CRITICAL FIX: Only store the PUBLIC coordinates here. Never upload hidden precise data.
 */
data class LocationData(
    var publicLatitude: Double = 0.0,
    var publicLongitude: Double = 0.0,
    var lastUpdatedTimestamp: Long = 0L
)

/**
 * Manages the user's privacy toggles.
 */
data class PrivacySettings(
    var isGlobalLocationOn: Boolean = true,
    var usePreciseLocation: Boolean = false,
    var activeBlackoutZones: List<String> = emptyList(),
    var isBackgroundLocationEnabled: Boolean = false,
    var backgroundUpdateIntervalSeconds: Int = 60
)