package com.sirelon.marsroverphotos.data.featureflags

import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlag
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import com.sirelon.marsroverphotos.utils.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * [FeatureFlags] backed by Firebase Remote Config.
 *
 * Two updates per launch: first whatever the previous launch activated (served from the SDK's
 * local store, so the flags are right within milliseconds), then the result of this launch's
 * `fetchAndActivate`. Any failure along the way is logged and leaves the last known values in
 * place — a flag can never throw its way into the UI.
 */
class FirebaseFeatureFlags(
    private val isDebug: Boolean,
    scope: CoroutineScope,
) : FeatureFlags {

    private companion object {
        const val TAG = "FeatureFlags"
        val RELEASE_FETCH_INTERVAL = 12.hours
    }

    private val flows = FeatureFlag.entries.associateWith { MutableStateFlow(it.initialValue()) }

    init {
        scope.launch {
            runCatching {
                val config = Firebase.remoteConfig
                // Debug builds refetch every launch so a console change shows up on the next start.
                config.settings { minimumFetchInterval = if (isDebug) Duration.ZERO else RELEASE_FETCH_INTERVAL }
                config.setDefaults(*FeatureFlag.entries.map { it.key to it.default }.toTypedArray())
                // Android loads the last activated config from disk asynchronously; without this the
                // first read can still be the in-code default.
                config.ensureInitialized()
                publish(config.readAll())
                config.fetchAndActivate()
                publish(config.readAll())
            }.onFailure { Logger.w(TAG) { "Remote Config unavailable, keeping defaults: ${it.message}" } }
        }
    }

    override fun isEnabled(flag: FeatureFlag): StateFlow<Boolean> = flows.getValue(flag).asStateFlow()

    private fun dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig.readAll(): Map<FeatureFlag, Boolean> =
        FeatureFlag.entries.associateWith { getValue(it.key).asBoolean() }

    private fun FeatureFlag.initialValue() = if (isDebug) enabledInDebug else default

    private fun publish(values: Map<FeatureFlag, Boolean>) {
        values.forEach { (flag, enabled) ->
            // A debug pin is not something the console can switch off (see FeatureFlag.enabledInDebug).
            flows.getValue(flag).value = if (isDebug && flag.enabledInDebug) true else enabled
        }
        Logger.d(TAG) { "Flags: ${values.entries.joinToString { "${it.key.key}=${it.value}" }}" }
    }
}
