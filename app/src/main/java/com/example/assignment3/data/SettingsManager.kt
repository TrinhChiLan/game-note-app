package com.example.assignment3.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val _isTallLayout = MutableStateFlow(prefs.getBoolean("is_tall_layout", false))
    val isTallLayout: StateFlow<Boolean> = _isTallLayout

    fun setTallLayout(enabled: Boolean) {
        prefs.edit().putBoolean("is_tall_layout", enabled).apply()
        _isTallLayout.value = enabled
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
