import SwiftUI
import shared
import FirebaseCore

@main
struct MarsRoverApp: App {
    init() {
        // Initialize Firebase before anything else (required for Analytics, Crashlytics, Firestore)
        FirebaseApp.configure()
        // Initialize Koin dependency injection from shared module
        KoinInitKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
