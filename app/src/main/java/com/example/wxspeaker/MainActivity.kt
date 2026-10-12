package com.example.wxspeaker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var ttsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        ttsText = findViewById(R.id.ttsText)
        Speaker.init(this)

        findViewById<Button>(R.id.btnPerm).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnAcc).setOnClickListener {
            Toast.makeText(this, "请在列表里找到「微信播报」打开", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            try {
                val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(i)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            Prefs.setEnabled(this, true)
            Toast.makeText(this, "播报已开启", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            Prefs.setEnabled(this, false)
            Speaker.stop()
            Toast.makeText(this, "播报已关闭", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            Speaker.init(this)
            Speaker.speak("测试，微信播报已开启")
            Toast.makeText(this, "已发出测试语音，听到了吗？", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        Prefs.setEnabled(this, true)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshLog()
    }

    override fun onDestroy() {
        super.onDestroy()
        Prefs.setEnabled(this, false)
        Speaker.stop()
    }

    private fun updateStatus() {
        statusText.text = buildString {
            append("通知使用权：")
            append(if (isNotificationAccessGranted()) "已授权 ✅" else "未授权 ❌（点①）")
            append("\n无障碍(自动播语音)：")
            append(if (isAccessibilityEnabled()) "已开启 ✅" else "未开启 ❌（点②）")
            append("\n悬浮窗：")
            append(if (Settings.canDrawOverlays(this@MainActivity)) "已允许 ✅" else "未允许 ❌（点③）")
            append("\n播报开关：")
            append(if (Prefs.isEnabled(this@MainActivity)) "已开启 ✅" else "已关闭")
        }
        ttsText.text = Speaker.statusText()
    }

    private fun refreshLog() {
        val s = LogStore.getAll(this)
        logText.text = if (s.isEmpty()) "（还没收到任何通知）" else s
    }

    private fun isNotificationAccessGranted(): Boolean {
        val cn = ComponentName(this, NotifyListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun isAccessibilityEnabled(): Boolean {
        val cn = ComponentName(this, VoicePlayer::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_accessibility_services")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
