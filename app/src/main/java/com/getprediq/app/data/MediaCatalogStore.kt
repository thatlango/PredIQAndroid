package com.getprediq.app.data

import androidx.compose.runtime.mutableStateMapOf
import com.getprediq.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object MediaCatalogStore {
    private val entities = mutableStateMapOf<String, MediaEntity>()
    private val loadMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    @Volatile private var lastLoadedMillis = 0L

    private fun key(type: String, sport: String, name: String) =
        "${type.lowercase()}|${sport.lowercase()}|${name.trim().lowercase()}"

    fun replace(response: MediaCatalogResponse) {
        entities.clear()
        response.entities.forEach { entities[key(it.entityType, it.sportCode, it.canonicalName)] = it }
        lastLoadedMillis = System.currentTimeMillis()
    }

    suspend fun ensureLoaded(force: Boolean = false) {
        val fresh = System.currentTimeMillis() - lastLoadedMillis < 30 * 60 * 1000L
        if (!force && fresh) return
        loadMutex.withLock {
            val stillFresh = System.currentTimeMillis() - lastLoadedMillis < 30 * 60 * 1000L
            if (!force && stillFresh) return@withLock
            runCatching {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("${BuildConfig.PREDIQ_API_BASE_URL}media/catalog?days=30")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("media catalog ${response.code}")
                        val body = response.body?.string().orEmpty()
                        json.decodeFromString<MediaCatalogResponse>(body)
                    }
                }
            }.onSuccess(::replace)
        }
    }

    fun team(name: String, sport: String = "football") = entities[key("team", sport, name)]
    fun competition(name: String, sport: String = "football") = entities[key("competition", sport, name)]
    fun player(name: String, sport: String = "football") = entities[key("player", sport, name)]
    fun participant(name: String, sport: String = "football") =
        if (sport in setOf("tennis", "boxing", "mma")) player(name, sport) else team(name, sport)
}
