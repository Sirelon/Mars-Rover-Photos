package com.sirelon.marsroverphotos.models

import com.google.firebase.firestore.Exclude

/**
 * @author romanishin
 * @since 31.10.16 on 10:58
 */
interface ViewType {

    @Exclude
    fun getViewType() : Int

    @Exclude
    fun getId(): Any

}