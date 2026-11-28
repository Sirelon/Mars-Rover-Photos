# Store screenshot reference

Checked 2026-11-28 on the official pages below. Re-open them before changing a size or adding a slot.

## Targets the compositor writes

| Target | Size | Console slot | Source capture |
| --- | --- | --- | --- |
| `app-store/iphone-6.9` | 1320x2868 | `APP_IPHONE_67` (iPhone 6.9", Dynamic Island large) | `iphone` 1320x2868 @3x (440x956 pt) |
| `app-store/iphone-6.5` | 1284x2778 | `APP_IPHONE_65` | the same `iphone` capture, on a smaller canvas |
| `app-store/ipad-13` | 2752x2064 landscape | `APP_IPAD_PRO_3GEN_129` (iPad 13") | `ipad` 2752x2064 @2x |
| `play-store/phone` | 1440x2560 | `phoneScreenshots` | `android-phone` 1080x2400 @2.625 |
| `play-store/tablet` | 2560x1440 | `sevenInchScreenshots` and `tenInchScreenshots` | `android-tablet` 2560x1600 @2x |

Language: English only (`en-US` on both stores), because the app is English only (AGENTS.md).

## Apple

Screenshot specifications,
https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/

- 1 to 10 screenshots per device size; `.jpeg`, `.jpg` or `.png`; no alpha channel.
- iPhone 6.9" accepts 1320x2868 (also 1290x2796, 1260x2736); 6.5" accepts 1284x2778 (also
  1242x2688); iPad 13" accepts 2064x2752 / 2752x2064 (also 2048x2732).
- iPad 13" is required when the app runs on iPad. Without a size, App Store Connect scales a larger
  provided set down.
- iPhone Duo: outer 1398x2034, inner 2007x2853. Required from April 2027 for apps built with the
  iOS 27.1 SDK or later. Not produced yet: the ASC API has no display type for it.
- The ASC API enum has no 6.9" or 13" value
  (https://developer.apple.com/documentation/appstoreconnectapi/screenshotdisplaytype): 6.9" goes
  into `APP_IPHONE_67`, 13" into `APP_IPAD_PRO_3GEN_129`.

App Review Guidelines, https://developer.apple.com/app-store/review/guidelines/:
- 2.3.3: screenshots show the app in use, not only title art, a login page or a splash screen;
  text and image overlays are allowed.
- 2.3.9: you need the rights to every image in a screenshot.
- 2.3.10: no names, icons or imagery of other mobile platforms. App Store images use the iPhone and
  generic tablet frames, never the Android one.

## Google Play

Add preview assets, https://support.google.com/googleplay/android-developer/answer/9866151

- Phone: 2 to 8, PNG or JPEG, up to 8 MB, each side 320 to 3,840 px, 9:16 or 16:9.
- Large screens (7" and 10" tablets, Chromebooks): at least 4, each side 1,080 to 7,680 px, 16:9 or 9:16.
- Foldable: 3 to 8. Not produced: `supply` has no foldable image type.
- Highly recommended (not mandatory): taglines at most 20% of the image; no "Best", "#1", "Top",
  "New", "Discount", "Sale" or download counts; no calls to action ("Download now", "Try now");
  full battery, Wi-Fi and signal, no extra notification icons.

Metadata policy, https://support.google.com/googleplay/android-developer/answer/9898842: screenshots
reflect the app's actual experience; no store-performance, ranking, price or promotion claims.

## NASA images

Images and Media usage guidelines, https://www.nasa.gov/nasa-brand-center/images-and-media/

- NASA content is generally not subject to copyright in the United States.
- Commercial use must not state or imply NASA endorsement. NASA insignia and logos are not public
  domain: keep the NASA "meatball" and worm out of captions and frames.
- NASA asks to be acknowledged as the source. The store description carries "Images courtesy of
  NASA/JPL-Caltech" and says the app is not affiliated with NASA.

So a caption may say where photos come from ("real rover photos") but never "official", "by NASA",
or anything that reads as NASA's product.

## Console state

Read it live with `bundle exec ruby store/tools/upload-screenshots.rb status`. After the
2026-11-28 upload: App Store 5.5.0 READY_FOR_SALE (locked, older set); draft 5.6.0
PREPARE_FOR_SUBMISSION with en-US `APP_IPHONE_67` (8), `APP_IPHONE_65` (8) and
`APP_IPAD_PRO_3GEN_129` (7), live with the next release; its Missions slot reads "Nine Mars
missions" (Phoenix). Play en-US: 8 phone, 7 seven-inch and 7 ten-inch screenshots, live; its
Missions slot still reads "Eight Mars missions", true until Phoenix ships. Once that release is on
Play, re-render `rovers` for the Android devices and run `play --apply`.
