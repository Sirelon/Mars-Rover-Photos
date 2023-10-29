package com.sirelon.marsroverphotos.network

import androidx.annotation.Keep
import com.sirelon.marsroverphotos.models.MarsPhoto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * @author romanishin
 * @since 31.10.16 on 15:49
 */
@OptIn(ExperimentalSerializationApi::class)
@Keep
@Serializable
data class PhotosResponse(
    @JsonNames("photos", "items")
    val list: List<MarsPhoto>
)

@Keep
@Serializable
class PerseverancePhotosResponse(
    @SerialName("images")
    val photos: List<PerseverancePhotoItemResponse>,
    @SerialName("total_images")
    val totalImages: Long?
)

@OptIn(ExperimentalSerializationApi::class)
@Keep
@Serializable
class PerseverancePhotoItemResponse(
    @SerialName(value = "imageid")
    val id: String,

    @SerialName(value = "sol")
    val sol: Long,

    @SerialName(value = "title")
    val name: String?,

    @SerialName(value = "caption")
    val description: String?,

    @SerialName(value = "credit")
    val credit: String?,

    @SerialName(value = "image_files")
    val imageSourceResponse: ImageSourceResponse?,

    @JsonNames("date_taken_utc", "date_received")
    val earthDate: String?,

    @SerialName(value = "camera")
    val camera: PerseveranceCameraResponse?
)

@Keep
@Serializable
class PerseveranceCameraResponse(
    @SerialName(value = "camera_model_type")
    val id: String,

    @SerialName(value = "filter_name")
    val name: String,

    @SerialName(value = "instrument")
    val fullName: String
)

@Keep
@Serializable
class ImageSourceResponse(
    @SerialName(value = "medium")
    val medium: String?,

    @SerialName(value = "small")
    val small: String?,
    @SerialName(value = "full_res")
    val full_res: String?,

    @SerialName(value = "large")
    val large: String?,
)