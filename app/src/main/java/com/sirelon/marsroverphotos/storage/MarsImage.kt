package com.sirelon.marsroverphotos.storage

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.RoverCamera
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 22.08.2020 17:40 for Mars-Rover-Photos.
 */
@Entity(tableName = "images")
data class MarsImage(
    @PrimaryKey
    val id: String,
    val order: Int,
    val sol: Long,
    val name: String?,
    val imageUrl: String,
    val earthDate: String,

    @Embedded(prefix = "camera_")
    val camera: RoverCamera?,

    val favorite: Boolean = false,
    val popular: Boolean = false,
    @Embedded(prefix = "counter_")
    val stats: Stats
) : ViewType {

    override fun getViewId() = id

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

    class Stats(
        val see: Long,
        val scale: Long,
        val save: Long,
        val share: Long,
        val favorite: Long
    )
}

class StatsUpdate(
    @PrimaryKey
    val id: String,
    @Embedded(prefix = "counter_")
    val stats: MarsImage.Stats
)
