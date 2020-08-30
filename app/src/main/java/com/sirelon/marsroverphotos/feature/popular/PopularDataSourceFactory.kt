package com.sirelon.marsroverphotos.feature.popular

import androidx.paging.DataSource
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos

/**
 * Created on 2/6/18 22:50 for Mars-Rover-Photos.
 */
class PopularDataSourceFactory(private val iFirebasePhotos: IFirebasePhotos) : DataSource.Factory<FirebasePhoto, FirebasePhoto>(){

    override fun create(): DataSource<FirebasePhoto, FirebasePhoto> = PopularPhotoDataSource(iFirebasePhotos)

}
