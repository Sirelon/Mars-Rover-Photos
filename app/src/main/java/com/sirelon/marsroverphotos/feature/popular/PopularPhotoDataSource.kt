package com.sirelon.marsroverphotos.feature.popular

import android.arch.paging.ItemKeyedDataSource
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


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

    override fun loadBefore(params: LoadParams<FirebasePhoto>,
                            callback: LoadCallback<FirebasePhoto>) {
        "LoadBefore".logD()
    }

    override fun loadInitial(params: LoadInitialParams<FirebasePhoto>,
                             callback: LoadInitialCallback<FirebasePhoto>) {
        "loadInitial".logD()
        firebasePhotos.loadPopularPhotos(count = params.requestedLoadSize,
                                         lastPhoto = params.requestedInitialKey)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    callback.onResult(it, 0, params.requestedLoadSize)
                }
                .apply { disposables.add(this) }
    }

    override fun loadAfter(params: LoadParams<FirebasePhoto>,
                           callback: LoadCallback<FirebasePhoto>) {
        "LoadAfter".logD()

        firebasePhotos.loadPopularPhotos(count = params.requestedLoadSize, lastPhoto = params.key)
                .subscribeOn(Schedulers.io())
                .doOnNext { it.logD() }
                .subscribe { callback.onResult(it) }
                .apply { disposables.add(this) }
    }

    override fun getKey(item: FirebasePhoto) = item
}