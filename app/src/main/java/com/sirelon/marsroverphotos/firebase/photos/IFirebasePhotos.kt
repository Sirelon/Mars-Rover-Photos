package com.sirelon.marsroverphotos.firebase.photos

import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created on 11/24/17 21:09 for Mars-Rover-Photos.
 */
interface IFirebasePhotos {

    suspend fun countOfInsightPhotos(): Long
    fun updatePhotoShareCounter(photo: MarsPhoto): Observable<Long>
    fun updatePhotoSaveCounter(photo: MarsPhoto): Observable<Long>
    fun updatePhotoScaleCounter(photo: MarsPhoto): Observable<Long>
    fun updatePhotoSeenCounter(photo: MarsPhoto): Observable<Long>
    fun loadPopularPhotos(
        count: Int = 10,
        lastPhotoId: String? = null
    ): Observable<List<FirebasePhoto>>
}