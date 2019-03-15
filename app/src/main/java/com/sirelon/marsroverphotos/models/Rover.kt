package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.extensions.createParcel

/**
 * @author romanishin
 * @since 31.10.16 on 15:14
 */
@Keep
@Entity
data class Rover(
        @SerializedName(value = "id")
        @PrimaryKey
        var id: Long,

        @SerializedName(value = "name")
        var name: String,

        var iamgeUrl: String?,

        @SerializedName(value = "landing_date")
        var landingDate: String,

        @SerializedName(value = "launch_date")
        var launchDate: String,

        @SerializedName(value = "status")
        var status: String,

        @SerializedName(value = "max_sol")
        var maxSol: Long,

        @SerializedName(value = "max_date")
        var maxDate: String,

        @SerializedName(value = "total_photos")
        var totalPhotos: Int) : ViewType, Parcelable {

    override fun getViewType(): Int = AdapterConstants.ROVER

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

const val SPIRIT = "spirit"
const val CURIOSITY = "curiosity"
const val OPPORTUNITY = "opportunity"
const val INSIGHT = "insight"


