package com.sirelon.marsroverphotos.firebase.photos

import androidx.annotation.WorkerThread
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
import com.sirelon.marsroverphotos.utils.randomInt
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single

/**
 * Created on 11/25/17 17:56 for Mars-Rover-Photos.
 */
internal class FirestorePhotos : IFirebasePhotos {

    private fun roversCollection() =
        FirebaseFirestore.getInstance().collection(FirebaseConstants.COLECTION_ROVERS)

    private fun photosCollection() =
        FirebaseFirestore.getInstance().collection(FirebaseConstants.COLECTION_PHOTOS)

    @WorkerThread
    suspend fun removePhoto(photo: MarsPhoto) {
        val task = photosCollection().document(photo.id).delete()
        Tasks.await(task)
    }

    @WorkerThread
    suspend fun makeItPopular(photo: MarsPhoto) {
        val see = randomInt(50, 500)
        val scale = randomInt(10, see)
        val db = mapOf(
            "seeCounter" to see,
            "scaleCounter" to scale,
            "saveCounter" to randomInt(0, scale),
            "shareCounter" to randomInt(0, scale),
        )

        //                .orderBy("shareCounter", queryDirection)
//                .orderBy("saveCounter", queryDirection)
//            .orderBy("seeCounter", queryDirection)
//            .orderBy("scaleCounter", queryDirection)
        val task = photosCollection().document(photo.id).update(db)

    }

    override suspend fun countOfInsightPhotos(): Long {
        val task = roversCollection().document(FirebaseConstants.DOCUMENT_INSIGHT).get()
        return Tasks.await(task).getLong("total_photos") ?: 0L
    }

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

    override fun loadPopularPhotos(
        count: Int,
        lastPhotoId: String?
    ): Observable<List<FirebasePhoto>> {
        return Observable.create<List<FirebasePhoto>> { emitter: ObservableEmitter<List<FirebasePhoto>> ->
            val queryDirection = Query.Direction.DESCENDING
            var first = photosCollection()
                .orderBy("shareCounter", queryDirection)
                .orderBy("saveCounter", queryDirection)
                .orderBy("scaleCounter", queryDirection)
                .orderBy("seeCounter", queryDirection)
                .limit(count.toLong())

//                .orderBy("seeCounter", queryDirection)
//                .orderBy("scaleCounter", queryDirection)

            if (lastPhotoId != null) {
                val documentSnapshot =
                    Tasks.await(photosCollection().document(lastPhotoId).get())
                first = first.startAfter(documentSnapshot)
            }

            first.get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val documentSnapshots = it.result

                    val objects = documentSnapshots?.documents?.map {
                        FirebasePhoto(
                            it["id"].toString(),
                            it["sol"] as Long,
                            it["name"] as String?,
                            it["imageUrl"] as String,
                            it["earthDate"] as String,
                            it["seeCounter"] as Long,
                            it["scaleCounter"] as Long,
                            it["saveCounter"] as Long,
                            it["shareCounter"] as Long,
                        )
                    }
                    emitter.onNext(objects ?: emptyList())
                    emitter.onComplete()
                } else {
                    emitter.onError(it.exception!!)
                }
            }
        }
    }

    private fun updatePhoto(photo: FirebasePhoto): Observable<FirebasePhoto> {
        return photosCollection().document(photo.id).setPhoto(photo)
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