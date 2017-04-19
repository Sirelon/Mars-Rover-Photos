package com.sirelon.marsroverphotos.tracker

import com.sirelon.marsroverphotos.models.MarsPhoto

/**
 * Created on 19/04/2017 15:32.
 */
interface ITracker {

    fun trackSeen(photo: MarsPhoto)

    fun trackScale(photo: MarsPhoto)

    fun trackShare(photo: MarsPhoto, packageName: String)

    fun trackSave(photo: MarsPhoto)

}