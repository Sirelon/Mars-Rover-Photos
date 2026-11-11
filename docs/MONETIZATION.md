# Monetization: support tiers, ads and consent

Read before touching the Support screen, RevenueCat, feature flags, the ad slot, or the UMP / ATT
prompts.

## Support tiers, ad-free and feature flags
"Support the developer" sells four tiers through RevenueCat (project `Mars Rover Photos`), one
package each (`support_small` … `support_huge`). Coffee (`support_small`, store product `tip_coffee`,
$0.99) is a consumable tip: buyable any number of times, each purchase removes ads for 30 days, and
purchases stack (`tipAdFreeUntil`). Lunch, Dinner and Feast (`support_medium` $1.99, `support_large`
$4.99, `support_huge` $9.99) are non-consumables that activate the `ad_free` entitlement for good and
restore on a new device. `tip_coffee` stays off that entitlement: RevenueCat would report it as
permanent, and App Review expects a permanent unlock to be a restorable non-consumable. The repository
derives the Coffee window from `CustomerInfo.nonSubscriptionTransactions`. `SupportRepository.isAdFree`
combines both, and the navigation root reads it to drop the ad slot. The UI keys icons and wording on the package id, not the
store product id, so a store-side rename never reaches the screen. Price, type and tier changes are
the owner's decision — propose, do not implement. The
Android and iOS entry points also skip the consent (UMP) and ATT prompts for supporters. Public SDK
keys are per build type: `BuildConfig.REVENUECAT_API_KEY` on Android, the `#if DEBUG` branch in
`MarsRoverApp.swift` on iOS — debug builds talk to RevenueCat's Test Store, so nothing is charged and
purchases can be made on any device with no store account setup.

User-facing copy says "support" or "tip", never "donation": both stores classify in-app donations as
a restricted category, and the purchase must not be tied to a cause.

The feature sits behind the Remote Config boolean `support_tiers_enabled` (`FeatureFlag.SUPPORT_TIERS`,
default `false`). The About row and the Support screen entry point are hidden while it is off; the
ad-free gate is not, so an owner keeps ad-free whatever the flag says. Flip it in the Firebase console
(project `mars-rover-photos`); debug builds pin it on (`FeatureFlag.enabledInDebug`) and refetch every
launch, release builds honour the remote value and refetch at most every 12 hours. The flag must be
on in production before a build carrying the in-app purchases is submitted to App Review, and the
review notes must say where the purchase UI is (About → Support the Developer): reviewers who cannot
reach an IAP reject the submission. Desktop has no Remote Config and no store: the flag is forced on and billing is faked so the
screen can be iterated through Compose Hot Reload.

## Ads & consent prompts
Banner ads (AdMob) sit in the app chrome on every non-fullscreen screen and load only once consent
is on record: `AndroidAdConsent.canRequestAds` on Android, `IosAdSlot.factory` on iOS. The consent
state is refreshed on every launch (`GdprHelper.init()` from `MainActivity`, `refreshConsent()` in
`MarsRoverApp.swift`), so a returning user gets ads at once. The prompts themselves — the UMP consent
form for EEA/UK users on both platforms, then App Tracking Transparency on iOS — wait for the
second rover the user opens, counted across sessions in `AppSettings` (`platform/ConsentPromptGate`,
opened by `RoversViewModel` on that tap and never at launch), so a first launch shows the rover
list and a whole first visit before any sheet. The iOS order is fixed: UMP before ATT before
`MobileAds.start`, and both prompts need the app foreground-active. Debug builds force EEA geography
so the form is always testable; the `hideAds` launch argument (store screenshots) skips the ads and
both prompts entirely.
