package org.wsb.mescidtv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/**
 * Watchdog for the 24/7 mosque display.
 * Runs as a foreground service (so Android does not kill it) and brings the screen back
 * if the app is not visible for more than 30 seconds, unless the person closed it on purpose.
 */
class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val check = object : Runnable {
        override fun run() {
            if (AppState.userStopped) {
                stopSelf()
                return
            }
            val hiddenFor = System.currentTimeMillis() - AppState.lastVisibleAt
            if (!AppState.visible && hiddenFor > GRACE_MS) {
                launchApp()
            }
            handler.postDelayed(this, CHECK_EVERY_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        handler.removeCallbacks(check)
        handler.postDelayed(check, CHECK_EVERY_MS)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(check)
        super.onDestroy()
    }

    private fun launchApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Will try again on the next check
        }
    }

    private fun startAsForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WSB Mescid TV",
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("WSB Mescid TV")
            .setContentText("Mescid ekranı çalışıyor")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // The activity keeps the screen alive on its own even if the service cannot start
        }
    }

    companion object {
        private const val CHANNEL_ID = "wsb_mescid_tv_keepalive"
        private const val NOTIFICATION_ID = 7724
        private const val CHECK_EVERY_MS = 10_000L
        private const val GRACE_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Not allowed right now; MainActivity starts it again when it comes to the front
            }
        }
    }
}
