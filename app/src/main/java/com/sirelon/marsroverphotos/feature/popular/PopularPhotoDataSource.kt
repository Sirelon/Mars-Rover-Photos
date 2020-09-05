package com.sirelon.marsroverphotos.feature.popular

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.ItemKeyedDataSource
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Created on 2/5/18 00:48 for Mars-Rover-Photos.
 */
class PopularPhotoDataSource(private val firebasePhotos: IFirebasePhotos) :
    ItemKeyedDataSource<FirebasePhoto, FirebasePhoto>() {

    private val disposables = CompositeDisposable()

    init {
        addInvalidatedCallback {
            disposables.clear()
        }
    }

    override fun loadBefore(
        params: LoadParams<FirebasePhoto>,
        callback: LoadCallback<FirebasePhoto>
    ) {
        "LoadBefore".logD()
    }

    override fun loadInitial(
        params: LoadInitialParams<FirebasePhoto>,
        callback: LoadInitialCallback<FirebasePhoto>
    ) {
//        "loadInitial".logD()
//        firebasePhotos.loadPopularPhotos(
//            count = params.requestedLoadSize,
//            lastPhoto = params.requestedInitialKey
//        )
//            .subscribeOn(Schedulers.io())
//            .onErrorReturnItem(emptyList())
//            .subscribe {
//                if (it.isEmpty()) {
//                    callback.onResult(it)
//                } else {
//                    callback.onResult(it, 0, params.requestedLoadSize)
//                }
//            }
//            .apply { disposables.add(this) }
    }

    override fun loadAfter(
        params: LoadParams<FirebasePhoto>,
        callback: LoadCallback<FirebasePhoto>
    ) {
        "LoadAfter".logD()

//        firebasePhotos.loadPopularPhotos(count = params.requestedLoadSize, lastPhoto = params.key)
//            .subscribeOn(Schedulers.io())
//            .doOnNext { it.logD() }
//            .subscribe { callback.onResult(it) }
//            .apply { disposables.add(this) }
    }

    override fun getKey(item: FirebasePhoto) = item
}

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
            return MediatorResult.Success(true)
        } catch (e: Throwable) {
            Log.e("SIrelon", "Error", e)
            return MediatorResult.Error(e)
        }
    }

}