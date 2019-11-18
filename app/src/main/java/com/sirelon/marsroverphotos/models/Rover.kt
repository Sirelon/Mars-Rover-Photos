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
        val id: Long,

        @SerializedName(value = "name")
        val name: String,

        val iamgeUrl: String?,

        @SerializedName(value = "landing_date")
        val landingDate: String,

        @SerializedName(value = "launch_date")
        val launchDate: String,

        @SerializedName(value = "status")
        val status: String,

        @SerializedName(value = "max_sol")
        var maxSol: Long,

        @SerializedName(value = "max_date")
        var maxDate: String,

        @SerializedName(value = "total_photos")
        val totalPhotos: Int) : ViewType, Parcelable {

    override fun getViewId() = id

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
            parcel.readString()!!,
            parcel.readString(),
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readLong(),
            parcel.readString()!!,
            parcel.readInt()
    )

//    override fun hashCode(): Int {
//        return id.hashCode();
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (other is Rover)
//            if (other.id == this.id)
//                return true
//
//        return false
//    }
}


