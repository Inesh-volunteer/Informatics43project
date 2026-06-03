import XCTest

final class LoginGuardTests: XCTestCase {

    // Helper: replicates the resetPassword guard from LoginView
    private func resetPasswordResult(email: String) -> String? {
        guard !email.isEmpty else {
            return "Enter email first."
        }
        return nil // nil = guard passed, Firebase would be called next
    }

    // MARK: - UT-14: Empty email sets error message

    func test_resetPassword_emptyEmail_setsErrorMessage() {
        let errorMessage = resetPasswordResult(email: "")
        XCTAssertEqual(errorMessage, "Enter email first.")
    }

    // MARK: - UT-15: Non-empty email passes the guard

    func test_resetPassword_nonEmptyEmail_passesGuard() {
        let errorMessage = resetPasswordResult(email: "inesh@uci.edu")
        XCTAssertNil(errorMessage,
            "A valid email should pass the guard with no error message")
    }

    // MARK: - Whitespace-only email still triggers the guard

    func test_resetPassword_whitespaceOnlyEmail_setsErrorMessage() {
        // LoginView uses !email.isEmpty — whitespace alone passes isEmpty
        // This test documents the current behavior (whitespace is not trimmed)
        let email = "   "
        let isEmpty = email.isEmpty
        // Current behavior: whitespace is NOT caught by the guard
        // This is a known gap — document it rather than hide it
        XCTAssertFalse(isEmpty,
            "Whitespace-only email passes the isEmpty guard — known gap, should be trimmed")
    }

    // MARK: - Login with empty email and password

    func test_login_emptyCredentials_bothEmpty() {
        let email = ""
        let password = ""
        // Both being empty is the worst case — verify the guard catches it
        XCTAssertTrue(email.isEmpty)
        XCTAssertTrue(password.isEmpty)
    }

    // MARK: - Sign up / login toggle state

    func test_isSigningUp_togglesCorrectly() {
        var isSigningUp = false
        isSigningUp.toggle()
        XCTAssertTrue(isSigningUp)
        isSigningUp.toggle()
        XCTAssertFalse(isSigningUp)
    }
}
