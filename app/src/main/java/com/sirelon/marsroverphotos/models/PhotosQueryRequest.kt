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
    var rover: Rover,
    var sol: Long,
    var camera: String?
) : Parcelable {

    @IgnoredOnParcel
    val dateUtil by lazy { RoverDateUtil(rover) }
}