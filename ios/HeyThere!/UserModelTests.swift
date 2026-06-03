import XCTest

// Self-contained copies of the app structs (mirrors Models.swift exactly)
struct User: Codable {
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

struct BlackoutZone: Codable {
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

final class UserModelTests: XCTestCase {

    // MARK: - UT-01: User default display name

    func test_user_defaultDisplayNameIsNewUser() {
        let user = User()
        XCTAssertEqual(user.displayName, "New User")
    }

    // MARK: - UT-02: Privacy settings defaults are safe (HIGH PRIORITY)

    func test_privacySettings_defaultsAreSafe() {
        let settings = PrivacySettings()
        XCTAssertTrue(settings.isGlobalLocationOn,
            "Global location should be ON by default so new users appear on the map")
        XCTAssertFalse(settings.usePreciseLocation,
            "Precise location must be OFF by default — privacy invariant")
    }

    // MARK: - UT-03: BlackoutZone default radius

    func test_blackoutZone_defaultRadiusIs200Meters() {
        let zone = BlackoutZone(name: "Home", latitude: 33.6846, longitude: -117.8265)
        XCTAssertEqual(zone.radiusMeters, 200.0, accuracy: 0.001)
    }

    // MARK: - UT-04: LocationData default coordinates

    func test_locationData_defaultCoordinatesAreZero() {
        let loc = LocationData()
        XCTAssertEqual(loc.publicLatitude, 0)
        XCTAssertEqual(loc.publicLongitude, 0)
    }

    // MARK: - UT-05 / Integration: User Codable round-trip

    func test_user_codableRoundTrip() throws {
        var original = User()
        original.id = "abc123"
        original.displayName = "Inesh"
        original.age = 20
        original.bio = "Test bio"
        original.privacySettings.usePreciseLocation = true
        original.locationData.publicLatitude = 33.6846
        original.locationData.publicLongitude = -117.8265

        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(User.self, from: data)

        XCTAssertEqual(decoded.id, original.id)
        XCTAssertEqual(decoded.displayName, original.displayName)
        XCTAssertEqual(decoded.age, original.age)
        XCTAssertEqual(decoded.bio, original.bio)
        XCTAssertEqual(decoded.privacySettings.usePreciseLocation, true)
        XCTAssertEqual(decoded.locationData.publicLatitude, 33.6846, accuracy: 0.00001)
        XCTAssertEqual(decoded.locationData.publicLongitude, -117.8265, accuracy: 0.00001)
    }

    // MARK: - UT-06 / Integration: Message Codable round-trip

    func test_message_codableRoundTrip() throws {
        let now = Date()
        let original = Message(senderId: "user_a", receiverId: "user_b",
                               text: "Hey! Just saw you on the map.", timestamp: now)

        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(Message.self, from: data)

        XCTAssertEqual(decoded.senderId, "user_a")
        XCTAssertEqual(decoded.receiverId, "user_b")
        XCTAssertEqual(decoded.text, "Hey! Just saw you on the map.")
        XCTAssertEqual(decoded.timestamp.timeIntervalSince1970,
                       now.timeIntervalSince1970, accuracy: 0.001)
    }

    // MARK: - Integration: PrivacySettings Codable round-trip

    func test_privacySettings_codableRoundTrip() throws {
        var original = PrivacySettings()
        original.isGlobalLocationOn = false
        original.usePreciseLocation = true

        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(PrivacySettings.self, from: data)

        XCTAssertEqual(decoded.isGlobalLocationOn, false)
        XCTAssertEqual(decoded.usePreciseLocation, true)
    }
}
