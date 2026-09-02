package com.getprediq.app.data

import android.content.Context
import android.os.Build
import com.getprediq.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class PaymentHistoryItem(
    val id: String = "",
    @SerialName("plan_code") val planCode: String = "",
    @SerialName("plan_name") val planName: String = "",
    val provider: String = "",
    @SerialName("tx_ref") val txRef: String = "",
    @SerialName("provider_reference") val providerReference: String? = null,
    @SerialName("amount_ugx") val amountUgx: Int = 0,
    val currency: String = "UGX",
    val status: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("settled_at") val settledAt: String? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
)

@Serializable
data class PaymentHistoryResponse(val payments: List<PaymentHistoryItem> = emptyList())

@Serializable
data class LeagueAlertsResponse(
    val leagues: List<String> = emptyList(),
    val available: List<String> = emptyList(),
)

@Serializable
data class DeviceRegistrationResponse(
    val id: String? = null,
    val platform: String = "android",
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("device_label") val deviceLabel: String? = null,
    val active: Boolean = true,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

class AccountFeatureRepository(context: Context) {
    private val session = SessionStore(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun raw(path: String, method: String = "GET", body: String? = null, retry: Boolean = true): String = withContext(Dispatchers.IO) {
        val token = session.accessToken.first()
        if (token.isNullOrBlank()) throw ApiException(401, "Sign in to continue")
        val builder = Request.Builder()
            .url(BuildConfig.PREDIQ_API_BASE_URL + path.trimStart('/'))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
        when (method) {
            "POST" -> builder.post((body ?: "{}").toRequestBody(mediaType))
            "PUT" -> builder.put((body ?: "{}").toRequestBody(mediaType))
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401 && retry) {
                val refreshed = runCatching { PrediqApi(session).me(); true }.getOrDefault(false)
                if (refreshed) return@withContext raw(path, method, body, false)
            }
            if (!response.isSuccessful) {
                val detail = runCatching { json.parseToJsonElement(text).jsonObject["detail"]?.toString()?.trim('"') }.getOrNull()
                throw ApiException(response.code, detail ?: "PredIQ request failed (${response.code})")
            }
            text
        }
    }

    suspend fun updateProfile(displayName: String, countryCode: String, currency: String): UserDto {
        val body = buildJsonObject {
            put("display_name", displayName.trim())
            put("country_code", countryCode.trim().uppercase())
            put("currency", currency.trim().uppercase())
        }.toString()
        return json.decodeFromString(raw("me/profile", "PUT", body))
    }

    suspend fun paymentHistory(): PaymentHistoryResponse = json.decodeFromString(raw("payments/history"))

    suspend fun leagueAlerts(): LeagueAlertsResponse = json.decodeFromString(raw("me/league-alerts"))

    suspend fun updateLeagueAlerts(leagues: List<String>): LeagueAlertsResponse {
        val body = buildJsonObject {
            putJsonArray("leagues") { leagues.distinct().take(50).forEach { add(it) } }
        }.toString()
        return json.decodeFromString(raw("me/league-alerts", "PUT", body))
    }

    suspend fun registerDevice(token: String): DeviceRegistrationResponse {
        val body = buildJsonObject {
            put("platform", "android")
            put("token", token)
            put("app_version", BuildConfig.VERSION_NAME)
            put("device_label", Build.MODEL.take(160))
        }.toString()
        return json.decodeFromString(raw("devices/register", "POST", body))
    }

    suspend fun deactivateDevice(token: String) {
        val body = buildJsonObject { put("token", token) }.toString()
        raw("devices/deactivate", "POST", body)
    }
}
