package com.fcmkeeper.iqoo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Messenger
import android.os.SystemClock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Asks Google Play Services for its FCM connection status.
 *
 * This is an UNDOCUMENTED request (a "local/status" message sent to GMS), so it
 * may change or stop working with GMS updates. Only status code "3" is known to
 * mean "disconnected"; any other code is treated as "not reported disconnected".
 *
 * Blocks up to ~3 seconds: call it from a background thread, never the main thread.
 */
object GcmStatus {
    class Result(val code: String?, val error: String?)

    const val DISCONNECTED = "3"

    private const val ACTION_SEND = "com.google.android.gcm.intent.SEND"
    private const val GTALK_PERMISSION = "com.google.android.gtalkservice.permission.GTALK_SERVICE"
    private const val LOCAL_STATUS = "https://gcm.googleapis.com/local/status"
    private const val TIMEOUT_MS = 3000L
    private val seq = AtomicInteger(1)

    fun query(ctx: Context): Result {
        val app = ctx.applicationContext
        if (!GmsHelper.isInstalled(app)) return Result(null, "GMS_MISSING")

        val latch = CountDownLatch(1)
        val ref = AtomicReference<Result?>(null)
        val messageId = "fcmkeeper-status-${seq.getAndIncrement()}-${SystemClock.elapsedRealtime()}"

        val handler = Handler(Looper.getMainLooper(), Handler.Callback { msg ->
            val reply = msg.obj as? Intent
            if (reply != null) {
                val replyTo = reply.getStringExtra("In-Reply-To")
                if (replyTo == null || replyTo == messageId) {
                    val code = reply.getStringExtra("status")
                    ref.compareAndSet(null, Result(code, if (code == null) "EMPTY" else null))
                    latch.countDown()
                }
            }
            true
        })

        // Identity placeholder required by the request; replies come through the Messenger.
        val identity = PendingIntent.getBroadcast(
            app, 0,
            Intent().setPackage("com.google.example.invalidpackage"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val intent = Intent(ACTION_SEND).setPackage(GmsHelper.GMS).apply {
            putExtra("app", identity)
            putExtra("google.to", LOCAL_STATUS)
            putExtra("google.message_id", messageId)
            putExtra("google.ttl", "0")
            putExtra("google.messenger", Messenger(handler))
        }

        try {
            app.sendOrderedBroadcast(intent, GTALK_PERMISSION)
        } catch (t: Throwable) {
            return Result(null, t.javaClass.simpleName)
        }

        val got = try {
            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            false
        }
        return if (got) ref.get() ?: Result(null, "EMPTY") else Result(null, "NO_RESPONSE")
    }
}
