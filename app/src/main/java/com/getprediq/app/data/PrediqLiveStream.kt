package com.getprediq.app.data

import com.getprediq.app.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit


data class PrediqLiveEvent(
    val type: String,
    val eventId: String? = null,
    val sport: String? = null,
    val homeParticipant: String? = null,
    val awayParticipant: String? = null,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val scoringSide: String? = null,
    val statusText: String? = null,
    val observedAt: String? = null,
)

class PrediqLiveStream(
    private val session: SessionStore,
    private val onEvent: (PrediqLiveEvent) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private var socket: WebSocket? = null

    suspend fun connect() {
        if (socket != null) return
        val token = session.accessToken.first()?.takeIf { it.isNotBlank() } ?: return
        val base = BuildConfig.PREDIQ_API_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .replace("/api/v1/", "/api/v2/")
        val request = Request.Builder()
            .url(base + "live/stream")
            .header("Authorization", "Bearer $token")
            .build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnectionChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parse(text)?.let(onEvent)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                onConnectionChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                onConnectionChanged(false)
            }
        })
    }

    fun close() {
        socket?.close(1000, "PredIQ screen closed")
        socket = null
        onConnectionChanged(false)
    }

    private fun parse(text: String): PrediqLiveEvent? = runCatching {
        val obj = json.parseToJsonElement(text) as? JsonObject ?: return@runCatching null
        fun value(key: String): String? = obj[key]?.jsonPrimitive?.contentOrNull
        val type = value("type") ?: return@runCatching null
        PrediqLiveEvent(
            type = type,
            eventId = value("event_id"),
            sport = value("sport"),
            homeParticipant = value("home_participant"),
            awayParticipant = value("away_participant"),
            homeScore = value("home_score"),
            awayScore = value("away_score"),
            scoringSide = value("scoring_side"),
            statusText = value("status_text"),
            observedAt = value("observed_at"),
        )
    }.getOrNull()
}
