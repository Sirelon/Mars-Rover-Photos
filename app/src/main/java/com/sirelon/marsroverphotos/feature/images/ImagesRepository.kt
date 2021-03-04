package com.sirelon.marsroverphotos.feature.images

import android.annotation.SuppressLint
import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sirelon.marsroverphotos.feature.photos.mapToUi
import com.sirelon.marsroverphotos.feature.popular.PopularRemoteMediator
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.flow.Flow

/**
 * Created on 22.08.2020 17:48 for Mars-Rover-Photos.
 */
class ImagesRepository(private val context: Context) {

    private val imagesDao: ImagesDao

    init {
        DataBaseProvider.init(context)
        imagesDao = DataBaseProvider.dataBase.imagesDao()
    }

    @SuppressLint("CheckResult")
    suspend fun saveImages(photos: List<MarsImage>) {
        imagesDao.insertImages(photos)
    }

    fun loadImages(ids: List<String>) = imagesDao.getImagesByIds(ids)

    fun loadPopularPhotos() = imagesDao.loadPopularImages()

    fun loadFirstImage() = imagesDao.getOneImage()

    fun loadFavoritePagedSource(): Flow<PagingData<MarsImage>> {
        return Pager(
            config = PagingConfig(10, 2),
            pagingSourceFactory = { imagesDao.loadFavoritePagedSource() }
        ).flow
    }

    suspend fun updateFavForImage(item: MarsImage) {
        val updated = item.copy(favorite = !item.favorite)
        imagesDao.update(updated)
    }

    fun getFavoriteImages() = imagesDao.getFavoriteImages()

    @OptIn(ExperimentalPagingApi::class)
    fun loadPopularPagedSource(): Flow<PagingData<MarsImage>> {
        return Pager(
            config = PagingConfig(20, 2),
            pagingSourceFactory = { imagesDao.loadPopularPagedSource() },
            remoteMediator = PopularRemoteMediator(FirebaseProvider.firebasePhotos, imagesDao)
        ).flow
    }
}