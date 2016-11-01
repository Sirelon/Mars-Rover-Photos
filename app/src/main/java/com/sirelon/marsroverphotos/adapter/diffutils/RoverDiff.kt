package com.sirelon.marsroverphotos.adapter.diffutils

import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import java.util.*

/**
 * @author romanishin
 * @since 01.11.16 on 12:31
 */

fun getChangePayload(oldModel: ViewType, newModel: ViewType): ArrayList<Int>? {
    if (oldModel is Rover && newModel is Rover)
        return getChangePayload(oldModel as Rover, newModel as Rover)
    return null
}

object RoverDiff {
    val IMAGE = 1
    val TOTAL_PHOTOS = 2
    val MAX_SOL = 3
    val MAX_DATE = 4
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

    return array
}