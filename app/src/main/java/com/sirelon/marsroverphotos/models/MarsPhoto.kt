package com.sirelon.marsroverphotos.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import kotlinx.android.parcel.Parcelize

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
@Keep
@Parcelize
data class MarsPhoto(
    @SerializedName(value = "id")
    val id: Long,

    @SerializedName(value = "sol")
    val sol: Long,

    @SerializedName(value = "name", alternate = ["title"])
    val name: String?,

    @SerializedName(value = "img_src", alternate = ["url"])
    val imageUrl: String,

    @SerializedName(value = "earth_date", alternate = ["created_at"])
    val earthDate: String,

    @SerializedName(value = "camera")
    val camera: RoverCamera?
) : ViewType, Parcelable {

    override fun getViewId() = id

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO
}