package com.fcmkeeper.iqoo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class KeepAliveService : Service() {

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        HeartbeatReceiver.schedule(this)
        registerNetworkCallback()
        if (Prefs.bindEnabled(this)) GmsBinder.start(this)

        val app = applicationContext
        Thread { Tick.run(app) }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        unregisterNetworkCallback()
        GmsBinder.stop(this)
        super.onDestroy()
    }

    /** When the network changes, FCM connections often die: re-check shortly after. */
    private fun registerNetworkCallback() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val app = applicationContext
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Thread {
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                    }
                    Tick.run(app)
                }.start()
            }

            override fun onLost(network: Network) {
                Prefs.recordConnectionState(app, 0)
            }
        }
        try {
            cm.registerDefaultNetworkCallback(cb)
            networkCallback = cb
        } catch (_: Exception) {
        }
    }

    private fun unregisterNetworkCallback() {
        val cb = networkCallback ?: return
        try {
            getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(cb)
        } catch (_: Exception) {
        }
        networkCallback = null
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Giữ kết nối thông báo", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("FCM Keeper đang chạy")
            .setContentText("Giữ kết nối thông báo Google hoạt động")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "keep_alive"
        private const val NOTIF_ID = 1
    }
}
