import SwiftUI
import FirebaseFirestore
import FirebaseAuth

struct ProfileView: View {
    @State private var displayName: String = ""
    @State private var userBio: String = ""
    @State private var birthDate = Date()
    @State private var interestTags: [String] = []
    @State private var newTag: String = ""
    @State private var isSaving = false
    @State private var errorMessage = ""

    var userAge: Int {
        Calendar.current.dateComponents([.year], from: birthDate, to: Date()).year ?? 0
    }

    var body: some View {
        Form {
            Section(header: Text("Public Profile")) {
                TextField("Display Name", text: $displayName)
                DatePicker("Birthday", selection: $birthDate, displayedComponents: .date)
                TextEditor(text: $userBio).frame(height: 80)
            }

            // INTERESTS SECTION: Only allowed if 18+
            Section(header: Text("Interests")) {
                if userAge >= 18 {
                    HStack {
                        TextField("Add tag (e.g. Hiking)", text: $newTag)
                        Button("Add") {
                            if !newTag.isEmpty {
                                interestTags.append(newTag)
                                newTag = ""
                            }
                        }
                    }
                    // List the added tags
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(interestTags, id: \.self) { tag in
                                Text("#\(tag)")
                                    .padding(8)
                                    .background(Color.orange.opacity(0.1))
                                    .foregroundColor(.orange)
                                    .cornerRadius(8)
                            }
                        }
                    }
                } else {
                    Text("⚠️ Interests are locked for users under 18.")
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Section {
                Button(action: saveProfile) {
                    if isSaving { ProgressView() } else { Text("Save Profile").bold() }
                }
                .disabled(userAge < 18 || isSaving)
                .listRowBackground(userAge >= 18 ? Color.orange : Color.gray.opacity(0.3))
                .foregroundColor(.white)
            }
        }
        .navigationTitle("Edit Profile")
        .onAppear(perform: fetchUserData)
    }

    func fetchUserData() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore(database: "users").collection("users").document(uid).getDocument { snap, _ in
            if let data = snap?.data() {
                self.displayName = data["displayName"] as? String ?? ""
                self.userBio = data["bio"] as? String ?? ""
                self.interestTags = data["subscribedTags"] as? [String] ?? []
            }
        }
    }

    func saveProfile() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        isSaving = true
        let userData: [String: Any] = [
            "displayName": displayName,
            "age": userAge,
            "bio": userBio,
            "subscribedTags": interestTags // Syncs with friend's tag system
        ]
        Firestore.firestore(database: "users").collection("users").document(uid).setData(userData, merge: true) { _ in isSaving = false }
    }
    
    func saveInterests(tags: [String]) {
            guard let uid = Auth.auth().currentUser?.uid else { return }
            
            isSaving = true
            let db = Firestore.firestore(database: "users")
            db.collection("users").document(uid).setData([
                "interests": tags,
                "lastUpdated": Timestamp(date: Date())
            ], merge: true) { error in
                // Stop the "hanging" state
                isSaving = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                } else {
                    print("✅ Saved to Firebase!")
                }
            }
        }
}
