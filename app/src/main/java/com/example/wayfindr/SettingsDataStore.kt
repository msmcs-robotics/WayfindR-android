package com.example.wayfindr

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore by preferencesDataStore(name = "WayfindR_Settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val LLM_URL_KEY = stringPreferencesKey("llm_url")
        val KIOSK_PASSWORD_KEY = stringPreferencesKey("kiosk_password")

        fun hashPassword(password: String): String {
            val bytes = password.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }

    // Use context.getString to get the default values from strings.xml
    private val defaultUrl: String get() = context.getString(R.string.default_llm_url)
    private val defaultPassword: String get() = context.getString(R.string.default_kiosk_password)

    suspend fun setLlmUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[LLM_URL_KEY] = url
        }
    }

    suspend fun getLlmUrl(): String {
        return context.dataStore.data
            .map { it[LLM_URL_KEY] ?: defaultUrl }
            .first()
    }

    suspend fun setKioskPasswordHash(passwordHash: String) {
        context.dataStore.edit { prefs ->
            prefs[KIOSK_PASSWORD_KEY] = passwordHash
        }
    }

    suspend fun getKioskPasswordHash(): String {
        return context.dataStore.data
            .map { it[KIOSK_PASSWORD_KEY] ?: hashPassword(defaultPassword) }
            .first()
    }

    suspend fun clearLlmUrl() {
        context.dataStore.edit { prefs ->
            prefs.remove(LLM_URL_KEY)
        }
    }

    // Fixed: Remove duplicate clearLlmUrl function and add clearKioskPassword
    suspend fun clearKioskPassword() {
        context.dataStore.edit { prefs ->
            prefs.remove(KIOSK_PASSWORD_KEY)
        }
    }
}