package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.extensions.createParcel

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
@Keep
data class MarsPhoto(
        @SerializedName(value = "id")
        val id: Long,

        @SerializedName(value = "sol")
        val sol: Long,

        val name: String?,

        @SerializedName(value = "img_src")
        val imageUrl: String,

        @SerializedName(value = "earth_date")
        val earthDate: String,

        @SerializedName(value = "camera")
        val camera: RoverCamera)

: ViewType, Parcelable {

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

    override fun writeToParcel(dest: Parcel, p1: Int) {
        dest.writeLong(id)
        dest.writeLong(sol)
        dest.writeString(name)
        dest.writeString(imageUrl)
        dest.writeString(earthDate)
        dest.writeParcelable(camera, p1)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField @Suppress("unused")
        val CREATOR = createParcel(::MarsPhoto)
    }

    protected constructor (parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable<RoverCamera>(RoverCamera::class.java.classLoader)
    )
}