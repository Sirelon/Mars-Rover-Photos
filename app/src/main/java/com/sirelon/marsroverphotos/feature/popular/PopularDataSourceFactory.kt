package com.sirelon.marsroverphotos.feature.popular

import android.arch.paging.DataSource
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto

/**
 * Created on 2/6/18 22:50 for Mars-Rover-Photos.
 */
class PopularDataSourceFactory : DataSource.Factory<String, FirebasePhoto>{

    override fun create(): DataSource<String, FirebasePhoto> = PopularPhotoDataSource()

}