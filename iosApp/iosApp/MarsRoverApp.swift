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
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

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

    /// The only safe moment to subscribe to a topic. Asking FCM for a token before APNs has issued
    /// its device token fails outright ("No APNS token specified before fetching FCM Token") rather
    /// than waiting, so the app cannot subscribe from the opt-in tap or from launch — both run
    /// while APNs registration is still in flight. This also fires on token rotation.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard fcmToken != nil else { return }
        Main_iosKt.onFcmRegistrationTokenAvailable()
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
            // Keep screen on during Maestro runs only; never for real users.
            UIApplication.shared.isIdleTimerDisabled = true
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
        // Consent state is refreshed once the scene is foreground-active, after the first frame.
        // The prompts it may lead to (UMP form, ATT) wait for ConsentPromptGate on the Kotlin side
        // to open (the second rover tap, counted across sessions), so a first launch shows the rover
        // list and a whole first visit before any sheet. Screenshot capture shows no ads and so
        // collects no consent at all.
        .onChange(of: scenePhase) { newPhase in
            guard newPhase == .active, !didBootstrapAds, !BuildInfo.shared.hideAds else { return }
            didBootstrapAds = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                guard UIApplication.shared.applicationState == .active else {
                    // App slipped to the background during the delay — retry on next .active.
                    didBootstrapAds = false
                    return
                }
                Self.refreshConsent()
            }
        }
    }

    /// Refreshes the UMP consent state. A user whose consent is already on record and whose ATT
    /// answer is known gets ads at once; anyone who still has a sheet to see waits for the gate.
    private static func refreshConsent() {
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
            let needsConsentForm = ConsentInformation.shared.consentStatus == .required
            let needsTrackingPrompt = ATTrackingManager.trackingAuthorizationStatus == .notDetermined
            if needsConsentForm || needsTrackingPrompt {
                Main_iosKt.onConsentPromptsAllowed { presentPrompts() }
            } else {
                startAdsIfAllowed()
            }
        }
    }

    /// UMP first, then ATT, then the SDK: UMP must collect consent before MobileAds.start so the
    /// SDK picks up the choice, and ATT must precede the first ad request so IDFA personalization
    /// is honored. Both need the app active — ATTrackingManager silently answers .denied otherwise,
    /// and the consent form has no view controller to present from — so a tap that somehow lands
    /// while inactive is retried rather than spent.
    private static func presentPrompts() {
        guard UIApplication.shared.applicationState == .active else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { presentPrompts() }
            return
        }
        ConsentForm.loadAndPresentIfRequired(from: nil) { _ in
            guard ConsentInformation.shared.canRequestAds else {
                IosAdSlot.shared.factory = nil
                NSLog("UMP canRequestAds=false, skipping Mobile Ads start")
                return
            }
            // Returns the recorded answer without UI when the status is already determined.
            ATTrackingManager.requestTrackingAuthorization { _ in
                startAds()
            }
        }
    }

    private static func startAdsIfAllowed() {
        guard ConsentInformation.shared.canRequestAds else {
            IosAdSlot.shared.factory = nil
            NSLog("UMP canRequestAds=false, skipping Mobile Ads start")
            return
        }
        startAds()
    }

    private static func startAds() {
        MobileAds.shared.start { _ in
            IosAdSlot.shared.factory = BannerAdFactoryImpl()
        }
    }
}
