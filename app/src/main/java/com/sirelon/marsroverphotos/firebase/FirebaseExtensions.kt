package com.sirelon.marsroverphotos.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

/**
 * Created on 13/04/2017 18:54.
 */


inline fun <reified T : Any> DatabaseReference.setValueObservable(arg: T): io.reactivex
.Observable<T> {
    return io.reactivex.Observable.create<T> { emitter ->
        this.setValue(arg)
                .addOnSuccessListener {
                    emitter.onNext(arg)
                    emitter.onComplete()
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
    }
}

inline fun DatabaseReference.clearValueObservable(): io.reactivex.Observable<Boolean> {
    return io.reactivex.Observable.create<Boolean> { emitter ->
        this.setValue(null)
                .addOnSuccessListener {
                    emitter.onNext(true)
                    emitter.onComplete()
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
    }
}

inline fun DatabaseReference.singleEventFirebase(): io.reactivex.Observable<DataSnapshot> {
    return io.reactivex.Observable.create<DataSnapshot> { emitter ->
        this.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                emitter.onError(p0.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                emitter.onNext(dataSnapshot)
                emitter.onComplete()
            }
        })
    }
}

inline fun DatabaseReference.isExist(): io.reactivex.Observable<Boolean> {
    return singleEventFirebase().map { it.exists() }
}
