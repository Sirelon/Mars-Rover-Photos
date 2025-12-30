package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.dao.RoverDao
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.network.models.RoverInfo
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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

        initializationJob = applicationScope.launch(SupervisorJob()) {
            try {
                // Seed initial rover data
                seedRovers()

                // Start observing Perseverance photo count updates
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

                // Load latest rover info from NASA API
                launch {
                    try {
                        loadFromServer()
                    } catch (e: Exception) {
                        Logger.e("RoversRepository", e) { "Error loading rover info from server" }
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
            val perseverance = Rover(
                id = PERSEVERANCE_ID,
                name = "Perseverance",
                drawableName = "img_perseverance",
                landingDate = "2021-02-18",
                launchDate = "2020-07-30",
                status = "active",
                maxSol = 95,
                maxDate = "2021-07-30",
                totalPhotos = 74525
            )
            val dateUtilP = RoverDateUtil(perseverance)
            perseverance.maxDate = dateUtilP.parseTime(currentTimeMillis)
            perseverance.maxSol = dateUtilP.solFromDate(currentTimeMillis)

            // Insight (active)
            val insight = Rover(
                id = INSIGHT_ID,
                name = "Insight",
                drawableName = "img_insight",
                landingDate = "2018-11-26",
                launchDate = "2018-05-05",
                status = "active",
                maxSol = 126,
                maxDate = "2021-05-26",
                totalPhotos = 5731
            )
            val dateUtil = RoverDateUtil(insight)
            insight.maxDate = dateUtil.parseTime(currentTimeMillis)
            insight.maxSol = dateUtil.solFromDate(currentTimeMillis)

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
            Logger.d("RoversRepository") { "Rovers seeded successfully" }
        } catch (e: Exception) {
            Logger.e("RoversRepository", e) { "Error seeding rovers" }
        }
    }

    /**
     * Load rover information from NASA API and update the database.
     * Only updates Curiosity, Opportunity, and Spirit (older rovers with stable API).
     */
    private suspend fun loadFromServer() = coroutineScope {
        try {
            val roversName = listOf("curiosity", "opportunity", "spirit")
            val rovers = roversName.asFlow()
                .map { async { api.getRoverInfo(it) } }
                .map { it.await() }
                .toList()

            updateRoversByInfo(rovers)
            Logger.d("RoversRepository") { "Loaded ${rovers.size} rovers from server" }
        } catch (e: Exception) {
            Logger.e("RoversRepository", e) { "Error loading rovers from server" }
        }
    }

    private suspend fun updateRoversByInfo(list: List<RoverInfo>) {
        list.forEach { updateRoverByInfo(it) }
    }

    private suspend fun updateRoverByInfo(roverInfo: RoverInfo) {
        try {
            Logger.d("RoversRepository") { "Updating rover: ${roverInfo.name}" }
            roverDao.updateRover(
                name = roverInfo.name,
                landingDate = roverInfo.landingDate,
                launchDate = roverInfo.launchDate,
                maxSol = roverInfo.maxSol,
                maxDate = roverInfo.maxDate,
                totalPhotos = roverInfo.totalPhotos
            )
        } catch (e: Exception) {
            Logger.e("RoversRepository", e) { "Error updating rover: ${roverInfo.name}" }
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
