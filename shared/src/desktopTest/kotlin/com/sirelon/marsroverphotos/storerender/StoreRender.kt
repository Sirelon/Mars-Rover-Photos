package com.sirelon.marsroverphotos.storerender

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.sirelon.marsroverphotos.data.database.AppDataBase
import com.sirelon.marsroverphotos.data.network.createAppHttpClient
import com.sirelon.marsroverphotos.di.initKoinDesktop
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.repositories.CoverFrameCheck
import com.sirelon.marsroverphotos.data.repositories.PhotosRepositoryImpl
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.platform.DesktopPlatformPreferences
import com.sirelon.marsroverphotos.platform.FirebasePhotosImpl
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.platform.PlatformPreferences
import com.sirelon.marsroverphotos.platform.configureCommon
import com.sirelon.marsroverphotos.presentation.navigation.AboutCallbacks
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.AppNavigation
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toOkioPath
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.util.Locale
import java.util.prefs.Preferences

/**
 * Store-screenshot renderer: draws the real app headless, at store pixel sizes, in light and dark.
 * The screens are the app's own `AppNavigation` with the desktop Koin graph and live NASA data;
 * only the database (in memory) and the preferences (a scratch node) are swapped, so a render
 * never touches the user's desktop data.
 *
 * Output: `store/captures/<device>/<scene>_<theme>.png`, the input of
 * `store/tools/generate-store-screenshots.mjs`. Workflow: `.claude/skills/mars-store-screenshots/`.
 */
enum class StoreDevice(
    val dir: String,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    /** Status-bar strip the compositor draws its clock and icons into. The JVM has no window insets. */
    val topInset: Dp,
    /** Home-indicator / gesture-bar strip. */
    val bottomInset: Dp,
) {
    // iPhone 6.9" (Dynamic Island large), 1320x2868 @3x = 440x956 pt: the App Store slot size.
    IPHONE("iphone", 1320, 2868, 3f, 62.dp, 34.dp),

    // iPad 13" landscape, 2752x2064 @2x = 1376x1032 pt.
    IPAD("ipad", 2752, 2064, 2f, 24.dp, 20.dp),

    // A 20:9 Android phone, 1080x2400 at 420 dpi = 411x914 dp.
    ANDROID_PHONE("android-phone", 1080, 2400, 2.625f, 36.dp, 24.dp),

    // A 16:10 Android tablet in landscape, 2560x1600 @2x = 1280x800 dp.
    ANDROID_TABLET("android-tablet", 2560, 1600, 2f, 28.dp, 24.dp),
}

/** What colour the harness paints the system-bar strips in, to match what the app draws under them. */
enum class StripStyle {
    /** Screen background on top; the bottom bar's colour below on phones. */
    App,

    /** The fullscreen viewer: black. */
    Fullscreen,

    /** A modal bottom sheet: its scrim over the background on top, the sheet's colour below. */
    Sheet,
}

class StoreScene(
    val id: String,
    /** The navigation stack to open, bottom first; the last entry is the screen captured. */
    val backStack: List<AppDestination>,
    val devices: Set<StoreDevice> = StoreDevice.entries.toSet(),
    val strips: StripStyle = StripStyle.App,
    /** Shortest wait before the frame may count as settled: network photos need time. */
    val minSettleMs: Long = 4_000,
    /**
     * Sol a rover's feed opens on, by rover id. The app opens most feeds on a random sol; this
     * makes the scene repeatable. Curiosity always opens on its latest sol and ignores it.
     */
    val feedSols: Map<Long, Long> = emptyMap(),
    /** Runs once per scene before rendering, with the app's Koin graph: seed favorites, prefetch. */
    val prepare: suspend Koin.() -> Unit = {},
)

object StoreRenderer {

    private val repoRoot: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    val capturesRoot = File(repoRoot, "store/captures")

    private val koin: Koin by lazy { startApp() }

    fun renderAll(scenes: List<StoreScene>) {
        Locale.setDefault(Locale.US)
        val sceneFilter = envList("STORE_RENDER_SCENES")
        val deviceFilter = envList("STORE_RENDER_DEVICES")
        val themeFilter = envList("STORE_RENDER_THEMES")
        // By default only what store/tools/scenes.json places in a slot; STORE_RENDER_ALL=1 renders
        // every scene in both themes on every device, to browse candidates.
        val wanted = if (System.getenv("STORE_RENDER_ALL") == "1") null else manifestCaptures()
        var count = 0
        val failures = mutableListOf<String>()
        for (scene in scenes) {
            if (sceneFilter != null && scene.id !in sceneFilter) continue
            if (wanted != null && wanted.none { it.endsWith("/${scene.id}_light") || it.endsWith("/${scene.id}_dark") }) continue
            runBlocking { scene.prepare(koin) }
            PinnedSolPhotosRepository.pins = scene.feedSols
            for (device in scene.devices) {
                if (deviceFilter != null && device.dir !in deviceFilter) continue
                for (dark in listOf(false, true)) {
                    val theme = if (dark) "dark" else "light"
                    if (themeFilter != null && theme !in themeFilter) continue
                    if (wanted != null && "${device.dir}/${scene.id}_$theme" !in wanted) continue
                    runCatching { render(scene, device, dark) }
                        .onSuccess { count++ }
                        .onFailure {
                            it.printStackTrace()
                            failures += "${device.dir}/${scene.id}_$theme: ${it.message?.take(200)}"
                        }
                }
            }
        }
        println("store-render: $count files under ${capturesRoot.absolutePath}")
        check(failures.isEmpty()) { "store-render failed:\n" + failures.joinToString("\n") }
    }

    /** `<device>/<scene>_<theme>` for every slot in the manifest: phone lists feed both phones, tablet both tablets. */
    private fun manifestCaptures(): Set<String> {
        val manifest = Json.parseToJsonElement(File(repoRoot, "store/tools/scenes.json").readText()).jsonObject
        val devicesByList = mapOf(
            "phone" to listOf(StoreDevice.IPHONE, StoreDevice.ANDROID_PHONE),
            "tablet" to listOf(StoreDevice.IPAD, StoreDevice.ANDROID_TABLET),
        )
        return devicesByList.flatMap { (list, devices) ->
            manifest[list]?.jsonArray.orEmpty().flatMap { slot ->
                val entry = slot.jsonObject
                val capture = "${entry["scene"]!!.jsonPrimitive.content}_${entry["theme"]!!.jsonPrimitive.content}"
                devices.map { "${it.dir}/$capture" }
            }
        }.toSet()
    }

    private fun startApp(): Koin {
        GlobalContext.getOrNull()?.let { stopKoin() }
        System.setProperty("app.version", System.getProperty("app.version") ?: "unknown")
        // firebase-java-sdk keeps Firestore's cache under java.io.tmpdir; a running desktop app
        // holds that database locked, and Firestore then panics. Give the render its own.
        System.setProperty("java.io.tmpdir", File(repoRoot, "store/cache/tmp").apply { mkdirs() }.absolutePath)
        val prefsNode = Preferences.userRoot().node("com/sirelon/marsroverphotos/store-render")
        prefsNode.clear()
        initKoinDesktop(
            overrides = listOf(
                module {
                    single<PlatformPreferences> { DesktopPlatformPreferences(prefsNode) }
                    single<IFirebasePhotos> { ReadOnlyFirebasePhotos(FirebasePhotosImpl()) }
                    single<PhotosRepository> {
                        PinnedSolPhotosRepository(
                            PhotosRepositoryImpl(
                                api = get(),
                                vikingCatalog = get(),
                                isGoodCover = CoverFrameCheck(api = get())::isGoodCover,
                            ),
                        )
                    }
                    single<AppDataBase> {
                        Room.inMemoryDatabaseBuilder<AppDataBase>()
                            .setDriver(BundledSQLiteDriver())
                            .configureCommon()
                            .build()
                    }
                },
            ),
        )
        return GlobalContext.get()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun render(scene: StoreScene, device: StoreDevice, dark: Boolean) = runBlocking {
        val theme = if (dark) "dark" else "light"
        val out = File(capturesRoot, "${device.dir}/${scene.id}_$theme.png")
        out.parentFile.mkdirs()

        // Everything Compose touches runs on the Swing thread, where the app's view models already
        // live (viewModelScope is Dispatchers.Main): one thread, so no cross-thread layout errors.
        val holder = withContext(Dispatchers.Main) { SceneOwners() }
        val composeScene = withContext(Dispatchers.Main) {
            ImageComposeScene(
                width = device.widthPx,
                height = device.heightPx,
                density = Density(device.density),
                coroutineContext = Dispatchers.Main,
            ) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides holder,
                    LocalViewModelStoreOwner provides holder,
                ) {
                    Harness(scene, device, dark)
                }
            }
        }
        try {
            val start = System.nanoTime()
            val elapsedMs = { (System.nanoTime() - start) / 1_000_000 }
            var lastHash = 0
            var stableSince = 0L
            var image = withContext(Dispatchers.Main) { composeScene.render(0) }
            while (true) {
                delay(150)
                image = withContext(Dispatchers.Main) { composeScene.render(System.nanoTime() - start) }
                val hash = Bitmap.makeFromImage(image).readPixels()?.contentHashCode() ?: 0
                val now = elapsedMs()
                if (hash != lastHash) {
                    lastHash = hash
                    stableSince = now
                }
                // Settled: unchanged for 2 s after the minimum wait. Looping animations never
                // settle; the cap keeps them from hanging the run.
                if (now >= scene.minSettleMs && now - stableSince >= 2_000) break
                if (now >= scene.minSettleMs + 40_000) {
                    println("store-render: ${scene.id} still changing after ${now / 1000} s, captured anyway")
                    break
                }
            }
            out.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            println("store-render: ${out.relativeTo(repoRoot)}")
        } finally {
            withContext(Dispatchers.Main) {
                composeScene.close()
                holder.destroy()
            }
        }
    }

    private fun envList(name: String): Set<String>? =
        System.getenv(name)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
            ?.takeIf { it.isNotEmpty() }

    internal val imageCacheDir = File(repoRoot, "store/cache/images")
}

/**
 * Reads the live Popular feed and facts, but never writes: a render opens the viewer and favorites
 * photos, which in the app bump the shared view / favorite counters in Firestore.
 */
private class ReadOnlyFirebasePhotos(private val real: IFirebasePhotos) : IFirebasePhotos by real {
    override suspend fun updatePhotoShareCounter(photo: MarsImage) = photo.stats.share
    override suspend fun updatePhotoSaveCounter(photo: MarsImage) = photo.stats.save
    override suspend fun updatePhotoScaleCounter(photo: MarsImage) = photo.stats.scale
    override suspend fun updatePhotoSeenCounter(photo: MarsImage) = photo.stats.see
    override suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean) = photo.stats.favorite
    override suspend fun deletePhoto(photoId: String) = Unit
}

/**
 * The feed's random opening sol comes from [PhotosRepository.nearestSolWithPhotosAtOrAfter]; answering
 * it with the scene's sol pins the feed. Everything else goes to the real repository.
 */
private class PinnedSolPhotosRepository(private val real: PhotosRepository) : PhotosRepository by real {
    override suspend fun nearestSolWithPhotosAtOrAfter(roverId: Long, fromSol: Long): Long? =
        pins[roverId] ?: real.nearestSolWithPhotosAtOrAfter(roverId, fromSol)

    companion object {
        @Volatile var pins: Map<Long, Long> = emptyMap()
    }
}

/** The lifecycle and view-model store a desktop window gives the app; a headless scene has neither. */
private class SceneOwners : LifecycleOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore = ViewModelStore()

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

/** The parts of `App()` a screenshot needs, around the app's real navigation, plus system-bar strips. */
@Composable
private fun Harness(scene: StoreScene, device: StoreDevice, dark: Boolean) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.40).build() }
            // A disk cache makes reruns fast and identical; App() has none on desktop.
            .diskCache {
                DiskCache.Builder().directory(StoreRenderer.imageCacheDir.toOkioPath()).build()
            }
            .components { add(KtorNetworkFetcherFactory(httpClient = { createAppHttpClient() })) }
            .build()
    }
    MarsRoverPhotosTheme(darkTheme = dark, dynamicColor = false) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val colors = MaterialTheme.colorScheme
            val compact = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()) ==
                NavigationSuiteType.NavigationBar
            val topColor = when (scene.strips) {
                StripStyle.Fullscreen -> Color.Black
                // BottomSheetDefaults.ScrimColor: scrim at 32 %.
                StripStyle.Sheet -> colors.scrim.copy(alpha = 0.32f).compositeOver(colors.background)
                StripStyle.App -> colors.background
            }
            // The bottom bar sits on the home strip on phones; tablets show the rail at the side.
            val bottomColor = when {
                scene.strips == StripStyle.Fullscreen -> Color.Black
                scene.strips == StripStyle.Sheet -> colors.surfaceContainerLow
                compact -> colors.surfaceContainer
                else -> colors.background
            }
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.fillMaxWidth().height(device.topInset).background(topColor))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    CompositionLocalProvider(
                        LocalAboutCallbacks provides AboutCallbacks(
                            onRateApp = {},
                            appVersion = System.getProperty("app.version") ?: "",
                            rateAppUrl = "",
                        ),
                    ) {
                        AppNavigation(
                            modifier = Modifier.fillMaxSize(),
                            initialBackStack = scene.backStack,
                        )
                    }
                }
                Spacer(Modifier.fillMaxWidth().height(device.bottomInset).background(bottomColor))
            }
        }
    }
}
