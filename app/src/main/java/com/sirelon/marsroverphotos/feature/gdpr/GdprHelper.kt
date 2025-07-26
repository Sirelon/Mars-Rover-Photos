package com.sirelon.marsroverphotos.feature.gdpr

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.sirelon.marsroverphotos.extensions.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber


/**
 * Created on 07.09.2021 23:51 for Mars-Rover-Photos.
 */
class GdprHelper(private val activity: Activity) {

    private val consentInformation by lazy { UserMessagingPlatform.getConsentInformation(activity) }

    val acceptGdpr = MutableStateFlow(false)

    fun init() {
//        consentInformation.reset()
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("62FB47CDEF3CE930EF49BB808381622F")
            .build()

        // Set tag for underage of consent. false means users are not underage.
        // Set tag for underage of consent. false means users are not underage.
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity, params, {
                Timber.d("init() called $consentInformation")
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                } else {
                    acceptGdpr.value = true
                }
            },
            this::onError
        )
    }

    private fun onError(it: FormError) {
        recordException(RuntimeException(it.message + it.errorCode))
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            activity,
            this::showConsentForm,
            this::onError
        )
    }

    private fun showConsentForm(consentForm: ConsentForm) {
        Timber.d("loadForm() called with: consentForm = $consentForm annd consentInformation.consentStatus ${consentInformation.consentStatus}")
        if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
            consentForm.show(activity) { // Handle dismissal by reloading form.
                loadForm()
            }
        } else {
            acceptGdpr.value = true
        }
    }

}