package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.utils.Logger

/**
 * [PagingSource] that pages Mars rover photos by **sol** (Martian day).
 *
 * Each page is one non-empty sol's photos:
 *  - APPEND  walks forward  (sol + 1, sol + 2, …) — scroll down / swipe to next days
 *  - PREPEND walks backward (sol - 1, sol - 2, …) — scroll up / swipe to earlier days
 *
 * Empty sols are skipped **inside a single [load]** by scanning to the next non-empty sol — we
 * never return an empty `LoadResult.Page` mid-stream, because Paging does not reliably re-trigger
 * the next load after an empty page (that is the classic "scrolling sometimes stalls" bug). A load
 * therefore returns either a non-empty page, a continuation page (a scan budget was hit before any
 * photos, see below), or a terminal page (`prevKey`/`nextKey` = null) once the rover's
 * [minSol]/[maxSol] bound is actually reached.
 *
 * To bound per-load latency every scan is capped at a finite budget ([scanBudget]) so a single load
 * can never fire an unbounded run of network probes across a long gap (rover safe-mode/solar-
 * conjunction outages, or a sparse camera's long tail of non-matching sols). When the budget is hit
 * before the bound we return an empty continuation page that preserves the directional key, so the
 * next load resumes the scan — reachable photos beyond the gap are never dead-ended.
 *
 * On Refresh we scan forward then backward, so an unlucky random/picked anchor still lands on the
 * nearest day that actually has photos.
 *
 * Fetched photos are **written through** to Room ([cacheAndMerge]) and any persisted favorite/stats
 * are merged back, so the cache stays a passive side-store (this remains a network-driven source)
 * while the viewer shows correct favorite state.
 */
class SolPagingSource(
    private val photosRepository: PhotosRepository,
    private val imagesDao: ImagesDao,
    private val roverId: Long,
    private val camera: String?,
    private val initialSol: Long,
    private val minSol: Long,
    private val maxSol: Long,
) : PagingSource<Long, MarsImage>() {

    /** Inserts since the last cache eviction; eviction runs only every [EVICT_EVERY_N_LOADS]. */
    private var loadsSinceEviction = 0

    /** Refresh re-centers on the sol of the item closest to the current scroll anchor. */
    override fun getRefreshKey(state: PagingState<Long, MarsImage>): Long? {
        val anchor = state.anchorPosition ?: return null
        return state.closestItemToPosition(anchor)?.sol
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, MarsImage> {
        return try {
            when (params) {
                is LoadParams.Append -> when (val r = findDay(params.key, step = +1)) {
                    is FindResult.Found -> r.day.toPage()
                    // Budget hit, not the bound: keep the forward key so the next load resumes.
                    is FindResult.Exhausted ->
                        continuationPage(prevKey = null, nextKey = clampMax(r.nextSol))
                    FindResult.End -> terminalPage()
                }

                is LoadParams.Prepend -> when (val r = findDay(params.key, step = -1)) {
                    is FindResult.Found -> r.day.toPage()
                    // Budget hit, not the bound: keep the backward key so the next load resumes.
                    is FindResult.Exhausted ->
                        continuationPage(prevKey = clampMin(r.nextSol), nextKey = null)
                    FindResult.End -> terminalPage()
                }

                is LoadParams.Refresh -> {
                    val start = params.key ?: initialSol
                    when (val forward = findDay(start, step = +1)) {
                        is FindResult.Found -> forward.day.toPage()
                        is FindResult.Exhausted ->
                            continuationPage(prevKey = clampMin(start - 1), nextKey = clampMax(forward.nextSol))
                        FindResult.End -> when (val backward = findDay(start - 1, step = -1)) {
                            is FindResult.Found -> backward.day.toPage()
                            is FindResult.Exhausted ->
                                continuationPage(prevKey = clampMin(backward.nextSol), nextKey = null)
                            FindResult.End -> terminalPage()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("SolPagingSource", e) { "Error loading sol page (rover=$roverId)" }
            LoadResult.Error(e)
        }
    }

    /**
     * Walks [step] sols from [startSol] toward the rover bound, returning the first sol that has
     * (camera-filtered) photos. Capped at [budget] network probes per call.
     */
    private suspend fun findDay(startSol: Long, step: Int, budget: Int = scanBudget): FindResult {
        var sol = startSol
        var probes = 0
        while (sol in minSol..maxSol) {
            if (probes >= budget) return FindResult.Exhausted(sol)
            // Fetch the whole sol (camera = null) and filter in-memory, so the empty-sol skip is
            // camera-aware for every rover — Perseverance/Insight raw APIs ignore the camera param.
            val photos = photosRepository
                .refreshImages(PhotosQueryRequest(roverId, sol, camera = null))
                .filterByCamera(camera)
            probes++
            if (photos.isNotEmpty()) {
                return FindResult.Found(Day(sol, cacheAndMerge(photos)))
            }
            sol += step
        }
        return FindResult.End
    }

    /**
     * Per-load scan budget. Both feeds are bounded so a single [load] can never fire an unbounded
     * run of sequential network probes across a long gap (rover safe-mode/solar-conjunction
     * outages). The unfiltered feed gets a larger budget since its data is dense; a hit budget
     * returns a continuation page so the next load resumes the scan.
     */
    private val scanBudget: Int
        get() = if (camera.isNullOrBlank()) UNFILTERED_SCAN_BUDGET else FILTER_SCAN_BUDGET

    private fun Day.toPage(): LoadResult.Page<Long, MarsImage> = LoadResult.Page(
        data = photos,
        prevKey = clampMin(sol - 1),
        nextKey = clampMax(sol + 1),
    )

    /** Empty page that ends pagination in both directions (a real bound was reached). */
    private fun terminalPage(): LoadResult.Page<Long, MarsImage> =
        LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

    /** Empty page that keeps paging moving (a gap longer than the per-load scan budget). */
    private fun continuationPage(prevKey: Long?, nextKey: Long?): LoadResult.Page<Long, MarsImage> =
        LoadResult.Page(data = emptyList(), prevKey = prevKey, nextKey = nextKey)

    private fun clampMin(sol: Long): Long? = sol.takeIf { it >= minSol }
    private fun clampMax(sol: Long): Long? = sol.takeIf { it <= maxSol }

    /**
     * Write-through cache: persist the freshly-fetched photos (INSERT IGNORE — never clobbers
     * existing favorite/stats), then return them with any persisted favorite/stats merged back,
     * preserving the network ordering. Falls back to the network photos if the DB write fails.
     */
    private suspend fun cacheAndMerge(networkPhotos: List<MarsImage>): List<MarsImage> {
        return try {
            imagesDao.insertImages(networkPhotos)
            // Eviction is a full-table DELETE … ORDER BY … LIMIT, too costly for the hot scroll
            // path; run it only every Nth cached page (the limit is a soft ceiling, not exact).
            if (++loadsSinceEviction >= EVICT_EVERY_N_LOADS) {
                loadsSinceEviction = 0
                try {
                    imagesDao.deleteNonUserImagesBeyondCount(CACHE_KEEP_LIMIT)
                } catch (e: Exception) {
                    Logger.e("SolPagingSource", e) { "Cache eviction failed; continuing" }
                }
            }
            val persisted = imagesDao
                .loadImagesByIds(networkPhotos.map { it.id })
                .associateBy { it.id }
            networkPhotos.map { persisted[it.id] ?: it }
        } catch (e: Exception) {
            Logger.e("SolPagingSource", e) { "Write-through cache failed; serving network photos" }
            networkPhotos
        }
    }

    /** In-memory camera filter matching [RoverCamera.name] or [RoverCamera.fullName]. */
    private fun List<MarsImage>.filterByCamera(camera: String?): List<MarsImage> {
        if (camera.isNullOrBlank()) return this
        return filter { image ->
            val cam = image.camera ?: return@filter false
            cam.name.equals(camera, ignoreCase = true) ||
                cam.fullName.equals(camera, ignoreCase = true)
        }
    }

    private class Day(val sol: Long, val photos: List<MarsImage>)

    private sealed interface FindResult {
        data class Found(val day: Day) : FindResult
        /** Scan budget hit before the bound; [nextSol] is where to resume. */
        data class Exhausted(val nextSol: Long) : FindResult
        /** Reached the rover bound without finding photos — true end of pagination. */
        data object End : FindResult
    }

    private companion object {
        /** Max sols probed per load() when a camera filter is active (bounds per-call latency). */
        private const val FILTER_SCAN_BUDGET = 60
        /** Max sols probed per load() for the unfiltered feed; larger since its data is dense. */
        private const val UNFILTERED_SCAN_BUDGET = 300
        private const val CACHE_KEEP_LIMIT = 2000
        /** Run cache eviction once every this many cached pages (keeps it off the hot path). */
        private const val EVICT_EVERY_N_LOADS = 20
    }
}
