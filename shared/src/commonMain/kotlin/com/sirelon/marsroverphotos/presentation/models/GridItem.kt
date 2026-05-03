package com.sirelon.marsroverphotos.presentation.models

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact

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
