import GoogleMobileAds
import UIKit
import shared

/// Swift implementation of the Kotlin `AdBannerFactory` interface declared in the shared module.
/// Builds a `BannerView` configured with the production ad unit and presents it inside the
/// Compose `AdSlot` via the `UIKitView` interop.
final class BannerAdFactoryImpl: NSObject, AdBannerFactory, BannerViewDelegate {

    // MARK: - Production ad unit IDs
    // App ID registered in Info.plist: ca-app-pub-7516059448019339~1086903880

    /// Banner shown alongside list / grid screens (rovers, favorites, popular, about).
    private static let bannerListUnitID = "ca-app-pub-7516059448019339/7887281803"

    /// Banner shown alongside content / detail screens (photos grid, mission info).
    private static let bannerAtContentUnitID = "ca-app-pub-7516059448019339/6993836680"

    /// Banner shown inside the image gallery screen.
    private static let bannerAtImageUnitID = "ca-app-pub-7516059448019339/9920068688"

    private static let adFormat = "banner"

    /// The banner outlives the Compose slot that shows it.
    ///
    /// `AdSlot` is navigation chrome, so it leaves composition every time a fullscreen destination
    /// collapses the chrome — on this app's hottest path, every photo opened and closed. Letting
    /// the view go there discards an impression that has already been paid for and makes the
    /// chrome come back to an empty slot and a fresh request, so the loaded banner is kept here
    /// and handed back on re-attach. Keyed by width, so rotating still gets a correctly sized one.
    private var retained: [Int: BannerView] = [:]

    /// Widths whose banner has a request in flight or an ad loaded. A failed load drops out again
    /// so the next attach retries, instead of leaving the slot empty for the rest of the session.
    private var requested: Set<Int> = []

    func createBanner(widthPoints: Double) -> UIView {
        let key = Int(widthPoints.rounded())
        let banner = retained[key] ?? makeBanner(widthPoints: widthPoints, key: key)

        // Compose is re-attaching it, and whichever controller it was created against may be gone.
        banner.removeFromSuperview()
        banner.rootViewController = UIApplication.shared.topMostViewController()

        if !requested.contains(key) {
            requested.insert(key)
            banner.load(Request())
        }
        return banner
    }

    private func makeBanner(widthPoints: Double, key: Int) -> BannerView {
        let size = currentOrientationAnchoredAdaptiveBanner(width: CGFloat(widthPoints))
        let banner = BannerView(adSize: size)
        // The current single AdSlot placement lives in the main navigation shell,
        // shown alongside list/grid screens — use the List unit.
        banner.adUnitID = Self.bannerListUnitID
        banner.delegate = self
        banner.paidEventHandler = { [weak banner] adValue in
            // The only report of what an impression was actually worth for this user and session;
            // the AdMob console breaks revenue down by day and ad unit, and no further.
            Main_iosKt.trackAdImpression(
                adSource: banner?.responseInfo?.loadedAdNetworkResponseInfo?.adSourceName ?? "unknown",
                adFormat: Self.adFormat,
                adUnitName: Self.bannerListUnitID,
                value: adValue.value.doubleValue,
                currencyCode: adValue.currencyCode,
                precision: Self.precisionName(adValue.precision)
            )
        }
        retained[key] = banner
        return banner
    }

    private static func precisionName(_ precision: AdValuePrecision) -> String {
        switch precision {
        case .estimated: return "estimated"
        case .publisherProvided: return "publisher_provided"
        case .precise: return "precise"
        default: return "unknown"
        }
    }

    private func key(of bannerView: BannerView) -> Int? {
        retained.first { $0.value === bannerView }?.key
    }

    // MARK: - BannerViewDelegate

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        Main_iosKt.trackAdEvent(event: "ad_loaded", params: ["ad_format": Self.adFormat])
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        if let key = key(of: bannerView) {
            requested.remove(key)
        }
        let nsError = error as NSError
        Main_iosKt.trackAdEvent(
            event: "ad_load_failed",
            params: [
                "ad_format": Self.adFormat,
                "error_code": String(nsError.code),
                // GA4 truncates string params at 100 characters, and the head carries the cause.
                "reason": String(nsError.localizedDescription.prefix(100)),
            ]
        )
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
