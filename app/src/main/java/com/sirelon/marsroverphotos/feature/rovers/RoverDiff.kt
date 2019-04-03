package com.sirelon.marsroverphotos.feature.rovers

import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import java.util.ArrayList

/**
 * @author romanishin
 * @since 01.11.16 on 12:31
 */


object RoverDiff {
    val IMAGE = 1
    val TOTAL_PHOTOS = 2
    val MAX_SOL = 3
    val LAST_PHOTO_DATE = 4
    val LAUNCH_DATE = 5
    val LANDING_DATE = 6

    fun getChangePayload(oldModel: ViewType, newModel: ViewType): ArrayList<Int>? {
        if (oldModel is Rover && newModel is Rover)
            return getChangePayload(oldModel, newModel)
        return null
    }

    fun getChangePayload(oldRover: Rover, newRover: Rover): ArrayList<Int>? {
        val array = ArrayList<Int>()
        if (!oldRover.iamgeUrl.equals(newRover.iamgeUrl)) {
            array.add(1)
        }
        if (oldRover.totalPhotos != newRover.totalPhotos) {
            array.add(2)
        }
        if (oldRover.maxSol != newRover.maxSol) {
            array.add(3)
        }
        if (!oldRover.maxDate.equals(newRover.maxDate)) {
            array.add(4)
        }
        if (!oldRover.launchDate.equals(newRover.launchDate)) {
            array.add(5)
        }
        if (!oldRover.landingDate.equals(newRover.landingDate)) {
            array.add(6)
        }

        return array
    }
}

