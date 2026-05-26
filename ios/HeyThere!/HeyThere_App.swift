import SwiftUI
import FirebaseCore

// This part tells the app how to connect to Firebase when it starts
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        return true
    }
}

@main
struct HeyThere_App: App {
    // This connects the Firebase setup above to your actual app
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView() // This tells the app to open ContentView first
        }
    }
}
