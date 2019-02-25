package com.sirelon.marsroverphotos.network

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sirelon.marsroverphotos.models.MarsPhoto

/**
 * @author romanishin
 * @since 31.10.16 on 15:49
 */
@Keep
data class PhotosResponse(
    @SerializedName("photos", alternate = ["items"])
    val photos: List<MarsPhoto>
)