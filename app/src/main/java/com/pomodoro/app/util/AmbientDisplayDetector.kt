package com.pomodoro.app.util

import android.content.Context
import android.provider.Settings

/**
 * Best-effort check for whether the system's Always-On Display / Ambient
 * Display setting is turned on. This reads a public Settings.Secure key that
 * most OEMs expose (Android's own AOD toggle uses it); it's advisory only —
 * some manufacturers use a different key or block reads — so callers should
 * treat a `false` result as "unknown" and still offer the manual toggle.
 */
object AmbientDisplayDetector {
    private const val DOZE_ALWAYS_ON_KEY = "doze_always_on"

    fun isSystemAmbientDisplayEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, DOZE_ALWAYS_ON_KEY, 0) == 1
        } catch (e: Settings.SettingNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
