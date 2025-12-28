package com.sirelon.marsroverphotos.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks which educational facts have been shown to avoid repetition.
 * Uses session ID to track facts shown during the current app lifecycle.
 */
@Entity(tableName = "fact_displays")
data class FactDisplay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val factId: String,                                  // References Firestore fact document ID
    val displayTimestamp: Long = System.currentTimeMillis(),
    val sessionId: String                                 // Session UUID for tracking
)
