package com.sirelon.marsroverphotos.domain.models

import com.sirelon.marsroverphotos.data.database.entities.MarsImage

/**
 * What one mission photographed on one Earth date (UTC). The input of "A day on Mars": the user
 * picks a date and every mission that was on the surface that day answers with one of these.
 */
data class MissionDay(
    val rover: Rover,
    val photos: Photos,
) {
    sealed interface Photos {
        /**
         * Photos taken that date, in the order they were taken. [images] can be a sample: [total]
         * is how many the mission took that date. [sols] are the sols they belong to, ascending
         * (a UTC date overlaps one or two sols).
         */
        data class Found(val sols: List<Long>, val images: List<MarsImage>, val total: Int) : Photos

        /** The mission was on the surface, but its archive holds no photo taken that date. */
        data object None : Photos

        /**
         * The mission's photo source carries no shot date, so it cannot answer by date. Spirit and
         * Opportunity: their images.nasa.gov records are dated by press release, not by shot.
         */
        data object NotDated : Photos

        /** The source could not be reached. Other missions on the same date are unaffected. */
        data object Failed : Photos
    }
}

/**
 * One mission's photos taken on one Earth date: [images], at most one page of the source, and
 * [total], how many the source holds for that date.
 */
data class DayPhotos(val images: List<MarsImage>, val total: Int)
