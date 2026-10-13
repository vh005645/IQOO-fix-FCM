package com.fcmkeeper.iqoo

import android.content.Context

/**
 * One check cycle (every few minutes):
 *  - Read the FCM status from GMS.
 *  - Connected: send only a heartbeat, no reconnect.
 *  - Disconnected: send a heartbeat, wait briefly, check again, and send
 *    reconnect only if GMS still reports disconnected.
 *  - Unknown (GMS did not answer): fall back to a TCP check of the FCM server.
 */
object Tick {
    private const val RECHECK_MS = 2000L
    private val lock = Any()

    fun run(ctx: Context) {
        synchronized(lock) {
            val app = ctx.applicationContext
            if (Prefs.isEnabled(app) && Prefs.bindEnabled(app)) GmsBinder.start(app)

            if (!Adaptive.hasNetwork(app)) {
                // Nothing useful can be sent without a network; the network callback re-checks later.
                Prefs.recordConnectionState(app, 0)
                Prefs.setStatus(app, null, "NO_NETWORK")
                Prefs.setLastBeat(app, System.currentTimeMillis())
                return
            }

            var status = GcmStatus.query(app)

            when {
                status.code == GcmStatus.DISCONNECTED -> {
                    GmsHelper.heartbeat(app)
                    try {
                        Thread.sleep(RECHECK_MS)
                    } catch (_: InterruptedException) {
                    }
                    status = GcmStatus.query(app)
                    if (status.code == GcmStatus.DISCONNECTED) GmsHelper.reconnect(app)
                }
                status.code != null -> {
                    // Connected (or any status other than "disconnected"): heartbeat only.
                    GmsHelper.heartbeat(app)
                }
                else -> {
                    // GMS did not report a status: use the network path as a weaker signal.
                    if (FcmProbe.reachable()) GmsHelper.heartbeat(app) else GmsHelper.reconnect(app)
                }
            }

            val state = when {
                status.code == null -> -1
                status.code == GcmStatus.DISCONNECTED -> 0
                else -> 1
            }
            Prefs.recordConnectionState(app, state)
            Prefs.setStatus(app, status.code, status.error)
            Prefs.setLastBeat(app, System.currentTimeMillis())
        }
    }
}
