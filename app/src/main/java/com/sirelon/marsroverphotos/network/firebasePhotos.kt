package com.sirelon.marsroverphotos.network

import com.google.firebase.database.FirebaseDatabase
import com.sirelon.marsroverphotos.extensions.setValueObservable
import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable

/**
 * Created on 12/04/2017 17:55.
 */
object firebasePhotos {

    fun addMarsPhoto(photo: MarsPhoto): Observable<MarsPhoto> {
        val ref = FirebaseDatabase.getInstance().getReference("${FirebaseConstants.PHOTOS_TABLE}/${photo.id}")
        return ref.setValueObservable(photo.toFireBaseMap()).map { photo }
    }

    private fun MarsPhoto.toFireBaseMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        map.put(FirebaseConstants.PHOTOS_EARTH_DATE, this.earthDate)
        map.put(FirebaseConstants.PHOTOS_IMAGE, this.imageUrl)
        map.put(FirebaseConstants.PHOTO_SOL, this.sol)
        return map
    }
}