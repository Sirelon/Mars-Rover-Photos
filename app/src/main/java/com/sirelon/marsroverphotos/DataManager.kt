package com.sirelon.marsroverphotos

import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.network.RestApi
import io.reactivex.Observable

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
class DataManager(private val api: RestApi = RestApi()) {

    fun getMarsPhotos(): Observable<List<MarsPhoto>> {
        return Observable.create {
            subscriber ->

            val callResponse = api.getCuriosityPhotos(1000, null)
            val response = callResponse.execute()
            if (response.isSuccessful) {
                val marsPhotos = response.body().photos
                subscriber.onNext(marsPhotos)
                subscriber.onComplete()
            } else
                subscriber.onError(Throwable(response.message()))
        }
    }
}