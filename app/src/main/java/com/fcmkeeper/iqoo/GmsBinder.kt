package com.fcmkeeper.iqoo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Keeps a bound connection from this app (which runs a foreground service) to a
 * Google Play Services service. On Android, a process that another important
 * process is bound to is ranked higher, which can make it less likely to be
 * killed or frozen as a "cached" background app.
 *
 * EXPERIMENTAL: the service action names are internal to GMS and may differ
 * between GMS versions, and OEM freezers (like OriginOS's) may ignore this.
 * If every candidate fails the app simply carries on without it.
 */
object GmsBinder {
    private const val GMS = "com.google.android.gms"
    private const val SERVICE = "com.google.android.gms.chimera.PersistentApiService"
    private const val BIND_ACTION = "com.google.android.chimera.BoundService.START"
    private val TARGETS = listOf(
        "com.google.android.gms.common.telemetry.service.START",
        "com.google.android.gms.appusage.service.START",
        "com.google.android.gms.pseudonymous.service.START"
    )
    private const val CONNECT_TIMEOUT_MS = 3000L

    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var active = false
    private var current: ServiceConnection? = null

    /** "off", "connecting", "bound" or "failed". */
    @Volatile
    var state: String = "off"
        private set

    fun start(ctx: Context) {
        val app = ctx.applicationContext
        if (!GmsHelper.isInstalled(app)) {
            state = "failed"
            return
        }
        synchronized(lock) {
            if (active) return
            active = true
            state = "connecting"
        }
        main.post { tryCandidate(app, 0) }
    }

    fun stop(ctx: Context) {
        val app = ctx.applicationContext
        val conn: ServiceConnection?
        synchronized(lock) {
            active = false
            conn = current
            current = null
            state = "off"
        }
        if (conn != null) safeUnbind(app, conn)
    }

    private fun tryCandidate(app: Context, index: Int) {
        synchronized(lock) {
            if (!active) return
            if (index >= TARGETS.size) {
                active = false
                current = null
                state = "failed"
                return
            }
        }

        val target = TARGETS[index]
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                synchronized(lock) { if (current === this) state = "bound" }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Android reconnects automatically because of BIND_AUTO_CREATE.
                synchronized(lock) { if (current === this) state = "connecting" }
            }

            override fun onBindingDied(name: ComponentName?) {
                safeUnbind(app, this)
                val wasCurrent = synchronized(lock) {
                    if (current === this) {
                        current = null
                        active = false
                        state = "failed"
                        true
                    } else false
                }
                if (wasCurrent) {
                    main.postDelayed({
                        if (Prefs.isEnabled(app) && Prefs.bindEnabled(app)) start(app)
                    }, 1500L)
                }
            }

            override fun onNullBinding(name: ComponentName?) {
                safeUnbind(app, this)
                synchronized(lock) { if (current === this) current = null }
                main.postDelayed({ tryCandidate(app, index + 1) }, 250L)
            }
        }

        synchronized(lock) { current = conn }

        val intent = Intent(BIND_ACTION)
            .setComponent(ComponentName(GMS, SERVICE))
            .setData(Uri.parse("chimera-action:$target"))

        val accepted = try {
            app.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        } catch (_: Throwable) {
            false
        }

        if (!accepted) {
            safeUnbind(app, conn)
            synchronized(lock) { if (current === conn) current = null }
            main.postDelayed({ tryCandidate(app, index + 1) }, 250L)
            return
        }

        // If GMS accepted the request but never connects, try the next candidate.
        main.postDelayed({
            val waiting = synchronized(lock) { current === conn && state == "connecting" }
            if (waiting) {
                safeUnbind(app, conn)
                synchronized(lock) { if (current === conn) current = null }
                tryCandidate(app, index + 1)
            }
        }, CONNECT_TIMEOUT_MS)
    }

    private fun safeUnbind(ctx: Context, conn: ServiceConnection) {
        try {
            ctx.unbindService(conn)
        } catch (_: Throwable) {
        }
    }
}
