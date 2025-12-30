package com.sirelon.marsroverphotos.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

/**
 * Tracks which educational facts have been shown to avoid repetition.
 * Uses session ID to track facts shown during the current app lifecycle.
 */
@Entity(tableName = "fact_displays")
data class FactDisplay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val factId: String,                                  // References Firestore fact document ID
    val displayTimestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val sessionId: String                                 // Session UUID for tracking
)
