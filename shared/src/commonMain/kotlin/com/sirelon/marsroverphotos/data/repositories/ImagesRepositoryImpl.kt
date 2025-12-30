package com.sirelon.marsroverphotos.data.repositories

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.paging.PopularRemoteMediator
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * Implementation of ImagesRepository.
 * Manages local image cache, favorites, and popular photos from Firebase.
 * Created on 22.08.2020 17:48 for Mars-Rover-Photos.
 */
class ImagesRepositoryImpl(
    private val imagesDao: ImagesDao,
    private val firebasePhotos: IFirebasePhotos
) : ImagesRepository {

    override suspend fun saveImages(photos: List<MarsImage>) {
        imagesDao.insertImages(photos)
    }

    override fun loadImages(ids: List<String>): Flow<List<MarsImage>> {
        return imagesDao.getImagesByIds(ids)
    }

    override fun loadFavoritePagedSource(): Flow<PagingData<MarsImage>> {
        return Pager(
            config = PagingConfig(pageSize = 10, initialLoadSize = 10),
            pagingSourceFactory = { imagesDao.loadFavoritePagedSource() }
        ).flow
    }

    override suspend fun updateFavForImage(item: MarsImage) {
        supervisorScope {
            val favorite = !item.favorite
            val m = if (favorite) 1 else -1
            val counter = item.stats.favorite + (1 * m)

            imagesDao.updateFavorite(item.id, favorite, counter)

            // Update Firebase counter in background
            launch {
                try {
                    firebasePhotos.updatePhotoFavoriteCounter(item, favorite)
                } catch (e: Exception) {
                    // Log error but don't fail the operation
                    // Firebase update is best-effort
                }
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun loadPopularPagedSource(): Flow<PagingData<MarsImage>> {
        return Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20),
            pagingSourceFactory = { imagesDao.loadPopularPagedSource() },
            remoteMediator = PopularRemoteMediator(firebasePhotos, imagesDao)
        ).flow
    }
}
