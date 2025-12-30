package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto
import com.sirelon.marsroverphotos.utils.Logger

/**
 * Web stub implementation of IFirebasePhotos.
 *
 * Note: This is a stub implementation. For full Firebase support on Web,
 * integrate Firebase JS SDK and implement actual functionality.
 *
 * To implement:
 * 1. Add Firebase JS SDK dependencies
 * 2. Initialize Firebase with project config
 * 3. Implement Firestore operations using Firebase JS SDK
 * 4. Update this class to use the Firebase Web SDK
 */
class WebFirebasePhotos : IFirebasePhotos {

    override suspend fun countOfInsightPhotos(): Long {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - returning 0" }
        return 0L
    }

    override suspend fun updatePhotoShareCounter(photo: MarsImage): Long {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - share counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoSaveCounter(photo: MarsImage): Long {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - save counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoScaleCounter(photo: MarsImage): Long {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - scale counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoSeenCounter(photo: MarsImage): Long {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - seen counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - favorite counter not updated" }
        return 0L
    }

    override suspend fun loadPopularPhotos(count: Int, lastPhotoId: String?): List<FirebasePhoto> {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - returning empty list" }
        return emptyList()
    }

    override suspend fun loadEducationalFacts(): List<EducationalFact> {
        Logger.d("WebFirebasePhotos") { "Firebase not implemented on Web - returning empty list" }
        return emptyList()
    }
}
