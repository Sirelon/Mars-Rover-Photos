package com.sirelon.marsroverphotos.firebase.photos

import com.google.firebase.database.*
import com.sirelon.marsroverphotos.feature.firebase.FirebaseConstants
import com.sirelon.marsroverphotos.feature.firebase.setValueObservable
import com.sirelon.marsroverphotos.feature.firebase.singleEventFirebase
import com.sirelon.marsroverphotos.feature.firebase.toFireBase

import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created on 12/04/2017 17:55.
 */
internal class FirebasePhotos : IFirebasePhotos {
    override fun countOfAllPhotos(): Single<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updatePhotoShareCounter(photo: MarsPhoto): Observable<Long> {
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

    override fun updatePhotoSaveCounter(photo: MarsPhoto): Observable<Long> {
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

    override fun updatePhotoScaleCounter(photo: MarsPhoto): Observable<Long> {
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

    override fun updatePhotoSeenCounter(photo: MarsPhoto): Observable<Long> {
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
                .map { it.getValue(Long::class.java)!! + 1 }
                .flatMap { saveRef.setValueObservable(it) }
    }


    private fun incrementSaveCount(photo: MarsPhoto): Observable<Long> {
        val saveRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SAVE)
        return saveRef.singleEventFirebase()
                .map { it.getValue(Long::class.java)!! + 1 }
                .flatMap { saveRef.setValueObservable(it) }
    }

    private fun incrementSeenCount(photo: MarsPhoto): Observable<Long> {
        val seenRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SEEN)
        return seenRef.singleEventFirebase()
                .map { it.getValue(Long::class.java)!! + 1 }
                .flatMap { seenRef.setValueObservable(it) }
    }

    private fun incrementScaleCount(photo: MarsPhoto): Observable<Long> {
        val scaleRef = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo) + "/" +
                FirebaseConstants.PHOTOS_SCALE)
        return scaleRef.singleEventFirebase()
                .map { it.getValue(Long::class.java)!! + 1 }
                .flatMap { scaleRef.setValueObservable(it) }
    }

    private fun firebasePhotoRef(photo: MarsPhoto) = "${FirebaseConstants.PHOTOS_TABLE}/${photo.id}"

    private fun addMarsPhoto(photo: MarsPhoto): Observable<MarsPhoto> {
        val ref = FirebaseDatabase.getInstance().getReference(firebasePhotoRef(photo))
        return ref.setValueObservable(photo.toFireBase()).map { photo }
    }
}