package com.sirelon.marsroverphotos.feature.firebase

import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.RoverCamera

/**
 * Created on 13/04/2017 18:54.
 */
fun FirebasePhoto.toMarsPhoto() = MarsPhoto(
        id,
        sol,
        name,
        imageUrl,
        earthDate,
        RoverCamera.empty()
)

fun MarsPhoto.toFireBase(): FirebasePhoto = FirebasePhoto(this)