# Editorial notes

Decisions behind [RELEASE_NOTES.md](RELEASE_NOTES.md) and
[ReleaseNotes.kt.proposed](ReleaseNotes.kt.proposed), and the open items that need your call.

**Result:** 45 versions in VERSIONS.md → 22 releases with user-facing notes → 31 changes,
of which 7 are umbrellas. 9 new `ChangeType` constants proposed. `OFFLINE_CACHE` is still unused.

Three editorial rules produced this cut:

1. **Every `major`.** A raw finding marked `user_impact: "major"` always becomes a change, with
   any `minor` items pointing at it through `related_to` merged into its copy.
2. **One umbrella per release, where earned.** Where three or more `minor` items in a release
   share a coherent theme, I author a single entry telling the combined story. It needs no
   corresponding major in the raw record. Maximum one per release; where two themes competed, the
   stronger won and the loser is recorded in §4.
3. **Silence on monetisation.** No ad change is written up in either direction.

No standalone `minor` appears on its own. Everything dropped is still in `raw/*.json`.

---

## 1. What the umbrella rule fixed

Compared with the majors-only cut (19 releases / 24 changes):

- **5.0.0 has its headline back.** The fullscreen viewer overhaul was eight `minor` items hanging
  off an umbrella name — *"Smoother fullscreen photo viewer transitions"* — that is not itself a
  listed finding, so majors-only left the newest release with a single Mission Info entry. It is
  now the second entry on the newest release.
- **The app has a visual history again.** 1.4.0 (blue/orange palette, single typeface, tightened
  margins, shared-element photo transitions, empty states) has no major at all and vanished
  entirely under majors-only, which made `REDESIGN` first appear at 2.4.5 in 2024. It is back.
- **Saving photos is announced somewhere.** 2.1.1 has no major either; the umbrella is the only
  reason save-to-gallery appears in the app's history at all.
- **2.3.1 exists again.** Its only major is an ad change, excluded by rule 3.

Still absent and worth knowing:

- **4.0.0** — see §3. A recorded decision, not an accident.
- **Sharing from the viewer (2.2.0)** — a single `minor` in a release with nothing else. Sharing
  appears in the timeline only as 1.2.4's "it was broken, now it works".
- ~~**The Stand with Ukraine screen (2.4.0)**~~ — **RESOLVED: included by explicit user override
  (2026-08-09).** 2.4.0 is back in the list with a single change, `id = "ukraine"`, typed
  `ChangeType.UKRAINE` (a 10th new constant, needing its own `ChangeTypeIcon.kt` mapping).
  The editorial objection still stands on its own terms — it is a lone `minor` with no umbrella
  available — and was overruled deliberately on non-editorial grounds. The other two items in
  that release (new launcher icon, Android 11 save fix) remain omitted.
- **1.7.2's wrong-sol fix** — a `minor` that does not fit that release's viewer umbrella. It fixed
  a date-to-sol conversion wrong since 2016, which is a better story than its impact rating
  suggests.

---

## 2. Final `ChangeType` list

Each new constant costs an icon mapping in `ChangeTypeIcon.kt`, whose `when` is exhaustive over
the enum, so this is as short as the copy allows.

**9 new constants:**

| Constant | Uses | Covers | Used by |
|---|---:|---|---|
| `BUG_FIX` | 6 | Crash, correctness and reliability | 3.0.0, 2.3.2, 1.7.1, 1.6.1, 1.2.4, 1.1.1 |
| `PHOTO_VIEWER` | 4 | The fullscreen viewer | 5.0.0, 2.0.0, 1.7.2, 1.2.0 |
| `NAVIGATION` | 2 | App shell: bottom bar, tabs, in-place screens | 1.7.2, 1.7.1 |
| `NEW_ROVER` | 2 | A rover or lander added to the list | 1.7.0, 1.3.1 |
| `BROWSING` | 1 | Finding photos: camera filters, date/page jump, search | 3.0.0 |
| `SAVE_PHOTO` | 1 | Saving a photo to the device gallery | 2.1.1 |
| `THEMING` | 1 | Light/dark choice and palettes | 2.3.0 |
| `WIDGET` | 1 | The home screen widget | 2.5.3 |
| `FACTS` | 1 | Educational "Did You Know?" content | 2.5.3 |

`BROWSING` and `SAVE_PHOTO` are **revived** by the 3.0.0 and 2.1.1 umbrellas; they had no user
under majors-only. `ABOUT` and `SHARING` stay **withdrawn** — the Ukraine screen (2.4.0) and the
2.2.0 share feature did not survive, and 1.2.4's "Sharing Works" is typed `BUG_FIX` because the
raw finding sits in `fixes`: sharing was silently broken and was made to work. Restore `SHARING`
if you would rather that feature have its own identity; it is a one-line change.

**Existing constants:** `REDESIGN` ×4, `MISSION_INFO` ×3, `POPULAR` ×3, `MULTIPLATFORM` ×1,
`FAVORITES` ×1, and **`OFFLINE_CACHE` ×0**.

`OFFLINE_CACHE` has no user in this cut and no honest candidate for one. Clear cache (1.6.0) is a
lone `minor`, and 5.0.0's progressive loading and larger image cache are absorbed into the
`PHOTO_VIEWER` umbrella, where they belong. **Your call:** delete the constant and its
`ChangeTypeIcon.kt` mapping, or leave it for future use. Nothing in the proposal depends on it.

---

## 3. Releases dropped, and why

**Maintenance only (6):** 1.0.1, 1.4.3, 1.4.5, 1.5.1, 2.1.0, 2.4.4.

**No raw record (1):** **1.0** (2017-03-05, version code 1) has no file in `raw/`. A plain "First
release — browse photos from NASA's Mars rovers" entry would be reasonable and would fill the
oldest slot, but I will not write copy without evidence. Say the word.

**Nothing at all after the `invisible` filter (2):** 1.2.1, 2.4.3.

**Only ad majors, and no umbrella-worthy minors (2):** 1.1 (ads introduced; its one `minor` is a
viewer regression reverted in 1.2.0), 2.5.2 (ads re-enabled; its one `minor` is the Android 15
layout rework).

**No major, and no coherent 3+ minor theme (12).** Each was checked against the umbrella rule:

| Version | Minors | Why no umbrella |
|---|---:|---|
| **4.0.0** | 1 | **One item only — a widget fix. Cannot form an umbrella. Recorded decision: the list steps 3.0.0 → 5.0.0.** Overrule by admitting the single minor if you would rather have no gap. |
| 1.2.2 | 3 | Two cohere (rover list survives a failed request, thumbnails get their height); the third is a share-menu crash. Theme too thin. |
| 1.2.3 | 1 | Single medium-confidence crash fix. |
| 1.4.1 | 3 | Three unrelated fixes — a bucket, not a theme. |
| 1.4.4 | 4 | Best theme (camera picker dialog + viewer loading feedback) is 2 items; the rest are unrelated crashes. |
| 1.5.0 | 5 | Closest near-miss. The only 3-item theme is crash fixes, which would announce the release's least interesting half while its actual feature — swipe to dismiss the viewer — is a lone minor that would still go untold. |
| 2.2.0 | 1 | Single item (share from the viewer). |
| 2.4.0 | 3 | Ukraine screen, new launcher icon, Android 11 save fix — three genuinely unrelated things. See §1. |
| 2.4.1 | 2 | Grid/list toggle plus its persistence; the rest are `invisible`. |
| 2.4.2 | 6 | Strongest theme (tap-to-hide + smoother pinch zoom) is 2 items; the only 3-item grouping is assorted crash fixes. |
| 2.5.0 | 3 | Palette rework plus its rover-detail recolor is a related pair, not three; the third item is an unexplained app-bar fix. |
| 2.5.1 | 2 | Two items. |

---

## 4. The seven umbrellas, and what each absorbed

Raw titles as they appear in `raw/*.json`.

**5.0.0 — "Seamless Photos"** (`PHOTO_VIEWER`), 8 minors:
Shared-element photo transition extended to Favorite and Popular · Favorite and Popular screens
restore scroll position · Navigation chrome animates with the fullscreen photo viewer ·
Fullscreen viewer info icon moved to bottom-left · Drag-to-dismiss integrated with predictive
back · Faster, flash-free fullscreen photo loading · Fullscreen photo viewer opening/closing
glitches · Rover Photos grid jumped scroll position unnecessarily.
*Not absorbed:* the two iOS fixes (blank version badge, ATT prompt) — different theme, and
neither is something a user wants read back to them.

**3.0.0 — "Filters"** (`BROWSING`), 3 minors:
Filter rover photos by camera · Tap a camera on Mission Info to jump to its photos · Search rovers
by name.
*Competing theme, lost to the one-per-release cap:* the redesign — Redesigned Rovers screen ·
Redesigned About screen · In-app star rating · New app icon and splash artwork · Shared element
transition from grid to viewer · Smoother light/dark theme switching (6 items). It absorbs more,
but 3.0.0 already carries two large stories and a new capability serves the reader better than a
third "we restyled things". Swap them if you disagree — the 6-item version is the stronger
umbrella by count.
*Also not absorbed:* Double-tap to like photos · Deep links to a rover or photo (medium
confidence, and the raw record questions whether the https App Links work end to end).

**2.3.1 — "Cleaner Cards"** (`REDESIGN`), 3 minors:
About screen shows a single app logo instead of four rover images · Removed overscroll glow when
swiping through full-screen photos · Photo stat counters no longer show a bare "0".

**2.1.1 — "Save Photos"** (`SAVE_PHOTO`), 4 minors, all in the fullscreen viewer:
Save photo to device gallery · Crash prevented when opening the photo viewer with an invalid
selection · Liking a photo no longer resets the viewer to the first image · Double-tap zoom no
longer leaves the image panned off-position.
*Not absorbed:* Shimmer placeholder while thumbnails load · Refresh button jumps to the newest
photos (a genuinely useful behaviour that goes untold) · ad banner sizing (rule 3).

**1.7.2 — "Blurred Backdrop"** (`PHOTO_VIEWER`), 3 minors:
Blurred backdrop behind photos in the full-screen viewer · Explicit back button in the full-screen
photo viewer · Favorite icon now updates correctly in the full-screen viewer.
*Not absorbed:* Fixed wrong Sol calculated from a chosen Earth date (see §1) · Popular photos no
longer lose favorite/stat state on refresh · Primary theme color changed to purple (medium
confidence, affects only leftover XML screens).

**1.4.0 — "Fresh Look"** (`REDESIGN`), 3 minors:
App-wide visual refresh: new color palette, custom font, tighter margins · Shared-element
transition animations opening a photo · Empty-state view when a rover/sol has no photos.
*Not absorbed:* Rover stats cached locally (medium, and the raw record asks whether it was
perceptibly faster) · Photo-loading placeholder image fixed (medium, raw record asks whether the
old one was actually broken) · No-connection screen · Crash fix refreshing rover info ·
Opportunity status corrected · ads removed (rule 3).

**1.2.4 — "Crisper Photos"** (`REDESIGN`), 3 minors:
Sharper photo grid and rover-picker thumbnails · Rover picker photos bundled with the app · New
adaptive app icon.
*Not absorbed:* Random-photo button scroll behaviour · Rover mission stats now refresh (the same
story is told better at 1.4.2, where it is a high-confidence major).

**Considered and rejected: 2.0.0.** Empty state for the Favorites tab · Clearer empty-date message
· Smoother loading transitions is a legitimate 3-item "the app tells you what is going on" theme,
but 2.0.0's major is a full viewer rebuild, and following it with "we added empty states" weakens
the release rather than strengthening it. Add it if you want 2.0.0 to have two entries.

---

## 5. Merges kept from `related_to`

- **5.0.0 Mission Info** ← Browse Photos button, varying fun fact.
- **3.0.0 Photos Restored** ← unified jump-to-date / jump-to-page picker.
- **2.4.5 New Look** ← dynamic color on Android 12+, collapsing app bar.
- **2.3.0 Pick A Theme** ← the About refresh that made room for the theme card.
- **2.0.0 Zoom And Swipe** ← the empty-load retry fix.
- **1.7.1 Bottom Bar** ← Favorites row on the rover list, Popular grid shown in place.
- **1.2.0 Swipe Through** ← camera-filter-aware swiping, gallery crash/memory-leak fix.

---

## 6. Hedged, and deliberately omitted

**Hedged (medium confidence, marked ⚠️ in RELEASE_NOTES.md):**
- **1.3.1 InSight** — shipped visibly unfinished: a hardcoded batch of photos and stats copied
  from Spirit under a "TODO fill in correct data" comment. The copy admits the placeholders.
- **1.7.0 Popular Fixed** — evidence shows the old mapping was failure-prone, not that users
  definitely saw blank tiles. Written as "could come through".

Every other change is high confidence, including all seven umbrellas: no umbrella leans on a
medium-confidence item, and the medium ones (1.4.0 placeholder image and rover caching, 1.7.2
purple theme) were deliberately left out of the umbrellas they could have padded.

**Omitted:**
- **1.6.0 "Favoriting removed from the UI"** (`major`, high confidence). **Confirmed real and
  deliberate** — the rover-list shortcut was commented out with
  `// ACtivate it when favorite photos logic will be enabled`, `favBtn.setOnClickListener` was
  deleted and `item_favorite_photo.xml` was removed; restored in 1.7.1. No longer an open
  question. It stays out anyway: announcing the removal of a feature announced in 1.5.2 and
  restored in 1.7.1 reads as a defect, not a change. 1.6.0 is carried by "Popular Opens".
- **All ad changes**, per rule 3.

---

## 7. Things to verify before shipping

1. **The current `ReleaseNotes.kt` is placeholder data.** 4.2.0 never existed (no version code, no
   raw record), and the 5.0.0 and 4.0.0 dates are off by one to two years from VERSIONS.md. The
   proposal replaces the list wholesale. Ids `mission_info`, `multiplatform`, `favorites` and
   `popular` are preserved on the releases that actually shipped those features (2.5.3, 3.0.0,
   1.5.2, 1.3.0). `offline_cache` has no home; `redesign` became `material_three` at 2.4.5.
2. **Umbrellas are authored copy, not reconstructed findings.** The seven 🧵 entries are my
   synthesis of what the listed minors add up to. They are the entries most worth reading closely
   — particularly 5.0.0's, since you reviewed that work personally and will know immediately if
   the emphasis is wrong.
3. **Ordering** is by version code descending, which is release order, not date order. VERSIONS.md
   dates are not monotonic. Among the 22 kept releases both orders agree, so this matters only if
   you add 1.0. Note 1.4.2 and 1.4.0 share 2019-04-03 and are ordered by code.
4. **5.0.0 and 2.5.3 are both titled "Mission Info"** — one introduces the screen, the other
   rebuilds it. Ids differ (`mission_info_redesign` vs `mission_info`). Consider retitling the
   5.0.0 entry, e.g. "Mission Info, Rebuilt".
5. **All 31 ids are unique across the whole list**, not just within a release, in case they are
   ever used as stable keys for "already seen" state.
6. **Sol wording.** Copy uses "sol", "day" and "Martian day" depending on what reads better. Tell
   me if there is a house rule and I will normalise.
