package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.DayPhotos
import com.sirelon.marsroverphotos.domain.models.MissionDay
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val curiosity = rover(CURIOSITY_ID, "Curiosity", landing = "2012-08-06", maxDate = "2017-09-18", status = "active")
private val spirit = rover(SPIRIT_ID, "Spirit", landing = "2004-01-04", maxDate = "2010-03-21")
private val viking1 = rover(VIKING_1_ID, "Viking 1", landing = "1976-07-20", maxDate = "1982-10-28")

private val TODAY = LocalDate(2026, 11, 28)

class DayOnMarsRepositoryImplTest {

    @Test
    fun foundPhotosAreInShotOrder_withTheirSolsAndTheDateTotal() = runTest {
        val date = LocalDate(2023, 11, 7)
        val photos = FakeDatedPhotos(
            CURIOSITY_ID to DayPhotos(
                images = listOf(image("late", 4001, "2023-11-07T22:00:00Z"), image("early", 4000, "2023-11-07T00:10:00Z")),
                total = 335,
            ),
        )

        val found = repository(listOf(curiosity), photos).missionsOn(date).single().photos

        assertIs<MissionDay.Photos.Found>(found)
        assertEquals(listOf("early", "late"), found.images.map { it.id })
        assertEquals(listOf(4000L, 4001L), found.sols)
        assertEquals(335, found.total)
        assertEquals(listOf(CURIOSITY_ID to date), photos.asked)
    }

    @Test
    fun leavesOutMissionsNotOnTheSurfaceThatDate() = runTest {
        val repo = repository(listOf(viking1, spirit, curiosity), FakeDatedPhotos())

        assertEquals(listOf(viking1), repo.missionsOn(LocalDate(1980, 1, 1)).map { it.rover })
        assertEquals(listOf(spirit), repo.missionsOn(LocalDate(2005, 1, 1)).map { it.rover })
        assertTrue(repo.missionsOn(LocalDate(1970, 1, 1)).isEmpty())
    }

    @Test
    fun anActiveRoverCoversDatesPastItsStoredMaxDate() = runTest {
        val repo = repository(listOf(curiosity), FakeDatedPhotos())

        assertEquals(listOf(curiosity), repo.missionsOn(LocalDate(2025, 1, 1)).map { it.rover })
    }

    @Test
    fun aSourceWithoutShotDatesAnswersNotDated() = runTest {
        val repo = repository(listOf(spirit), FakeDatedPhotos(undated = setOf(SPIRIT_ID)))

        assertEquals(MissionDay.Photos.NotDated, repo.missionsOn(LocalDate(2005, 1, 1)).single().photos)
    }

    @Test
    fun aDateWithNoPhotosAnswersNone() = runTest {
        val repo = repository(listOf(viking1), FakeDatedPhotos())

        assertEquals(MissionDay.Photos.None, repo.missionsOn(LocalDate(1980, 1, 1)).single().photos)
    }

    @Test
    fun oneFailingMissionDoesNotHideTheOthers_andTheDateIsNotCached() = runTest {
        val photos = FakeDatedPhotos(failFor = setOf(CURIOSITY_ID))
        // Curiosity and Viking 1 never overlapped; stretch Viking's span so both answer this date.
        val viking = viking1.copy(maxDate = "2030-01-01")
        val repo = repository(listOf(viking, curiosity), photos)
        val date = LocalDate(2020, 1, 1)

        val first = repo.missionsOn(date)
        assertEquals(MissionDay.Photos.None, first[0].photos)
        assertEquals(MissionDay.Photos.Failed, first[1].photos)

        repo.missionsOn(date)
        assertEquals(4, photos.asked.size, "a date with a failure must be asked again")
    }

    @Test
    fun aSettledDateIsCached() = runTest {
        val photos = FakeDatedPhotos()
        val repo = repository(listOf(curiosity), photos)
        val date = LocalDate(2023, 11, 7)

        repo.missionsOn(date)
        repo.missionsOn(date)

        assertEquals(1, photos.asked.size)
    }

    @Test
    fun aRecentDateOfAnActiveRoverIsNotCached() = runTest {
        val photos = FakeDatedPhotos()
        val repo = repository(listOf(curiosity), photos)
        val date = LocalDate(2026, 11, 10)

        repo.missionsOn(date)
        repo.missionsOn(date)

        assertEquals(2, photos.asked.size)
    }

    @Test
    fun aCancelledFetchIsRethrown_notReportedAsFailed_andNotCached() = runTest {
        val photos = FakeDatedPhotos(cancelFor = setOf(CURIOSITY_ID))
        val repo = repository(listOf(curiosity), photos)
        val date = LocalDate(2023, 11, 7)

        assertFailsWith<CancellationException> { repo.missionsOn(date) }
        assertFailsWith<CancellationException> { repo.missionsOn(date) }
        assertEquals(2, photos.asked.size)
    }

    @Test
    fun photosWithoutAnImageUrlAreDropped() = runTest {
        val photos = FakeDatedPhotos(
            CURIOSITY_ID to DayPhotos(listOf(image("blank", 4000, "2023-11-07T01:00:00Z", url = "")), total = 1),
        )

        val day = repository(listOf(curiosity), photos).missionsOn(LocalDate(2023, 11, 7)).single()

        assertEquals(MissionDay.Photos.None, day.photos)
    }

    @Test
    fun aRosterThatNeverSeedsAnswersEmpty_insteadOfHanging() = runTest {
        val repo = DayOnMarsRepositoryImpl(
            roversRepository = FakeRovers(MutableStateFlow(emptyList())),
            photosRepository = FakeDatedPhotos(),
            today = { TODAY },
        )

        assertTrue(repo.missionsOn(LocalDate(2023, 11, 7)).isEmpty())
        assertNull(repo.dateRange())
    }

    @Test
    fun theCacheKeepsOnlyTheMostRecentDates() = runTest {
        val photos = FakeDatedPhotos()
        val repo = repository(listOf(curiosity), photos)
        val first = LocalDate(2020, 1, 1)

        repo.missionsOn(first)
        repeat(20) { repo.missionsOn(LocalDate(2020, 2, 1 + it)) }
        repo.missionsOn(first)

        assertEquals(22, photos.asked.size, "the oldest date was evicted and asked again")
    }

    @Test
    fun dateRangeRunsFromTheFirstLandingToToday() = runTest {
        val repo = repository(listOf(curiosity, viking1), FakeDatedPhotos())

        assertEquals(LocalDate(1976, 7, 20)..TODAY, repo.dateRange())
    }

    private fun repository(rovers: List<Rover>, photos: PhotosRepository) = DayOnMarsRepositoryImpl(
        roversRepository = FakeRovers(rovers),
        photosRepository = photos,
        today = { TODAY },
    )
}

private class FakeRovers(private val rovers: Flow<List<Rover>>) : RoversRepository {
    constructor(rovers: List<Rover>) : this(flowOf(rovers))

    override fun initialize() = Unit
    override fun getRovers(): Flow<List<Rover>> = rovers
    override suspend fun loadRoverById(id: Long): Rover? = null
    override suspend fun updateRoverCountPhotos(roverId: Long, photos: Long) = Unit
}

private class FakeDatedPhotos(
    vararg days: Pair<Long, DayPhotos>,
    private val failFor: Set<Long> = emptySet(),
    private val undated: Set<Long> = emptySet(),
    private val cancelFor: Set<Long> = emptySet(),
) : PhotosRepository {
    private val byRover = days.toMap()
    val asked = mutableListOf<Pair<Long, LocalDate>>()

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> = emptyList()

    override suspend fun photosTakenOn(roverId: Long, date: LocalDate): DayPhotos? {
        asked += roverId to date
        if (roverId in failFor) error("offline")
        if (roverId in cancelFor) throw CancellationException("screen left")
        if (roverId in undated) return null
        return byRover[roverId] ?: DayPhotos(emptyList(), 0)
    }
}

private fun rover(id: Long, name: String, landing: String, maxDate: String, status: String = "complete") = Rover(
    id = id,
    name = name,
    drawableName = "",
    landingDate = landing,
    launchDate = "",
    status = status,
    maxSol = 0L,
    maxDate = maxDate,
    totalPhotos = 0,
)

private fun image(id: String, sol: Long, earthDate: String, url: String = "https://example.test/$id.jpg") = MarsImage(
    id = id,
    order = 0,
    sol = sol,
    name = null,
    imageUrl = url,
    earthDate = earthDate,
    camera = null,
    stats = MarsImage.Stats(0, 0, 0, 0, 0),
)
