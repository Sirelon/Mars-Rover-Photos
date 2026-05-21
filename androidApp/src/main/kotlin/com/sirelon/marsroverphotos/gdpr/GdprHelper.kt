package com.sirelon.marsroverphotos.gdpr

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.platform.recordException
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Helper for Google User Messaging Platform (UMP) GDPR consent.
 * Shows a consent form for EEA users when personalized ads are in use.
 * For non-EEA users or when consent is not required, [acceptGdpr] emits `true` silently.
 */
class GdprHelper(private val activity: Activity) {

    private val consentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(activity)
    }

    /** Emits `true` once consent has been obtained or is not required. */
    val acceptGdpr = MutableStateFlow(false)

    fun init() {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            // In debug builds, force EEA geography so the consent form is always testable.
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("62FB47CDEF3CE930EF49BB808381622F")
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                Logger.d(TAG) { "Consent info updated: $consentInformation" }
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                } else {
                    updateAcceptanceFromConsentState()
                }
            },
            ::onError
        )
    }

    private fun onError(error: FormError) {
        Logger.w(TAG) { "UMP error ${error.errorCode}: ${error.message}" }
        recordException(RuntimeException("UMP ${error.errorCode}: ${error.message}"))
        updateAcceptanceFromConsentState()
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            activity,
            ::showConsentForm,
            ::onError
        )
    }

    private fun showConsentForm(consentForm: ConsentForm) {
        Logger.d(TAG) { "showConsentForm status=${consentInformation.consentStatus}" }
        if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
            consentForm.show(activity) { formError ->
                if (formError != null) {
                    Logger.w(TAG) { "Consent form dismissed with error ${formError.errorCode}: ${formError.message}" }
                    recordException(RuntimeException("UMP form dismiss ${formError.errorCode}: ${formError.message}"))
                }
                // Reload form after dismissal so it is ready for future re-requests.
                loadForm()
                updateAcceptanceFromConsentState()
            }
        } else {
            updateAcceptanceFromConsentState()
        }
    }

    private fun updateAcceptanceFromConsentState() {
        // UMP source of truth: only true when ad requests are currently allowed.
        acceptGdpr.value = consentInformation.canRequestAds()
    }

    private companion object {
        private const val TAG = "GdprHelper"
    }
}
