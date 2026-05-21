package com.sirelon.marsroverphotos.feature.gdpr

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.sirelon.marsroverphotos.platform.recordException
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow

class GdprHelper(private val activity: Activity) {

    private val consentInformation by lazy { UserMessagingPlatform.getConsentInformation(activity) }

    val acceptGdpr = MutableStateFlow(false)

    fun init() {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("62FB47CDEF3CE930EF49BB808381622F")
            .build()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                Logger.d("GdprHelper") { "Consent info updated: $consentInformation" }
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                } else {
                    acceptGdpr.value = true
                }
            },
            this::onError
        )
    }

    private fun onError(error: FormError) {
        recordException(RuntimeException("${error.message} (code ${error.errorCode})"))
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            activity,
            this::showConsentForm,
            this::onError
        )
    }

    private fun showConsentForm(consentForm: ConsentForm) {
        if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
            consentForm.show(activity) {
                // On dismissal, reload the form so a subsequent change of mind can be honored.
                loadForm()
            }
        } else {
            acceptGdpr.value = true
        }
    }
}
