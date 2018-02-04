package com.sirelon.marsroverphotos.firebase.photos

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.firebase.FirebaseConstants
import com.sirelon.marsroverphotos.feature.firebase.singleEventFirebase
import io.reactivex.Observable

/**
 * Created on 11/18/17 23:23 for Mars-Rover-Photos.
 */
class FirebaseTestPhotos {
    // Removes all items with zero counters
    fun deleteUnusedItems() {

        val collection =
            FirebaseFirestore.getInstance().collection(FirebaseConstants.COLECTION_PHOTOS)
        collection.addSnapshotListener(object : EventListener<QuerySnapshot> {
            override fun onEvent(p0: QuerySnapshot?, p1: FirebaseFirestoreException?) {
                p0?.let {
                    it.documents.map {
                        it.data
                    }
                        .filter {
                            val saveCounter = it["saveCounter"] ?: 0
                            val scaleCounter = it["scaleCounter"] ?: 0
                            val seeCounter = it["seeCounter"] ?: 0
                            val shareCounter = it["shareCounter"] ?: 0
                            val count =
                                saveCounter.toString().toLong() + scaleCounter.toString().toLong() + seeCounter.toString().toLong() + shareCounter.toString().toLong()
                            count == 0L
                        }
                        .forEach {
                            it.logD()
                            collection.document(it["id"].toString()).delete()
                                .addOnCompleteListener(OnCompleteListener {
                                    it.exception?.printStackTrace()
                                    if (it.isSuccessful) {
                                        "SuccessFull".logD()
                                    }
                                })
                        }
                }
            }

        })

//        val photoRef = FirebaseDatabase.getInstance().reference.child("MarsPhotos")
//        Observable.just(photoRef)
//                .flatMap { photoRef.singleEventFirebase() }
//                .map { it.value as Map<Long, Map<String, Any>> }
//                .doOnNext { "The all count ${it.count()}".logD() }
//                .map { it.entries }
//                .flatMap { Observable.fromIterable(it) }
//                .map { it.value }
//                .filter {
//                    val saveCounter = it["saveCounter"] ?: 0
//                    val scaleCounter = it["scaleCounter"] ?: 0
//                    val seeCounter = it["seeCounter"] ?: 0
//                    val shareCounter = it["shareCounter"] ?: 0
//                    val count = saveCounter.toString().toLong() + scaleCounter.toString().toLong() + seeCounter.toString().toLong() + shareCounter.toString().toLong()
//                    count == 0L
//                }
//                .doOnNext { it.logD() }
//                .doOnNext { photoRef.child(it["id"].toString()).removeValue { p0, _ -> p0?.toException()?.printStackTrace() } }
//                .toList()
//                .doOnSuccess { "The all after  count ${it.count()}".logD() }
//                .doOnError { it.printStackTrace() }
//                .subscribe({}, Throwable::printStackTrace)
    }

    fun migrateToFirestore() {

        val firestore = FirebaseFirestore.getInstance()
        firestore.document("")


        val photoRef = FirebaseDatabase.getInstance().reference.child("MarsPhotos")
        Observable.just(photoRef)
            .flatMap { photoRef.singleEventFirebase() }
            .map { it.value as Map<Long, Map<String, Any>> }
            .doOnNext { "The all count ${it.count()}".logD() }
            .map { it.entries }
            .flatMap { Observable.fromIterable(it) }
            .map { it.value }
            .doOnNext {

                firestore.collection(FirebaseConstants.COLECTION_PHOTOS)
                    .document(it["id"].toString())
                    .set(it)
                    .addOnSuccessListener {
                        "Successed $it".logD()
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                    }
            }
            .subscribe({}, Throwable::printStackTrace)
    }
}