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

class PerseverancePhotosResponse(
    @SerializedName("images")
    val photos: List<PerseverancePhotoItemResponse>
)

class PerseverancePhotoItemResponse(
    @SerializedName(value = "imageid")
    val id: String,

    @SerializedName(value = "sol")
    val sol: Long,

    @SerializedName(value = "title")
    val name: String?,

    @SerializedName(value = "caption")
    val description: String?,

    @SerializedName(value = "credit")
    val credit: String?,

    @SerializedName(value = "image_files")
    val imageSourceResponse: ImageSourceResponse?,

    @SerializedName(value = "date_taken_utc", alternate = ["date_received"])
    val earthDate: String?,

    @SerializedName(value = "camera")
    val camera: PerseveranceCameraResponse?
)

class PerseveranceCameraResponse(
    @SerializedName(value = "camera_model_type")
    val id: String,

    @SerializedName(value = "filter_name")
    val name: String,

    @SerializedName(value = "instrument")
    val fullName: String
)

class ImageSourceResponse(
    @SerializedName(value = "medium")
    val medium: String?,

    @SerializedName(value = "small")
    val small: String?,
    @SerializedName(value = "full_res")
    val full_res: String?,

    @SerializedName(value = "large")
    val large: String?,
)