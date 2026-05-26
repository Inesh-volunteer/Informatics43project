import Foundation
import CoreLocation

// Main User data for the database
struct User: Identifiable, Codable {
    var id: String = ""
    var displayName: String = "New User"
    var age: Int = 0
    var bio: String = ""
    var locationData: LocationData = LocationData()
    var privacySettings: PrivacySettings = PrivacySettings()
}

struct LocationData: Codable {
    var publicLatitude: Double = 0
    var publicLongitude: Double = 0
}

struct PrivacySettings: Codable {
    var isGlobalLocationOn: Bool = true
    var usePreciseLocation: Bool = false
}

// For the orange 'H' pins on the map
struct UserPin: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
}

struct BlackoutZone: Identifiable, Codable {
    var id = UUID()
    var name: String
    var latitude: Double
    var longitude: Double
    var radiusMeters: Double = 200.0
}

struct Message: Identifiable, Codable {
    var id: String?
    var senderId: String
    var receiverId: String
    var text: String
    var timestamp: Date
}
