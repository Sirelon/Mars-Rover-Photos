package com.sirelon.marsroverphotos.firebase.photos

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sirelon.marsroverphotos.feature.firebase.FirebaseConstants
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.feature.firebase.toFireBase
import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single

/**
 * Created on 11/25/17 17:56 for Mars-Rover-Photos.
 */
internal class FirestorePhotos : IFirebasePhotos {

    override fun countOfAllPhotos(): Single<Int> {
        return Single.just(1)
    }

    private fun photosCollection() =
        FirebaseFirestore.getInstance().collection(FirebaseConstants.COLECTION_PHOTOS)

    override fun updatePhotoShareCounter(photo: MarsPhoto): Observable<Long> {
        return getOrCreate(photo)
            .flatMapObservable {
                updatePhoto(it.apply { it.shareCounter++ })
            }
            .map { it.shareCounter }
    }

    override fun updatePhotoSaveCounter(photo: MarsPhoto): Observable<Long> {
        return getOrCreate(photo)
            .flatMapObservable {
                updatePhoto(it.apply { it.saveCounter++ })
            }
            .map { it.saveCounter }
    }

    override fun updatePhotoScaleCounter(photo: MarsPhoto): Observable<Long> {
        return getOrCreate(photo)
            .flatMapObservable {
                updatePhoto(it.apply { it.scaleCounter++ })
            }
            .map { it.scaleCounter }
    }

    override fun updatePhotoSeenCounter(photo: MarsPhoto): Observable<Long> {
        return getOrCreate(photo)
            .flatMapObservable {
                updatePhoto(it.apply { it.seeCounter++ })
            }
            .map { it.seeCounter }
    }

    override fun loadPopularPhotos(count: Int, lastPhoto: FirebasePhoto?): Observable<List<FirebasePhoto>> {
        return Observable.create<List<FirebasePhoto>> { emitter: ObservableEmitter<List<FirebasePhoto>> ->
            val queryDirection = Query.Direction.DESCENDING
            var first = photosCollection()
                .orderBy("shareCounter", queryDirection)
                .orderBy("saveCounter", queryDirection)
                .orderBy("scaleCounter", queryDirection)
                .orderBy("seeCounter", queryDirection)
                .limit(count.toLong())

            if (lastPhoto != null){
                val documentSnapshot = Tasks.await(photosCollection().document(lastPhoto.id.toString()).get())
                first = first.startAfter(documentSnapshot)
            }

            first.get().addOnCompleteListener({
                if (it.isSuccessful) {
                    val documentSnapshots = it.result
                    val objects = documentSnapshots.toObjects(FirebasePhoto::class.java)
                    emitter.onNext(objects)
                    emitter.onComplete()
                } else {
                    emitter.onError(it.exception!!)
                }
            })
        }
    }

    private fun updatePhoto(photo: FirebasePhoto): Observable<FirebasePhoto> {
        return photosCollection().document(photo.id.toString()).setPhoto(photo)
    }

    private fun getOrCreate(photo: MarsPhoto) =
        getDataSnapshot(photo)
            .flatMap {
                if (it.exists())
                    Single.just(it.toObject(FirebasePhoto::class.java))
                else
                    createPhoto(photo)
            }

    /**
     * If the document does not exist, it will be created.
     * If the document does exist, the data should be merged into the existing document
     */
    private fun DocumentReference.setPhoto(photo: FirebasePhoto): Observable<FirebasePhoto> =
        Observable.create { emitter ->
            this.set(photo, SetOptions.merge())
                .addOnSuccessListener {
                    emitter.onNext(photo)
                    emitter.onComplete()
                }
                .addOnFailureListener { emitter.onError(it) }
        }

    private fun createPhoto(photo: MarsPhoto): Single<FirebasePhoto> {
        val fireBasePhoto = photo.toFireBase()
        return photosCollection().document(photo.id.toString())
            .setPhoto(fireBasePhoto)
            .first(fireBasePhoto)
    }

    private fun getDataSnapshot(photo: MarsPhoto): Single<DocumentSnapshot> =
        Single.create { emitter ->
            photosCollection().document(photo.id.toString())
                .get()
                .addOnSuccessListener { emitter.onSuccess(it) }
                .addOnFailureListener { emitter.onError(it) }
        }
}