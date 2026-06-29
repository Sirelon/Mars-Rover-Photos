package com.sirelon.marsroverphotos.data.repositories

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.paging.PopularRemoteMediator
import com.sirelon.marsroverphotos.domain.repositories.FavoriteSortOrder
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Implementation of ImagesRepository.
 * Manages local image cache, favorites, and popular photos from Firebase.
 * Created on 22.08.2020 17:48 for Mars-Rover-Photos.
 */
class ImagesRepositoryImpl(
    private val imagesDao: ImagesDao,
    private val firebasePhotos: IFirebasePhotos,
    private val appScope: CoroutineScope
) : ImagesRepository {

    override suspend fun saveImages(photos: List<MarsImage>) {
        imagesDao.insertImages(photos)
    }

    override fun loadImages(ids: List<String>): Flow<List<MarsImage>> {
        return imagesDao.getImagesByIds(ids)
    }

    override fun loadFavoriteImages(): Flow<List<MarsImage>> = imagesDao.loadFavoriteImages()

    override fun loadPopularImages(): Flow<List<MarsImage>> = imagesDao.loadPopularImages()

    private val favoritePagedConfig = PagingConfig(pageSize = 10, initialLoadSize = 10, enablePlaceholders = false)

    override fun loadFavoritePaged(sort: FavoriteSortOrder, roverId: Long?): Flow<PagingData<MarsImage>> =
        Pager(config = favoritePagedConfig, pagingSourceFactory = {
            when (sort) {
                FavoriteSortOrder.Recent -> imagesDao.loadFavoritePagedRecent(roverId)
                FavoriteSortOrder.MostViewed -> imagesDao.loadFavoritePagedByViews(roverId)
                FavoriteSortOrder.ByCamera -> imagesDao.loadFavoritePagedByCamera(roverId)
            }
        }).flow

    override suspend fun updateFavForImage(item: MarsImage) {
        setFavorite(item, !item.favorite)
    }

    override suspend fun setFavorite(item: MarsImage, favorite: Boolean) {
        val m = if (favorite) 1 else -1
        val counter = (item.stats.favorite + m).coerceAtLeast(0)

        // Ensure the row exists (feed photos may not be cached yet); IGNORE keeps any
        // existing row untouched so the explicit UPDATE below is the single source of truth.
        imagesDao.insertImages(listOf(item))
        imagesDao.updateFavorite(item.id, favorite, counter)

        // Update the Firebase counter as a genuine fire-and-forget background task on the
        // app scope, so it outlives this call and never blocks the favorite toggle (best-effort).
        appScope.launch {
            try {
                firebasePhotos.updatePhotoFavoriteCounter(item, favorite)
            } catch (e: Exception) {
                Logger.e("ImagesRepositoryImpl", e) { "Failed to update Firebase favorite counter for ${item.id}" }
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun loadPopularPagedSource(): Flow<PagingData<MarsImage>> {
        return Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { imagesDao.loadPopularPagedSource() },
            remoteMediator = PopularRemoteMediator(firebasePhotos, imagesDao)
        ).flow
    }
}
