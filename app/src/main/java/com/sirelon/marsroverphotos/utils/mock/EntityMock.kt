package com.sirelon.marsroverphotos.utils.mock

import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.utils.randomImageUrl
import com.sirelon.marsroverphotos.utils.randomInt
import com.sirelon.marsroverphotos.utils.randomLong
import com.sirelon.marsroverphotos.utils.randomRole

/**
 * Created on 2/6/18 23:07 for Mars-Rover-Photos.
 */
fun randomFirebasePhoto() = FirebasePhoto(
    id = randomLong(),
    sol = randomLong(),
    imageUrl = randomImageUrl(),
    earthDate = randomRole(),
    name = randomRole(),
    seeCounter = randomLong(),
    scaleCounter = randomLong(),
    saveCounter = randomLong(),
    shareCounter = randomLong()
)
