package com.sirelon.marsroverphotos.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto
import com.sirelon.marsroverphotos.domain.models.toFirebasePhoto
import com.sirelon.marsroverphotos.utils.Logger

class FirebasePhotosImpl : IFirebasePhotos {

    private val firestore get() = Firebase.firestore

    private fun roversCollection() = firestore.collection("rovers")
    private fun photosCollection() = firestore.collection("mars-rover-photos")

    override suspend fun countOfInsightPhotos(): Long {
        return try {
            roversCollection().document("insight").get().get<Long>("total_photos") ?: 0L
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "Error counting Insight photos" }
            0L
        }
    }

    override suspend fun updatePhotoShareCounter(photo: MarsImage): Long {
        return try {
            val fp = getOrCreate(photo)
            val updated = fp.copy(shareCounter = fp.shareCounter + 1)
            updatePhoto(updated)
            updated.shareCounter
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "shareCounter update failed" }
            0L
        }
    }

    override suspend fun updatePhotoSaveCounter(photo: MarsImage): Long {
        return try {
            val fp = getOrCreate(photo)
            val updated = fp.copy(saveCounter = fp.saveCounter + 1)
            updatePhoto(updated)
            updated.saveCounter
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "saveCounter update failed" }
            0L
        }
    }

    override suspend fun updatePhotoScaleCounter(photo: MarsImage): Long {
        return try {
            val fp = getOrCreate(photo)
            val updated = fp.copy(scaleCounter = fp.scaleCounter + 1)
            updatePhoto(updated)
            updated.scaleCounter
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "scaleCounter update failed" }
            0L
        }
    }

    override suspend fun updatePhotoSeenCounter(photo: MarsImage): Long {
        return try {
            val fp = getOrCreate(photo)
            val updated = fp.copy(seeCounter = fp.seeCounter + 1)
            updatePhoto(updated)
            updated.seeCounter
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "seenCounter update failed" }
            0L
        }
    }

    override suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long {
        return try {
            val fp = getOrCreate(photo)
            val updated = fp.copy(favoriteCounter = (fp.favoriteCounter + if (increment) 1 else -1).coerceAtLeast(0))
            updatePhoto(updated)
            updated.favoriteCounter
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "favoriteCounter update failed" }
            0L
        }
    }

    override suspend fun loadPopularPhotos(count: Int, cursor: IFirebasePhotos.PopularCursor?): List<FirebasePhoto> {
        var query = photosCollection()
            .orderBy("shareCounter", Direction.DESCENDING)
            .orderBy("saveCounter", Direction.DESCENDING)
            .orderBy("scaleCounter", Direction.DESCENDING)
            .orderBy("seeCounter", Direction.DESCENDING)
            .limit(count)
        if (cursor != null) {
            query = query.startAfterFieldValues {
                add(cursor.shareCounter)
                add(cursor.saveCounter)
                add(cursor.scaleCounter)
                add(cursor.seeCounter)
            }
        }
        return query.get().documents.map { it.toFirebasePhoto() }
    }

    override suspend fun loadEducationalFacts(): List<EducationalFact> {
        return try {
            firestore.collection("educational_facts").get().documents.mapNotNull { doc ->
                val text = doc.get<String>("text")
                if (!text.isNullOrBlank()) EducationalFact(id = doc.id, text = text) else null
            }
        } catch (e: Exception) {
            Logger.e("FirebasePhotosImpl", e) { "loadEducationalFacts failed" }
            emptyList()
        }
    }

    private suspend fun getOrCreate(image: MarsImage): FirebasePhoto {
        val doc = photosCollection().document(image.id).get()
        return if (doc.exists) {
            doc.toFirebasePhoto().withBackfilledRoverId(image.roverId)
        } else {
            image.toFirebasePhoto()
        }
    }

    override suspend fun deletePhoto(photoId: String) {
        photosCollection().document(photoId).delete()
    }

    private suspend fun updatePhoto(photo: FirebasePhoto) {
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
            "favoriteCounter" to photo.favoriteCounter,
            "roverId" to photo.roverId
        )
        photosCollection().document(photo.id).set(data, merge = true)
    }
}

private fun DocumentSnapshot.toFirebasePhoto() = FirebasePhoto(
    id = get<String?>("id") ?: id,
    sol = get<Long?>("sol") ?: 0L,
    name = get<String?>("name") ?: "",
    imageUrl = get<String?>("imageUrl") ?: "",
    earthDate = get<String?>("earthDate") ?: "",
    seeCounter = get<Long?>("seeCounter") ?: 0L,
    scaleCounter = get<Long?>("scaleCounter") ?: 0L,
    saveCounter = get<Long?>("saveCounter") ?: 0L,
    shareCounter = get<Long?>("shareCounter") ?: 0L,
    favoriteCounter = get<Long?>("favoriteCounter") ?: 0L,
    roverId = get<Long?>("roverId") ?: 0L
)

private fun FirebasePhoto.withBackfilledRoverId(roverId: Long): FirebasePhoto {
    return if (this.roverId == 0L && roverId != 0L) copy(roverId = roverId) else this
}
