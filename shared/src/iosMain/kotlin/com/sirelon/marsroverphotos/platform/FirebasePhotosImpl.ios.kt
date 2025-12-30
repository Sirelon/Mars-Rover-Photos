package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto
import com.sirelon.marsroverphotos.utils.Logger

/**
 * iOS stub implementation of IFirebasePhotos.
 *
 * Note: This is a stub implementation. For full Firebase support on iOS,
 * integrate Firebase iOS SDK via Cocoapods and implement actual functionality.
 *
 * To implement:
 * 1. Add Firebase pods to Podfile (FirebaseFirestore, FirebaseAnalytics)
 * 2. Implement actual Firestore operations using cinterop
 * 3. Update this class to use the Firebase iOS SDK
 */
class IosFirebasePhotos : IFirebasePhotos {

    override suspend fun countOfInsightPhotos(): Long {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - returning 0" }
        return 0L
    }

    override suspend fun updatePhotoShareCounter(photo: MarsImage): Long {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - share counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoSaveCounter(photo: MarsImage): Long {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - save counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoScaleCounter(photo: MarsImage): Long {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - scale counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoSeenCounter(photo: MarsImage): Long {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - seen counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - favorite counter not updated" }
        return 0L
    }

    override suspend fun loadPopularPhotos(count: Int, lastPhotoId: String?): List<FirebasePhoto> {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - returning empty list" }
        return emptyList()
    }

    override suspend fun loadEducationalFacts(): List<EducationalFact> {
        Logger.w("IosFirebasePhotos") { "Firebase not implemented on iOS - returning empty list" }
        return emptyList()
    }
}
