package io.github.jqssun.airplay.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Fire OS turns the TV on over HDMI-CEC (One Touch Play) only on a Home key press, not when the
 * box merely wakes up. An app can't press Home; an accessibility service can. It also lets the
 * remote's Back close the code card. Reads no screen content.
 * Enabled by the kit over ADB (secure setting enabled_accessibility_services).
 */
class TvWakeAccessibility : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    /** Remote keys pass through untouched unless [onKey] eats one (Back while the code is up). */
    override fun onKeyEvent(event: KeyEvent): Boolean = onKey?.invoke(event) ?: false

    companion object {
        @Volatile private var instance: TvWakeAccessibility? = null
        @Volatile var onKey: ((KeyEvent) -> Boolean)? = null

        /** Press Home so the box tells the TV to turn on and switch to it. False when not enabled. */
        fun turnOnTv(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false

        /** Put the box back to sleep (Android 9+), so the next AirPlay wakes it and turns the TV on again. */
        fun sleep(): Boolean = Build.VERSION.SDK_INT >= 28 &&
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
    }
}
