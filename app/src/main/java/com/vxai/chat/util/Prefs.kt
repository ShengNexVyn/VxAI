package com.vxai.chat.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("vxai_chat", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString(KEY_BASE_URL, "http://10.0.2.2:20128/v1/") ?: "http://10.0.2.2:20128/v1/"
        set(v) = sp.edit().putString(KEY_BASE_URL, v.trim()).apply()

    var apiKey: String
        get() = sp.getString(KEY_API_KEY, "") ?: ""
        set(v) = sp.edit().putString(KEY_API_KEY, v.trim()).apply()

    var model: String
        get() = sp.getString(KEY_MODEL, "") ?: ""
        set(v) = sp.edit().putString(KEY_MODEL, v).apply()

    var theme: String
        get() = sp.getString(KEY_THEME, "system") ?: "system"
        set(v) = sp.edit().putString(KEY_THEME, v).apply()

    var stream: Boolean
        get() = sp.getBoolean(KEY_STREAM, true)
        set(v) = sp.edit().putBoolean(KEY_STREAM, v).apply()

    var history: String
        get() = sp.getString(KEY_HISTORY, "") ?: ""
        set(v) = sp.edit().putString(KEY_HISTORY, v).apply()

    fun applyTheme() {
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_THEME = "theme"
        private const val KEY_STREAM = "stream"
        private const val KEY_HISTORY = "history"
    }
}
