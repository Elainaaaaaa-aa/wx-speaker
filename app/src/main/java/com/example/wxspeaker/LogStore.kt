package com.example.wxspeaker

/** 记录最近收到的通知，方便在 App 界面里排查问题 */
object LogStore {
    private val items = ArrayList<String>()

    @Synchronized
    fun add(s: String) {
        items.add(0, s)
        while (items.size > 30) items.removeAt(items.size - 1)
    }

    @Synchronized
    fun getAll(): List<String> = ArrayList(items)

    @Synchronized
    fun clear() {
        items.clear()
    }
}
