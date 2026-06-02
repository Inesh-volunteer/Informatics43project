import XCTest
import Combine
@testable import HeyThere

final class ChatManagerTests: XCTestCase {
    var cancellables = Set<AnyCancellable>()

    func testInitialState() {
        let manager = ChatManager()
        XCTAssertTrue(manager.messages.isEmpty, "messages should start empty")
    }

    func testSendMessage_noAuth_doesNothing() {
        let manager = ChatManager()
        // With no authenticated user in the test environment, sendMessage should return early and not crash
        manager.sendMessage(to: "receiver", text: "Hello")
        XCTAssertTrue(manager.messages.isEmpty, "sendMessage without auth should not add to local messages")
    }

    func testListenForMessages_noAuth_doesNothing() {
        let manager = ChatManager()
        // With no authenticated user, listenForMessages should return early and not crash
        manager.listenForMessages()
        XCTAssertTrue(manager.messages.isEmpty, "listenForMessages without auth should not populate messages")
    }

    func testStopListening_isSafeToCall() {
        let manager = ChatManager()
        // stopListening should be safe to call even if nothing is listening
        manager.stopListening()
        manager.stopListening()
        XCTAssertTrue(manager.messages.isEmpty, "stopListening should not modify messages")
    }

    func testPublishedMessages_emitsWhenSet() {
        let manager = ChatManager()
        let exp = expectation(description: "messages published")
        var receivedCount: Int?

        manager.$messages
            .dropFirst() // ignore the initial value
            .sink { msgs in
                receivedCount = msgs.count
                exp.fulfill()
            }
            .store(in: &cancellables)

        manager.messages = [Message(id: "1", senderId: "a", receiverId: "b", text: "hi", timestamp: Date())]

        waitForExpectations(timeout: 1.0)
        XCTAssertEqual(receivedCount, 1)
    }
}
