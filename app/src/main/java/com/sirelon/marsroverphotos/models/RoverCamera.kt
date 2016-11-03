package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import com.sirelon.marsroverphotos.extensions.createParcel
import com.squareup.moshi.Json

/**
 * @author romanishin
 * @since 03.11.16 on 17:01
 */
data class RoverCamera(
        @Json(name = "id")
        val id: Int,

        @Json(name = "rover_id")
        val roverId: Long,

        @Json(name = "name")
        val name: String,

        @Json(name = "full_name")
        val fullName: String
) : Parcelable {

    override fun writeToParcel(dest: Parcel, p1: Int) {
        dest.writeInt(id)
        dest.writeLong(roverId)
        dest.writeString(name)
        dest.writeString(fullName)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField @Suppress("unused")
        val CREATOR = createParcel(::RoverCamera)
    }

    protected constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readString()
    )
}