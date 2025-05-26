package com.example.wayfindr

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Define this ONCE at the top level, outside any class
val Context.dataStore by preferencesDataStore(name = "WayfindR_Settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val LLM_URL_KEY = stringPreferencesKey("llm_url")
        const val DEFAULT_URL = "http://192.168.0.100:5000"
    }

    suspend fun setLlmUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[LLM_URL_KEY] = url
        }
    }

    suspend fun getLlmUrl(): String {
        return context.dataStore.data
            .map { it[LLM_URL_KEY] ?: DEFAULT_URL }
            .first()
    }
}