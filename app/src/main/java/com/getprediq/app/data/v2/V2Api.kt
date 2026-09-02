package com.getprediq.app.data.v2

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

class V2ApiException(val statusCode: Int, override val message: String) : Exception(message)

class V2Api(private val session: SessionStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val v1Base = BuildConfig.PREDIQ_API_BASE_URL
    private val v2Base = BuildConfig.PREDIQ_API_BASE_URL.replace("/api/v1/", "/api/v2/")

    private suspend fun raw(
        path: String,
        method: String = "GET",
        body: String? = null,
        retry: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(v2Base + path.trimStart('/'))
            .header("Accept", "application/json")
        session.accessToken.first()?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
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
                throw V2ApiException(response.code, detail ?: "PredIQ request failed (${response.code})")
            }
            text
        }
    }

    private suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = session.refreshToken.first() ?: return@withContext false
        runCatching {
            val request = Request.Builder()
                .url(v1Base + "auth/refresh")
                .header("Accept", "application/json")
                .post(buildJsonObject { put("refresh_token", refreshToken) }.toString().toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val auth = json.decodeFromString<AuthResponse>(response.body?.string().orEmpty())
                session.save(auth.accessToken, auth.refreshToken)
                true
            }
        }.getOrDefault(false)
    }

    suspend fun today(sport: String? = null, competition: String? = null, followingOnly: Boolean = false, since: String? = null): V2TodayResponse {
        val q = queryOf("sport" to sport, "competition" to competition, "following_only" to followingOnly.takeIf { it }?.toString(), "since" to since)
        return json.decodeFromString(raw("today$q"))
    }

    suspend fun live(sport: String? = null, competition: String? = null, followingOnly: Boolean = false): V2LiveResponse {
        val q = queryOf("sport" to sport, "competition" to competition, "following_only" to followingOnly.takeIf { it }?.toString())
        return json.decodeFromString(raw("live$q"))
    }

    suspend fun prediction(decisionRef: String): V2PredictionDetail =
        json.decodeFromString(raw("predictions/${enc(decisionRef)}"))

    suspend fun resultsSummary(periodDays: Int = 30): V2ResultsSummary =
        json.decodeFromString(raw("results/summary?period_days=${periodDays.coerceIn(1, 3650)}"))

    suspend fun results(
        periodDays: Int = 30,
        sport: String? = null,
        competition: String? = null,
        market: String? = null,
        outcome: String? = null,
        limit: Int = 50,
        before: String? = null,
    ): V2ResultsFeed {
        val q = queryOf(
            "period_days" to periodDays.coerceIn(1, 3650).toString(),
            "sport" to sport,
            "competition" to competition,
            "market" to market,
            "outcome" to outcome,
            "limit" to limit.coerceIn(1, 100).toString(),
            "before" to before,
        )
        return json.decodeFromString(raw("results$q"))
    }

    suspend fun resultReview(id: String): V2ResultReview = json.decodeFromString(raw("results/${enc(id)}"))
    suspend fun research(): V2ResearchResponse = json.decodeFromString(raw("research"))
    suspend fun search(query: String, limit: Int = 30): V2SearchResponse = json.decodeFromString(raw("search?q=${enc(query)}&limit=${limit.coerceIn(1, 50)}"))
    suspend fun team(id: String): V2TeamDetail = json.decodeFromString(raw("teams/${enc(id)}"))
    suspend fun player(id: String): V2PlayerDetail = json.decodeFromString(raw("players/${enc(id)}"))
    suspend fun competition(id: String): V2CompetitionDetail = json.decodeFromString(raw("competitions/${enc(id)}"))
    suspend fun me(): V2AccountResponse = json.decodeFromString(raw("me"))
    suspend fun follows(): V2FollowsResponse = json.decodeFromString(raw("follows"))
    suspend fun notifications(): V2NotificationSettings = json.decodeFromString(raw("me/notifications"))

    suspend fun saveNotifications(settings: V2NotificationSettings): V2NotificationSettings {
        val body = buildJsonObject {
            put("push_enabled", settings.pushEnabled)
            put("email_enabled", settings.emailEnabled)
            put("sms_enabled", settings.smsEnabled)
            put("whatsapp_enabled", settings.whatsappEnabled)
            put("timezone", settings.timezone)
            put("alerts", buildJsonObject {
                put("daily_picks", settings.alerts.dailyPicks)
                put("live_changes", settings.alerts.liveChanges)
                put("lineup_changes", settings.alerts.lineupChanges)
                put("results", settings.alerts.results)
                put("subscription", settings.alerts.subscription)
            })
        }.toString()
        return json.decodeFromString(raw("me/notifications", "PUT", body))
    }

    suspend fun follow(entityType: String, entityKey: String, label: String?, alerts: V2FollowAlerts = V2FollowAlerts()): V2Follow {
        val body = buildJsonObject {
            put("entity_type", entityType)
            put("entity_key", entityKey)
            label?.let { put("entity_label", it) }
            put("alerts", buildJsonObject {
                put("prediction_changes", alerts.predictionChanges)
                put("lineup", alerts.lineup)
                put("live", alerts.live)
                put("result", alerts.result)
                put("team_news", alerts.teamNews)
            })
        }.toString()
        return json.decodeFromString(raw("follows", "POST", body))
    }

    suspend fun updateFollow(id: String, alerts: V2FollowAlerts): V2Follow {
        val body = buildJsonObject {
            put("alerts", buildJsonObject {
                put("prediction_changes", alerts.predictionChanges)
                put("lineup", alerts.lineup)
                put("live", alerts.live)
                put("result", alerts.result)
                put("team_news", alerts.teamNews)
            })
        }.toString()
        return json.decodeFromString(raw("follows/${enc(id)}", "PATCH", body))
    }

    suspend fun unfollow(id: String) { raw("follows/${enc(id)}", "DELETE") }

    private fun queryOf(vararg pairs: Pair<String, String?>): String {
        val items = pairs.mapNotNull { (key, value) -> value?.takeIf { it.isNotBlank() }?.let { "${enc(key)}=${enc(it)}" } }
        return if (items.isEmpty()) "" else "?" + items.joinToString("&")
    }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
