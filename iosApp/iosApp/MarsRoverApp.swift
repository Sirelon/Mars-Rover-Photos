import SwiftUI
import shared
import FirebaseCore

@main
struct MarsRoverApp: App {
    init() {
        // Initialize Firebase before anything else (required for Analytics, Crashlytics, Firestore)
        FirebaseApp.configure()
        // Initialize Koin dependency injection from shared module
        IosApp.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Deep link handling (ticket 6.4)
                // Forwards marsrover:// URLs to Kotlin for parsing and navigation.
                .onOpenURL { url in
                    Main_iosKt.pushDeepLink(urlString: url.absoluteString)
                }
        }
    }
}
