package com.sirelon.marsroverphotos.feature.firebase

import com.google.firebase.firestore.Exclude
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 12/04/2017 19:05.
 */
data class FirebasePhoto(
    val id: Long,
    val sol: Long,
    val name: String?,
    val imageUrl: String,
    val earthDate: String,
    var seeCounter: Long,
    var scaleCounter: Long,
    var saveCounter: Long,
    var shareCounter: Long
) : ViewType {

    @Exclude
    override fun getId() = id

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

    override fun getViewType() = AdapterConstants.POPULAR_PHOTO
}