package com.sirelon.marsroverphotos.domain.releasenotes

import kotlinx.datetime.LocalDate

val RELEASES: List<Release> = listOf(
    Release(
        version = "5.0.0",
        date = LocalDate(2025, 8, 1),
        changes = listOf(
            Release.Change(
                id = "mission_info",
                type = ChangeType.MISSION_INFO,
                title = "Mission Info",
                summary = "Explore each rover's full mission profile",
                detail = "Tap any rover to open its dedicated Mission Info screen — launch date, landing site, active cameras, and a visual timeline of its journey from Earth to Mars.",
            ),
            Release.Change(
                id = "multiplatform",
                type = ChangeType.MULTIPLATFORM,
                title = "All Your Devices",
                summary = "Now on Android, iOS, and Desktop",
                detail = "Mars Rover Photos is now built with Kotlin Multiplatform and Compose Multiplatform. One codebase, the same experience everywhere — Android, iPhone, iPad, and macOS.",
            ),
            Release.Change(
                id = "offline_cache",
                type = ChangeType.OFFLINE_CACHE,
                title = "Smarter Caching",
                summary = "Photos load faster, even offline",
                detail = "Images you've already viewed are cached locally. Browse your recent photos without a connection, and enjoy faster load times when switching between rovers.",
            ),
        ),
    ),
    Release(
        version = "4.2.0",
        date = LocalDate(2024, 11, 1),
        changes = listOf(
            Release.Change(
                id = "favorites",
                type = ChangeType.FAVORITES,
                title = "Favorites",
                summary = "Save your best shots to revisit later",
                detail = "Tap the heart on any photo to add it to your Favorites collection. Browse saved photos from the bottom navigation bar — no internet required.",
            ),
            Release.Change(
                id = "popular",
                type = ChangeType.POPULAR,
                title = "Popular Photos",
                summary = "Community-curated highlights from each mission",
                detail = "Discover the most-viewed images from each Mars mission. A new tab surfaces the best of what NASA's rovers have captured.",
            ),
        ),
    ),
    Release(
        version = "4.0.0",
        date = LocalDate(2024, 5, 1),
        changes = listOf(
            Release.Change(
                id = "redesign",
                type = ChangeType.REDESIGN,
                title = "New Look",
                summary = "Redesigned from the ground up",
                detail = "Material You theming, dynamic colors, and a refined layout that adapts to every screen size — the biggest visual overhaul in the app's history.",
            ),
        ),
    ),
)
