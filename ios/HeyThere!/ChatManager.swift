import FirebaseFirestore
import FirebaseAuth
import Combine

class ChatManager: ObservableObject {
    @Published var messages: [Message] = []
    private var db = Firestore.firestore(database: "users")
    private var listener: ListenerRegistration?

    func sendMessage(to receiverId: String, text: String) {
        guard let currentId = Auth.auth().currentUser?.uid else { return }
        let newMessage = Message(senderId: currentId, receiverId: receiverId, text: text, timestamp: Date())
        _ = try? db.collection("messages").addDocument(from: newMessage)
    }

    func listenForMessages() {
        if listener != nil { return }
        guard let currentId = Auth.auth().currentUser?.uid else { return }
        
        // This query finds messages you sent OR received
        listener = db.collection("messages")
            .whereField("receiverId", isEqualTo: currentId)
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                self.messages = docs.compactMap { doc in
                    var m = try? doc.data(as: Message.self)
                    m?.id = doc.documentID
                    return m
                }.sorted(by: { $0.timestamp > $1.timestamp })
            }
    }

    func deleteMessage(messageId: String) {
        db.collection("messages").document(messageId).delete()
    }

    func stopListening() {
        listener?.remove()
        listener = nil
    }
}
