package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger

class IosTracker : Tracker {
    override fun trackClick(event: String) { Logger.d("Tracker") { "trackClick: $event" } }
    override fun trackEvent(event: String, params: Map<String, String>) { Logger.d("Tracker") { "trackEvent: $event $params" } }
    override fun trackFavorite(photo: MarsImage, from: String, fav: Boolean) { Logger.d("Tracker") { "trackFavorite: ${photo.id} fav=$fav from=$from" } }
    override fun trackSeen(photo: MarsImage) { Logger.d("Tracker") { "trackSeen: ${photo.id}" } }
    override fun trackScale(photo: MarsImage) { Logger.d("Tracker") { "trackScale: ${photo.id}" } }
    override fun trackSave(photo: MarsImage) { Logger.d("Tracker") { "trackSave: ${photo.id}" } }
    override fun trackShare(photo: MarsImage, packageName: String?) { Logger.d("Tracker") { "trackShare: ${photo.id}" } }
}
