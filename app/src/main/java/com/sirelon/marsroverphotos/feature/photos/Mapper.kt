package com.sirelon.marsroverphotos.feature.photos

import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.RoverCamera
import com.sirelon.marsroverphotos.network.ImageSourceResponse
import com.sirelon.marsroverphotos.network.PerseveranceCameraResponse
import com.sirelon.marsroverphotos.network.PerseverancePhotoItemResponse
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 21.02.2021 20:22 for Mars-Rover-Photos.
 */

fun List<MarsPhoto>.mapToUi(): List<MarsImage> {
    return mapIndexed { index, marsPhoto ->
        // It's okay to use not correct data for favorite and popular with Stats, 'cause if these images already in database, we'll ignore replacing them.
        MarsImage(
            id = marsPhoto.id,
            sol = marsPhoto.sol,
            name = marsPhoto.name,
            imageUrl = marsPhoto.imageUrl,
            earthDate = marsPhoto.earthDate,
            camera = marsPhoto.camera,
            favorite = false,
            popular = false,
            order = index,
            stats = defaultStats()
        )
    }
}


fun List<PerseverancePhotoItemResponse>.preveranceToUI(): List<MarsImage> {
    return mapIndexed { index, response ->
        // It's okay to use not correct data for favorite and popular with Stats, 'cause if these images already in database, we'll ignore replacing them.
        MarsImage(
            id = response.id,
            sol = response.sol,
            name = response.name,
            imageUrl = response.imageSourceResponse?.image() ?: "",
            earthDate = response.earthDate ?: "",
            camera = response.camera?.toUI(),
            favorite = false,
            popular = false,
            order = index,
            stats = defaultStats()
        )
    }
}

private fun defaultStats() = MarsImage.Stats(0, 0, 0, 0, 0)

fun ImageSourceResponse.image(): String {
    return medium ?: small ?: fullRes ?: large ?: ""
}

fun PerseveranceCameraResponse.toUI(): RoverCamera {
    // TODO: correct Id
    return RoverCamera(id = this.id.hashCode(), name = this.name, fullName = this.fullName)
}
