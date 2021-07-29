package com.sirelon.marsroverphotos.firebase.photos

import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 11/24/17 21:09 for Mars-Rover-Photos.
 */
interface IFirebasePhotos {

    suspend fun countOfInsightPhotos(): Long
    suspend fun updatePhotoShareCounter(photo: MarsImage): Long
    suspend fun updatePhotoSaveCounter(photo: MarsImage): Long
    suspend fun updatePhotoScaleCounter(photo: MarsImage): Long
    suspend fun updatePhotoSeenCounter(photo: MarsImage): Long
    suspend fun loadPopularPhotos(
        count: Int = 10,
        lastPhotoId: String? = null
    ): List<FirebasePhoto>
}