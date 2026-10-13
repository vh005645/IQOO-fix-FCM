package com.fcmkeeper.iqoo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.isEnabled(context)) return
        try {
            ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
        } catch (_: Exception) {
            // If the service cannot start, the user can reopen the app and enable it again.
        }
        HeartbeatReceiver.schedule(context)
    }
}
