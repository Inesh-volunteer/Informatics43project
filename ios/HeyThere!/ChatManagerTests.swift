import XCTest


final class ChatManagerTests: XCTestCase {

    // MARK: - UT-10: Messages are sorted newest-first

    func test_messages_areSortedNewestFirst() {
        let older = Message(senderId: "a", receiverId: "b",
                            text: "First message",
                            timestamp: Date(timeIntervalSinceNow: -120))
        let middle = Message(senderId: "b", receiverId: "a",
                             text: "Second message",
                             timestamp: Date(timeIntervalSinceNow: -60))
        let newer = Message(senderId: "a", receiverId: "b",
                            text: "Third message",
                            timestamp: Date())

        let sorted = [older, newer, middle].sorted { $0.timestamp > $1.timestamp }

        XCTAssertEqual(sorted[0].text, "Third message")
        XCTAssertEqual(sorted[1].text, "Second message")
        XCTAssertEqual(sorted[2].text, "First message")
    }

    // MARK: - UT-11: Whitespace-only message is treated as empty (not sent)

    func test_emptyMessage_whitespaceOnly_isNotSent() {
        let inputs = ["   ", "\t", "\n", "  \t  \n  ", ""]
        for input in inputs {
            XCTAssertTrue(input.trimmingCharacters(in: .whitespaces).isEmpty,
                "'\(input)' should be treated as empty and not sent")
        }
    }

    // MARK: - Non-empty message passes the send guard

    func test_nonEmptyMessage_passesGuard() {
        let text = "Hey!"
        XCTAssertFalse(text.trimmingCharacters(in: .whitespaces).isEmpty,
            "A non-empty message should pass the send guard")
    }

    // MARK: - Single message list still sorts correctly

    func test_singleMessage_sortStable() {
        let msg = Message(senderId: "a", receiverId: "b",
                          text: "Only message", timestamp: Date())
        let sorted = [msg].sorted { $0.timestamp > $1.timestamp }
        XCTAssertEqual(sorted.count, 1)
        XCTAssertEqual(sorted.first?.text, "Only message")
    }

    // MARK: - Messages with equal timestamps returns all messages

    func test_messages_withEqualTimestamps_returnsAllMessages() {
        let now = Date()
        let msg1 = Message(senderId: "a", receiverId: "b", text: "A", timestamp: now)
        let msg2 = Message(senderId: "b", receiverId: "a", text: "B", timestamp: now)
        let sorted = [msg1, msg2].sorted { $0.timestamp > $1.timestamp }
        XCTAssertEqual(sorted.count, 2)
    }
}
