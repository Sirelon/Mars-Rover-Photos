package com.sirelon.marsroverphotos.feature.popular

import android.arch.paging.DataSource
import android.arch.paging.ItemKeyedDataSource
import android.arch.paging.PositionalDataSource
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.utils.mock.randomFirebasePhoto
import javax.xml.transform.Templates


/**
 * Created on 2/5/18 00:48 for Mars-Rover-Photos.
 */
class PopularPhotoDataSource : ItemKeyedDataSource<String, FirebasePhoto>() {
    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<FirebasePhoto>) {
        "LoadBefore".logD()
    }

    override fun loadInitial(
        params: LoadInitialParams<String>, callback: LoadInitialCallback<FirebasePhoto>
    ) {
        "loadInitial".logD()
        val initialList = (1..params.requestedLoadSize).map { randomFirebasePhoto() }
        callback.onResult(initialList, 0, params.requestedLoadSize)
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<FirebasePhoto>) {
        "LoadAfter".logD()
        val initialList = (1..params.requestedLoadSize).map { randomFirebasePhoto() }
        callback.onResult(initialList)
    }

    override fun getKey(item: FirebasePhoto) = item.name ?: ""

}