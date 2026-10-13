package com.fcmkeeper.iqoo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object RomSettings {

    // Autostart / background screens of Chinese ROMs. Component names change
    // between versions, so each one is tried in turn; vivo / iQOO come first.
    private val candidates = listOf(
        // vivo / iQOO (OriginOS, Funtouch OS)
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        "com.vivo.abe" to "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        // Xiaomi
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        // OPPO / realme / OnePlus (ColorOS)
        "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        // Honor
        "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
    )

    /** Returns true if one of the screens opened. */
    fun openBackgroundSettings(ctx: Context): Boolean {
        for ((pkg, cls) in candidates) {
            try {
                ctx.startActivity(
                    Intent().setClassName(pkg, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    fun openAppDetails(ctx: Context, pkg: String) {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
