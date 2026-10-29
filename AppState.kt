package org.wsb.mescidtv

/** Tiny shared state between the activity, the watchdog service and the boot receiver. */
object AppState {
    /** True while MainActivity is on screen. */
    @Volatile
    var visible: Boolean = false

    /** Last moment the screen was seen (used by the watchdog). */
    @Volatile
    var lastVisibleAt: Long = System.currentTimeMillis()

    /** Set when the person deliberately closes the app (hold BACK for 3 seconds). Cleared on reboot or manual open. */
    @Volatile
    var userStopped: Boolean = false
}
