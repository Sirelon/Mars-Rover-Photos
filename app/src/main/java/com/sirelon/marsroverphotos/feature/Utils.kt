package com.sirelon.marsroverphotos.feature

import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 04.05.2021 17:42 for Mars-Rover-Photos.
 */
fun List<MarsImage>.imageIds(): List<String> = map(MarsImage::id)
