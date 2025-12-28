package com.sirelon.marsroverphotos.firebase.photos

import androidx.annotation.WorkerThread
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sirelon.marsroverphotos.feature.facts.EducationalFact
import com.sirelon.marsroverphotos.feature.firebase.FirebaseConstants
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.feature.firebase.toFireBase
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.utils.randomInt

/**
 * Created on 11/25/17 17:56 for Mars-Rover-Photos.
 */
@Suppress("TooManyFunctions")
internal class FirestorePhotos : IFirebasePhotos {

    private fun roversCollection() =
        FirebaseFirestore.getInstance().collection(FirebaseConstants.COLECTION_ROVERS)

    private fun photosCollection() =
        FirebaseFirestore.getInstance().collection(FirebaseConstants.COLECTION_PHOTOS)

    @WorkerThread
    suspend fun removePhoto(photo: MarsImage) {
        val task = photosCollection().document(photo.id).delete()
        Tasks.await(task)
    }

    @WorkerThread
    suspend fun makeItPopular(photo: MarsImage) {
        val see = randomInt(0, 1000)
        val scale = randomInt(0, 1000)
        val db = mapOf(
            "seeCounter" to see,
            "scaleCounter" to scale,
            "saveCounter" to randomInt(0, scale),
            "shareCounter" to randomInt(0, scale),
            "favoriteCounter" to randomInt(0, scale),
        )
        val task = photosCollection().document(photo.id).update(db)
        Tasks.await(task)
    }

    override suspend fun countOfInsightPhotos(): Long {
        val task = roversCollection().document(FirebaseConstants.DOCUMENT_INSIGHT).get()
        return Tasks.await(task).getLong("total_photos") ?: 0L
    }

    override suspend fun updatePhotoShareCounter(photo: MarsImage): Long {
        return getOrCreate(photo)
            .let {
                it.shareCounter++
                updatePhoto(it)
                it.shareCounter
            }
    }

    override suspend fun updatePhotoSaveCounter(photo: MarsImage): Long {
        return getOrCreate(photo)
            .let {
                it.saveCounter++
                updatePhoto(it)
                it.saveCounter
            }
    }

    override suspend fun updatePhotoScaleCounter(photo: MarsImage): Long {
        return getOrCreate(photo)
            .let {
                it.scaleCounter++
                updatePhoto(it)
                it.scaleCounter
            }
    }

    suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long {
        return getOrCreate(photo)
            .let {
                if (increment) {
                    it.favoriteCounter++
                } else {
                    it.favoriteCounter--
                }
                updatePhoto(it)
                it.favoriteCounter
            }
    }

    override suspend fun updatePhotoSeenCounter(photo: MarsImage): Long {
        return getOrCreate(photo)
            .let {
                it.seeCounter++
                updatePhoto(it)
                it.seeCounter
            }
    }

    override suspend fun loadPopularPhotos(
        count: Int,
        lastPhotoId: String?
    ): List<FirebasePhoto> {

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

        val documentSnapshots = first.get().await()
        return documentSnapshots?.documents?.map(DocumentSnapshot::toFirebasePhoto) ?: emptyList()
    }

    private suspend fun updatePhoto(photo: FirebasePhoto): FirebasePhoto {
        return photosCollection().document(photo.id).setPhoto(photo)
    }

    private suspend fun getOrCreate(image: MarsImage): FirebasePhoto {
        val task = photosCollection().document(image.id).get()
        return Tasks.await(task).let {
            if (it.exists()) {
                val fbPhoto = it.toFirebasePhoto()
                fbPhoto
            } else {
                createPhoto(image)
            }
        }
    }

    /**
     * If the document does not exist, it will be created.
     * If the document does exist, the data should be merged into the existing document
     */
    private suspend fun DocumentReference.setPhoto(photo: FirebasePhoto): FirebasePhoto {
        this.set(photo, SetOptions.merge()).await()
        return photo
    }

    private suspend fun createPhoto(photo: MarsImage): FirebasePhoto {
        val fireBasePhoto = photo.toFireBase()
        return photosCollection().document(photo.id)
            .setPhoto(fireBasePhoto)
    }

    override suspend fun loadEducationalFacts(): List<EducationalFact> {
        val collection = FirebaseFirestore.getInstance()
            .collection("educational_facts")

        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val text = doc.getString("text")
            if (text != null && text.isNotBlank()) {
                EducationalFact(
                    id = doc.id,
                    text = text
                )
            } else {
                null
            }
        }
    }
}

private suspend fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

private fun DocumentSnapshot.toFirebasePhoto() = FirebasePhoto(
    this["id"].toString(),
    this["sol"] as Long,
    this["name"] as String? ?: "",
    this["imageUrl"] as String,
    this["earthDate"] as String,
    this["seeCounter"] as Long,
    this["scaleCounter"] as Long,
    this["saveCounter"] as Long,
    this["shareCounter"] as Long,
    this["favoriteCounter"] as? Long ?: 0,
)
