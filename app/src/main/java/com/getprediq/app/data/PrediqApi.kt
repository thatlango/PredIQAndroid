package com.getprediq.app.data

import com.getprediq.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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

class ApiException(val status: Int, override val message: String) : Exception(message)

class PrediqApi(private val session: SessionStore) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun raw(path: String, method: String = "GET", body: String? = null, auth: Boolean = false, retry: Boolean = true): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(BuildConfig.PREDIQ_API_BASE_URL + path.trimStart('/')).header("Accept", "application/json")
        if (auth) session.accessToken.first()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        when (method) { "POST" -> builder.post((body ?: "{}").toRequestBody(mediaType)); "PUT" -> builder.put((body ?: "{}").toRequestBody(mediaType)); else -> builder.get() }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401 && auth && retry && refresh()) return@withContext raw(path, method, body, auth, false)
            if (!response.isSuccessful) {
                val detail = runCatching { json.parseToJsonElement(text).jsonObject["detail"]?.toString()?.trim('"') }.getOrNull()
                throw ApiException(response.code, detail ?: "PredIQ request failed (${response.code})")
            }
            text
        }
    }

    private suspend fun refresh(): Boolean {
        val refresh = session.refreshToken.first() ?: return false
        return runCatching {
            val body = buildJsonObject { put("refresh_token", refresh) }.toString()
            val response = json.decodeFromString<AuthResponse>(raw("auth/refresh", "POST", body, false, false))
            session.save(response.accessToken, response.refreshToken); true
        }.getOrElse { session.clear(); false }
    }

    suspend fun login(email: String, password: String): AuthResponse = json.decodeFromString(raw("auth/login", "POST", buildJsonObject { put("email", email.trim()); put("password", password) }.toString()))
    suspend fun register(name: String, email: String, password: String, country: String, consent: Boolean, referralCode: String?): AuthResponse = json.decodeFromString(raw("auth/register", "POST", buildJsonObject { put("name", name.trim()); put("email", email.trim()); put("password", password); put("country", country.trim().uppercase()); put("consent", consent); referralCode?.takeIf(String::isNotBlank)?.let { put("referral_code", it) } }.toString()))
    suspend fun logout() { runCatching { raw("auth/logout", "POST", "{}", true) }; session.clear() }
    suspend fun me() = json.decodeFromString<AccountResponse>(raw("me", auth = true))
    suspend fun picks() = json.decodeFromString<PicksResponse>(raw("picks-of-day", auth = true))
    suspend fun filters() = json.decodeFromString<FilterOptions>(raw("filters", auth = true))

    suspend fun assessments(status: String? = null, sport: String? = null, country: String? = null, competition: String? = null, confidence: String? = null, market: String? = null): AssessmentsResponse {
        val q = mutableListOf<String>(); fun add(key: String, value: String?) { if (!value.isNullOrBlank()) q += "$key=${enc(value)}" }
        add("status", status); add("sport", sport); add("country", country); add("competition", competition); add("confidence", confidence); add("market", market)
        return json.decodeFromString(raw("assessments${if (q.isEmpty()) "" else "?" + q.joinToString("&")}", auth = true))
    }

    suspend fun live() = json.decodeFromString<LiveResponse>(raw("live", auth = true))
    suspend fun resultsDashboard() = json.decodeFromString<ResultsDashboard>(raw("results/dashboard", auth = true))

    suspend fun results(days: Int = 30, outcome: String? = null, sport: String? = null, country: String? = null, competition: String? = null, market: String? = null, confidence: String? = null): ResultsResponse {
        val q = mutableListOf("days=$days"); fun add(key: String, value: String?) { if (!value.isNullOrBlank()) q += "$key=${enc(value)}" }
        add("outcome", outcome); add("sport", sport); add("country", country); add("competition", competition); add("market", market); add("confidence", confidence)
        return json.decodeFromString(raw("results?${q.joinToString("&")}", auth = true))
    }

    suspend fun plans() = json.decodeFromString<PlansResponse>(raw("plans"))
    suspend fun paymentCapabilities() = json.decodeFromString<PaymentCapabilities>(raw("payments/capabilities"))
    suspend fun checkout(plan: String, phone: String): CheckoutResponse = json.decodeFromString(raw("payments/checkout", "POST", buildJsonObject { put("plan_code", plan); put("phone", phone) }.toString(), true))
    suspend fun notificationSettings() = json.decodeFromString<NotificationSettings>(raw("me/notifications", auth = true))
    suspend fun updateNotificationSettings(settings: NotificationSettings): NotificationSettings {
        val a = settings.alerts
        val body = buildJsonObject { put("push_enabled", settings.pushEnabled); put("email_enabled", settings.emailEnabled); put("sms_enabled", settings.smsEnabled); put("whatsapp_enabled", settings.whatsappEnabled); put("daily_picks", a.dailyPicks); put("live_changes", a.liveChanges); put("lineup_changes", a.lineupChanges); put("results", a.results); put("subscription", a.subscription); put("timezone", settings.timezone) }.toString()
        return json.decodeFromString(raw("me/notifications", "PUT", body, true))
    }
    suspend fun matchIntelligence(eventId: String) = json.decodeFromString<MatchIntelligenceResponse>(raw("intelligence/matches/${enc(eventId)}", auth = true))
    suspend fun leagueForecasts() = json.decodeFromString<LeagueForecastsResponse>(raw("intelligence/league-winners", auth = true))
    suspend fun players(sport: String = "football", query: String = "") = json.decodeFromString<PlayersResponse>(raw("intelligence/players?sport=${enc(sport)}&limit=50${if (query.isBlank()) "" else "&q=${enc(query)}"}", auth = true))
    suspend fun player(playerId: String) = json.decodeFromString<PlayerDetail>(raw("intelligence/player?player_id=${enc(playerId)}", auth = true))
    suspend fun squad(team: String) = json.decodeFromString<SquadDepthResponse>(raw("intelligence/squad?team=${enc(team)}&sport=football", auth = true))
    suspend fun affiliate() = json.decodeFromString<AffiliateDashboard>(raw("me/affiliate", auth = true))
    suspend fun startTuku(state: String, challenge: String, referralCode: String?): TukuStartResponse = json.decodeFromString(raw("auth/tuku/mobile/start", "POST", buildJsonObject { put("state", state); put("code_challenge", challenge); referralCode?.takeIf(String::isNotBlank)?.let { put("referral_code", it) } }.toString()))
    suspend fun exchangeTuku(code: String, state: String, verifier: String): AuthResponse = json.decodeFromString(raw("auth/tuku/mobile/exchange", "POST", buildJsonObject { put("code", code); put("state", state); put("code_verifier", verifier) }.toString()))
    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
