package com.sirelon.marsroverphotos.feature.firebase

import com.google.firebase.firestore.Exclude
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.ViewType
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 12/04/2017 19:05.
 */
data class FirebasePhoto(
    val id: String,
    val sol: Long,
    val name: String?,
    val imageUrl: String,
    val earthDate: String,
    var seeCounter: Long,
    var favoriteCounter: Long,
    var scaleCounter: Long,
    var saveCounter: Long,
    var shareCounter: Long
) : ViewType {

    @Exclude
    override fun getViewId() = id

    constructor(photo: MarsPhoto) : this(
        id = photo.id,
        sol = photo.sol,
        name = photo.name,
        imageUrl = photo.imageUrl,
        earthDate = photo.earthDate,
        seeCounter = 0,
        scaleCounter = 0,
        saveCounter = 0,
        shareCounter = 0,
        favoriteCounter = 0,
    )

    constructor() : this(
        id = "",
        sol = -1,
        name = null,
        imageUrl = "",
        earthDate = "",
        seeCounter = 0,
        scaleCounter = 0,
        saveCounter = 0,
        shareCounter = 0,
        favoriteCounter = 0,
    )

    override fun getViewType() = AdapterConstants.POPULAR_PHOTO

    fun toMarsImage(order: Int) = MarsImage(
        id = id,
        sol = sol,
        name = name,
        imageUrl = imageUrl,
        earthDate = earthDate,
        camera = null, // TODO: Load camera and favorite from DataBase
        favorite = false, // TODO: Load camera and favorite from DataBase
        popular = true,
        order = order,
        stats = MarsImage.Stats(
            see = seeCounter,
            scale = scaleCounter,
            save = saveCounter,
            share = shareCounter,
            favorite = favoriteCounter,
        )
    )
}