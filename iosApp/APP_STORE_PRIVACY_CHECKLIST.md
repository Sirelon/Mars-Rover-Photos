# App Store Review — Ads / Privacy Checklist (iOS)

The app serves Google AdMob ads and accesses the IDFA, which is the most heavily
scrutinized category in App Review. The code-side requirements are implemented (see
git history); the items below are done **in App Store Connect / on the web** and must
match the code or the build is rejected.

## Code side — already done (for reference)
- [x] `NSUserTrackingUsageDescription` in `Info.plist`
- [x] ATT prompt (`ATTrackingManager.requestTrackingAuthorization`), fired only when scene is `.active`
- [x] UMP consent flow, ordering: **UMP consent → ATT → `MobileAds.start()`**
- [x] `PrivacyInfo.xcprivacy` privacy manifest (tracking=true, IDFA, UserDefaults reason)
- [x] Full Google `SKAdNetworkItems` list (50 IDs)
- [x] `GADApplicationIdentifier`

## B1 — App Privacy "nutrition label"  (App Store Connect → App Privacy)
Declarations MUST match `PrivacyInfo.xcprivacy` and the bundled SDKs (AdMob, Firebase):
- [ ] **Identifiers → Device ID**: collected · **linked to identity** · **Used for Tracking = YES** · purpose *Third-Party Advertising*
- [ ] **Usage Data → Product Interaction**: collected · Analytics  *(Firebase Analytics)*
- [ ] **Diagnostics → Crash Data**: collected · App Functionality  *(Crashlytics)*
- [ ] **Diagnostics → Performance Data**: collected · App Functionality  *(Firebase Performance)*
- [ ] **Purchases → Purchase History**: collected · App Functionality · not linked to identity ·
      not used for tracking  *(RevenueCat: receipt + anonymous app user id for the ad-free unlock)*
- [ ] Confirm the **"Used for Tracking"** toggle is ON for Device ID — this is what makes
      Apple *expect* the ATT prompt. ATT present but tracking not declared (or vice-versa) → rejection.

## B2 — Make the ATT prompt reviewable
- [ ] Reviewers (typically US region) must SEE the ATT dialog. Our prompt is gated behind
      UMP `canRequestAds`; in non-EEA regions `canRequestAds` defaults true, so it fires.
      Verify on a clean US-region device/simulator.
- [ ] App Review Information → **Notes**: add a line — "The app shows banner ads and
      requests App Tracking Transparency authorization ~1s after the first screen appears."

## B2b — Make the in-app purchases reviewable
- [ ] Remote Config `support_tiers_enabled` = true before submitting, or the Support row is hidden.
- [ ] App Review Information → **Notes**: "About → Support the Developer offers four one-time
      tips (Coffee, Lunch, Dinner, Feast) that remove ads. No account needed; Restore purchases
      is on the same screen."
- [ ] The four IAPs are submitted together with the version (first IAP review).

## B3 — Privacy Policy URL  (mandatory for ad-serving apps)
- [ ] App Store Connect → App Information → **Privacy Policy URL**.
- [ ] The policy text must mention AdMob, the IDFA, and third-party advertising.
- [ ] The policy text must mention RevenueCat and the in-app purchases (section "In-App Purchases").

## Flag (separate from ads, verify before release)
- [ ] `GoogleService-Info.plist` — confirm it's the real Firebase config, not a template,
      or Analytics/Crashlytics won't report.

## Final parity check before submitting
- [ ] Xcode → Product → Archive → **Generate Privacy Report** — confirm tracking + data
      types appear and there are no "missing manifest" warnings.
- [ ] Every data type in the Privacy Report is also declared in B1. Mismatch = rejection.
