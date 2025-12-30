package com.sirelon.marsroverphotos.domain.models

/**
 * @author romanishin
 * @since 31.10.16 on 19:53
 */
data class PhotosQueryRequest(
    val roverId: Long,
    val sol: Long,
    val camera: String?
)
