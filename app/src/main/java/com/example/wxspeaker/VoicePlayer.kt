package com.example.wxspeaker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务：在微信里找到“最新的一条语音”并点开播放。
 */
class VoicePlayer : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun playLatestVoice() {
        Thread {
            for (attempt in 1..5) {
                try {
                    val root = rootInActiveWindow
                    if (root == null) {
                        LogStore.add(this, "语音播放[$attempt]：拿不到界面")
                    } else {
                        val pkg = root.packageName?.toString() ?: "?"
                        val list = ArrayList<AccessibilityNodeInfo>()
                        collect(root, list)
                        LogStore.add(this, "语音播放[$attempt]：界面=" + pkg + "，候选气泡=" + list.size)
                        if (list.isNotEmpty()) {
                            clickNode(list[list.size - 1])
                            return@Thread
                        }
                        if (attempt == 1) {
                            val dump = ArrayList<String>()
                            dumpNodes(root, dump)
                            LogStore.add(this, "【界面结构】" + dump.joinToString("  ●  "))
                        }
                    }
                } catch (e: Exception) {
                    LogStore.add(this, "语音播放异常：" + e)
                }
                try { Thread.sleep(1200) } catch (e: Exception) {}
            }
            LogStore.add(this, "语音播放：多次尝试仍未找到语音气泡")
        }.start()
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
        if (node == null || out.size > 300) return
        val d = node.contentDescription?.toString() ?: ""
        val t = node.text?.toString() ?: ""
        for (k in VOICE_KEYS) {
            if (d.contains(k) || t.contains(k)) {
                out.add(node)
                break
            }
        }
        for (i in 0 until node.childCount) {
            collect(node.getChild(i), out)
        }
    }

    /** 把有文字/描述的节点全打出来： 类名|文字|描述 */
    private fun dumpNodes(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null || out.size >= 60) return
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
        val t = node.text?.toString() ?: ""
        val d = node.contentDescription?.toString() ?: ""
        if (t.isNotEmpty() || d.isNotEmpty()) {
            out.add(cls + "|" + t + "|" + d)
        }
        for (i in 0 until node.childCount) {
            dumpNodes(node.getChild(i), out)
        }
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        var n: AccessibilityNodeInfo? = node
        var depth = 0
        while (n != null && !n.isClickable && depth < 6) {
            n = n.parent
            depth++
        }
        val target = n ?: node
        val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        LogStore.add(this, "语音播放：点击结果=" + ok)
    }

    companion object {
        @Volatile var instance: VoicePlayer? = null

        // 语音气泡的关键词（微信更新后可能要改这里）
        private val VOICE_KEYS = arrayOf("语音", "Voice")
    }
}
