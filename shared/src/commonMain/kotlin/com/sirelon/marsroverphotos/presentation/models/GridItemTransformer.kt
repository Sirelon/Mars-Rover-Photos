package com.sirelon.marsroverphotos.presentation.models

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact

/**
 * Transforms a list of photos into a mixed list of photos and fact cards.
 * Inserts educational facts at regular intervals in the photo grid.
 */
object GridItemTransformer {

    private const val FACT_FREQUENCY = 10 // Insert fact every 10 photos

    /**
     * Creates a mixed list of photo items and fact items.
     * Facts are inserted every FACT_FREQUENCY photos.
     *
     * @param photos List of photos to display
     * @param facts List of facts to insert (should have enough facts for the photo count)
     * @param factsEnabled Whether to insert facts at all
     * @return Mixed list of GridItem.PhotoItem and GridItem.FactItem
     */
    fun createGridItems(
        photos: List<MarsImage>,
        facts: List<EducationalFact>,
        factsEnabled: Boolean
    ): List<GridItem> {
        if (!factsEnabled || photos.isEmpty()) {
            return photos.map { GridItem.PhotoItem(it) }
        }

        val gridItems = mutableListOf<GridItem>()
        var factIndex = 0

        photos.forEachIndexed { index, photo ->
            // Add photo
            gridItems.add(GridItem.PhotoItem(photo))

            // Insert fact card after every FACT_FREQUENCY photos
            // But not after the last photo
            val isFactPosition = (index + 1) % FACT_FREQUENCY == 0
            val notLastPhoto = index < photos.size - 1
            val hasMoreFacts = factIndex < facts.size

            if (isFactPosition && notLastPhoto && hasMoreFacts) {
                val fact = facts[factIndex]
                gridItems.add(GridItem.FactItem(fact, gridItems.size))
                factIndex++
            }
        }

        return gridItems
    }

    /**
     * Calculate how many facts are needed for a given photo count.
     * Used to determine how many facts to fetch from the repository.
     */
    fun calculateRequiredFacts(photoCount: Int): Int {
        if (photoCount <= FACT_FREQUENCY) return 0
        return (photoCount - 1) / FACT_FREQUENCY
    }
}
