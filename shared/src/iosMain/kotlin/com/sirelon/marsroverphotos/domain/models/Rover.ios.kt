package com.sirelon.marsroverphotos.domain.models

/**
 * iOS implementation: Return 0 as placeholder.
 * iOS will use image names directly from assets.
 */
actual fun Rover.getDrawableResourceId(): Int {
    return 0
}
