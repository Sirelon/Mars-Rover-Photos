package com.sirelon.marsroverphotos.firebase.photos

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.*
import com.sirelon.marsroverphotos.feature.firebase.FirebaseConstants
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.feature.firebase.toFireBase
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.RoverCamera
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import java.time.ZoneOffset

/**
 * Created on 11/25/17 17:56 for Mars-Rover-Photos.
 */
internal class FirestorePhotos : IFirebasePhotos {

    override fun countOfAllPhotos(): Single<Int> {
        val camera = RoverCamera.empty()
        val marsPhoto = MarsPhoto(
            1003,
            614,
            null,
            "https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00614/opgs/edr/rcam/RRB_451998405EDR_F0311330RHAZ00337M_.JPG",
            "2014-04-28",
            camera
        )

//        Observable.zip(
//                updatePhotoSaveCounter(marsPhoto),
//                updatePhotoScaleCounter(marsPhoto),
//                updatePhotoSeenCounter(marsPhoto),
//                updatePhotoShareCounter(marsPhoto),
//                Function4<Long, Long, Long, Long, Long>
//                { i1: Long, i2: Long, i3: Long, i4: Long -> i1 + i2 + i3 + i4 }
//        )
//                .subscribe({
//                    it.logD()
//                }, Throwable::printStackTrace)
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
            var first = photosCollection()
                .orderBy("shareCounter")
                .orderBy("saveCounter")
                .orderBy("scaleCounter")
                .orderBy("seeCounter")
                .limit(count.toLong())

            if (lastPhoto != null){
                first = first.startAfter(lastPhoto)
            }

            first.get().addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    val documentSnapshots = it.result
                    emitter.onNext(it.result.toObjects(FirebasePhoto::class.java))
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