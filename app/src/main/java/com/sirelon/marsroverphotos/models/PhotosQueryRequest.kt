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
    val rover: Rover,
    val sol: Long,
    val camera: String?
) : Parcelable {

    @IgnoredOnParcel
    val dateUtil by lazy { RoverDateUtil(rover) }
}