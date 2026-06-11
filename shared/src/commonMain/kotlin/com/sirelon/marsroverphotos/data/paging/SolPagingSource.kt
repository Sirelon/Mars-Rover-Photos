package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlin.math.abs

/**
 * [PagingSource] that pages Mars rover photos by **sol** (Martian day). Used for every rover whose
 * feed is sol-keyed: Curiosity, Perseverance, and Insight. Spirit and Opportunity instead use
 * [ImagesSearchPagingSource] (see [FeedMode] and `Long.usesPageFeed`).
 *
 * The key type is [Long] = sol number: APPEND walks sol+1, sol+2, …; PREPEND walks sol-1, sol-2, …
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
 * On Refresh we scan forward then backward (alternating, one budget at a time) until a non-empty sol
 * is found, the rover's full range is exhausted, or the cumulative [REFRESH_SCAN_LIMIT] is reached.
 * Refresh never returns a continuation page: an empty itemCount produces no viewport hints, so
 * Paging would never fire append/prepend to resume the scan.
 *
 * Fetched photos are **written through** to Room ([cacheAndMerge]) and any persisted favorite/stats
 * are merged back, so the cache stays a passive side-store (this remains a network-driven source)
 * while the viewer shows correct favorite state.
 */
class SolPagingSource(
    private val photosRepository: PhotosRepository,
    private val imagesDao: ImagesDao,
    private val roverId: Long,
    private val cameras: Set<String>,
    private val initialSol: Long,
    private val minSol: Long,
    private val maxSol: Long,
) : PagingSource<Long, MarsImage>() {

    /** Inserts since the last cache eviction; eviction runs only every [EVICT_EVERY_N_LOADS]. */
    private var loadsSinceEviction = 0

    /**
     * Sols already probed and found empty, so re-scans over the same gap (e.g. a PREPEND walking
     * back through sols a Refresh already probed) skip them without re-firing a network request.
     * Safe per-instance only: [cameras] is fixed for the lifetime of a PagingSource (a filter
     * change builds a new source), so "empty" here means "no photos matching THIS camera set" —
     * a sol that is empty for one filter may have photos for another, but never for this instance.
     */
    private val knownEmptySols = HashSet<Long>()

    /** Refresh re-centers on the sol of the item closest to the current scroll anchor. */
    override fun getRefreshKey(state: PagingState<Long, MarsImage>): Long? {
        val anchor = state.anchorPosition ?: return null
        return state.closestItemToPosition(anchor)?.sol
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, MarsImage> {
        return try {
            when (params) {
                is LoadParams.Append -> findNextPage(params.key, step = +1)
                is LoadParams.Prepend -> findNextPage(params.key, step = -1)
                is LoadParams.Refresh -> loadRefresh(params.key ?: initialSol)
            }
        } catch (e: Exception) {
            Logger.e("SolPagingSource", e) { "Error loading sol page (rover=$roverId)" }
            LoadResult.Error(e)
        }
    }

    /**
     * Finds the next non-empty page in [step] direction.
     *
     * For unfiltered feeds: returns a continuation page as soon as a single [findDay] segment
     * hits its budget, so each [load] stays bounded. Paging 3 auto-enqueues another load from
     * the continuation key.
     *
     * For camera-filtered feeds: keeps scanning across segments within the same [load] (filtered
     * data is sparse, so yielding an empty continuation page too eagerly risks stalling — Paging 3
     * does not reliably re-trigger loads after empty pages). The cumulative scan is capped at
     * [FILTER_LOAD_BUDGET], after which we yield a continuation page anyway, so a far-off or absent
     * match can never walk the rover's whole sol range in one synchronous load.
     */
    private suspend fun findNextPage(start: Long, step: Int): LoadResult<Long, MarsImage> {
        var key = start
        while (true) {
            when (val r = findDay(key, step)) {
                is FindResult.Found -> return r.day.toPage()
                is FindResult.Exhausted -> {
                    val reachedLoadBudget = cameras.isNotEmpty() &&
                        abs(r.nextSol - start) >= FILTER_LOAD_BUDGET
                    if (cameras.isEmpty() || reachedLoadBudget) {
                        return if (step > 0) continuationPage(prevKey = null, nextKey = clampMax(r.nextSol))
                        else continuationPage(prevKey = clampMin(r.nextSol), nextKey = null)
                    }
                    key = r.nextSol
                }
                FindResult.End -> return terminalPage()
            }
        }
    }

    /**
     * Refresh must never return a continuation page (empty data with non-null keys). An empty
     * itemCount produces no viewport hints, so Paging 3 never enqueues the append/prepend that
     * would resume the scan — the UI stays permanently stuck on the empty/retry state.
     *
     * Instead, scan alternating forward/backward (one [scanBudget] at a time) until a non-empty
     * sol is found or both directions hit the rover bounds. The loop is finite: each pass advances
     * the frontier by [scanBudget] sols, bounded by [minSol]..[maxSol]. As an extra guard — Refresh
     * is user-triggered, so its latency is visible — a direction also stops once its frontier passes
     * [REFRESH_SCAN_LIMIT] sols from the anchor without a hit, so a refresh anchored inside a long
     * empty gap returns a terminal page instead of probing the rover's whole range synchronously.
     */
    private suspend fun loadRefresh(start: Long): LoadResult<Long, MarsImage> {
        var fwdSol: Long? = start
        var bwdSol: Long? = start - 1
        while (fwdSol != null || bwdSol != null) {
            fwdSol?.let { sol ->
                when (val r = findDay(sol, step = +1)) {
                    is FindResult.Found -> return r.day.toPage()
                    is FindResult.Exhausted ->
                        fwdSol = r.nextSol.takeIf { abs(it - start) < REFRESH_SCAN_LIMIT }
                    FindResult.End -> fwdSol = null
                }
            }
            bwdSol?.let { sol ->
                when (val r = findDay(sol, step = -1)) {
                    is FindResult.Found -> return r.day.toPage()
                    is FindResult.Exhausted ->
                        bwdSol = r.nextSol.takeIf { abs(it - start) < REFRESH_SCAN_LIMIT }
                    FindResult.End -> bwdSol = null
                }
            }
        }
        return terminalPage()
    }

    /**
     * Walks [step] sols from [startSol] toward the rover bound, returning the first sol that has
     * (camera-filtered) photos. Capped at [budget] network probes per call; sols memoized in
     * [knownEmptySols] are skipped for free.
     */
    private suspend fun findDay(startSol: Long, step: Int, budget: Int = scanBudget): FindResult {
        var sol = startSol
        var probes = 0
        while (sol in minSol..maxSol) {
            // Memoized empty sols are skipped without a network probe and without spending budget;
            // the loop stays bounded because the sol range minSol..maxSol is finite.
            if (sol in knownEmptySols) {
                sol += step
                continue
            }
            if (probes >= budget) return FindResult.Exhausted(sol)
            // Fetch the whole sol (camera = null) and filter in-memory, so the empty-sol skip is
            // camera-aware for every rover — Perseverance/Insight raw APIs ignore the camera param.
            val photos = photosRepository
                .refreshImages(PhotosQueryRequest(roverId, sol, camera = null))
                .filterByCameras(cameras)
            probes++
            if (photos.isNotEmpty()) {
                return FindResult.Found(Day(sol, cacheAndMerge(photos)))
            }
            knownEmptySols += sol
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
        get() = if (cameras.isEmpty()) UNFILTERED_SCAN_BUDGET else FILTER_SCAN_BUDGET

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
            // Merge ONLY user state (favorite + stats) from Room — the rest of the row may be
            // stale (the insert is IGNORE, so e.g. `order` keeps its first-cached value).
            networkPhotos.map { network ->
                val row = persisted[network.id] ?: return@map network
                network.copy(favorite = row.favorite, stats = row.stats)
            }
        } catch (e: Exception) {
            Logger.e("SolPagingSource", e) { "Write-through cache failed; serving network photos" }
            networkPhotos
        }
    }

    /**
     * In-memory camera filter.
     *
     * Standard rovers (Curiosity, Opportunity, Spirit) return abbreviated camera names in
     * [RoverCamera.name] (e.g. "FHAZ"), so a direct name equality check is sufficient.
     *
     * Perseverance returns a compact instrument identifier in [RoverCamera.fullName]
     * (e.g. "FRONT_HAZCAM_LEFT_A", "NAVCAM_LEFT", "MCZ_RIGHT"). The [RoverMissionData]
     * spec's [fullName][CameraSpec.fullName] is stored as a prefix of those identifiers
     * (e.g. "FRONT_HAZCAM", "NAVCAM", "MCZ"), so we use a startsWith check.
     */
    private fun List<MarsImage>.filterByCameras(cameras: Set<String>): List<MarsImage> {
        if (cameras.isEmpty()) return this
        val specsByName = com.sirelon.marsroverphotos.domain.models.mission.RoverMissionData
            .getCamerasForRover(roverId)
            .associateBy { it.name.uppercase() }
        return filter { image ->
            val cam = image.camera ?: return@filter false
            cameras.any { filter ->
                val spec = specsByName[filter.uppercase()]
                cam.name.equals(filter, ignoreCase = true) ||
                    cam.fullName.equals(filter, ignoreCase = true) ||
                    (spec != null && cam.fullName.startsWith(spec.fullName, ignoreCase = true))
            }
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
        /** Max sols probed per [findDay] segment when a camera filter is active. */
        private const val FILTER_SCAN_BUDGET = 60
        /**
         * Cumulative cap on sols scanned across one filtered [load]. The filtered feed scans
         * several [FILTER_SCAN_BUDGET] segments synchronously to keep continuation pages rare, but
         * this ceiling bounds the worst case so a sparse/absent camera can't fire an unbounded run
         * of sequential network probes in a single load.
         */
        private const val FILTER_LOAD_BUDGET = 300
        /** Max sols probed per load() for the unfiltered feed; larger since its data is dense. */
        private const val UNFILTERED_SCAN_BUDGET = 300
        /**
         * Cumulative per-direction cap for a single Refresh scan. Bounds the worst case (a refresh
         * anchored inside a long empty gap) to ~2 × this many sequential probes instead of the
         * rover's whole sol range. Generous enough to cover any realistic gap for the dense
         * sol-keyed feeds (Curiosity, Perseverance, Insight).
         */
        private const val REFRESH_SCAN_LIMIT = 2000
        private const val CACHE_KEEP_LIMIT = 2000
        /** Run cache eviction once every this many cached pages (keeps it off the hot path). */
        private const val EVICT_EVERY_N_LOADS = 20
    }
}
