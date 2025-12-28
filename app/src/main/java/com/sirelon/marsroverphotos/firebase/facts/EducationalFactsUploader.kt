package com.sirelon.marsroverphotos.firebase.facts

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Utility class for uploading educational facts from assets to Firebase Firestore.
 *
 * This is a one-time data seeding utility that reads facts from JSON files
 * in the assets folder and uploads them to the "educational_facts" collection.
 */
internal class EducationalFactsUploader(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val factsCollection = firestore.collection("educational_facts")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Uploads all educational facts to Firestore.
     * @return FactsUploadResult with success/failure counts and details
     */
    suspend fun uploadAllFacts(): FactsUploadResult {
        val rawFacts = try {
            loadFactsFromAssets()
        } catch (e: Exception) {
            Timber.e(e, "Failed to load educational facts from assets")
            return FactsUploadResult(
                successCount = 0,
                failureCount = 1,
                details = listOf(
                    SingleFactUploadResult(
                        factId = ASSET_PATH,
                        textPreview = "",
                        success = false,
                        error = "Failed to read facts: ${e.message}"
                    )
                )
            )
        }

        val normalized = normalizeFacts(rawFacts)
        val results = mutableListOf<SingleFactUploadResult>()

        normalized.invalidFacts.forEach { invalid ->
            results.add(
                SingleFactUploadResult(
                    factId = invalid.id.ifBlank { "<missing-id>" },
                    textPreview = invalid.text,
                    success = false,
                    error = invalid.error
                )
            )
        }

        for (fact in normalized.validFacts) {
            val result = uploadSingleFact(fact)
            results.add(result)
        }

        val successCount = results.count { it.success }
        val failureCount = results.count { !it.success }

        Timber.i("Educational fact upload complete: $successCount succeeded, $failureCount failed")

        return FactsUploadResult(
            successCount = successCount,
            failureCount = failureCount,
            details = results
        )
    }

    /**
     * Verifies uploaded data by reading it back from Firestore.
     */
    suspend fun verifyUploadedData(): FactsVerificationResult {
        val rawFacts = try {
            loadFactsFromAssets()
        } catch (e: Exception) {
            Timber.e(e, "Failed to load educational facts from assets")
            return FactsVerificationResult(
                totalChecked = 1,
                foundCount = 0,
                verifications = listOf(
                    SingleFactVerification(
                        factId = ASSET_PATH,
                        found = false,
                        textPresent = false,
                        error = "Failed to read facts: ${e.message}"
                    )
                )
            )
        }

        val normalized = normalizeFacts(rawFacts)
        val verifications = mutableListOf<SingleFactVerification>()

        normalized.invalidFacts.forEach { invalid ->
            verifications.add(
                SingleFactVerification(
                    factId = invalid.id.ifBlank { "<missing-id>" },
                    found = false,
                    textPresent = false,
                    error = invalid.error
                )
            )
        }

        for (fact in normalized.validFacts) {
            try {
                val doc = factsCollection.document(fact.id).get().await()
                if (doc.exists()) {
                    val text = doc.getString("text")
                    val hasText = !text.isNullOrBlank()

                    Timber.d("Verified fact ${fact.id} (textPresent=$hasText)")

                    verifications.add(
                        SingleFactVerification(
                            factId = fact.id,
                            found = true,
                            textPresent = hasText,
                            error = if (hasText) null else "Text field is blank"
                        )
                    )
                } else {
                    Timber.w("Fact ${fact.id} not found in Firestore")
                    verifications.add(
                        SingleFactVerification(
                            factId = fact.id,
                            found = false,
                            textPresent = false,
                            error = "Document not found"
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error verifying fact ${fact.id}")
                verifications.add(
                    SingleFactVerification(
                        factId = fact.id,
                        found = false,
                        textPresent = false,
                        error = "Verification failed: ${e.message}"
                    )
                )
            }
        }

        val foundCount = verifications.count { it.found }
        return FactsVerificationResult(
            totalChecked = verifications.size,
            foundCount = foundCount,
            verifications = verifications
        )
    }

    private suspend fun uploadSingleFact(fact: EducationalFactSeed): SingleFactUploadResult {
        return try {
            factsCollection.document(fact.id)
                .set(
                    hashMapOf(
                        "text" to fact.text
                    )
                )
                .await()

            Timber.d("Uploaded fact ${fact.id}")

            SingleFactUploadResult(
                factId = fact.id,
                textPreview = fact.text,
                success = true,
                error = null
            )
        } catch (e: Exception) {
            Timber.e(e, "Error uploading fact ${fact.id}")
            SingleFactUploadResult(
                factId = fact.id,
                textPreview = fact.text,
                success = false,
                error = "Upload failed: ${e.message}"
            )
        }
    }

    private fun loadFactsFromAssets(): List<EducationalFactSeed> {
        val jsonContent = readAssetFile(ASSET_FILE_NAME)
        return json.decodeFromString<List<EducationalFactSeed>>(jsonContent)
    }

    private fun readAssetFile(fileName: String): String {
        return context.assets.open("facts/$fileName").bufferedReader().use { it.readText() }
    }

    private fun normalizeFacts(rawFacts: List<EducationalFactSeed>): NormalizedFacts {
        val seenIds = mutableSetOf<String>()
        val validFacts = mutableListOf<EducationalFactSeed>()
        val invalidFacts = mutableListOf<InvalidFact>()

        rawFacts.forEach { fact ->
            val id = fact.id.trim()
            val text = fact.text.trim()

            when {
                id.isEmpty() -> invalidFacts.add(
                    InvalidFact(
                        id = id,
                        text = text,
                        error = "Missing fact id"
                    )
                )
                text.isEmpty() -> invalidFacts.add(
                    InvalidFact(
                        id = id,
                        text = text,
                        error = "Missing fact text"
                    )
                )
                !seenIds.add(id) -> invalidFacts.add(
                    InvalidFact(
                        id = id,
                        text = text,
                        error = "Duplicate fact id"
                    )
                )
                else -> validFacts.add(
                    EducationalFactSeed(
                        id = id,
                        text = text
                    )
                )
            }
        }

        return NormalizedFacts(
            validFacts = validFacts,
            invalidFacts = invalidFacts
        )
    }

    private data class NormalizedFacts(
        val validFacts: List<EducationalFactSeed>,
        val invalidFacts: List<InvalidFact>
    )

    private data class InvalidFact(
        val id: String,
        val text: String,
        val error: String
    )

    companion object {
        private const val ASSET_FILE_NAME = "educational_facts.json"
        private const val ASSET_PATH = "assets/facts/educational_facts.json"
    }
}

@Serializable
internal data class EducationalFactSeed(
    val id: String = "",
    val text: String = ""
)

/**
 * Result of uploading all facts.
 */
internal data class FactsUploadResult(
    val successCount: Int,
    val failureCount: Int,
    val details: List<SingleFactUploadResult>
) {
    val isFullSuccess: Boolean get() = failureCount == 0
    val totalProcessed: Int get() = successCount + failureCount
}

/**
 * Result of uploading a single fact.
 */
internal data class SingleFactUploadResult(
    val factId: String,
    val textPreview: String,
    val success: Boolean,
    val error: String?
)

/**
 * Result of verification.
 */
internal data class FactsVerificationResult(
    val totalChecked: Int,
    val foundCount: Int,
    val verifications: List<SingleFactVerification>
) {
    val allFound: Boolean get() = foundCount == totalChecked
}

/**
 * Single verification result.
 */
internal data class SingleFactVerification(
    val factId: String,
    val found: Boolean,
    val textPresent: Boolean,
    val error: String?
)
