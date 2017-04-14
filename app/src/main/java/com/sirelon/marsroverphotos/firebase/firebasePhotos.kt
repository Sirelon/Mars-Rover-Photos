package com.sirelon.marsroverphotos.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
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

    fun updatePhotoShareCounter(photo: MarsPhoto): Observable<Long> {
        val photoRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo))

        return Observable.just(photoRef)
                .flatMap { photoRef.singleEventFirebase() }
                .flatMap {
                    if (!it.exists()) {
                        addMarsPhoto(photo).map { 1L }
                    } else {
                        incrementShareCount(photo)
                    }
                }
    }

    fun updatePhotoSaveCounter(photo: MarsPhoto): Observable<Long> {
        val photoRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo))

        return Observable.just(photoRef)
                .flatMap { photoRef.singleEventFirebase() }
                .flatMap {
                    if (!it.exists()) {
                        addMarsPhoto(photo).map { 1L }
                    } else {
                        incrementSaveCount(photo)
                    }
                }
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

    private fun incrementShareCount(photo: MarsPhoto): Observable<Long> {
        val saveRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SHARE)
        return saveRef.singleEventFirebase()
                .map { it.getValue(Long::class.java) + 1 }
                .flatMap { saveRef.setValueObservable<Long>(it) }
    }


    private fun incrementSaveCount(photo: MarsPhoto): Observable<Long> {
        val saveRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SAVE)
        return saveRef.singleEventFirebase()
                .map { it.getValue(Long::class.java) + 1 }
                .flatMap { saveRef.setValueObservable<Long>(it) }
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

    private fun firebasePhotoRef(photo: MarsPhoto) = "${FirebaseConstants.PHOTOS_TABLE}/${photo.id}"

}