package com.sirelon.marsroverphotos.feature.images

import android.annotation.SuppressLint
import android.content.Context
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

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
    fun saveImages(photos: List<MarsPhoto>) {
        Observable
            .fromIterable(photos)
            .map { MarsImage(it.id.toInt(), it.sol, it.name, it.imageUrl, it.earthDate, it.camera) }
            .toList()
            .subscribeOn(Schedulers.io())
            .subscribe { t1, t2 -> imagesDao.insertImages(t1) }
    }

    fun loadImages(ids: List<Int>) = imagesDao.getImagesByIds(ids)

    fun loadFirstImage() = imagesDao.getOneImage()

    fun updateFavForImage(item: MarsImage) {
        Observable.fromCallable {
            val updated = item.copy(favorite = !item.favorite)
            imagesDao.update(updated)
        }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}