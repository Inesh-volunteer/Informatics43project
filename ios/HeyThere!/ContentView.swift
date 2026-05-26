import SwiftUI
import FirebaseAuth
import FirebaseFirestore

struct ContentView: View {
    @State private var isUserLoggedIn = Auth.auth().currentUser != nil
    @State private var selectedTab = "map"
    
    var body: some View {
        Group {
            if isUserLoggedIn {
                TabView(selection: $selectedTab) {
                    MapView()
                        .tabItem { Label("Map", systemImage: "map") }
                        .tag("map")
                    
                    NavigationStack {
                        MessagesListView()
                    }
                    .tabItem { Label("Messages", systemImage: "bubble.left") }
                    .tag("messages")
                    
                    ProfileView()
                        .tabItem { Label("Profile", systemImage: "person") }
                        .tag("profile")
                        
                    SettingsView()
                        .tabItem { Label("Settings", systemImage: "gear") }
                        .tag("settings")
                }
                .accentColor(.orange)
            } else {
                LoginView()
            }
        }
        .onAppear {
            // Adding '_ =' silences the "unused" warning
            _ = Auth.auth().addStateDidChangeListener { _, user in
                self.isUserLoggedIn = (user != nil)
            }
        }
    }
}

struct MessagesListView: View {
    @StateObject var chatManager = ChatManager()
    @State private var showingUserSearch = false // Track if search is open
    
    var body: some View {
        List(chatManager.messages) { msg in
            // Logic to determine the "Other Person's" ID
            let chatPartnerId = msg.senderId == Auth.auth().currentUser?.uid ? msg.receiverId : msg.senderId
            
            NavigationLink(destination: ChatDetailView(recipientId: chatPartnerId, recipientName: "Chat")) {
                VStack(alignment: .leading, spacing: 5) {
                    Text(msg.text)
                        .font(.body)
                        .lineLimit(1)
                    Text(msg.timestamp, style: .time)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .padding(.vertical, 4)
            }
        }
        .navigationTitle("Messages")
        // THE NEW FEATURE: The button in the top corner
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showingUserSearch = true
                } label: {
                    Image(systemName: "square.and.pencil")
                        .foregroundColor(.orange)
                }
            }
        }
        // This popup will appear when you tap the button
        .sheet(isPresented: $showingUserSearch) {
            NavigationStack {
                VStack {
                    Text("Search for users to message...")
                        .foregroundColor(.gray)
                        .padding()
                    Spacer()
                }
                .navigationTitle("New Message")
                .toolbar {
                    Button("Done") { showingUserSearch = false }
                }
            }
        }
        .onAppear {
            chatManager.listenForMessages()
        }
        .onDisappear {
            chatManager.stopListening()
        }
    }
}
