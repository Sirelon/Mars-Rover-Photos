package com.sirelon.marsroverphotos.feature.images

import android.annotation.SuppressLint
import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
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
    suspend fun saveImages(photos: List<MarsPhoto>) {
        val t1 = photos.mapIndexed { index, it ->
            // It's okay to use not correct data for favorite and popular with Stats, 'cause if these images already in database, we'll ignore replacing them.
            MarsImage(
                id = it.id.toInt(),
                sol = it.sol,
                name = it.name,
                imageUrl = it.imageUrl,
                earthDate = it.earthDate,
                camera = it.camera,
                favorite = false,
                popular = false,
                order = index,
                stats = MarsImage.Stats(0, 0, 0, 0)
            )
        }
        imagesDao.insertImages(t1)
    }

    fun loadImages(ids: List<Int>) = imagesDao.getImagesByIds(ids)

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