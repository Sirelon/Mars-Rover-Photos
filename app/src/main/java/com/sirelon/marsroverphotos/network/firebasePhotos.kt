package com.sirelon.marsroverphotos.network

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.sirelon.marsroverphotos.extensions.setValueObservable
import com.sirelon.marsroverphotos.extensions.singleEventFirebase
import com.sirelon.marsroverphotos.models.FirebasePhoto
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.RoverCamera
import io.reactivex.Observable

/**
 * Created on 12/04/2017 17:55.
 */
object firebasePhotos {

    fun addMarsPhoto(photo: MarsPhoto): Observable<MarsPhoto> {
        val ref = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo))
        return ref.setValueObservable(photo.toFireBase()).map { photo }
    }

    fun updatePhotoScaleCounter(photo: MarsPhoto): Observable<Long> {
        val photoRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo))

        return Observable.just(photoRef)
                .flatMap { photoRef.singleEventFirebase() }
                .flatMap {
                    if (!it.exists()) {
                        addMarsPhoto(photo).map { 1L }
                    } else {
                        incrementScaleCount(photo)
                    }
                }
    }

    fun updatePhotoSeenCounter(photo: MarsPhoto): Observable<Long> {
        val photoRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo))

        return Observable.just(photoRef)
                .flatMap { photoRef.singleEventFirebase() }
                .flatMap {
                    if (!it.exists()) {
                        addMarsPhoto(photo).map { 1L }
                    } else {
                        incrementSeenCount(photo)
                    }
                }
    }

    private fun incrementSeenCount(photo: MarsPhoto): Observable<Long> {
        val seenRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SEEN)
        return seenRef.singleEventFirebase()
                .map { it.getValue(Long::class.java) + 1 }
                .flatMap { seenRef.setValueObservable<Long>(it) }
    }

    private fun incrementScaleCount(photo: MarsPhoto): Observable<Long> {
        val scaleRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SCALE)
        return scaleRef.singleEventFirebase()
                .map { it.getValue(Long::class.java) + 1 }
                .flatMap { scaleRef.setValueObservable<Long>(it) }
    }

    private fun DataSnapshot.toFirebasePhoto(): FirebasePhoto {
        return this.getValue(FirebasePhoto::class.java)
    }

    private fun FirebasePhoto.toMarsPhoto() = MarsPhoto(
            id,
            sol,
            name,
            imageUrl,
            earthDate,
            RoverCamera.empty()
    )

    private fun firebasePhotoRef(photo: MarsPhoto) = "${FirebaseConstants.PHOTOS_TABLE}/${photo.id}"

    private fun MarsPhoto.toFireBase(): FirebasePhoto = FirebasePhoto(this)

}