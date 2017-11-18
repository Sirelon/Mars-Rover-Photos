package com.sirelon.marsroverphotos.firebase

import com.google.firebase.database.FirebaseDatabase
import com.sirelon.marsroverphotos.extensions.logD
import io.reactivex.Observable

/**
 * Created on 11/18/17 23:23 for Mars-Rover-Photos.
 */
val proideTestFirebase: FirebaseTestProvider by lazy { FirebaseTestProvider() }

class FirebaseTestProvider{
    // Removes all items with zero counters
    fun deleteUnusedItems(){
        val photoRef = FirebaseDatabase.getInstance().reference.child("MarsPhotos")
        Observable.just(photoRef)
                .flatMap { photoRef.singleEventFirebase() }
                .map { it.value as Map<Long, Map<String, Any>> }
                .doOnNext { "The all count ${it.count()}".logD() }
                .map { it.entries }
                .flatMap { Observable.fromIterable(it) }
                .map { it.value }
                .filter {
                    val saveCounter = it["saveCounter"] ?: 0
                    val scaleCounter = it["scaleCounter"] ?: 0
                    val seeCounter = it["seeCounter"] ?: 0
                    val shareCounter = it["shareCounter"] ?: 0
                    val count = saveCounter.toString().toLong() + scaleCounter.toString().toLong() + seeCounter.toString().toLong() + shareCounter.toString().toLong()
                    count == 0L
                }
                .doOnNext { it.logD() }
                .doOnNext { photoRef.child(it["id"].toString()).removeValue { p0, _ -> p0?.toException()?.printStackTrace() } }
                .toList()
                .doOnSuccess { "The all after  count ${it.count()}".logD() }
                .doOnError { it.printStackTrace() }
                .subscribe({}, Throwable::printStackTrace)
    }
}