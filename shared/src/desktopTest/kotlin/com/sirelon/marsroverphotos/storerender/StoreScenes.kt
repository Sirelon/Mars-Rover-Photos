package com.sirelon.marsroverphotos.storerender

import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import org.koin.core.Koin
import java.io.File
import kotlin.test.Test

/** Rover ids, as in docs/DEEP_LINKING.md. */
private const val PERSEVERANCE = 3L
private const val CURIOSITY = 5L
private const val VIKING_1 = 8L

/**
 * Every scene the store screenshots are cut from. Order here is not the store order: the
 * manifest `store/tools/scenes.json` picks scenes and binds each one to its caption.
 *
 * Run: `STORE_RENDER=1 ./gradlew :shared:desktopTest --tests '*StoreScenesTest*' --rerun`, narrowed
 * with `STORE_RENDER_SCENES`, `STORE_RENDER_DEVICES`, `STORE_RENDER_THEMES` (comma-separated).
 * Without `STORE_RENDER=1` the test does nothing, so the verify pair stays fast and offline.
 */
val storeScenes: List<StoreScene> = listOf(
    // Today on Mars arrives a few seconds after the mission list; wait for it.
    StoreScene(id = "rovers", backStack = listOf(AppDestination.Rovers), minSettleMs = 10_000),
    // Sol 22: Mastcam-Z colour frames of the rocks around the landing site.
    StoreScene(
        id = "feed_perseverance",
        backStack = listOf(AppDestination.Rovers, AppDestination.Photos(PERSEVERANCE)),
        feedSols = mapOf(PERSEVERANCE to 22L),
    ),
    StoreScene(id = "feed_curiosity", backStack = listOf(AppDestination.Rovers, AppDestination.Photos(CURIOSITY))),
    // Sol 0: the first photograph taken on the surface of Mars, 20 July 1976.
    StoreScene(
        id = "feed_viking",
        backStack = listOf(AppDestination.Rovers, AppDestination.Photos(VIKING_1)),
        feedSols = mapOf(VIKING_1 to 0L),
    ),
    StoreScene(id = "mission", backStack = listOf(AppDestination.Rovers, AppDestination.Mission(PERSEVERANCE))),
    StoreScene(
        id = "filters",
        backStack = listOf(
            AppDestination.Rovers,
            AppDestination.Photos(PERSEVERANCE),
            AppDestination.PhotosFilters(PERSEVERANCE),
        ),
        strips = StripStyle.Sheet,
        feedSols = mapOf(PERSEVERANCE to 22L),
    ),
    StoreScene(
        id = "date_jump",
        backStack = listOf(
            AppDestination.Rovers,
            AppDestination.Photos(PERSEVERANCE),
            AppDestination.PhotosDateJumpPicker(PERSEVERANCE),
        ),
        strips = StripStyle.Sheet,
        feedSols = mapOf(PERSEVERANCE to 22L),
    ),
    StoreScene(id = "popular", backStack = listOf(AppDestination.Popular)),
    StoreScene(id = "favorites", backStack = listOf(AppDestination.Favorite), prepare = { seedFavorites() }),
    StoreScene(
        id = "viewer",
        backStack = listOf(
            AppDestination.Popular,
            AppDestination.Images(selectedId = HERO_PHOTO, source = AppDestination.ImagesSource.POPULAR),
        ),
        strips = StripStyle.Fullscreen,
        minSettleMs = 6_000,
        prepare = { popularPhotos() },
    ),
    StoreScene(id = "about", backStack = listOf(AppDestination.About)),
    StoreScene(id = "whats_new", backStack = listOf(AppDestination.About, AppDestination.AllVersions)),
)

/** The Popular photo the viewer opens on; `store/cache/popular.txt` lists the candidates. */
private const val HERO_PHOTO = "ZR0_0048_0671199445_053ECM_N0031708ZCAM08012_110085J"

/**
 * Favorites, picked from the Popular feed by eye (`store/cache/popular.txt`): colour landscapes and
 * hardware shots that load. A photo whose link has died is skipped.
 */
private val FAVORITE_PHOTOS = listOf(
    "ZR0_0048_0671199445_053ECM_N0031708ZCAM08012_110085J",
    "NRF_0341_0697214868_161ECM_N0091914NCAM00367_01_295J",
    "961341",
    "568699",
    "NLF_0018_0668543839_862EBY_N0030578NCAM00190_04_0LLJ",
    "459199",
    "NLF_0430_0705117714_636ECM_N0241992NCAM03428_10_195J",
    "ZRF_0022_0668905196_091FDR_N0030770ZCAM07000_1100LUJ",
    "NRF_0084_0674405207_678ECM_N0040000NCAM03084_01_295J",
    "834340",
    "NRF_0015_0668277843_723EBY_N0030376NCAM03000_02_0LLJ",
    "ZR0_0048_0671199463_053ECM_N0031708ZCAM08012_110085J",
)

private var popularCache: List<MarsImage>? = null

/** The live Popular feed (Firestore, read only), also written to `store/cache/popular.txt` to pick from. */
private suspend fun Koin.popularPhotos(): List<MarsImage> = popularCache ?: run {
    val photos = get<ImagesRepository>().loadPopularPage(count = 40)
    File(StoreRenderer.capturesRoot.parentFile, "cache/popular.txt").apply { parentFile.mkdirs() }
        .writeText(photos.joinToString("\n") { "${it.id}\trover=${it.roverId}\tsol=${it.sol}\t${it.camera?.name}\t${it.imageUrl}" })
    photos.also { popularCache = it }
}

/** Favorites as a user would build them: photos from the Popular feed, stored only in the in-memory database. */
private suspend fun Koin.seedFavorites() {
    val dao = get<ImagesDao>()
    val byId = popularPhotos().associateBy { it.id }
    val photos = FAVORITE_PHOTOS.mapNotNull { byId[it] }
    dao.insertImages(photos)
    photos.forEach { dao.updateFavorite(it.id, favorite = true, counter = it.stats.favorite) }
}

class StoreScenesTest {
    @Test
    fun render() {
        if (System.getenv("STORE_RENDER") != "1") return
        StoreRenderer.renderAll(storeScenes)
    }
}
