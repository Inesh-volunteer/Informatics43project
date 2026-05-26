import SwiftUI
import FirebaseAuth

struct SettingsView: View {
    var body: some View {
        NavigationStack {
            List {
                Section(header: Text("Account Actions")) {
                    Button(role: .destructive) {
                        do {
                            try Auth.auth().signOut()
                        } catch {
                            print("Error signing out")
                        }
                    } label: {
                        HStack {
                            Text("Sign Out")
                            Spacer()
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                        }
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}
