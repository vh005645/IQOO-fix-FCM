package com.fcmkeeper.iqoo

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Checks whether this phone can open a TCP connection to Google's FCM server.
 * This is NOT the internal connection state of Google Play Services (Android has
 * no public API for that); it only tells whether the network path to FCM works.
 */
object FcmProbe {
    private val targets = listOf("mtalk.google.com" to 5228, "mtalk.google.com" to 443)
    private const val CONNECT_TIMEOUT_MS = 3000
    private const val TOTAL_TIMEOUT_S = 4L

    fun reachable(): Boolean {
        val pool = Executors.newFixedThreadPool(targets.size)
        return try {
            val tasks = targets.map { (host, port) ->
                Callable {
                    Socket().use { it.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS) }
                    true
                }
            }
            // Returns as soon as one target connects; throws if none do within the deadline.
            pool.invokeAny(tasks, TOTAL_TIMEOUT_S, TimeUnit.SECONDS)
        } catch (_: Exception) {
            false
        } finally {
            pool.shutdownNow()
        }
    }
}
