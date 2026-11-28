package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.MissionDay
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.DayOnMarsRepository
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Answers "what did every mission photograph on this Earth date" through
 * [PhotosRepository.photosTakenOn], one request per mission, all missions in parallel.
 *
 * Answers are cached per date for the process, because the screen re-asks on every visit. Two
 * kinds are not cached, so the next ask goes to the network again: a date where any mission
 * failed, and a date in the last [RECENT_DAYS] days while an active rover covers it, because an
 * active rover downlinks a day's photos over the following days and weeks.
 */
class DayOnMarsRepositoryImpl(
    private val roversRepository: RoversRepository,
    private val photosRepository: PhotosRepository,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.UTC) },
) : DayOnMarsRepository {

    private val mutex = Mutex()
    private val cache = mutableMapOf<LocalDate, List<MissionDay>>()

    override suspend fun dateRange(): ClosedRange<LocalDate>? {
        val first = rovers().mapNotNull { parse(it.landingDate) }.minOrNull() ?: return null
        return first..today()
    }

    override suspend fun missionsOn(date: LocalDate): List<MissionDay> {
        mutex.withLock { cache[date] }?.let { return it }
        val missions = coroutineScope {
            rovers()
                .filter { date in it.photoDates() }
                .map { rover -> async { MissionDay(rover, photosOf(rover, date)) } }
                .awaitAll()
        }
        if (missions.isCacheable(date)) mutex.withLock { cache[date] = missions }
        return missions
    }

    private fun List<MissionDay>.isCacheable(date: LocalDate): Boolean {
        if (any { it.photos == MissionDay.Photos.Failed }) return false
        val settled = date < today().minus(RECENT_DAYS, DateTimeUnit.DAY)
        return settled || none { it.rover.isActive }
    }

    private suspend fun photosOf(rover: Rover, date: LocalDate): MissionDay.Photos = try {
        val day = photosRepository.photosTakenOn(rover.id, date)
        when {
            day == null -> MissionDay.Photos.NotDated
            day.images.isEmpty() -> MissionDay.Photos.None
            else -> MissionDay.Photos.Found(
                sols = day.images.map { it.sol }.distinct().sorted(),
                images = day.images.sortedBy(MarsImage::earthDate),
                total = maxOf(day.total, day.images.size),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e("DayOnMarsRepository", e) { "Photos of ${rover.name} on $date failed" }
        MissionDay.Photos.Failed
    }

    /** The roster, waiting out the first launch's seeding instead of answering with nothing. */
    private suspend fun rovers(): List<Rover> = roversRepository.getRovers().first { it.isNotEmpty() }

    /**
     * The dates this mission can have photos for: landing to its last date, or to today while it
     * is active (an active rover's stored `maxDate` trails its feed).
     */
    private fun Rover.photoDates(): ClosedRange<LocalDate> {
        val start = parse(landingDate) ?: return EMPTY_RANGE
        val end = if (isActive) today() else parse(maxDate) ?: return EMPTY_RANGE
        return start..end
    }

    private fun parse(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

    private companion object {
        /** How long an active rover's day can still gain photos. */
        const val RECENT_DAYS = 30

        val EMPTY_RANGE: ClosedRange<LocalDate> = LocalDate(1, 1, 2)..LocalDate(1, 1, 1)
    }
}
