package com.sirelon.marsroverphotos.feature.firebase

import com.sirelon.marsroverphotos.models.MarsPhoto

/**
 * Created on 12/04/2017 19:05.
 */
class FirebasePhoto(
        val id: Long,
        val sol: Long,
        val name: String?,
        val imageUrl: String,
        val earthDate: String,
        val seeCounter: Long,
        val scaleCounter: Long,
        val saveCounter: Long,
        val shareCounter: Long
) {

    constructor(photo: MarsPhoto) : this(
            id = photo.id,
            sol = photo.sol,
            name = photo.name,
            imageUrl = photo.imageUrl,
            earthDate = photo.earthDate,
            seeCounter = 0,
            scaleCounter = 0,
            saveCounter = 0,
            shareCounter = 0
    )

    constructor() : this(
            id = -1,
            sol = -1,
            name = null,
            imageUrl = "",
            earthDate = "",
            seeCounter = 0,
            scaleCounter = 0,
            saveCounter = 0,
            shareCounter = 0
    )

}