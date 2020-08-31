package com.sirelon.marsroverphotos.feature.popular

import androidx.paging.ItemKeyedDataSource
import androidx.paging.PagingSource
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
        "loadInitial".logD()
        firebasePhotos.loadPopularPhotos(
            count = params.requestedLoadSize,
            lastPhoto = params.requestedInitialKey
        )
            .subscribeOn(Schedulers.io())
            .onErrorReturnItem(emptyList())
            .subscribe {
                if (it.isEmpty()) {
                    callback.onResult(it)
                } else {
                    callback.onResult(it, 0, params.requestedLoadSize)
                }
            }
            .apply { disposables.add(this) }
    }

    override fun loadAfter(
        params: LoadParams<FirebasePhoto>,
        callback: LoadCallback<FirebasePhoto>
    ) {
        "LoadAfter".logD()

        firebasePhotos.loadPopularPhotos(count = params.requestedLoadSize, lastPhoto = params.key)
            .subscribeOn(Schedulers.io())
            .doOnNext { it.logD() }
            .subscribe { callback.onResult(it) }
            .apply { disposables.add(this) }
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