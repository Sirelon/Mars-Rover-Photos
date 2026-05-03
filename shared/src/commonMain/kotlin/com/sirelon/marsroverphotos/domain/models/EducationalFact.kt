package com.sirelon.marsroverphotos.domain.models

/**
 * Educational fact about Mars rovers, missions, and discoveries.
 * Facts are fetched from Firebase Firestore and cached in memory.
 */
data class EducationalFact(
    val id: String,      // Firestore document ID
    val text: String     // The fact content
)
