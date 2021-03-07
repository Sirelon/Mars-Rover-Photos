package com.sirelon.marsroverphotos.models

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author romanishin
 * @since 31.10.16 on 19:53
 */
@Parcelize
data class PhotosQueryRequest(
    val roverId: Long,
    val sol: Long,
    val camera: String?
) : Parcelable {

}