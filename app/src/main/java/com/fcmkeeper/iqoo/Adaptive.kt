package com.fcmkeeper.iqoo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object Adaptive {
    /** Interval between checks, in minutes. Change this single value to tune it. */
    const val INTERVAL_MINUTES = 3

    fun hasNetwork(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
