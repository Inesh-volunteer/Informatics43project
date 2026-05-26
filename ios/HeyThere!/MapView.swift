import SwiftUI
import MapKit
import FirebaseFirestore

struct MapView: View {
    @StateObject var locationManager = LocationManager()
    @StateObject var chatManager = ChatManager()
    @State private var nearbyUsers: [UserPin] = []
    @State private var position: MapCameraPosition = .userLocation(fallback: .automatic)
    @State private var searchRadius: Double = 14.0

    var body: some View {
        ZStack(alignment: .bottom) {
            Map(position: $position) {
                UserAnnotation()
                ForEach(nearbyUsers) { user in
                    Annotation(user.name, coordinate: user.coordinate) {
                        Image(systemName: "mappin.circle.fill")
                            .resizable()
                            .frame(width: 35, height: 35)
                            .foregroundColor(.orange)
                            .background(Circle().fill(.white))
                    }
                }
            }
            .ignoresSafeArea()

            // Sliding Panel
            VStack(spacing: 0) {
                Capsule().fill(.gray.opacity(0.5)).frame(width: 40, height: 5).padding(.top, 10)
                
                VStack(alignment: .leading) {
                    Text("Nearby Users").font(.headline).bold()
                    Slider(value: $searchRadius, in: 1...50).tint(.orange)
                    
                    ScrollView {
                        ForEach(nearbyUsers) { user in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(user.name).bold()
                                    Text("Active now").font(.caption).foregroundColor(.green)
                                }
                                Spacer()
                                // The "Say Hey" Button
                                Button("Say Hey") {
                                    chatManager.sendMessage(to: user.id, text: "Hey! Just saw you on the map.")
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(.orange)
                                .font(.caption.bold())
                            }
                            .padding(.vertical, 8)
                        }
                    }
                }
                .padding()
                // CRITICAL FIX: This padding keeps the buttons from hiding behind the Tab Bar
                .padding(.bottom, 90)
            }
            .frame(height: 420)
            .background(Color(UIColor.systemBackground))
            .cornerRadius(25)
            .shadow(radius: 10)
            .offset(y: 150) // Tucks the panel so it doesn't block the map view
        }
        .onAppear(perform: fetchAllUsers)
    }

    func fetchAllUsers() {
        Firestore.firestore(database: "users").collection("users").addSnapshotListener { snapshot, _ in
            guard let docs = snapshot?.documents else { return }
            self.nearbyUsers = docs.compactMap { doc in
                let data = doc.data()
                let loc = data["locationData"] as? [String: Any]
                let lat = loc?["publicLatitude"] as? Double ?? 0
                let lon = loc?["publicLongitude"] as? Double ?? 0
                if lat == 0 { return nil }
                return UserPin(id: doc.documentID, name: data["displayName"] as? String ?? "User", coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lon))
            }
        }
    }
}
