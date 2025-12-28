package com.sirelon.marsroverphotos.firebase.mission

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
 * Debug utility for uploading mission data to Firebase.
 *
 * This composable can be added to any debug screen for easy data upload.
 *
 * Usage:
 * ```
 * if (BuildConfig.DEBUG) {
 *     MissionDataUploadDebugPanel()
 * }
 * ```
 */
@Composable
fun MissionDataUploadDebugPanel() {
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
                text = "🚀 Mission Data Uploader",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Upload rover mission facts to Firebase Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    scope.launch {
                        isUploading = true
                        uploadStatus = "Uploading..."
                        try {
                            val uploader = MissionDataUploader(context)
                            val result = uploader.uploadAllMissions()

                            uploadStatus = buildString {
                                append("✅ Upload complete!\n")
                                append("Success: ${result.successCount}\n")
                                append("Failed: ${result.failureCount}\n\n")
                                result.details.forEach { detail ->
                                    if (detail.success) {
                                        append("✓ ${detail.roverName}\n")
                                    } else {
                                        append("✗ ${detail.roverName}: ${detail.error}\n")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            uploadStatus = "❌ Error: ${e.message}"
                            Timber.e(e, "Upload failed")
                        } finally {
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isUploading) "Uploading..." else "Upload Mission Data")
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
                            val uploader = MissionDataUploader(context)
                            val result = uploader.verifyUploadedData()

                            verificationStatus = buildString {
                                append("🔍 Verification complete!\n")
                                append("Found: ${result.foundCount}/${result.totalChecked}\n\n")
                                result.verifications.forEach { verification ->
                                    if (verification.found) {
                                        append("✓ ${verification.roverName} ")
                                        append("(Obj: ${verification.objectivesCount}, ")
                                        append("Facts: ${verification.funFactsCount})\n")
                                    } else {
                                        append("✗ Rover ${verification.roverId} not found\n")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            verificationStatus = "❌ Verification error: ${e.message}"
                            Timber.e(e, "Verification failed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify Uploaded Data")
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
 * Programmatic helper function to upload mission data.
 *
 * This can be called from anywhere in the app for one-time data seeding.
 *
 * Usage:
 * ```
 * lifecycleScope.launch {
 *     uploadMissionDataToFirebase(context) { result ->
 *         if (result.isFullSuccess) {
 *             println("All missions uploaded successfully!")
 *         } else {
 *             println("Upload completed with ${result.failureCount} failures")
 *         }
 *     }
 * }
 * ```
 */
suspend fun uploadMissionDataToFirebase(
    context: Context,
    onComplete: (UploadResult) -> Unit = {}
) {
    try {
        val uploader = MissionDataUploader(context)
        val result = uploader.uploadAllMissions()
        onComplete(result)
    } catch (e: Exception) {
        Timber.e(e, "Failed to upload mission data")
        onComplete(
            UploadResult(
                successCount = 0,
                failureCount = 5,
                details = emptyList()
            )
        )
    }
}

/**
 * Programmatic helper function to verify uploaded data.
 */
suspend fun verifyMissionDataInFirebase(
    context: Context,
    onComplete: (VerificationResult) -> Unit = {}
) {
    try {
        val uploader = MissionDataUploader(context)
        val result = uploader.verifyUploadedData()
        onComplete(result)
    } catch (e: Exception) {
        Timber.e(e, "Failed to verify mission data")
        onComplete(
            VerificationResult(
                totalChecked = 5,
                foundCount = 0,
                verifications = emptyList()
            )
        )
    }
}
