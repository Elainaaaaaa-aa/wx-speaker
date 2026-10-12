package com.example.wxspeaker

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 监听系统通知：只处理微信，把消息朗读出来；语音消息尝试自动打开微信播放。
 */
class NotifyListener : NotificationListenerService() {

    private val lastSpoken = HashMap<String, Pair<String, Long>>()

    override fun onCreate() {
        super.onCreate()
        Speaker.init(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try {
            val ex = sbn.notification.extras
            val t = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val x = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            LogStore.add(this, sbn.packageName + " ｜ " + t + " ｜ " + x)
            handle(sbn)
        } catch (e: Exception) {
            LogStore.add(this, "异常：" + e)
        }
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName != WECHAT_PKG) return
        if (!Prefs.isEnabled(this)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isEmpty()) return

        val core = text.replace(Regex("^\\[\\d+条\\]"), "")

        val now = System.currentTimeMillis()
        val last = lastSpoken[title]
        if (last != null && last.first == core && now - last.second < DEDUP_MS) return
        lastSpoken[title] = Pair(core, now)

        val who = if (title.isNotEmpty()) "$title，" else ""

        // ① 语音消息
        if (core.contains("[语音]") || text.contains("[语音]")) {
            Speaker.speak(who + "发来一条语音消息")
            autoPlayVoice(sbn)
            return
        }

        val m = Regex("^([^:：]{1,24})[:：]([\\s\\S]*)$").find(core)
        val content = m?.groupValues?.get(2) ?: core
        val alreadyNamed = (m != null && m.groupValues[1] == title)

        // ② 媒体消息
        val c = content.replace(Regex("\\s"), "")
        val media = MEDIA[c]
        if (media != null) {
            Speaker.speak(who + media)
            return
        }

        // ③ 普通文字（含纯标点）
        var shown = if (alreadyNamed) content else core
        if (isPunctOnly(shown)) shown = punctToSpeech(shown)
        Speaker.speak(who + shown)
    }

    /** 用通知自带的入口打开微信对应聊天，再让无障碍点开最新语音 */
    private fun autoPlayVoice(sbn: StatusBarNotification) {
        if (VoicePlayer.instance == null) {
            LogStore.add(this, "语音自动播放：无障碍未开启，跳过")
            return
        }
        try {
            sbn.notification.contentIntent?.send()
            LogStore.add(this, "语音自动播放：已请求打开微信")
        } catch (e: Exception) {
            LogStore.add(this, "语音自动播放：打开微信失败 " + e)
        }
        Thread {
            try {
                Thread.sleep(1500) // 等微信打开
                VoicePlayer.instance?.playLatestVoice()
            } catch (e: Exception) {
            }
        }.start()
    }

    companion object {
        private const val WECHAT_PKG = "com.tencent.mm"
        private const val DEDUP_MS = 120000L

        private val MEDIA = mapOf(
            "[图片]" to "收到一张图片",
            "[视频]" to "收到一段视频",
            "[动画表情]" to "收到一个表情",
            "[表情]" to "收到一个表情",
            "[文件]" to "收到一个文件",
            "[链接]" to "收到一条链接",
            "[位置]" to "收到一个位置",
            "[名片]" to "收到一张名片",
            "[微信红包]" to "收到一个红包",
            "[转账]" to "收到一笔转账"
        )

        private val PUNCT_NAMES = mapOf(
            '。' to "句号", '，' to "逗号", '、' to "顿号", '；' to "分号", '：' to "冒号",
            '？' to "问号", '！' to "感叹号", '…' to "省略号", '—' to "破折号", '·' to "间隔号",
            '～' to "波浪号", '~' to "波浪号",
            '“' to "左引号", '”' to "右引号", '‘' to "左单引号", '’' to "右单引号",
            '《' to "左书名号", '》' to "右书名号", '〈' to "左尖括号", '〉' to "右尖括号",
            '（' to "左括号", '）' to "右括号", '【' to "左方括号", '】' to "右方括号",
            '〔' to "左六角括号", '〕' to "右六角括号", '「' to "引号", '」' to "引号",
            '『' to "双引号", '』' to "双引号",
            '!' to "感叹号", '?' to "问号", ',' to "逗号", '.' to "点", ';' to "分号", ':' to "冒号",
            '\'' to "单引号", '"' to "双引号", '`' to "反引号",
            '(' to "左括号", ')' to "右括号", '[' to "左方括号", ']' to "右方括号",
            '{' to "左花括号", '}' to "右花括号", '<' to "左尖括号", '>' to "右尖括号",
            '|' to "竖线", '\\' to "反斜杠", '/' to "斜杠", '-' to "短横线", '_' to "下划线",
            '+' to "加号", '=' to "等号", '*' to "星号", '&' to "和号", '^' to "脱字符",
            '%' to "百分号", '$' to "美元符", '#' to "井号", '@' to "艾特", '￥' to "人民币"
        )

        private fun isPunctOnly(s: String): Boolean {
            if (s.isEmpty()) return false
            var has = false
            for (ch in s) {
                if (ch.isWhitespace()) continue
                if (!PUNCT_NAMES.containsKey(ch)) return false
                has = true
            }
            return has
        }

        private fun punctToSpeech(s: String): String {
            val out = ArrayList<String>()
            val noCount = "…—"
            var i = 0
            while (i < s.length) {
                val ch = s[i]
                if (ch.isWhitespace()) { i++; continue }
                var j = i
                while (j < s.length && s[j] == ch) j++
                val n = j - i
                val nm = PUNCT_NAMES[ch] ?: ch.toString()
                if (n >= 2 && noCount.indexOf(ch) < 0) out.add("${n}个$nm") else out.add(nm)
                i = j
            }
            return out.joinToString("，")
        }
    }
}
