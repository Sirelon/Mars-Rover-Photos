package com.sirelon.marsroverphotos.models

/**
 * @author romanishin
 * @since 31.10.16 on 19:53
 */
data class PhotosQueryRequest(
        val roverName: String,
        val sol: Long?,
        val camera: String?
) {
}