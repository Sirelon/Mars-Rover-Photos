package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.extensions.createParcel
import com.squareup.moshi.Json

/**
 * @author romanishin
 * @since 31.10.16 on 15:14
 */
data class Rover(
        @Json(name = "id")
        var id: Long,

        @Json(name = "name")
        var name: String,

        var iamgeUrl: String?,

        @Json(name = "landing_date")
        var landingDate: String,

        @Json(name = "launch_date")
        var launchDate: String,

        @Json(name = "status")
        var status: String,

        @Json(name = "max_sol")
        var maxSol: Long,

        @Json(name = "max_date")
        var maxDate: String,

        @Json(name = "total_photos")
        var totalPhotos: Int) : ViewType, Parcelable {

    override fun getViewType(): Int = AdapterConstants.ROVER

    override fun hashCode(): Int {
        return id.hashCode();
    }

    override fun equals(other: Any?): Boolean {
        if (other is Rover)
            if (other.id == this.id)
                return true

        return false
    }

    override fun writeToParcel(dest: Parcel, p1: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeString(iamgeUrl)
        dest.writeString(landingDate)
        dest.writeString(launchDate)
        dest.writeString(status)
        dest.writeLong(maxSol)
        dest.writeString(maxDate)
        dest.writeInt(totalPhotos)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField @Suppress("unused")
        val CREATOR = createParcel(::Rover)
    }

    protected constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readInt()
    )
}