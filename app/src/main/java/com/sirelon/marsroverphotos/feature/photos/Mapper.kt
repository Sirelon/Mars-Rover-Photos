package com.sirelon.marsroverphotos.feature.photos

import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 21.02.2021 20:22 for Mars-Rover-Photos.
 */

fun List<MarsPhoto>.mapToUi(): List<MarsImage> {
    return mapIndexed { index, it ->
        // It's okay to use not correct data for favorite and popular with Stats, 'cause if these images already in database, we'll ignore replacing them.
        MarsImage(
            id = it.id.toInt(),
            sol = it.sol,
            name = it.name,
            imageUrl = it.imageUrl,
            earthDate = it.earthDate,
            camera = it.camera,
            favorite = false,
            popular = false,
            order = index,
            stats = MarsImage.Stats(0, 0, 0, 0)
        )
    }
}
