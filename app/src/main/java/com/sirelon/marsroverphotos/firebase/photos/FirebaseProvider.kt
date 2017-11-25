package com.sirelon.marsroverphotos.firebase.photos

/**
 * Created on 11/24/17 21:11 for Mars-Rover-Photos.
 */
object FirebaseProvider {

    val firebasePhotos: IFirebasePhotos by lazy {
        FirebasePhotos()
    }

    val proideTestFirebase: FirebaseTestPhotos by lazy {
        FirebaseTestPhotos()
    }
}
