package com.example.wxspeaker

import android.content.Context

/** 记录最近收到的通知（存本地，进程被杀也不丢），方便在 App 界面里排查 */
object LogStore {
    private const val PREF = "wxspeaker_log"
    private const val KEY = "lines"
    private const val SEP = "\n@@\n"
    private const val MAX = 30

    @Synchronized
    fun add(ctx: Context, s: String) {
        try {
            val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val old = sp.getString(KEY, "") ?: ""
            val items = if (old.isEmpty()) ArrayList() else ArrayList(old.split(SEP))
            items.add(0, s)
            while (items.size > MAX) items.removeAt(items.size - 1)
            sp.edit().putString(KEY, items.joinToString(SEP)).apply()
        } catch (e: Exception) {
        }
    }

    @Synchronized
    fun getAll(ctx: Context): String {
        return try {
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
