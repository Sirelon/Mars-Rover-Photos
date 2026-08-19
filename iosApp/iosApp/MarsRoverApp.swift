import SwiftUI
import shared
import FirebaseCore
import FirebaseMessaging
import AppTrackingTransparency
import GoogleMobileAds
import UserMessagingPlatform
import UserNotifications

/// Owns the notification plumbing that has to exist from the first instant of launch.
///
/// This can't live in a Koin singleton on the Kotlin side: those are created lazily, so the
/// delegate would first exist when some screen injected it — long after a notification that
/// cold-launched the app had already been delivered and dropped.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        // APNs issues a device token only in response to registerForRemoteNotifications, and the
        // token does not survive the process — so an already-authorized user has to re-register on
        // every launch. Skipping this would leave Messaging.apnsToken nil from the second launch
        // onward, which is also what repairs a token lost to a backup restore or reinstall.
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                DispatchQueue.main.async { application.registerForRemoteNotifications() }
            default:
                break
            }
        }
        return true
    }

    /// Hands FCM the APNs token explicitly. Firebase's app-delegate proxy relies on swizzling the
    /// delegate class, which is not something a SwiftUI-lifecycle app can depend on — and when it
    /// misses, subscription and sends both report success while nothing is ever delivered.
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NSLog("APNs registration failed: \(error.localizedDescription)")
    }

    /// Matches Android: a push that arrives while the app is open is not shown. Someone already
    /// browsing Mars photos does not need to be told there are Mars photos.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([])
    }

    /// Routes a tap through the same deep-link path as `marsrover://` URLs opened from anywhere else.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let link = response.notification.request.content.userInfo["link"] as? String {
            Main_iosKt.pushDeepLink(urlString: link)
        }
        completionHandler()
    }
}

@main
struct MarsRoverApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
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

        // Screenshot capture passes `hideAds` as a launch argument to hide all ads. Tolerate the
        // forms it can arrive in: `-hideAds YES` (iOS maps it into UserDefaults) or a bare `hideAds`
        // argv token (how Maestro forwards launchApp arguments). Always false in normal use.
        let launchArgs = ProcessInfo.processInfo.arguments
        let hideAdsArg = launchArgs.contains { $0.range(of: "hideAds", options: .caseInsensitive) != nil }
        if UserDefaults.standard.bool(forKey: "hideAds") || hideAdsArg {
            BuildInfo.shared.hideAds = true
        }

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
            // Skip the ATT / consent prompts entirely during screenshot capture.
            guard newPhase == .active, !didBootstrapAds, !BuildInfo.shared.hideAds else { return }
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
