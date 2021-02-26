package com.sirelon.marsroverphotos.feature.popular

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Created on 2/5/18 00:48 for Mars-Rover-Photos.
 */

// Leave it just as example for load directly from net
class PopularPhotosSource(private val firebasePhotos: IFirebasePhotos) :
    PagingSource<FirebasePhoto, FirebasePhoto>() {

    override suspend fun load(params: LoadParams<FirebasePhoto>): LoadResult<FirebasePhoto, FirebasePhoto> =
        withContext(Dispatchers.IO) {
            try {
                val list = firebasePhotos.loadPopularPhotos(params.loadSize, null).blockingFirst()
                LoadResult.Page(list, null, list.lastOrNull())
            } catch (e: Exception) {
                e.printStackTrace()
                LoadResult.Error(e)
            }
        }

    override fun getRefreshKey(state: PagingState<FirebasePhoto, FirebasePhoto>): FirebasePhoto? {
        TODO("Not yet implemented")
    }
}

@ExperimentalPagingApi
class PopularRemoteMediator(
    private val firebasePhotos: IFirebasePhotos,
    private val dao: ImagesDao
) : RemoteMediator<Int, MarsImage>() {

    @ExperimentalPagingApi
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MarsImage>
    ) = withContext(Dispatchers.IO) {
        loadSync(loadType, state)
    }

    private suspend fun loadSync(
        loadType: LoadType,
        state: PagingState<Int, MarsImage>
    ): MediatorResult {
        val alreadyInDb = state.pages.map { it.data.size }.sum()
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(endOfPaginationReached = true)

        try {
            val loadSize = state.config.pageSize
            val list =
                firebasePhotos.loadPopularPhotos(loadSize, state.lastItemOrNull()?.id?.toString())
                    .blockingFirst()
                    .mapIndexed { index, item -> item.toMarsImage(index + alreadyInDb) }

            dao.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    dao.deleteAllPopular()
                }

                dao.replaceImages(list)
            }

            val endOfPaginationReached: Boolean = list.lastOrNull() == null
            return MediatorResult.Success(endOfPaginationReached)
        } catch (e: Throwable) {
            Log.e("SIrelon", "Error", e)
            return MediatorResult.Error(e)
        }
    }

}