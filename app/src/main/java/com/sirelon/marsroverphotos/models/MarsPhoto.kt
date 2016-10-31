package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.createParcel
import com.squareup.moshi.Json

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
data class MarsPhoto(
        @Json(name = "id")
        val id: Long,

        @Json(name = "sol")
        val sol: Long,

        val name: String?,

        @Json(name = "img_src")
        val imageUrl: String,

        @Json(name = "earth_date")
        val earthDate: String) : ViewType, Parcelable {

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

    override fun writeToParcel(dest: Parcel, p1: Int) {
        dest.writeLong(id)
        dest.writeLong(sol)
        dest.writeString(name)
        dest.writeString(imageUrl)
        dest.writeString(earthDate)
//        dest.writeTypedObject()
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField @Suppress("unused")
        val CREATOR = createParcel(::MarsPhoto)
    }

    protected constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()
    )
}