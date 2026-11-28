package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.models.MissionDay
import kotlinx.datetime.LocalDate

/**
 * "A day on Mars": the photos every mission took on one Earth date.
 *
 * Dates are UTC calendar dates, the same zone every photo source reports its shot time in.
 */
interface DayOnMarsRepository {

    /**
     * The dates a user can pick: the first landing of any mission up to today. Null until the
     * mission roster is available.
     */
    suspend fun dateRange(): ClosedRange<LocalDate>?

    /**
     * One [MissionDay] for every mission on the surface on [date], in the roster's display order.
     * Missions that had not landed yet or had already ended are left out, so a date before 1976
     * returns an empty list. Never throws for a network failure: that mission answers
     * [MissionDay.Photos.Failed] and the others still answer.
     */
    suspend fun missionsOn(date: LocalDate): List<MissionDay>
}
