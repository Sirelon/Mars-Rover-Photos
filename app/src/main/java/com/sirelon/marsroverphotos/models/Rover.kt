package com.sirelon.marsroverphotos.models

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author romanishin
 * @since 31.10.16 on 15:14
 */
@Keep
@Entity
@Serializable
data class Rover(
        @SerialName(value = "id")
        @PrimaryKey
        val id: Long,

        @SerialName(value = "name")
        val name: String,

        val drawableName: String,

        @SerialName(value = "landing_date")
        val landingDate: String,

        @SerialName(value = "launch_date")
        val launchDate: String,

        @SerialName(value = "status")
        val status: String,

        @SerialName(value = "max_sol")
        var maxSol: Long,

        @SerialName(value = "max_date")
        var maxDate: String,

        @SerialName(value = "total_photos")
        val totalPhotos: Int
)


@DrawableRes
fun Rover.drawableRes(context: Context): Int {
        return context.resources
                .getIdentifier(drawableName, "drawable", context.packageName)
}


