package com.getprediq.app.data

import android.content.Context
import kotlinx.coroutines.flow.first

class PrediqRepository(context: Context) {
    private val session = SessionStore(context.applicationContext)
    private val api = PrediqApi(session)

    suspend fun hasSession(): Boolean = !session.accessToken.first().isNullOrBlank()
    suspend fun login(email: String, password: String): AccountResponse { val auth = api.login(email, password); session.save(auth.accessToken, auth.refreshToken); return api.me() }
    suspend fun register(name: String, email: String, password: String, country: String, consent: Boolean, referralCode: String?): AccountResponse { val auth = api.register(name, email, password, country, consent, referralCode); session.save(auth.accessToken, auth.refreshToken); return api.me() }
    suspend fun logout() = api.logout()
    suspend fun me() = api.me()
    suspend fun picks() = api.picks()
    suspend fun filters() = api.filters()
    suspend fun assessments(status: String? = null, sport: String? = null, country: String? = null, competition: String? = null, confidence: String? = null, market: String? = null) = api.assessments(status, sport, country, competition, confidence, market)
    suspend fun live() = api.live()
    suspend fun resultsDashboard() = api.resultsDashboard()
    suspend fun results(days: Int = 30, outcome: String? = null, sport: String? = null, country: String? = null, competition: String? = null, market: String? = null, confidence: String? = null) = api.results(days, outcome, sport, country, competition, market, confidence)
    suspend fun plans() = api.plans()
    suspend fun paymentCapabilities() = api.paymentCapabilities()
    suspend fun checkout(plan: String, phone: String) = api.checkout(plan, phone)
    suspend fun notificationSettings() = api.notificationSettings()
    suspend fun updateNotificationSettings(settings: NotificationSettings) = api.updateNotificationSettings(settings)
    suspend fun matchIntelligence(eventId: String) = api.matchIntelligence(eventId)
    suspend fun leagueForecasts() = api.leagueForecasts()
    suspend fun leagueIntelligence(sport: String = "football") = api.leagueIntelligence(sport)
    suspend fun teams(sport: String = "football", competition: String = "") = api.teams(sport, competition)
    suspend fun team(team: String, sport: String = "football") = api.team(team, sport)
    suspend fun players(sport: String = "football", query: String = "") = api.players(sport, query)
    suspend fun player(playerId: String) = api.player(playerId)
    suspend fun squad(team: String) = api.squad(team)
    suspend fun affiliate() = api.affiliate()
    suspend fun startTuku(state: String, challenge: String, referralCode: String?) = api.startTuku(state, challenge, referralCode)
    suspend fun finishTuku(code: String, state: String, verifier: String): AccountResponse { val auth = api.exchangeTuku(code, state, verifier); session.save(auth.accessToken, auth.refreshToken); return api.me() }
    suspend fun saveHandoff(state: String, verifier: String) = session.saveHandoff(state, verifier)
    suspend fun consumeHandoff(): Pair<String?, String?> = session.consumeHandoff()
}
