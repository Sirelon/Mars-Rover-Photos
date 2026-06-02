import GoogleMobileAds
import UIKit
import shared

/// Swift implementation of the Kotlin `AdBannerFactory` interface declared in the shared module.
/// Builds a `GADBannerView` configured with the production ad unit and presents it inside the
/// Compose `AdSlot` via the `UIKitView` interop.
final class BannerAdFactoryImpl: NSObject, AdBannerFactory {

    // MARK: - Production ad unit IDs
    // App ID registered in Info.plist: ca-app-pub-7516059448019339~1086903880

    /// Banner shown alongside list / grid screens (rovers, favorites, popular, about).
    private static let bannerListUnitID = "ca-app-pub-7516059448019339/7887281803"

    /// Banner shown alongside content / detail screens (photos grid, mission info).
    private static let bannerAtContentUnitID = "ca-app-pub-7516059448019339/6993836680"

    /// Banner shown inside the image gallery screen.
    private static let bannerAtImageUnitID = "ca-app-pub-7516059448019339/9920068688"

    func createBanner(widthPoints: Double) -> UIView {
        let width = CGFloat(widthPoints)
        let size = currentOrientationAnchoredAdaptiveBanner(width: width)
        let banner = BannerView(adSize: size)
        // The current single AdSlot placement lives in the main navigation shell,
        // shown alongside list/grid screens — use the List unit.
        banner.adUnitID = Self.bannerListUnitID
        banner.rootViewController = UIApplication.shared.topMostViewController()
        banner.load(Request())
        return banner
    }
}

private extension UIApplication {
    /// Best-effort lookup of the topmost UIViewController, mirroring the pattern used by
    /// `IosImageOperations` when presenting the share sheet.
    func topMostViewController() -> UIViewController? {
        let scene = connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? connectedScenes.compactMap { $0 as? UIWindowScene }.first
        guard var top = scene?.keyWindow?.rootViewController else { return nil }
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
