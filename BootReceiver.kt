package org.wsb.mescidtv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts the mosque screen automatically when the TV / Fire TV powers up after a power cut,
 * and again after the app has been updated.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            AppState.userStopped = false
            KeepAliveService.start(context)
            try {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
            } catch (_: Exception) {
                // The watchdog service will open the screen within a few seconds
            }
        }
    }
}
