package com.getprediq.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prediqDataStore by preferencesDataStore(name = "prediq_session")

class SessionStore(private val context: Context) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    val accessToken: Flow<String?> = context.prediqDataStore.data.map { it[accessKey] }
    val refreshToken: Flow<String?> = context.prediqDataStore.data.map { it[refreshKey] }

    suspend fun save(accessToken: String, refreshToken: String?) {
        context.prediqDataStore.edit { prefs ->
            prefs[accessKey] = accessToken
            if (refreshToken.isNullOrBlank()) prefs.remove(refreshKey) else prefs[refreshKey] = refreshToken
        }
    }

    suspend fun clear() {
        context.prediqDataStore.edit { it.clear() }
    }
}
