package com.sirelon.marsroverphotos

import com.sirelon.marsroverphotos.models.MarsPhoto
import io.reactivex.Observable

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
class DataManager {

    fun getMarsPhotos() : Observable<List<MarsPhoto>> {
        return Observable.create { subsciber ->
            subsciber.onNext(mockPhotos());
            subsciber.onComplete();
        }
    }

    private fun mockPhotos(): MutableList<MarsPhoto> {
        val photos = mutableListOf<MarsPhoto>()

//        for (i in 1..25) {
//            photos.add(MarsPhoto(
//                    "Name $i",
//                    "http://lorempixel.com/200/200/?fake=$i"
//            ))
//        }
        return photos
    }
}