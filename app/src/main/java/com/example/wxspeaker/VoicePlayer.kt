package com.example.wxspeaker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务：用来在微信里找到“最新的一条语音”并点开播放。
 *
 * ⚠️ 微信更新后如果界面变了，可能失效 —— 需要调整的关键词都在 VOICE_KEYS 里。
 */
class VoicePlayer : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 本服务只在被主动调用时工作，不需要处理事件
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** 找到当前界面上最后（最新）的一条语音气泡并点击 */
    fun playLatestVoice() {
        try {
            val root = rootInActiveWindow
            if (root == null) {
                LogStore.add(this, "语音播放：拿不到当前界面")
                return
            }
            val list = ArrayList<AccessibilityNodeInfo>()
            collect(root, list)
            LogStore.add(this, "语音播放：找到候选气泡 " + list.size + " 个")
            if (list.isEmpty()) return

            val node = list[list.size - 1] // 最后一个 = 最新的
            clickNode(node)
        } catch (e: Exception) {
            LogStore.add(this, "语音播放异常：" + e)
        }
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val d = node.contentDescription?.toString() ?: ""
        for (k in VOICE_KEYS) {
            if (d.contains(k)) {
                out.add(node)
                break
            }
        }
        for (i in 0 until node.childCount) {
            collect(node.getChild(i), out)
        }
    }

    /** 节点本身不可点时，往上找可点的父节点 */
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

        // 语音气泡的无障碍描述关键词（微信更新后可能要改这里）
        private val VOICE_KEYS = arrayOf("语音", "Voice")
    }
}
