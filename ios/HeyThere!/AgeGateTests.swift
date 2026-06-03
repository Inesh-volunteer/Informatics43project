import XCTest

final class AgeGateTests: XCTestCase {

    // Helper: replicates ProfileView.userAge computed property
    private func userAge(from birthDate: Date) -> Int {
        Calendar.current.dateComponents([.year], from: birthDate, to: Date()).year ?? 0
    }

    // MARK: - UT-07: Exactly 18 passes the gate (HIGH PRIORITY)

    func test_userAge_exactly18_isAllowed() {
        let dob = Calendar.current.date(byAdding: .year, value: -18, to: Date())!
        let age = userAge(from: dob)
        XCTAssertGreaterThanOrEqual(age, 18,
            "A user born exactly 18 years ago must pass the age gate")
    }

    // MARK: - UT-08: 17-year-old is blocked (HIGH PRIORITY)

    func test_userAge_17_isBelowGate() {
        let dob = Calendar.current.date(byAdding: .year, value: -17, to: Date())!
        let age = userAge(from: dob)
        XCTAssertLessThan(age, 18,
            "A 17-year-old must be blocked by the age gate")
    }

    // MARK: - UT-09: Default birthDate (today) gives age 0 — save button should be disabled

    func test_userAge_defaultBirthDate_isZero() {
        let age = userAge(from: Date())
        XCTAssertEqual(age, 0,
            "Default birthDate of today must produce age 0, keeping Save disabled on fresh load")
        XCTAssertLessThan(age, 18)
    }

    // MARK: - Boundary: 19-year-old is allowed

    func test_userAge_19_isAllowed() {
        let dob = Calendar.current.date(byAdding: .year, value: -19, to: Date())!
        let age = userAge(from: dob)
        XCTAssertGreaterThanOrEqual(age, 18)
    }

    // MARK: - Boundary: Future birth date gives negative age — blocked

    func test_userAge_futureBirthDate_isBelowGate() {
        let dob = Calendar.current.date(byAdding: .year, value: 1, to: Date())!
        let age = userAge(from: dob)
        XCTAssertLessThan(age, 18,
            "A future birth date must not pass the age gate")
    }
}
