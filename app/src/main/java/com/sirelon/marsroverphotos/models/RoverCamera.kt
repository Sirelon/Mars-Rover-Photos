package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sirelon.marsroverphotos.extensions.createParcel

/**
 * @author romanishin
 * @since 03.11.16 on 17:01
 */
@Keep
data class RoverCamera(
        @SerializedName(value = "id")
        val id: Int,

        @SerializedName(value = "rover_id")
        val roverId: Long,

        @SerializedName(value = "name")
        val name: String,

        @SerializedName(value = "full_name")
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

        fun empty(): RoverCamera = RoverCamera(-1, -1, "Default", "Default")
    }

    protected constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readString()
    )
}