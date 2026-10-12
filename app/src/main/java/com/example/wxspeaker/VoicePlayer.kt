package com.example.wxspeaker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务：尝试在微信里找到“最新的一条语音”并点开播放。
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
            for (attempt in 1..6) {
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
                        // 最后一次截图整个界面结构，方便定位
                        if (attempt == 6) {
                            val out = ArrayList<String>()
                            val counter = intArrayOf(0)
                            walk(root, out, counter)
                            LogStore.add(this, "【界面结构·末次】总节点=" + counter[0] + "，文本节点=" + out.size)
                            LogStore.add(this, "【结构明细】" + out.joinToString(" ● "))
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

    /** 遍历并统计/记录有文字或描述的节点： 类名|文字|描述 */
    private fun walk(node: AccessibilityNodeInfo?, out: MutableList<String>, counter: IntArray) {
        if (node == null || counter[0] > 600) return
        counter[0]++
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
        val t = node.text?.toString() ?: ""
        val d = node.contentDescription?.toString() ?: ""
        if (t.isNotEmpty() || d.isNotEmpty()) {
            if (out.size < 80) out.add(cls + "|" + t + "|" + d)
        }
        for (i in 0 until node.childCount) {
            walk(node.getChild(i), out, counter)
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
        private val VOICE_KEYS = arrayOf("语音", "Voice")
    }
}
