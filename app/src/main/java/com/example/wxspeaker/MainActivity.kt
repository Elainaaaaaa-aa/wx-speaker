package com.example.wxspeaker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        Speaker.init(this)

        findViewById<Button>(R.id.btnPerm).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
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
        }

        // 打开 App 即自动开启播报
        Prefs.setEnabled(this, true)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshLog()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 把 App 从后台划掉 → 停止播报
        Prefs.setEnabled(this, false)
        Speaker.stop()
    }

    private fun updateStatus() {
        val granted = isNotificationAccessGranted()
        val enabled = Prefs.isEnabled(this)
        statusText.text = buildString {
            append("通知使用权：")
            append(if (granted) "已授权 ✅" else "未授权 ❌ （请点下面第①个按钮）")
            append("\n播报开关：")
            append(if (enabled) "已开启 ✅" else "已关闭")
        }
    }

    private fun refreshLog() {
        val list = LogStore.getAll()
        logText.text = if (list.isEmpty()) "（还没收到任何通知）" else list.joinToString("\n\n")
    }

    private fun isNotificationAccessGranted(): Boolean {
        val cn = ComponentName(this, NotifyListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
