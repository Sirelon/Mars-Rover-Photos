package com.sirelon.marsroverphotos.feature.firebase

import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 13/04/2017 18:54.
 */
fun MarsImage.toFireBase(): FirebasePhoto = FirebasePhoto(this)
