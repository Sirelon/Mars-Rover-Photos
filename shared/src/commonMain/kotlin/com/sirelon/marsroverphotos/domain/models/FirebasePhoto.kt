package com.sirelon.marsroverphotos.domain.models

import com.sirelon.marsroverphotos.data.database.entities.MarsImage

/**
 * Firebase representation of a Mars rover photo with engagement counters.
 * Used for tracking popular photos and user interactions.
 * Created on 12/04/2017 19:05.
 */
data class FirebasePhoto(
    val id: String,
    val sol: Long,
    val name: String,
    val imageUrl: String,
    val earthDate: String,
    var seeCounter: Long,
    var scaleCounter: Long,
    var saveCounter: Long,
    var shareCounter: Long,
    var favoriteCounter: Long
) {

    constructor(photo: MarsImage) : this(
        id = photo.id,
        sol = photo.sol,
        name = photo.name ?: "",
        imageUrl = photo.imageUrl,
        earthDate = photo.earthDate,
        seeCounter = 0,
        scaleCounter = 0,
        saveCounter = 0,
        shareCounter = 0,
        favoriteCounter = 0,
    )

    /**
     * Convert Firebase photo to MarsImage entity.
     * @param order Display order for the image
     * @return MarsImage entity
     */
    fun toMarsImage(order: Int) = MarsImage(
        id = id,
        sol = sol,
        name = name,
        imageUrl = imageUrl,
        earthDate = earthDate,
        camera = null, // Camera info not stored in Firebase
        favorite = false, // Favorite status managed locally
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

/**
 * Convert MarsImage to FirebasePhoto.
 */
fun MarsImage.toFirebasePhoto(): FirebasePhoto = FirebasePhoto(this)
