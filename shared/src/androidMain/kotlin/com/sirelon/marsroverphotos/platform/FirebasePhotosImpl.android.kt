package com.sirelon.marsroverphotos.platform

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto
import com.sirelon.marsroverphotos.domain.models.toFirebasePhoto

/**
 * Android implementation of IFirebasePhotos using Firebase Firestore SDK.
 * Provides analytics tracking and popular photo management via Firebase.
 *
 * Created on 11/25/17 17:56 for Mars-Rover-Photos.
 */
class AndroidFirebasePhotos : IFirebasePhotos {

    private companion object {
        const val COLLECTION_PHOTOS = "mars-rover-photos"
        const val COLLECTION_ROVERS = "rovers"
        const val DOCUMENT_INSIGHT = "insight"
        const val COLLECTION_EDUCATIONAL_FACTS = "educational_facts"
    }

    private fun roversCollection() =
        FirebaseFirestore.getInstance().collection(COLLECTION_ROVERS)

    private fun photosCollection() =
        FirebaseFirestore.getInstance().collection(COLLECTION_PHOTOS)

    override suspend fun countOfInsightPhotos(): Long {
        val task = roversCollection().document(DOCUMENT_INSIGHT).get()
        return Tasks.await(task).getLong("total_photos") ?: 0L
    }

    override suspend fun updatePhotoShareCounter(photo: MarsImage): Long {
        return getOrCreate(photo).let {
            it.shareCounter++
            updatePhoto(it)
            it.shareCounter
        }
    }

    override suspend fun updatePhotoSaveCounter(photo: MarsImage): Long {
        return getOrCreate(photo).let {
            it.saveCounter++
            updatePhoto(it)
            it.saveCounter
        }
    }

    override suspend fun updatePhotoScaleCounter(photo: MarsImage): Long {
        return getOrCreate(photo).let {
            it.scaleCounter++
            updatePhoto(it)
            it.scaleCounter
        }
    }

    override suspend fun updatePhotoSeenCounter(photo: MarsImage): Long {
        return getOrCreate(photo).let {
            it.seeCounter++
            updatePhoto(it)
            it.seeCounter
        }
    }

    override suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long {
        return getOrCreate(photo).let {
            if (increment) {
                it.favoriteCounter++
            } else {
                it.favoriteCounter--
            }
            updatePhoto(it)
            it.favoriteCounter
        }
    }

    override suspend fun loadPopularPhotos(
        count: Int,
        lastPhotoId: String?
    ): List<FirebasePhoto> {
        val queryDirection = Query.Direction.DESCENDING
        var query = photosCollection()
            .orderBy("shareCounter", queryDirection)
            .orderBy("saveCounter", queryDirection)
            .orderBy("scaleCounter", queryDirection)
            .orderBy("seeCounter", queryDirection)
            .limit(count.toLong())

        if (lastPhotoId != null) {
            val documentSnapshot = Tasks.await(photosCollection().document(lastPhotoId).get())
            query = query.startAfter(documentSnapshot)
        }

        val documentSnapshots = query.get().await()
        return documentSnapshots?.documents?.map { it.toFirebasePhoto() } ?: emptyList()
    }

    override suspend fun loadEducationalFacts(): List<EducationalFact> {
        val collection = FirebaseFirestore.getInstance()
            .collection(COLLECTION_EDUCATIONAL_FACTS)

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

    // Private helper methods

    private suspend fun updatePhoto(photo: FirebasePhoto): FirebasePhoto {
        return photosCollection().document(photo.id).setPhoto(photo)
    }

    private suspend fun getOrCreate(image: MarsImage): FirebasePhoto {
        val task = photosCollection().document(image.id).get()
        return Tasks.await(task).let {
            if (it.exists()) {
                it.toFirebasePhoto()
            } else {
                createPhoto(image)
            }
        }
    }

    /**
     * If the document does not exist, it will be created.
     * If the document does exist, the data should be merged into the existing document.
     */
    private suspend fun DocumentReference.setPhoto(photo: FirebasePhoto): FirebasePhoto {
        val data = mapOf(
            "id" to photo.id,
            "sol" to photo.sol,
            "name" to photo.name,
            "imageUrl" to photo.imageUrl,
            "earthDate" to photo.earthDate,
            "seeCounter" to photo.seeCounter,
            "scaleCounter" to photo.scaleCounter,
            "saveCounter" to photo.saveCounter,
            "shareCounter" to photo.shareCounter,
            "favoriteCounter" to photo.favoriteCounter
        )
        this.set(data, SetOptions.merge()).await()
        return photo
    }

    private suspend fun createPhoto(photo: MarsImage): FirebasePhoto {
        val firebasePhoto = photo.toFirebasePhoto()
        return photosCollection().document(photo.id).setPhoto(firebasePhoto)
    }
}

// Extension functions

private suspend fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

private fun DocumentSnapshot.toFirebasePhoto() = FirebasePhoto(
    id = this["id"].toString(),
    sol = this["sol"] as Long,
    name = this["name"] as String? ?: "",
    imageUrl = this["imageUrl"] as String,
    earthDate = this["earthDate"] as String,
    seeCounter = this["seeCounter"] as Long,
    scaleCounter = this["scaleCounter"] as Long,
    saveCounter = this["saveCounter"] as Long,
    shareCounter = this["shareCounter"] as Long,
    favoriteCounter = this["favoriteCounter"] as? Long ?: 0L
)
