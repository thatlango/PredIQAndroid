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
    private val handoffStateKey = stringPreferencesKey("tuku_handoff_state")
    private val handoffVerifierKey = stringPreferencesKey("tuku_handoff_verifier")

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

    suspend fun saveHandoff(state: String, verifier: String) { context.prediqDataStore.edit { it[handoffStateKey] = state; it[handoffVerifierKey] = verifier } }
    suspend fun consumeHandoff(): Pair<String?, String?> {
        var state: String? = null; var verifier: String? = null
        context.prediqDataStore.edit { prefs -> state = prefs[handoffStateKey]; verifier = prefs[handoffVerifierKey]; prefs.remove(handoffStateKey); prefs.remove(handoffVerifierKey) }
        return state to verifier
    }
}
