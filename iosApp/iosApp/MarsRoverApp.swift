import SwiftUI
import shared
import FirebaseCore
import AppTrackingTransparency

@main
struct MarsRoverApp: App {
    init() {
        // Initialize Firebase before anything else (required for Analytics, Crashlytics, Firestore)
        FirebaseApp.configure()
        // Initialize Koin dependency injection from shared module
        IosApp.shared.start()

        // Request App Tracking Transparency authorization (iOS 14+).
        // The prompt is deferred by 1 s so it appears after the app's first frame is rendered,
        // which is required by Apple's HIG. The status is used when ads are added in the future.
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            ATTrackingManager.requestTrackingAuthorization { status in
                // .authorized  — user allowed tracking; use personalized ads
                // .denied / .restricted — non-personalized ads only
                // .notDetermined — authorization not yet requested (should not happen here)
                // Wire into Google Mobile Ads SDK initialization when ads land on iOS.
                _ = status
            }
        }
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
