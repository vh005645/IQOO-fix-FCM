package com.fcmkeeper.iqoo

import android.content.Context

object Prefs {
    private const val FILE = "fcm_keeper"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_BEAT = "last_beat"
    private const val KEY_NEXT_BEAT = "next_beat"
    private const val KEY_INTERVAL = "interval_min"
    private const val KEY_CONN = "conn_state"
    private const val KEY_LAST_RECONNECT = "last_reconnect"
    private const val KEY_STATUS_CODE = "status_code"
    private const val KEY_STATUS_ERROR = "status_error"
    private const val KEY_BIND = "bind_gms"
    private const val KEY_CONNECTED_SINCE = "connected_since"
    private const val KEY_LAST_DISCONNECT = "last_disconnect"

    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(KEY_ENABLED, v).apply()

    fun lastBeat(ctx: Context): Long = p(ctx).getLong(KEY_LAST_BEAT, 0L)
    fun setLastBeat(ctx: Context, v: Long) = p(ctx).edit().putLong(KEY_LAST_BEAT, v).apply()

    fun nextBeat(ctx: Context): Long = p(ctx).getLong(KEY_NEXT_BEAT, 0L)
    fun intervalMin(ctx: Context): Int = p(ctx).getInt(KEY_INTERVAL, 0)
    fun setNext(ctx: Context, nextWall: Long, intervalMin: Int) =
        p(ctx).edit().putLong(KEY_NEXT_BEAT, nextWall).putInt(KEY_INTERVAL, intervalMin).apply()

    /** 1 = connected, 0 = disconnected, -1 = unknown. */
    fun connState(ctx: Context): Int = p(ctx).getInt(KEY_CONN, -1)
    fun setConnState(ctx: Context, v: Int) = p(ctx).edit().putInt(KEY_CONN, v).apply()

    fun connectedSince(ctx: Context): Long = p(ctx).getLong(KEY_CONNECTED_SINCE, 0L)
    fun lastDisconnect(ctx: Context): Long = p(ctx).getLong(KEY_LAST_DISCONNECT, 0L)

    /**
     * Record confirmed connection-state transitions.
     * 1 = connected, 0 = disconnected, -1 = unknown.
     * Unknown clears the continuous timer but does not claim a confirmed disconnect.
     */
    @Synchronized
    fun recordConnectionState(ctx: Context, state: Int, now: Long = System.currentTimeMillis()) {
        val prefs = p(ctx)
        val previous = prefs.getInt(KEY_CONN, -1)
        val edit = prefs.edit().putInt(KEY_CONN, state)
        when (state) {
            1 -> {
                if (previous != 1 || prefs.getLong(KEY_CONNECTED_SINCE, 0L) == 0L) {
                    edit.putLong(KEY_CONNECTED_SINCE, now)
                }
            }
            0 -> {
                edit.putLong(KEY_CONNECTED_SINCE, 0L)
                if (previous != 0) edit.putLong(KEY_LAST_DISCONNECT, now)
            }
            else -> edit.putLong(KEY_CONNECTED_SINCE, 0L)
        }
        edit.apply()
    }

    /** Raw status code reported by GMS (null if it could not be read), and the reason if so. */
    fun statusCode(ctx: Context): String? = p(ctx).getString(KEY_STATUS_CODE, null)
    fun statusError(ctx: Context): String? = p(ctx).getString(KEY_STATUS_ERROR, null)
    fun setStatus(ctx: Context, code: String?, error: String?) =
        p(ctx).edit().putString(KEY_STATUS_CODE, code).putString(KEY_STATUS_ERROR, error).apply()

    fun lastReconnect(ctx: Context): Long = p(ctx).getLong(KEY_LAST_RECONNECT, 0L)
    fun setLastReconnect(ctx: Context, v: Long) = p(ctx).edit().putLong(KEY_LAST_RECONNECT, v).apply()

    /** Whether to keep the experimental bound connection to GMS (default on). */
    fun bindEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_BIND, true)
    fun setBindEnabled(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(KEY_BIND, v).apply()
}
