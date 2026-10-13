package com.fcmkeeper.iqoo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var connectionTime: TextView
    private lateinit var disconnectTime: TextView
    private lateinit var status: TextView
    private lateinit var serviceButton: Button
    private lateinit var bindButton: Button
    private val uiHandler = Handler(Looper.getMainLooper())
    private val refreshTicker = object : Runnable {
        override fun run() {
            refresh()
            uiHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        TextView(this).apply {
            text = "IQOO fix"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            root.addView(this)
        }
        connectionTime = TextView(this).apply {
            text = "Kết nối liên tục: --:--:--"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, pad / 2, 0, 0)
            root.addView(this)
        }
        disconnectTime = TextView(this).apply {
            text = "Disconnect gần nhất: chưa ghi nhận"
            textSize = 13f
            setPadding(0, 0, 0, pad)
            root.addView(this)
        }
        status = TextView(this).apply {
            textSize = 14f
            root.addView(this)
        }

        fun button(text: String, action: () -> Unit): Button {
            val b = Button(this).apply {
                this.text = text
                setOnClickListener {
                    action()
                    refresh()
                }
            }
            root.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return b
        }

        button("1. Cấp quyền thông báo") { requestNotificationPermission() }
        button("2. Bỏ tối ưu pin cho app này") { requestIgnoreBattery() }
        button("3. Mở cài đặt chạy nền / tự khởi động (OriginOS 5)") { openRomSettings() }
        button("4. Cài đặt pin / nền Google Play Services (GMS)") { openAppInfo(GmsHelper.GMS) }
        button("5. Cài đặt pin / nền Google Services Framework (GSF)") { openAppInfo(GmsHelper.GSF) }
        serviceButton = button("Dịch vụ giữ kết nối: OFF") { toggleService() }
        button("7. Kiểm tra kết nối ngay") {
            val app = applicationContext
            Thread { Tick.run(app) }.start()
            Toast.makeText(this, "Đang kiểm tra… trạng thái sẽ tự cập nhật", Toast.LENGTH_SHORT).show()
        }
        button("8. Làm mới trạng thái") { }
        bindButton = button("Liên kết với GMS (thử nghiệm): ON") {
            val on = !Prefs.bindEnabled(this)
            Prefs.setBindEnabled(this, on)
            if (on) {
                if (Prefs.isEnabled(this)) GmsBinder.start(this)
            } else {
                GmsBinder.stop(this)
            }
        }

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
        refresh()
    }

    override fun onResume() {
        super.onResume()
        uiHandler.removeCallbacks(refreshTicker)
        uiHandler.post(refreshTicker)
    }

    override fun onPause() {
        uiHandler.removeCallbacks(refreshTicker)
        super.onPause()
    }

    private fun refresh() {
        val gms = GmsHelper.isInstalled(this)
        val gmsSystem = GmsHelper.isSystemApp(this)
        val notif = NotificationManagerCompat.from(this).areNotificationsEnabled()
        val pm = getSystemService(PowerManager::class.java)
        val battery = pm.isIgnoringBatteryOptimizations(packageName)
        val enabled = Prefs.isEnabled(this)
        val last = Prefs.lastBeat(this)
        val lastText = if (last == 0L) "chưa có" else DateFormat.format("HH:mm:ss dd/MM", last).toString()
        val next = Prefs.nextBeat(this)
        val nextText = if (!enabled || next == 0L) "—" else DateFormat.format("HH:mm:ss dd/MM", next).toString()
        val every = Prefs.intervalMin(this)
        val net = Adaptive.hasNetwork(this)
        val conn = Prefs.connState(this)
        val code = Prefs.statusCode(this)
        val err = Prefs.statusError(this)
        val lastRe = Prefs.lastReconnect(this)
        val reText = if (lastRe == 0L) "chưa có" else DateFormat.format("HH:mm:ss dd/MM/yyyy", lastRe).toString()
        val lastDisconnect = Prefs.lastDisconnect(this)
        val disconnectText = if (lastDisconnect == 0L) "chưa ghi nhận" else DateFormat.format("dd/MM/yyyy HH:mm", lastDisconnect).toString()
        val connectedSince = Prefs.connectedSince(this)
        val connectedDuration = if (conn == 1 && connectedSince > 0L) formatDuration(System.currentTimeMillis() - connectedSince) else "--:--:--"

        connectionTime.text = "Kết nối liên tục: $connectedDuration"
        disconnectTime.text = "Disconnect gần nhất: $disconnectText"
        serviceButton.text = "Dịch vụ giữ kết nối: " + if (enabled) "ON" else "OFF"
        bindButton.text = "Liên kết với GMS (thử nghiệm): " + if (Prefs.bindEnabled(this)) "ON" else "OFF"

        fun mark(ok: Boolean) = if (ok) "✅" else "❌"
        status.text = buildString {
            appendLine("${mark(gms)} Google Play Services: " + if (gms) "đã cài" else "chưa cài")
            if (gms) appendLine("    Loại: " + if (gmsSystem) "app hệ thống" else "app cài thêm (người dùng)")
            appendLine("${mark(notif)} Thông báo của app này")
            appendLine("${mark(battery)} Bỏ tối ưu pin (app này)")
            appendLine("${mark(enabled)} Dịch vụ giữ kết nối: " + if (enabled) "ON" else "OFF")
            val bs = GmsBinder.state
            appendLine((if (bs == "bound") "✅" else if (bs == "failed") "❌" else "❔") + " Liên kết với GMS: " + when (bs) {
                "bound" -> "đã liên kết"
                "connecting" -> "đang kết nối"
                "failed" -> "không liên kết được"
                else -> if (Prefs.bindEnabled(this@MainActivity)) "ON (đang chờ)" else "OFF"
            })
            appendLine("${mark(net)} Mạng: " + if (net) "có" else "không")
            appendLine(when (conn) { 1 -> "✅"; 0 -> "❌"; else -> "❔" } + " Trạng thái FCM trong GMS: " + when (conn) {
                1 -> "đang kết nối (mã ${code ?: "?"})"
                0 -> "mất kết nối" + (code?.let { " (mã $it)" } ?: (err?.let { " ($it)" } ?: ""))
                else -> "chưa đọc được" + (err?.let { " ($it)" } ?: "")
            })
            appendLine("Bắt đầu phiên CONNECTED: " + if (conn == 1 && connectedSince > 0L) DateFormat.format("HH:mm:ss dd/MM/yyyy", connectedSince) else "—")
            appendLine("Reconnect gần nhất: $reText")
            appendLine("Heartbeat gần nhất: $lastText")
            appendLine("Heartbeat tiếp theo: $nextText" + if (every > 0) " (mỗi ~$every phút)" else "")
            appendLine()
            appendLine("OriginOS 5: vào Thông tin ứng dụng của GMS/GSF để kiểm tra Pin, mức sử dụng nền và tự khởi động. App này không thể tự cấp whitelist hệ thống cho GMS/GSF.")
            appendLine("FCM High priority phải được đặt ở phía gửi; APK không thể ép mức ưu tiên cho mọi ứng dụng.")
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        else Toast.makeText(this, "Android này không cần cấp riêng", Toast.LENGTH_SHORT).show()
    }

    private fun requestIgnoreBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Đã bỏ tối ưu pin rồi", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openRomSettings() {
        if (!RomSettings.openBackgroundSettings(this)) {
            RomSettings.openAppDetails(this, packageName)
            Toast.makeText(this, "Không mở thẳng được. Vào Pin → chạy nền tiêu thụ cao và Quyền → Tự khởi động.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppInfo(pkg: String) {
        if (!GmsHelper.isInstalled(this, pkg)) {
            Toast.makeText(this, "Chưa cài $pkg", Toast.LENGTH_SHORT).show()
            return
        }
        RomSettings.openAppDetails(this, pkg)
    }

    private fun toggleService() {
        if (Prefs.isEnabled(this)) {
            Prefs.setEnabled(this, false)
            HeartbeatReceiver.cancel(this)
            stopService(Intent(this, KeepAliveService::class.java))
        } else {
            Prefs.setEnabled(this, true)
            try {
                ContextCompat.startForegroundService(this, Intent(this, KeepAliveService::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Không khởi động được: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
