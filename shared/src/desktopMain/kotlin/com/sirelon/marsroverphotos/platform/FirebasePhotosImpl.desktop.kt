package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto
import com.sirelon.marsroverphotos.utils.Logger

/**
 * Desktop stub implementation of IFirebasePhotos.
 *
 * Note: This is a stub implementation. Firebase is typically not used on Desktop.
 * Consider implementing a REST API backend or using local storage for Desktop analytics.
 */
class DesktopFirebasePhotos : IFirebasePhotos {

    override suspend fun countOfInsightPhotos(): Long {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - returning 0" }
        return 0L
    }

    override suspend fun updatePhotoShareCounter(photo: MarsImage): Long {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - share counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoSaveCounter(photo: MarsImage): Long {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - save counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoScaleCounter(photo: MarsImage): Long {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - scale counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoSeenCounter(photo: MarsImage): Long {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - seen counter not updated" }
        return 0L
    }

    override suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - favorite counter not updated" }
        return 0L
    }

    override suspend fun loadPopularPhotos(count: Int, lastPhotoId: String?): List<FirebasePhoto> {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - returning empty list" }
        return emptyList()
    }

    override suspend fun loadEducationalFacts(): List<EducationalFact> {
        Logger.d("DesktopFirebasePhotos") { "Firebase not implemented on Desktop - returning empty list" }
        return emptyList()
    }
}
