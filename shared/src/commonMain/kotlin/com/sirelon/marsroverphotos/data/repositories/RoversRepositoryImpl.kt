package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.dao.RoverDao
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.utils.RoverDateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Implementation of RoversRepository.
 * Manages rover metadata and synchronization with NASA API.
 *
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
class RoversRepositoryImpl(
    private val roverDao: RoverDao,
    private val api: RestApi,
    private val applicationScope: CoroutineScope
) : RoversRepository {

    private var initializationJob: Job? = null

    /**
     * Initialize the repository by seeding the database with rover data
     * and starting background synchronization tasks.
     * Should be called once on app startup.
     */
    override fun initialize() {
        if (initializationJob?.isActive == true) {
            Logger.d("RoversRepository") { "Already initialized" }
            return
        }

        initializationJob = applicationScope.launch {
            try {
                // Seed initial rover data
                seedRovers()

                // Observe and persist photo counts for page-based rovers as they are fetched.
                launch {
                    try {
                        api.perseveranceTotalImages.collectLatest { totalPhotos ->
                            Logger.d("RoversRepository") { "Perseverance total photos: $totalPhotos" }
                            roverDao.updateRoverCountPhotos(PERSEVERANCE_ID, totalPhotos)
                        }
                    } catch (e: Exception) {
                        Logger.e("RoversRepository", e) { "Error observing Perseverance photo count" }
                    }
                }
                launch {
                    try {
                        api.insightTotalImages.collectLatest { totalPhotos ->
                            Logger.d("RoversRepository") { "Insight total photos: $totalPhotos" }
                            roverDao.updateRoverCountPhotos(INSIGHT_ID, totalPhotos)
                        }
                    } catch (e: Exception) {
                        Logger.e("RoversRepository", e) { "Error observing Insight photo count" }
                    }
                }

                // Proactively fetch 1 Perseverance photo so total_images is populated early,
                // giving randomize() an accurate page range on first open.
                launch {
                    try {
                        api.getPerseveranceLatestPhotos(count = 1)
                    } catch (e: Exception) {
                        Logger.e("RoversRepository", e) { "Error pre-fetching Perseverance count" }
                    }
                }

                // Derive Curiosity maxSol from the MSL raw feed (mars-photos manifests are dead).
                // Runs concurrently with seeding so the DB is updated before the photos screen
                // opens, ensuring goToLatest() anchors on the real latest sol.
                launch {
                    try {
                        val latestSol = api.getCuriosityLatestSol() ?: return@launch
                        val curiosity = roverDao.loadRoverById(CURIOSITY_ID) ?: return@launch
                        val dateUtil = RoverDateUtil(curiosity)
                        val maxDate = dateUtil.parseTime(dateUtil.dateFromSol(latestSol))
                        roverDao.updateMaxSolAndDate(CURIOSITY_ID, latestSol, maxDate)
                        Logger.d("RoversRepository") { "Updated Curiosity maxSol=$latestSol, maxDate=$maxDate" }
                    } catch (e: Exception) {
                        Logger.e("RoversRepository", e) { "Error fetching Curiosity maxSol from MSL feed" }
                    }
                }

            } catch (e: Exception) {
                Logger.e("RoversRepository", e) { "Error initializing RoversRepository" }
            }
        }
    }

    /**
     * Seed the database with initial rover data.
     * Uses current time to calculate max dates and sols for active rovers.
     */
    private suspend fun seedRovers() {
        try {
            val currentTimeMillis = Clock.System.now().toEpochMilliseconds()

            // Perseverance (active)
            val perseveranceBase = Rover(
                id = PERSEVERANCE_ID,
                name = "Perseverance",
                drawableName = "img_perseverance",
                landingDate = "2021-02-18",
                launchDate = "2020-07-30",
                status = "active",
                maxSol = 95,
                maxDate = "2021-07-30",
                // Rough estimate for June 2026 (~1900 sols active); updated at runtime from API.
                totalPhotos = 350_000
            )
            val dateUtilP = RoverDateUtil(perseveranceBase)
            val perseverance = perseveranceBase.copy(
                maxDate = dateUtilP.parseTime(currentTimeMillis),
                maxSol = dateUtilP.solFromDate(currentTimeMillis)
            )

            // Insight (mission ended ~Dec 2022, sol ~1435)
            // The mission is complete — do not extend maxSol beyond the actual last sol.
            val insight = Rover(
                id = INSIGHT_ID,
                name = "Insight",
                drawableName = "img_insight",
                landingDate = "2018-11-26",
                launchDate = "2018-05-05",
                status = "complete",
                maxSol = 1435,
                maxDate = "2022-12-21",
                totalPhotos = 5731
            )

            // Curiosity (active)
            val curiosity = Rover(
                id = CURIOSITY_ID,
                name = "Curiosity",
                drawableName = "img_curiosity",
                landingDate = "2012-08-06",
                launchDate = "2011-11-26",
                status = "active",
                maxSol = 1505,
                maxDate = "2017-09-18",
                totalPhotos = 320999
            )

            // Opportunity (complete)
            val opportunity = Rover(
                id = OPPORTUNITY_ID,
                name = "Opportunity",
                drawableName = "img_opportunity",
                landingDate = "2004-01-25",
                launchDate = "2003-07-07",
                status = "complete",
                maxSol = 4535,
                maxDate = "2017-02-22",
                totalPhotos = 187093
            )

            // Spirit (complete)
            val spirit = Rover(
                id = SPIRIT_ID,
                name = "Spirit",
                drawableName = "img_spirit",
                landingDate = "2004-01-04",
                launchDate = "2003-06-10",
                status = "complete",
                maxSol = 2208,
                maxDate = "2010-03-21",
                totalPhotos = 124550
            )

            roverDao.insertRovers(perseverance, insight, curiosity, opportunity, spirit)
            // Force-update Insight's mission-complete bounds for upgraded installs that
            // had stale active/wrong-sol data from a previous version (insertRovers ignores conflicts).
            roverDao.updateRoverMissionBounds(INSIGHT_ID, "complete", 1435, "2022-12-21", 5731)
            Logger.d("RoversRepository") { "Rovers seeded successfully" }
        } catch (e: Exception) {
            Logger.e("RoversRepository", e) { "Error seeding rovers" }
        }
    }

    override suspend fun updateRoverCountPhotos(roverId: Long, photos: Long) {
        Logger.d("RoversRepository") { "Updating photo count for rover $roverId: $photos" }
        roverDao.updateRoverCountPhotos(roverId, photos)
    }

    override fun getRovers(): Flow<List<Rover>> {
        return roverDao.getRovers()
    }

    override suspend fun loadRoverById(id: Long): Rover? {
        return roverDao.loadRoverById(id)
    }
}
