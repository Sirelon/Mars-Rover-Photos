package com.sirelon.marsroverphotos.feature.photos

import com.sirelon.marsroverphotos.feature.facts.EducationalFact
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Sealed class representing items that can appear in the photos grid.
 * Supports mixed content types: photos and educational fact cards.
 */
sealed class GridItem {

    /**
     * Photo item - displays a Mars rover photo.
     */
    data class PhotoItem(val image: MarsImage) : GridItem() {
        val id: String get() = image.id
    }

    /**
     * Fact card item - displays educational information.
     * Uses position to ensure unique keys even if same fact is shown multiple times.
     */
    data class FactItem(
        val fact: EducationalFact,
        val position: Int
    ) : GridItem() {
        val id: String get() = "fact_${fact.id}_$position"
    }
}
