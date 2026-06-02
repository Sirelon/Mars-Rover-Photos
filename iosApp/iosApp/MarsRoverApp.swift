import SwiftUI
import shared
import FirebaseCore
import AppTrackingTransparency
import GoogleMobileAds
import UserMessagingPlatform

@main
struct MarsRoverApp: App {
    init() {
        // Initialize Firebase before anything else (required for Analytics, Crashlytics, Firestore)
        FirebaseApp.configure()
        // Initialize Koin dependency injection from shared module
        IosApp.shared.start()

        // Keep screen on during testing
        UIApplication.shared.isIdleTimerDisabled = true

        // UMP must collect consent before MobileAds.start so the SDK can pick up the user's
        // choice; ATT must run before any ad request so IDFA personalization is honored.
        // The chain is deferred by 1s so the prompts appear after the app's first frame
        // (Apple HIG requirement for ATT).
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            Self.bootstrapAds()
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
