package com.sirelon.marsroverphotos.data.database.entities

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sirelon.marsroverphotos.domain.models.RoverCamera

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

    val roverId: Long = 0L,

    @Embedded(prefix = "camera_")
    val camera: RoverCamera?,

    val favorite: Boolean = false,
    val popular: Boolean = false,
    @Embedded(prefix = "counter_")
    val stats: Stats,

    // Perseverance-specific metadata
    val description: String? = null,
    val credit: String? = null,
) {
    data class Stats(
        val see: Long,
        val scale: Long,
        val save: Long,
        val share: Long,
        val favorite: Long
    )
}

/**
 * Helper class for marking an existing row as popular: updates the popular flag,
 * the ranking order and stats without touching the rest of the row.
 */
class PopularUpdate(
    @PrimaryKey
    val id: String,
    val popular: Boolean,
    val order: Int,
    @Embedded(prefix = "counter_")
    val stats: MarsImage.Stats
)
