package com.sirelon.marsroverphotos.firebase.photos

import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable

/**
 * Created on 11/24/17 21:09 for Mars-Rover-Photos.
 */
interface IFirebasePhotos {
    fun addMarsPhoto(photo: MarsPhoto): Observable<MarsPhoto>
    fun updatePhotoShareCounter(photo: MarsPhoto): Observable<Long>
    fun updatePhotoSaveCounter(photo: MarsPhoto): Observable<Long>
    fun updatePhotoScaleCounter(photo: MarsPhoto): Observable<Long>
    fun updatePhotoSeenCounter(photo: MarsPhoto): Observable<Long>
}