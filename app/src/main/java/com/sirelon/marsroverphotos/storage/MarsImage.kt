package com.sirelon.marsroverphotos.storage

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.RoverCamera
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 22.08.2020 17:40 for Mars-Rover-Photos.
 */
@Entity(tableName = "images")
data class MarsImage(
    @PrimaryKey
    val id: Int,
    val sol: Long,
    val name: String?,
    val imageUrl: String,
    val earthDate: String,

    @Embedded(prefix = "camera_")
    val camera: RoverCamera?,

    val favorite: Boolean = false
) : ViewType {

    override fun getViewId() = id

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

    fun toMarsPhoto() = MarsPhoto(id.toLong(), sol, name, imageUrl, earthDate, camera)

}