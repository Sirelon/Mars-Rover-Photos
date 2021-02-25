package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sirelon.marsroverphotos.extensions.createParcel
import kotlinx.parcelize.Parcelize


/**
 * @author romanishin
 * @since 03.11.16 on 17:01
 */
@Keep
@Parcelize
class RoverCamera(
        @SerializedName(value = "id")
        val id: Int,

        @SerializedName(value = "name")
        val name: String,

        @SerializedName(value = "full_name")
        val fullName: String
) : Parcelable {

    companion object {
        fun empty(): RoverCamera = RoverCamera(-1, "Default", "Default")
    }
}