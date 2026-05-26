import Foundation
import CoreLocation
import Combine
import FirebaseFirestore
import FirebaseAuth

class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var userLocation: CLLocation?
    
    override init() {
        super.init()
        manager.delegate = self
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last, let uid = Auth.auth().currentUser?.uid else { return }
        self.userLocation = location
        
        // Matches friend's setup: collection "users", document is UID
        let data: [String: Any] = [
            "locationData": [
                "publicLatitude": location.coordinate.latitude,
                "publicLongitude": location.coordinate.longitude
            ]
        ]
        Firestore.firestore(database: "users").collection("users").document(uid).setData(data, merge: true)
    }
}
