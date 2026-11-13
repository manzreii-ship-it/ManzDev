package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.manznimeDataStore: DataStore<Preferences> by preferencesDataStore(name = "manznime_prefs")

data class PlayerPreferences(
    val defaultSpeed: Float = 1.0f,
    val autoPlayNextEpisode: Boolean = true,
    val isGuestMode: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next_episode")
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    }

    val preferencesFlow: Flow<PlayerPreferences> = context.manznimeDataStore.data.map { prefs ->
        PlayerPreferences(
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            isGuestMode = prefs[Keys.IS_GUEST_MODE] ?: false
        )
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.manznimeDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_SPEED] = speed
        }
    }

    suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
        context.manznimeDataStore.edit { prefs ->
            prefs[Keys.AUTO_PLAY_NEXT] = enabled
        }
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.manznimeDataStore.edit { prefs ->
            prefs[Keys.IS_GUEST_MODE] = enabled
        }
    }
}
