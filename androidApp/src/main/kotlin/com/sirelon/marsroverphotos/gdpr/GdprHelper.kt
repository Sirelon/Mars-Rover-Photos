package com.sirelon.marsroverphotos.gdpr

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.platform.AndroidAdConsent
import com.sirelon.marsroverphotos.platform.recordException
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Helper for Google User Messaging Platform (UMP) GDPR consent.
 *
 * [init] refreshes the consent state on every launch, so a user whose consent is already on record
 * gets ads at once. The consent form itself — shown to EEA/UK users once — waits for [onPromptsAllowed],
 * which the Activity calls when `ConsentPromptGate` opens (the second rover tap, counted across
 * sessions), so a first launch shows the rover list and a whole first visit before any sheet. For non-EEA users or when consent is not required,
 * [acceptGdpr] emits `true` silently.
 */
class GdprHelper(private val activity: Activity) {

    private val consentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(activity)
    }

    /** Emits `true` once consent has been obtained or is not required. */
    val acceptGdpr = MutableStateFlow(false)

    private var promptsAllowed = false

    /** A form that loaded before [onPromptsAllowed]; shown the moment the prompts are allowed. */
    private var pendingForm: ConsentForm? = null

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

    /**
     * The gate opened: a form that has been waiting goes up now, and one that loads later goes up
     * as soon as it is ready.
     */
    fun onPromptsAllowed() {
        promptsAllowed = true
        pendingForm?.let { form ->
            pendingForm = null
            showConsentForm(form)
        }
    }

    private fun onError(error: FormError) {
        Logger.w(TAG) { "UMP error ${error.errorCode}: ${error.message}" }
        if (!error.isTransientNetworkError) {
            recordException(RuntimeException("UMP ${error.errorCode}: ${error.message}"))
        }
        updateAcceptanceFromConsentState()
    }

    /** INTERNET_ERROR/TIME_OUT are expected on flaky connections, not actionable bugs. */
    private val FormError.isTransientNetworkError: Boolean
        get() = errorCode == FormError.ErrorCode.INTERNET_ERROR || errorCode == FormError.ErrorCode.TIME_OUT

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            activity,
            ::showConsentForm,
            ::onError
        )
    }

    private fun showConsentForm(consentForm: ConsentForm) {
        Logger.d(TAG) { "showConsentForm status=${consentInformation.consentStatus} allowed=$promptsAllowed" }
        // The load callback can land after the Activity was destroyed (rotation, back); showing then
        // fails with "Activity is destroyed". The next Activity instance runs init() again.
        if (activity.isFinishing || activity.isDestroyed) return
        if (consentInformation.consentStatus != ConsentInformation.ConsentStatus.REQUIRED) {
            updateAcceptanceFromConsentState()
            return
        }
        if (!promptsAllowed) {
            // Consent is required but the gate has not opened yet: hold the form, and mirror the
            // current (denied) state so no ad is requested in the meantime.
            pendingForm = consentForm
            updateAcceptanceFromConsentState()
            return
        }
        consentForm.show(activity) { formError ->
            if (formError != null) {
                Logger.w(TAG) { "Consent form dismissed with error ${formError.errorCode}: ${formError.message}" }
                if (!formError.isTransientNetworkError) {
                    recordException(RuntimeException("UMP form dismiss ${formError.errorCode}: ${formError.message}"))
                }
            }
            // Reload form after dismissal so it is ready for future re-requests.
            if (!activity.isFinishing && !activity.isDestroyed) loadForm()
            updateAcceptanceFromConsentState()
        }
    }

    private fun updateAcceptanceFromConsentState() {
        // UMP source of truth: only true when ad requests are currently allowed.
        val canRequestAds = consentInformation.canRequestAds()
        acceptGdpr.value = canRequestAds
        // Mirror into shared state so AdSlot knows whether it may request ads at all.
        AndroidAdConsent.canRequestAds.value = canRequestAds
        // Mirror into the shared state so AdSlot can switch to NPA when consent is denied.
        AndroidAdConsent.personalizedAds.value = canRequestAds
    }

    private companion object {
        private const val TAG = "GdprHelper"
    }
}
