package com.sirelon.marsroverphotos.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author romanishin
 * @since 03.11.16 on 17:01
 */
@Serializable
data class RoverCamera(
    @SerialName(value = "id")
    val id: Int,

    @SerialName(value = "name")
    val name: String,

    @SerialName(value = "full_name")
    val fullName: String
)
