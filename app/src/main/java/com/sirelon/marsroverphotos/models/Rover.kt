package com.sirelon.marsroverphotos.models

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

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

        val drawableName: String,

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
        val totalPhotos: Int
)


@DrawableRes
fun Rover.drawableRes(context: Context): Int {
        return context.resources
                .getIdentifier(drawableName, "drawable", context.packageName)
}


