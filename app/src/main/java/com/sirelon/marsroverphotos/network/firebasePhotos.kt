package com.sirelon.marsroverphotos.network

import com.google.firebase.database.FirebaseDatabase
import com.sirelon.marsroverphotos.extensions.setValueObservable
import com.sirelon.marsroverphotos.models.FirebasePhoto
import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable

/**
 * Created on 12/04/2017 17:55.
 */
object firebasePhotos {

    fun addMarsPhoto(photo: MarsPhoto): Observable<MarsPhoto> {
        val ref = FirebaseDatabase.getInstance().getReference("${FirebaseConstants.PHOTOS_TABLE}/${photo.id}")
        return ref.setValueObservable(photo.toFireBase()).map { photo }
    }

    private fun MarsPhoto.toFireBase(): FirebasePhoto = FirebasePhoto(this)
}