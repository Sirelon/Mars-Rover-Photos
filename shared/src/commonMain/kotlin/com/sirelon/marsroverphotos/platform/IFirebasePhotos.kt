package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto

/**
 * Platform-agnostic interface for Firebase photo operations.
 * Provides analytics and popular photo management functionality.
 *
 * Implementations:
 * - Android: Uses Firebase SDK (Firestore)
 * - iOS: Uses Firebase SDK via Cocoapods
 * - Desktop/Web: Stub implementation or alternative backend
 *
 * Created on 11/24/17 21:09 for Mars-Rover-Photos.
 */
interface IFirebasePhotos {

    /**
     * Get the count of Insight rover photos from Firebase.
     * @return Total number of Insight photos
     */
    suspend fun countOfInsightPhotos(): Long

    /**
     * Update the share counter for a photo.
     * @param photo The image that was shared
     * @return Updated share counter value
     */
    suspend fun updatePhotoShareCounter(photo: MarsImage): Long

    /**
     * Update the save counter for a photo.
     * @param photo The image that was saved
     * @return Updated save counter value
     */
    suspend fun updatePhotoSaveCounter(photo: MarsImage): Long

    /**
     * Update the scale/zoom counter for a photo.
     * @param photo The image that was zoomed
     * @return Updated scale counter value
     */
    suspend fun updatePhotoScaleCounter(photo: MarsImage): Long

    /**
     * Update the seen counter for a photo.
     * @param photo The image that was viewed
     * @return Updated seen counter value
     */
    suspend fun updatePhotoSeenCounter(photo: MarsImage): Long

    /**
     * Update the favorite counter for a photo.
     * @param photo The image that was favorited/unfavorited
     * @param increment True to increment, false to decrement
     * @return Updated favorite counter value
     */
    suspend fun updatePhotoFavoriteCounter(photo: MarsImage, increment: Boolean): Long

    /**
     * Load popular photos from Firebase with pagination support.
     * @param count Number of photos to load
     * @param lastPhotoId ID of the last photo from previous page (null for first page)
     * @return List of popular photos sorted by engagement metrics
     */
    suspend fun loadPopularPhotos(
        count: Int = 10,
        lastPhotoId: String? = null
    ): List<FirebasePhoto>

    /**
     * Load educational facts about Mars from Firebase.
     * @return List of educational facts
     */
    suspend fun loadEducationalFacts(): List<EducationalFact>
}
