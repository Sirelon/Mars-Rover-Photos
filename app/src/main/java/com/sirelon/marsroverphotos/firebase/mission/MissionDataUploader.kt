package com.sirelon.marsroverphotos.firebase.mission

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException

/**
 * Utility class for uploading rover mission data from assets to Firebase Firestore.
 *
 * This is a one-time data seeding utility that reads mission data from JSON files
 * in the assets folder and uploads them to the "rover-missions" collection.
 *
 * Usage:
 * ```
 * val uploader = MissionDataUploader(context)
 * val result = uploader.uploadAllMissions()
 * ```
 */
class MissionDataUploader(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val missionCollection = firestore.collection("rover-missions")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Uploads all rover mission data to Firestore.
     * @return UploadResult with success/failure counts and details
     */
    suspend fun uploadAllMissions(): UploadResult {
        val missions = listOf(
            MissionFile(roverId = 3, fileName = "perseverance.json"),
            MissionFile(roverId = 4, fileName = "insight.json"),
            MissionFile(roverId = 5, fileName = "curiosity.json"),
            MissionFile(roverId = 6, fileName = "opportunity.json"),
            MissionFile(roverId = 7, fileName = "spirit.json")
        )

        val results = mutableListOf<SingleUploadResult>()

        for (mission in missions) {
            val result = uploadSingleMission(mission)
            results.add(result)
        }

        val successCount = results.count { it.success }
        val failureCount = results.count { !it.success }

        Timber.i("Upload complete: $successCount succeeded, $failureCount failed")

        return UploadResult(
            successCount = successCount,
            failureCount = failureCount,
            details = results
        )
    }

    /**
     * Uploads a single rover mission to Firestore.
     */
    private suspend fun uploadSingleMission(missionFile: MissionFile): SingleUploadResult {
        return try {
            // Read JSON from assets
            val jsonContent = readAssetFile(missionFile.fileName)

            // Parse JSON to RoverMissionFacts
            val missionData = json.decodeFromString<RoverMissionFacts>(jsonContent)

            // Upload to Firestore
            missionCollection.document(missionFile.roverId.toString())
                .set(
                    hashMapOf(
                        "roverId" to missionData.roverId,
                        "roverName" to missionData.roverName,
                        "objectives" to missionData.objectives,
                        "funFacts" to missionData.funFacts
                    )
                )
                .await()

            Timber.d("✓ Uploaded ${missionData.roverName} (ID: ${missionFile.roverId})")
            Timber.d("  - Objectives: ${missionData.objectives.size}")
            Timber.d("  - Fun Facts: ${missionData.funFacts.size}")

            SingleUploadResult(
                roverId = missionFile.roverId,
                roverName = missionData.roverName,
                success = true,
                error = null
            )
        } catch (e: IOException) {
            Timber.e(e, "✗ Error reading file ${missionFile.fileName}")
            SingleUploadResult(
                roverId = missionFile.roverId,
                roverName = "Unknown",
                success = false,
                error = "Failed to read file: ${e.message}"
            )
        } catch (e: Exception) {
            Timber.e(e, "✗ Error uploading mission for rover ${missionFile.roverId}")
            SingleUploadResult(
                roverId = missionFile.roverId,
                roverName = "Unknown",
                success = false,
                error = "Upload failed: ${e.message}"
            )
        }
    }

    /**
     * Reads a file from the assets/missions folder.
     */
    private fun readAssetFile(fileName: String): String {
        return context.assets.open("missions/$fileName").bufferedReader().use { it.readText() }
    }

    /**
     * Verifies uploaded data by reading it back from Firestore.
     */
    suspend fun verifyUploadedData(): VerificationResult {
        val roverIds = listOf(3L, 4L, 5L, 6L, 7L)
        val verifications = mutableListOf<SingleVerification>()

        for (roverId in roverIds) {
            try {
                val doc = missionCollection.document(roverId.toString()).get().await()

                if (doc.exists()) {
                    val data = doc.data.orEmpty()
                    val objectivesCount = (data["objectives"] as? List<*>)?.size ?: 0
                    val funFactsCount = (data["funFacts"] as? List<*>)?.size ?: 0
                    val roverName = data["roverName"] as? String ?: "Unknown"

                    Timber.d("✓ $roverName verified (ID: $roverId)")
                    Timber.d("  - Objectives: $objectivesCount")
                    Timber.d("  - Fun Facts: $funFactsCount")

                    verifications.add(
                        SingleVerification(
                            roverId = roverId,
                            roverName = roverName,
                            found = true,
                            objectivesCount = objectivesCount,
                            funFactsCount = funFactsCount
                        )
                    )
                } else {
                    Timber.w("✗ Rover $roverId not found in Firestore")
                    verifications.add(
                        SingleVerification(
                            roverId = roverId,
                            roverName = "Unknown",
                            found = false,
                            objectivesCount = 0,
                            funFactsCount = 0
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "✗ Error verifying rover $roverId")
                verifications.add(
                    SingleVerification(
                        roverId = roverId,
                        roverName = "Unknown",
                        found = false,
                        objectivesCount = 0,
                        funFactsCount = 0
                    )
                )
            }
        }

        val foundCount = verifications.count { it.found }
        return VerificationResult(
            totalChecked = roverIds.size,
            foundCount = foundCount,
            verifications = verifications
        )
    }
}

/**
 * Represents a mission file in assets.
 */
private data class MissionFile(
    val roverId: Long,
    val fileName: String
)

/**
 * Result of uploading all missions.
 */
data class UploadResult(
    val successCount: Int,
    val failureCount: Int,
    val details: List<SingleUploadResult>
) {
    val isFullSuccess: Boolean get() = failureCount == 0
    val totalProcessed: Int get() = successCount + failureCount
}

/**
 * Result of uploading a single mission.
 */
data class SingleUploadResult(
    val roverId: Long,
    val roverName: String,
    val success: Boolean,
    val error: String?
)

/**
 * Result of verification.
 */
data class VerificationResult(
    val totalChecked: Int,
    val foundCount: Int,
    val verifications: List<SingleVerification>
) {
    val allFound: Boolean get() = foundCount == totalChecked
}

/**
 * Single verification result.
 */
data class SingleVerification(
    val roverId: Long,
    val roverName: String,
    val found: Boolean,
    val objectivesCount: Int,
    val funFactsCount: Int
)
