import SwiftUI
import Combine

import FirebaseFirestore

class FirebaseManager: ObservableObject {
    @Published var allUsers: [User] = []
    private var db = Firestore.firestore(database: "users")

    func startListening() {
        db.collection("users").addSnapshotListener { querySnapshot, error in
            if let error = error {
                print("Error fetching users: \(error)")
                return
            }
            
            // This turns the database data into our "User" list
            self.allUsers = querySnapshot?.documents.compactMap { document in
                try? document.data(as: User.self)
            } ?? []
        }
    }
}
