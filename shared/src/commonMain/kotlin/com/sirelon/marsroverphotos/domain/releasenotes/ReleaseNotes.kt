package com.sirelon.marsroverphotos.domain.releasenotes

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

// Version history shown in the What's New screen.
//
// Reconstructed from git history — NOT from commit messages, which are unreliable in this repo
// (the v1.3.0 release commit is titled "Update gradle" but shipped Popular Photos). Every entry
// was derived from the diff of its release's commit range. Editorial decisions, including
// deliberate omissions, are recorded in docs/release-notes/EDITORIAL_NOTES.md.
//
// Regenerate with the `release-notes` skill. Releases with nothing user-facing are omitted by
// design — do not add an entry to fill a gap.

val RELEASES: ImmutableList<Release> = persistentListOf(
    Release(
        version = "5.0.0",
        date = LocalDate(2026, 6, 29),
        changes = persistentListOf(
            Release.Change(
                id = "mission_info_redesign",
                type = ChangeType.MISSION_INFO,
                title = "Mission Info",
                summary = "A richer look at every rover's mission",
                detail = "Mission Info now opens with a full-width hero image, a scrollable mission timeline, and cameras you can expand one at a time. A Browse Photos button takes you straight into that rover's feed, and the fun fact changes each time you visit. On a wide window the whole screen splits into two panels.",
            ),
            Release.Change(
                id = "seamless_photo_transitions",
                type = ChangeType.PHOTO_VIEWER,
                title = "Seamless Photos",
                summary = "Opening a photo feels like one continuous move",
                detail = "Thumbnails grow into the fullscreen photo from every grid now, Favorite and Popular included, while the navigation bar folds quietly out of the way. Swiping down to close follows your finger and reveals the list underneath. Photos arrive faster and without a flash, and closing one puts you back exactly where you were.",
            ),
        ),
    ),
    Release(
        version = "3.0.0",
        date = LocalDate(2026, 6, 12),
        changes = persistentListOf(
            Release.Change(
                id = "multiplatform",
                type = ChangeType.MULTIPLATFORM,
                title = "All Your Devices",
                summary = "Now on Android, iPhone, iPad and desktop",
                detail = "Mars Rover Photos runs natively on iPhone and iPad, and as a desktop app on Mac, Windows and Linux. It is the same app everywhere, so your rovers, photos and settings look and behave the same on whichever screen you pick up.",
            ),
            Release.Change(
                id = "photos_restored",
                type = ChangeType.BUG_FIX,
                title = "Photos Restored",
                summary = "Curiosity, Spirit and Opportunity are browsable again",
                detail = "The NASA feed those three rovers relied on stopped answering, which left their photos completely unreachable. They now come straight from NASA's own image archives: Curiosity keeps day-by-day and camera browsing, while Spirit and Opportunity are browsed page by page. One picker handles jumping to a date or to a page, whichever the rover supports.",
            ),
            Release.Change(
                id = "camera_filters",
                type = ChangeType.BROWSING,
                title = "Filters",
                summary = "Narrow things down to what you want to see",
                detail = "A Filters sheet lets you pick cameras with chips, jump to a date or a page, and turn the camera name on each tile on or off. Tapping a camera on Mission Info opens the feed already filtered to it, and a search icon narrows the rover list itself.",
            ),
        ),
    ),
    Release(
        version = "2.5.3",
        date = LocalDate(2026, 5, 3),
        changes = persistentListOf(
            Release.Change(
                id = "mission_info",
                type = ChangeType.MISSION_INFO,
                title = "Mission Info",
                summary = "Explore each rover's full mission profile",
                detail = "An info button on any rover card opens a dedicated screen: the mission timeline, total photos and days active, the rover's cameras, and what it was sent to Mars to do. Fun facts about each mission are there too.",
            ),
            Release.Change(
                id = "did_you_know",
                type = ChangeType.FACTS,
                title = "Did You Know",
                summary = "Learn something while you scroll",
                detail = "A fact card appears every so often as you browse a rover's photos, with a short piece of Mars or mission trivia. If you would rather just see photos, switch them off in About.",
            ),
            Release.Change(
                id = "home_widget",
                type = ChangeType.WIDGET,
                title = "Home Widget",
                summary = "The latest Mars photo on your home screen",
                detail = "Add the Mars Photo widget, pick a rover, and its newest picture sits on your home screen and refreshes on its own. Tap the widget to open that photo in the app.",
            ),
        ),
    ),
    Release(
        version = "2.4.5",
        date = LocalDate(2024, 1, 16),
        changes = persistentListOf(
            Release.Change(
                id = "material_three",
                type = ChangeType.REDESIGN,
                title = "New Look",
                summary = "Refreshed colors, type and controls throughout",
                detail = "Colors, text styles, the bottom bar, cards and buttons were all rebuilt, giving the rover list, photo grid, Favorites, About and the photo viewer a cleaner and more consistent feel. On Android 12 and newer the app takes its palette from the colors your phone generates from your wallpaper, and the top bar on Favorites and Popular shrinks as you scroll.",
            ),
        ),
    ),
    Release(
        version = "2.4.0",
        date = LocalDate(2022, 9, 6),
        changes = persistentListOf(
            Release.Change(
                id = "ukraine",
                type = ChangeType.UKRAINE,
                title = "Stand with Ukraine",
                summary = "A message from the developer",
                detail = "A blue and yellow banner sits at the top of the rover list. Tapping it opens a short note about the war in Ukraine and ways to help.",
            ),
        ),
    ),
    Release(
        version = "2.3.2",
        date = LocalDate(2022, 9, 3),
        changes = persistentListOf(
            Release.Change(
                id = "reach_newer_photos",
                type = ChangeType.BUG_FIX,
                title = "Newer Photos",
                summary = "The date and sol pickers reach today",
                detail = "Each rover's mission details are refreshed from NASA when the app starts. The sol slider and date picker now cover everything a rover has sent, instead of stopping at a date frozen years earlier.",
            ),
        ),
    ),
    Release(
        version = "2.3.1",
        date = LocalDate(2022, 2, 16),
        changes = persistentListOf(
            Release.Change(
                id = "cleaner_cards",
                type = ChangeType.REDESIGN,
                title = "Cleaner Cards",
                summary = "Less clutter on cards and while swiping",
                detail = "Counters that would have read zero are hidden, so a card only shows numbers that mean something. Swiping past the first or last photo no longer stretches the screen, and About leads with a single logo instead of a row of rover thumbnails.",
            ),
        ),
    ),
    Release(
        version = "2.3.0",
        date = LocalDate(2021, 9, 7),
        changes = persistentListOf(
            Release.Change(
                id = "pick_a_theme",
                type = ChangeType.THEMING,
                title = "Pick A Theme",
                summary = "Light, dark, or whatever your phone is doing",
                detail = "About now offers White, Dark and System. Choose one and the whole app changes immediately and stays that way next time you open it. The screen was reorganised to make room, and now leads with the rovers themselves.",
            ),
        ),
    ),
    Release(
        version = "2.1.1",
        date = LocalDate(2021, 7, 25),
        changes = persistentListOf(
            Release.Change(
                id = "save_to_gallery",
                type = ChangeType.SAVE_PHOTO,
                title = "Save Photos",
                summary = "Keep a photo, in a steadier viewer",
                detail = "A save button in the fullscreen viewer downloads the picture to your device, confirms when it is done, and offers a shortcut to view it. The viewer around it is steadier too: liking a photo no longer snaps you back to the first image, and double-tapping to zoom out re-centres the picture instead of leaving it shifted.",
            ),
        ),
    ),
    Release(
        version = "2.0.0",
        date = LocalDate(2021, 5, 12),
        changes = persistentListOf(
            Release.Change(
                id = "zoom_and_swipe",
                type = ChangeType.PHOTO_VIEWER,
                title = "Zoom And Swipe",
                summary = "Pinch, double-tap and fling through photos",
                detail = "The fullscreen viewer was rebuilt: pinch to zoom, double-tap to snap in, drag around a zoomed photo, and fling to the next one. You can mark a photo as a favorite without leaving it, and a photo that comes up empty now retries instead of staying blank.",
            ),
        ),
    ),
    Release(
        version = "1.7.2",
        date = LocalDate(2021, 4, 4),
        changes = persistentListOf(
            Release.Change(
                id = "photos_in_one_shell",
                type = ChangeType.NAVIGATION,
                title = "One Screen",
                summary = "Rovers, Favorites and Popular live in one place",
                detail = "Tapping a rover or switching tabs now opens inside the same screen, with the bottom bar always in reach. A rover's photos gained a sol and Earth-date picker with a slider, a shuffle button, and a tap-to-retry message when a day has nothing.",
            ),
            Release.Change(
                id = "blurred_backdrop",
                type = ChangeType.PHOTO_VIEWER,
                title = "Blurred Backdrop",
                summary = "Photos sit on a soft blur of themselves",
                detail = "The fullscreen viewer fills the space around a photo with a blurred, darkened copy of it instead of flat color, and adds a back arrow at the top so you are never stuck. The heart updates straight away now when you favorite the photo you are looking at.",
            ),
        ),
    ),
    Release(
        version = "1.7.1",
        date = LocalDate(2021, 2, 26),
        changes = persistentListOf(
            Release.Change(
                id = "bottom_navigation",
                type = ChangeType.NAVIGATION,
                title = "Bottom Bar",
                summary = "Rovers, Favorite, Popular and About, one tap away",
                detail = "A bottom navigation bar replaces the single scrolling list and its hidden menu. Favorites and Popular are tabs of their own now, Popular shows its grid right there instead of on a separate screen, and a Favorites row joins the rover list itself.",
            ),
            Release.Change(
                id = "sol_filter_fix",
                type = ChangeType.BUG_FIX,
                title = "Day Filter",
                summary = "Changing the day reliably reloads photos",
                detail = "Switching to another sol now always fetches that day's pictures. Perseverance in particular used to ask for a computed date range that could return the wrong photos or none at all.",
            ),
        ),
    ),
    Release(
        version = "1.7.0",
        date = LocalDate(2021, 2, 22),
        changes = persistentListOf(
            Release.Change(
                id = "perseverance",
                type = ChangeType.NEW_ROVER,
                title = "Perseverance",
                summary = "NASA's newest rover joins the list",
                detail = "Perseverance landed in February 2021 and now sits in the rover list alongside Curiosity, Opportunity, Spirit and InSight. Pick it to browse its photos like any other mission.",
            ),
            Release.Change(
                id = "popular_entries_fix",
                type = ChangeType.POPULAR,
                title = "Popular Fixed",
                summary = "Popular entries load with all their details",
                detail = "Some photos on the Popular screen could come through with no image, name or date. Each entry is now read out field by field, so it arrives complete.",
            ),
        ),
    ),
    Release(
        version = "1.6.1",
        date = LocalDate(2021, 2, 21),
        changes = persistentListOf(
            Release.Change(
                id = "steady_lists",
                type = ChangeType.BUG_FIX,
                title = "Steady Lists",
                summary = "Every row shows its own photo",
                detail = "Rows in the photo grid, the rover list, Favorites and Popular were reading their contents from the wrong place, which could show another row's picture or close the app. Each row now draws itself correctly.",
            ),
        ),
    ),
    Release(
        version = "1.6.0",
        date = LocalDate(2020, 9, 6),
        changes = persistentListOf(
            Release.Change(
                id = "popular_opens_viewer",
                type = ChangeType.POPULAR,
                title = "Popular Opens",
                summary = "Tapping a popular photo opens it fullscreen",
                detail = "Photos on the Popular screen used to do nothing when tapped. They now open in the same fullscreen viewer as everywhere else, and you can swipe on through the rest.",
            ),
        ),
    ),
    Release(
        version = "1.5.2",
        date = LocalDate(2020, 8, 30),
        changes = persistentListOf(
            Release.Change(
                id = "favorites",
                type = ChangeType.FAVORITES,
                title = "Favorites",
                summary = "Save your best shots to revisit later",
                detail = "Tap the heart while viewing a photo to keep it. A tile on the home screen opens everything you have saved, using your most recent favorite as its cover, and you can remove a photo straight from that list.",
            ),
        ),
    ),
    Release(
        version = "1.4.2",
        date = LocalDate(2019, 4, 3),
        changes = persistentListOf(
            Release.Change(
                id = "live_rover_stats",
                type = ChangeType.MISSION_INFO,
                title = "Live Stats",
                summary = "Rover cards show real mission numbers",
                detail = "Landing and launch dates, the latest day and the total photo count were stuck at the values the app shipped with, or showed a bare 0. They now update from NASA.",
            ),
        ),
    ),
    Release(
        version = "1.4.0",
        date = LocalDate(2019, 4, 3),
        changes = persistentListOf(
            Release.Change(
                id = "fresh_look_140",
                type = ChangeType.REDESIGN,
                title = "Fresh Look",
                summary = "New colors, new type, smoother moves",
                detail = "The app moved to a blue and orange palette with a single typeface across every screen, dialogs and date pickers included, and margins were tightened so more of the screen belongs to the photos. Tapping a picture animates the thumbnail into the fullscreen view instead of cutting to it, and a day with no photos shows a proper empty screen rather than a blank list.",
            ),
        ),
    ),
    Release(
        version = "1.3.1",
        date = LocalDate(2019, 2, 25),
        changes = persistentListOf(
            Release.Change(
                id = "insight",
                type = ChangeType.NEW_ROVER,
                title = "InSight",
                summary = "NASA's InSight lander joins the list",
                detail = "InSight sits alongside Curiosity, Opportunity and Spirit and opens a set of its surface images. Its mission numbers on the card are still placeholders for now.",
            ),
        ),
    ),
    Release(
        version = "1.3.0",
        date = LocalDate(2018, 9, 2),
        changes = persistentListOf(
            Release.Change(
                id = "popular",
                type = ChangeType.POPULAR,
                title = "Popular Photos",
                summary = "Community-curated highlights from each mission",
                detail = "A card at the top of the rover list opens Most Popular Mars Photos: the images people view, save, zoom into and share the most, each showing its counts. More load as you keep scrolling.",
            ),
        ),
    ),
    Release(
        version = "1.2.4",
        date = LocalDate(2017, 11, 18),
        changes = persistentListOf(
            Release.Change(
                id = "sharing_works",
                type = ChangeType.BUG_FIX,
                title = "Sharing Works",
                summary = "Photos actually reach the app you pick",
                detail = "Choosing an app from the share sheet used to do nothing at all. The photo is now handed over properly, so the app you picked opens with it ready to send.",
            ),
            Release.Change(
                id = "crisper_thumbnails",
                type = ChangeType.REDESIGN,
                title = "Crisper Photos",
                summary = "Sharper thumbnails and a new app icon",
                detail = "Pictures in the grid and on the rover picker were being squeezed into a small box before being drawn; they now render at full resolution. The rover portraits ship with the app instead of being pulled from other websites, so they always appear, and the launcher icon was redrawn to sit properly on any home screen.",
            ),
        ),
    ),
    Release(
        version = "1.2.0",
        date = LocalDate(2016, 11, 17),
        changes = persistentListOf(
            Release.Change(
                id = "swipe_through_gallery",
                type = ChangeType.PHOTO_VIEWER,
                title = "Swipe Through",
                summary = "Open one photo and keep going",
                detail = "Tapping a picture opens a swipeable, pinch-zoomable gallery built from the grid you were browsing, starting on the photo you tapped. If you had filtered to one camera, swiping stays within that camera.",
            ),
        ),
    ),
    Release(
        version = "1.1.1",
        date = LocalDate(2016, 11, 10),
        changes = persistentListOf(
            Release.Change(
                id = "old_android_crash_fix",
                type = ChangeType.BUG_FIX,
                title = "Crash Fix",
                summary = "No more crashes on older Android versions",
                detail = "The retry, save and about icons closed the app on devices older than Android 5.0. Those screens now open normally.",
            ),
        ),
    ),
)
