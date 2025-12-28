package com.sirelon.marsroverphotos.firebase.facts

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Debug utility for uploading educational facts to Firebase.
 *
 * This composable can be added to any debug screen for easy data upload.
 */
@Composable
internal fun EducationalFactsUploadDebugPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploadStatus by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var verificationStatus by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Educational Facts Uploader",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Upload educational facts to Firebase Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    scope.launch {
                        isUploading = true
                        uploadStatus = "Uploading..."
                        try {
                            val uploader = EducationalFactsUploader(context)
                            val result = uploader.uploadAllFacts()

                            uploadStatus = buildString {
                                append("Upload complete!\n")
                                append("Success: ${result.successCount}\n")
                                append("Failed: ${result.failureCount}\n\n")
                                result.details.forEach { detail ->
                                    if (detail.success) {
                                        append("OK ${detail.factId}\n")
                                    } else {
                                        append("FAIL ${detail.factId}: ${detail.error}\n")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            uploadStatus = "Error: ${e.message}"
                            Timber.e(e, "Upload failed")
                        } finally {
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isUploading) "Uploading..." else "Upload Educational Facts")
            }

            if (uploadStatus.isNotEmpty()) {
                Text(
                    text = uploadStatus,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        verificationStatus = "Verifying..."
                        try {
                            val uploader = EducationalFactsUploader(context)
                            val result = uploader.verifyUploadedData()

                            verificationStatus = buildString {
                                append("Verification complete!\n")
                                append("Found: ${result.foundCount}/${result.totalChecked}\n\n")
                                result.verifications.forEach { verification ->
                                    when {
                                        verification.found && verification.textPresent ->
                                            append("OK ${verification.factId}\n")
                                        verification.found ->
                                            append(
                                                "WARN ${verification.factId}: ${verification.error ?: "Unknown issue"}\n"
                                            )
                                        else ->
                                            append(
                                                "MISSING ${verification.factId}: ${verification.error ?: "Unknown issue"}\n"
                                            )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            verificationStatus = "Verification error: ${e.message}"
                            Timber.e(e, "Verification failed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify Uploaded Facts")
            }

            if (verificationStatus.isNotEmpty()) {
                Text(
                    text = verificationStatus,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Programmatic helper function to upload educational facts.
 */
internal suspend fun uploadEducationalFactsToFirebase(
    context: Context,
    onComplete: (FactsUploadResult) -> Unit = {}
) {
    try {
        val uploader = EducationalFactsUploader(context)
        val result = uploader.uploadAllFacts()
        onComplete(result)
    } catch (e: Exception) {
        Timber.e(e, "Failed to upload educational facts")
        onComplete(
            FactsUploadResult(
                successCount = 0,
                failureCount = 1,
                details = emptyList()
            )
        )
    }
}

/**
 * Programmatic helper function to verify uploaded data.
 */
internal suspend fun verifyEducationalFactsInFirebase(
    context: Context,
    onComplete: (FactsVerificationResult) -> Unit = {}
) {
    try {
        val uploader = EducationalFactsUploader(context)
        val result = uploader.verifyUploadedData()
        onComplete(result)
    } catch (e: Exception) {
        Timber.e(e, "Failed to verify educational facts")
        onComplete(
            FactsVerificationResult(
                totalChecked = 0,
                foundCount = 0,
                verifications = emptyList()
            )
        )
    }
}
