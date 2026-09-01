package com.getprediq.app.data

import android.content.Context
import kotlinx.coroutines.flow.first

class PrediqRepository(context: Context) {
    private val session = SessionStore(context.applicationContext)
    private val api = PrediqApi(session)

    suspend fun hasSession(): Boolean = !session.accessToken.first().isNullOrBlank()
    suspend fun login(email: String, password: String): AccountResponse {
        val auth = api.login(email, password); session.save(auth.accessToken, auth.refreshToken); return api.me()
    }
    suspend fun register(name: String, email: String, password: String): AccountResponse {
        val auth = api.register(name, email, password); session.save(auth.accessToken, auth.refreshToken); return api.me()
    }
    suspend fun logout() = api.logout()
    suspend fun me() = api.me()
    suspend fun picks() = api.picks()
    suspend fun filters() = api.filters()
    suspend fun assessments(status: String? = null, sport: String? = null, competition: String? = null, confidence: String? = null, market: String? = null) = api.assessments(status, sport, competition, confidence, market)
    suspend fun live(full: Boolean) = api.live(full)
    suspend fun resultsDashboard() = api.resultsDashboard()
    suspend fun results(days: Int = 30, outcome: String? = null, sport: String? = null, competition: String? = null, market: String? = null, confidence: String? = null) = api.results(days, outcome, sport, competition, market, confidence)
    suspend fun plans() = api.plans()
    suspend fun paymentCapabilities() = api.paymentCapabilities()
    suspend fun checkout(plan: String, phone: String) = api.checkout(plan, phone)
    suspend fun notificationSettings() = api.notificationSettings()
    suspend fun updateNotificationSettings(settings: NotificationSettings) = api.updateNotificationSettings(settings)
    suspend fun matchIntelligence(eventId: String) = api.matchIntelligence(eventId)
    suspend fun leagueForecasts() = api.leagueForecasts()
}
