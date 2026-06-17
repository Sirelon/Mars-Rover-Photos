import SwiftUI
import shared
import FirebaseCore
import AppTrackingTransparency
import GoogleMobileAds
import UserMessagingPlatform

@main
struct MarsRoverApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @State private var didBootstrapAds = false

    init() {
        // Initialize Firebase before anything else (required for Analytics, Crashlytics, Firestore)
        FirebaseApp.configure()
        // Initialize Koin dependency injection from shared module
        #if DEBUG
        IosApp.shared.start(isDebug: true)
        #else
        IosApp.shared.start(isDebug: false)
        #endif

        // Keep screen on during testing
        UIApplication.shared.isIdleTimerDisabled = true
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
        // UMP must collect consent before MobileAds.start so the SDK can pick up the user's
        // choice; ATT must run before any ad request so IDFA personalization is honored.
        // We trigger this only once the scene is foreground-active: ATTrackingManager
        // silently returns .denied (no prompt shown) if requested while the app is not
        // active, and the UMP consent form has no view controller to present from.
        .onChange(of: scenePhase) { newPhase in
            guard newPhase == .active, !didBootstrapAds else { return }
            didBootstrapAds = true
            // Small delay so the prompts appear after the first frame (Apple HIG).
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                guard UIApplication.shared.applicationState == .active else {
                    // App slipped to the background during the delay — retry on next .active.
                    didBootstrapAds = false
                    return
                }
                Self.bootstrapAds()
            }
        }
    }

    private static func bootstrapAds() {
        let params = RequestParameters()
        params.isTaggedForUnderAgeOfConsent = false

        #if DEBUG
        let debugSettings = DebugSettings()
        debugSettings.geography = .EEA
        params.debugSettings = debugSettings
        #endif

        ConsentInformation.shared.requestConsentInfoUpdate(with: params) { umpError in
            if let umpError {
                NSLog("UMP requestConsentInfoUpdate error: \(umpError.localizedDescription)")
            }
            ConsentForm.loadAndPresentIfRequired(from: nil) { _ in
                guard ConsentInformation.shared.canRequestAds else {
                    IosAdSlot.shared.factory = nil
                    NSLog("UMP canRequestAds=false, skipping Mobile Ads start")
                    return
                }
                ATTrackingManager.requestTrackingAuthorization { _ in
                    MobileAds.shared.start { _ in
                        IosAdSlot.shared.factory = BannerAdFactoryImpl()
                    }
                }
            }
        }
    }
}
