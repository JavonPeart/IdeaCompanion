package com.javonpeart.ideacompanion.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private val phraseKey = stringPreferencesKey("activation_phrase")
    private val speakKey = booleanPreferencesKey("speech_enabled")
    val settings = context.dataStore.data.map { Settings(it[phraseKey] ?: "Hello Companion", it[speakKey] ?: true) }
    suspend fun setPhrase(value: String) { context.dataStore.edit { it[phraseKey] = value.trim() } }
    suspend fun setSpeechEnabled(value: Boolean) { context.dataStore.edit { it[speakKey] = value } }
}

data class Settings(val activationPhrase: String, val speechEnabled: Boolean)
