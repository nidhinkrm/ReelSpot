package com.reelspot.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user preferences for ReelSpot, persisting settings across
 * app restarts and APK updates without impacting database schemas.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _nearbyAlertsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_NEARBY_ALERTS, DEFAULT_NEARBY_ALERTS)
    )
    val nearbyAlertsEnabled: StateFlow<Boolean> = _nearbyAlertsEnabled.asStateFlow()

    fun setNearbyAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEARBY_ALERTS, enabled).apply()
        _nearbyAlertsEnabled.value = enabled
    }

    companion object {
        const val PREFS_NAME = "reelspot_settings"
        const val KEY_NEARBY_ALERTS = "key_nearby_alerts_enabled"
        const val DEFAULT_NEARBY_ALERTS = true

        /**
         * Fast synchronous lookup for background components (such as GeofenceBroadcastReceiver)
         * to verify if Nearby Alerts are enabled before posting notifications.
         */
        fun isNearbyAlertsEnabledSync(context: Context): Boolean {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_NEARBY_ALERTS, DEFAULT_NEARBY_ALERTS)
        }
    }
}
