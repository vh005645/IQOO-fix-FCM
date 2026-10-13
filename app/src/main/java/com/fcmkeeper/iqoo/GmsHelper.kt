package com.fcmkeeper.iqoo

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object GmsHelper {
    const val GMS = "com.google.android.gms"
    const val GSF = "com.google.android.gsf"

    fun isInstalled(ctx: Context, pkg: String = GMS): Boolean =
        try {
            ctx.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun isSystemApp(ctx: Context, pkg: String = GMS): Boolean =
        try {
            val info = ctx.packageManager.getApplicationInfo(pkg, 0)
            (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    private fun send(ctx: Context, actions: List<String>) {
        if (!isInstalled(ctx)) return
        for (action in actions) {
            try {
                ctx.sendBroadcast(Intent(action).setPackage(GMS))
            } catch (_: Exception) {
            }
        }
    }

    /** Best-effort heartbeat. Newer GMS versions may ignore these intents. */
    fun heartbeat(ctx: Context) {
        send(
            ctx,
            listOf(
                "com.google.android.intent.action.MCS_HEARTBEAT",
                "com.google.android.gms.gcm.ACTION_HEARTBEAT_NOW"
            )
        )
    }

    /** Best-effort request for GMS to reconnect to the FCM server. */
    fun reconnect(ctx: Context) {
        send(ctx, listOf("com.google.android.intent.action.GCM_RECONNECT"))
        Prefs.setLastReconnect(ctx, System.currentTimeMillis())
    }
}
