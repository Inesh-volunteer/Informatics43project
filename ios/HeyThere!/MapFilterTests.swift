import XCTest
import CoreLocation

struct UserPin: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
}

final class MapFilterTests: XCTestCase {

    // Helper: replicates the compactMap filter from MapView.fetchAllUsers
    private func applyFilter(rawData: [[String: Any]]) -> [UserPin] {
        return rawData.compactMap { data in
            let loc = data["locationData"] as? [String: Any]
            let lat = loc?["publicLatitude"] as? Double ?? 0
            let lon = loc?["publicLongitude"] as? Double ?? 0
            if lat == 0 { return nil }
            return UserPin(
                id: data["id"] as? String ?? "",
                name: data["displayName"] as? String ?? "User",
                coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lon)
            )
        }
    }

    // MARK: - UT-12: Zero-latitude users are filtered out (HIGH PRIORITY)

    func test_fetchAllUsers_filtersOutZeroLatitude() {
        let rawData: [[String: Any]] = [
            ["id": "user1", "displayName": "Alice",
             "locationData": ["publicLatitude": 33.6846, "publicLongitude": -117.8265]],
            ["id": "user2", "displayName": "Ghost",
             "locationData": ["publicLatitude": 0.0, "publicLongitude": 0.0]]
        ]
        let pins = applyFilter(rawData: rawData)
        XCTAssertEqual(pins.count, 1)
        XCTAssertEqual(pins.first?.name, "Alice")
    }

    // MARK: - UT-13: Non-zero latitude users are included

    func test_fetchAllUsers_includesNonZeroLatitude() {
        let rawData: [[String: Any]] = [
            ["id": "user1", "displayName": "Bob",
             "locationData": ["publicLatitude": 33.6846, "publicLongitude": -117.8265]]
        ]
        let pins = applyFilter(rawData: rawData)
        XCTAssertEqual(pins.count, 1)
        XCTAssertEqual(pins.first?.id, "user1")
    }

    // MARK: - All zero-lat users removed from a mixed list

    func test_fetchAllUsers_allZeroLat_returnsEmpty() {
        let rawData: [[String: Any]] = [
            ["id": "u1", "displayName": "Ghost1",
             "locationData": ["publicLatitude": 0.0, "publicLongitude": 0.0]],
            ["id": "u2", "displayName": "Ghost2",
             "locationData": ["publicLatitude": 0.0, "publicLongitude": 10.0]]
        ]
        let pins = applyFilter(rawData: rawData)
        XCTAssertEqual(pins.count, 0,
            "All users with lat==0 must be filtered regardless of longitude")
    }

    // MARK: - Missing locationData field is treated as zero and filtered

    func test_fetchAllUsers_missingLocationData_isFiltered() {
        let rawData: [[String: Any]] = [
            ["id": "u1", "displayName": "NoLocation"]
        ]
        let pins = applyFilter(rawData: rawData)
        XCTAssertEqual(pins.count, 0,
            "A user with no locationData should default to lat=0 and be filtered out")
    }

    // MARK: - Coordinates are correctly mapped to UserPin

    func test_fetchAllUsers_coordinatesAreCorrect() {
        let rawData: [[String: Any]] = [
            ["id": "u1", "displayName": "Inesh",
             "locationData": ["publicLatitude": 33.6846, "publicLongitude": -117.8265]]
        ]
        let pins = applyFilter(rawData: rawData)
        
        XCTAssertEqual(Double(pins.first!.coordinate.latitude), 33.6846, accuracy: 0.0001)
        XCTAssertEqual(Double(pins.first!.coordinate.longitude), -117.8265, accuracy: 0.0001)
    }
}
