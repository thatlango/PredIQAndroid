package com.getprediq.app.data.v3

import com.getprediq.app.BuildConfig
import com.getprediq.app.data.AuthResponse
import com.getprediq.app.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class V3ApiException(val statusCode: Int, override val message: String) : Exception(message)

class V3Api(private val session: SessionStore) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(35, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val v1Base = BuildConfig.PREDIQ_API_BASE_URL
    private val v3Base = BuildConfig.PREDIQ_API_BASE_URL.replace("/api/v1/", "/api/v3/intelligence/")

    private suspend fun raw(path: String, method: String = "GET", body: String? = null, retry: Boolean = true): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(v3Base + path.trimStart('/')).header("Accept", "application/json")
        session.accessToken.first()?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Authorization", "Bearer $it") }
        val requestBody = (body ?: "{}").toRequestBody(mediaType)
        when (method) {
            "POST" -> requestBuilder.post(requestBody)
            "PUT" -> requestBuilder.put(requestBody)
            "PATCH" -> requestBuilder.patch(requestBody)
            "DELETE" -> requestBuilder.delete(if (body == null) null else requestBody)
            else -> requestBuilder.get()
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401 && retry && refresh()) return@withContext raw(path, method, body, false)
            if (!response.isSuccessful) {
                val detail = runCatching { json.parseToJsonElement(text).jsonObject["detail"]?.toString()?.trim('"') }.getOrNull()
                throw V3ApiException(response.code, detail ?: "PredIQ intelligence request failed (${response.code})")
            }
            text
        }
    }

    private suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = session.refreshToken.first() ?: return@withContext false
        runCatching {
            val request = Request.Builder().url(v1Base + "auth/refresh").header("Accept", "application/json")
                .post(buildJsonObject { put("refresh_token", refreshToken) }.toString().toRequestBody(mediaType)).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val auth = json.decodeFromString<AuthResponse>(response.body?.string().orEmpty())
                session.save(auth.accessToken, auth.refreshToken); true
            }
        }.getOrDefault(false)
    }

    suspend fun priceSources(): V3BookmakerCatalog = json.decodeFromString(raw("bookmakers"))
    suspend fun bookmakers(): V3BookmakerCatalog = priceSources()

    suspend fun slate(hours: Int = 120, bookmaker: String = "prediq_reference", limit: Int = 15): V3SlateResponse =
        json.decodeFromString(raw("slate?hours=${hours.coerceIn(1,168)}&limit=${limit.coerceIn(1,50)}&bookmaker=${enc(bookmaker)}&top_europe=true"))

    suspend fun event(eventId: String, bookmaker: String = "prediq_reference"): V3EventDetail =
        json.decodeFromString(raw("events/${enc(eventId)}?bookmaker=${enc(bookmaker)}"))

    suspend fun buildTicket(targetOdds: Double, risk: String, bookmaker: String = "prediq_reference", hours: Int = 120): V3TicketResponse {
        val body = buildJsonObject {
            put("target_odds", targetOdds); put("risk_profile", risk); put("bookmaker", bookmaker)
            put("hours", hours.coerceIn(1,168)); put("max_legs", 14); put("top_europe", true)
        }.toString()
        return json.decodeFromString(raw("tickets/build", "POST", body))
    }

    suspend fun recalculateTicket(legs: List<V3TicketLeg>): V3TicketResponse {
        val body = buildJsonObject { put("legs", json.parseToJsonElement(json.encodeToString(legs))) }.toString()
        return json.decodeFromString(raw("tickets/recalculate", "POST", body))
    }

    suspend fun tickets(limit: Int = 30): V3SavedTicketsResponse =
        json.decodeFromString(raw("tickets?limit=${limit.coerceIn(1,100)}"))

    suspend fun deleteTicket(ticketId: String) { raw("tickets/${enc(ticketId)}", "DELETE") }

    suspend fun saveTicket(title: String, ticket: V3TicketResponse): V3TicketResponse {
        val body = buildJsonObject { put("title", title); put("ticket", json.parseToJsonElement(json.encodeToString(ticket))) }.toString()
        return json.decodeFromString(raw("tickets", "POST", body))
    }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
