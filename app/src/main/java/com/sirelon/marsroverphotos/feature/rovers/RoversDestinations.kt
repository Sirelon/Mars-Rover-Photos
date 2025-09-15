package com.sirelon.marsroverphotos.feature.rovers

import kotlinx.serialization.Serializable

@Serializable
object RoversRoute

@Serializable
object FavoriteRoute

@Serializable
object PopularRoute

@Serializable
object AboutRoute

@Serializable
object UkraineRoute

@Serializable
data class RoverRoute(val roverId: Long)

@Serializable
data class ImageViewerRoute(
    val pid: String,
    val ids: List<String>,
    val shouldTrack: Boolean = true,
)
