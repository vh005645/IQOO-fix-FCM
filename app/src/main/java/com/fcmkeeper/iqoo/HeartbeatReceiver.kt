package com.fcmkeeper.iqoo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat

class HeartbeatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.isEnabled(context)) return
        val app = context.applicationContext

        // Schedule first, so the next alarm exists even if the check fails.
        schedule(app)

        val pending = goAsync()
        Thread {
            try {
                Tick.run(app)
            } finally {
                pending.finish()
            }
        }.start()

        // If the system killed the service, try to bring it back.
        try {
            ContextCompat.startForegroundService(app, Intent(app, KeepAliveService::class.java))
        } catch (_: Exception) {
            // Android 15 may refuse a foreground-service start from the background.
        }
    }

    companion object {
        private fun pendingIntent(ctx: Context): PendingIntent =
            PendingIntent.getBroadcast(
                ctx,
                1001,
                Intent(ctx, HeartbeatReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        fun schedule(ctx: Context) {
            val minutes = Adaptive.INTERVAL_MINUTES
            val delay = minutes * 60_000L
            val triggerAt = SystemClock.elapsedRealtime() + delay
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pendingIntent(ctx)

            val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            try {
                if (canExact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
                }
            } catch (_: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            }
            Prefs.setNext(ctx, System.currentTimeMillis() + delay, minutes)
        }

        fun cancel(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingIntent(ctx))
            Prefs.setNext(ctx, 0L, 0)
        }
    }
}
