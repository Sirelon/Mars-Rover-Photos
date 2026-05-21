import GoogleMobileAds
import UIKit
import shared

/// Swift implementation of the Kotlin `AdBannerFactory` interface declared in the shared module.
/// Builds a `GADBannerView` configured with the test ad unit and presents it inside the Compose
/// `AdSlot` via the `UIKitView` interop.
final class BannerAdFactoryImpl: NSObject, AdBannerFactory {

    /// Universal Google test banner unit for iOS. Swap for the production unit
    /// once the app's iOS AdMob entry is provisioned.
    static let testBannerUnitID = "ca-app-pub-3940256099942544/2934735716"

    func createBanner(widthPoints: Double) -> UIView {
        let width = CGFloat(widthPoints)
        let size = GADCurrentOrientationAnchoredAdaptiveBannerAdSizeWithWidth(width)
        let banner = GADBannerView(adSize: size)
        banner.adUnitID = Self.testBannerUnitID
        banner.rootViewController = UIApplication.shared.topMostViewController()
        banner.load(GADRequest())
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
