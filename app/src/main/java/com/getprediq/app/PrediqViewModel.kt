package com.getprediq.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getprediq.app.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64


data class PrediqUiState(
    val account: AccountResponse? = null,
    val picks: List<PickDto> = emptyList(),
    val assessments: List<AssessmentDto> = emptyList(),
    val live: LiveResponse? = null,
    val resultsDashboard: ResultsDashboard = ResultsDashboard(),
    val results: List<ResultDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val paymentCapabilities: PaymentCapabilities = PaymentCapabilities(),
    val filterOptions: FilterOptions = FilterOptions(),
    val notifications: NotificationSettings? = null,
    val affiliate: AffiliateDashboard? = null,
    val matchIntelligence: MatchIntelligenceResponse? = null,
    val leagueForecasts: List<LeagueForecast> = emptyList(),
    val players: List<PlayerSummary> = emptyList(),
    val playerDetail: PlayerDetail? = null,
    val squadDepth: SquadDepthResponse? = null,
    val exploreBusy: Boolean = false,
    val exploreError: String? = null,
    val selectedSport: String = "",
    val selectedCountry: String = "",
    val selectedCompetition: String = "",
    val selectedConfidence: String = "",
    val selectedMarket: String = "",
    val todayMode: String = "today",
    val resultOutcome: String = "",
    val resultDays: Int = 30,
    val loadingToday: Boolean = true,
    val loadingLive: Boolean = true,
    val loadingResults: Boolean = true,
    val loadingAccount: Boolean = true,
    val liveError: String? = null,
    val todayError: String? = null,
    val resultError: String? = null,
    val authError: String? = null,
    val authBusy: Boolean = false,
    val paymentMessage: String? = null,
    val paymentBusy: Boolean = false,
)

class PrediqViewModel(private val repository: PrediqRepository) : ViewModel() {
    var state = androidx.compose.runtime.mutableStateOf(PrediqUiState())
        private set

    init { bootstrap() }

    private fun update(transform: (PrediqUiState) -> PrediqUiState) { state.value = transform(state.value) }
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try { Result.success(block()) } catch (error: Throwable) { Result.failure(error) }
    private var todayRequest = 0L

    private fun bootstrap() = viewModelScope.launch {
        val session = attempt { repository.hasSession() }.getOrDefault(false)
        val plansJob = async { attempt { repository.plans() }.getOrNull() }
        val capsJob = async { attempt { repository.paymentCapabilities() }.getOrNull() }
        val account = if (session) attempt { repository.me() }.getOrNull() else null
        val plans = plansJob.await()?.plans.orEmpty()
        val capabilities = capsJob.await() ?: PaymentCapabilities()
        update { it.copy(account = account, plans = plans, paymentCapabilities = capabilities, loadingAccount = false, loadingToday = false, loadingLive = false, loadingResults = false) }
        if (account?.access?.fullSelections == true) loadPaidData()
        if (account != null) { loadNotifications(); loadAffiliate() }
    }

    val fullAccess: Boolean get() = state.value.account?.access?.fullSelections == true

    private fun loadPaidData() = viewModelScope.launch {
        if (!fullAccess) return@launch
        val picks = async { attempt { repository.picks() }.getOrNull()?.picks.orEmpty() }
        val filters = async { attempt { repository.filters() }.getOrNull() ?: FilterOptions() }
        update { it.copy(picks = picks.await(), filterOptions = filters.await()) }
        loadToday(); loadLive(); loadResults()
    }

    fun loadToday() = viewModelScope.launch {
        val request = ++todayRequest
        update { it.copy(loadingToday = it.assessments.isEmpty(), todayError = null) }
        if (!fullAccess) { update { it.copy(loadingToday = false) }; return@launch }
        val current = state.value
        val status = if (current.todayMode == "upcoming") "upcoming" else null
        attempt { repository.assessments(status, current.selectedSport.takeIf(String::isNotBlank), current.selectedCountry.takeIf(String::isNotBlank), current.selectedCompetition.takeIf(String::isNotBlank), current.selectedConfidence.takeIf(String::isNotBlank), current.selectedMarket.takeIf(String::isNotBlank)) }
            .onSuccess { response -> if (request == todayRequest) update { it.copy(assessments = response.assessments, loadingToday = false, todayError = null) } }
            .onFailure { error -> if (request == todayRequest) update { it.copy(loadingToday = false, todayError = error.message ?: "Could not refresh today’s analysis") } }
    }

    fun loadLive() = viewModelScope.launch {
        if (!fullAccess) { update { it.copy(live = null, loadingLive = false) }; return@launch }
        val current = state.value
        val hadData = current.live != null
        update { it.copy(loadingLive = !hadData, liveError = null) }
        attempt { repository.live() }
            .onSuccess { response ->
                val games = response.games.filter { game ->
                    (current.selectedSport.isBlank() || game.sportCode == current.selectedSport) &&
                    (current.selectedCompetition.isBlank() || game.competition.equals(current.selectedCompetition, ignoreCase = true))
                }
                val scoped = if (fullAccess && (current.selectedSport.isNotBlank() || current.selectedCompetition.isNotBlank())) response.copy(
                    games = games,
                    liveCount = games.size,
                    strongCount = games.count { it.analysisPromotable },
                    predictedCount = games.count { it.predictionAvailable },
                ) else response
                update { it.copy(live = scoped, loadingLive = false, liveError = null) }
            }
            .onFailure { error -> update { it.copy(loadingLive = false, liveError = error.message ?: "Live could not refresh") } }
    }

    fun loadResults() = viewModelScope.launch {
        if (!fullAccess) { update { it.copy(results = emptyList(), resultsDashboard = ResultsDashboard(), loadingResults = false) }; return@launch }
        update { it.copy(loadingResults = it.results.isEmpty(), resultError = null) }
        val current = state.value
        val dashboardJob = async { attempt { repository.resultsDashboard() }.getOrNull() }
        val result = attempt { repository.results(current.resultDays, current.resultOutcome.takeIf(String::isNotBlank), current.selectedSport.takeIf(String::isNotBlank), current.selectedCountry.takeIf(String::isNotBlank), current.selectedCompetition.takeIf(String::isNotBlank), current.selectedMarket.takeIf(String::isNotBlank), current.selectedConfidence.takeIf(String::isNotBlank)) }
        val dashboard = dashboardJob.await()
        result.onSuccess { response -> update { it.copy(results = response.results, resultsDashboard = dashboard ?: it.resultsDashboard, loadingResults = false, resultError = null) } }
            .onFailure { error -> update { it.copy(loadingResults = false, resultError = error.message ?: "Results could not refresh") } }
    }

    fun selectSport(sport: String) { update { it.copy(selectedSport = sport, selectedCompetition = "") }; loadToday(); loadLive(); loadResults() }
    fun applyFilters(sport: String, country: String, competition: String, confidence: String, market: String) {
        update { it.copy(selectedSport = sport, selectedCountry = country, selectedCompetition = competition, selectedConfidence = confidence, selectedMarket = market) }
        loadToday(); loadLive(); loadResults()
    }
    fun clearAdvancedFilters() { update { it.copy(selectedSport = "", selectedCountry = "", selectedCompetition = "", selectedConfidence = "", selectedMarket = "") }; loadToday(); loadLive(); loadResults() }
    fun setTodayMode(mode: String) { update { it.copy(todayMode = mode) }; loadToday() }
    fun setResultOutcome(outcome: String) { update { it.copy(resultOutcome = outcome) }; loadResults() }
    fun setResultDays(days: Int) { update { it.copy(resultDays = days) }; loadResults() }

    fun login(email: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        attempt { repository.login(email, password) }.onSuccess { account -> update { it.copy(account = account, authBusy = false, authError = null) }; if (account.access.fullSelections) loadPaidData(); loadNotifications(); loadAffiliate(); onDone() }.onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Sign in failed") } }
    }
    fun register(name: String, email: String, password: String, country: String, consent: Boolean, referralCode: String?, onDone: () -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        if (country.trim().length != 2 || !consent) { update { it.copy(authBusy = false, authError = if (!consent) "Please agree to the terms and responsible-use notice." else "Use a two-letter country code, for example UG.") }; return@launch }
        attempt { repository.register(name, email, password, country.trim().uppercase(), consent, referralCode) }.onSuccess { account -> update { it.copy(account = account, authBusy = false, authError = null) }; loadPaidData(); loadNotifications(); loadAffiliate(); onDone() }.onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Account creation failed") } }
    }
    fun logout() = viewModelScope.launch { attempt { repository.logout() }; update { PrediqUiState(plans = it.plans, paymentCapabilities = it.paymentCapabilities, loadingAccount = false, loadingToday = false, loadingLive = false, loadingResults = false) } }
    fun refreshAccount() = viewModelScope.launch { if (!attempt { repository.hasSession() }.getOrDefault(false)) return@launch; attempt { repository.me() }.onSuccess { account -> update { it.copy(account = account, loadingAccount = false) } } }
    fun checkout(plan: String, phone: String) = viewModelScope.launch { update { it.copy(paymentBusy = true, paymentMessage = null) }; attempt { repository.checkout(plan, phone) }.onSuccess { response -> update { it.copy(paymentBusy = false, paymentMessage = response.message) }; refreshAccount() }.onFailure { error -> update { it.copy(paymentBusy = false, paymentMessage = error.message ?: "Payment request failed") } } }
    fun loadNotifications() = viewModelScope.launch { if (state.value.account == null) return@launch; attempt { repository.notificationSettings() }.onSuccess { settings -> update { it.copy(notifications = settings) } } }
    fun loadAffiliate() = viewModelScope.launch { if (state.value.account == null) return@launch; attempt { repository.affiliate() }.onSuccess { dashboard -> update { it.copy(affiliate = dashboard) } } }
    fun startTuku(referralCode: String? = null, openUrl: (String) -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        val random = SecureRandom(); val verifierBytes = ByteArray(64).also(random::nextBytes); val stateBytes = ByteArray(32).also(random::nextBytes)
        val verifier = Base64.encodeToString(verifierBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val handoffState = Base64.encodeToString(stateBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        attempt { repository.saveHandoff(handoffState, verifier); repository.startTuku(handoffState, challenge, referralCode) }
            .onSuccess { response -> update { it.copy(authBusy = false) }; openUrl(response.authorizeUrl) }
            .onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Tuku sign-in could not start") } }
    }
    fun finishTuku(code: String, callbackState: String) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        val (expectedState, verifier) = repository.consumeHandoff()
        if (expectedState.isNullOrBlank() || verifier.isNullOrBlank() || expectedState != callbackState) { update { it.copy(authBusy = false, authError = "This Tuku sign-in link is invalid or expired") }; return@launch }
        attempt { repository.finishTuku(code, callbackState, verifier) }
            .onSuccess { account -> update { it.copy(account = account, authBusy = false) }; if (account.access.fullSelections) loadPaidData(); loadNotifications(); loadAffiliate() }
            .onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Tuku sign-in failed") } }
    }
    fun saveNotifications(settings: NotificationSettings) = viewModelScope.launch { attempt { repository.updateNotificationSettings(settings) }.onSuccess { saved -> update { it.copy(notifications = saved) } } }
    fun loadMatch(eventId: String) = viewModelScope.launch { update { it.copy(matchIntelligence = null) }; attempt { repository.matchIntelligence(eventId) }.onSuccess { data -> update { it.copy(matchIntelligence = data) } } }
    fun loadLeagueForecasts() = viewModelScope.launch { attempt { repository.leagueForecasts() }.onSuccess { data -> update { it.copy(leagueForecasts = data.leagues) } } }
    fun searchPlayers(sport: String = "football", query: String = "") = viewModelScope.launch {
        update { it.copy(exploreBusy = true, exploreError = null) }
        attempt { repository.players(sport, query) }.onSuccess { data -> update { it.copy(players = data.players, playerDetail = null, exploreBusy = false) } }.onFailure { error -> update { it.copy(exploreBusy = false, exploreError = error.message ?: "Players could not load") } }
    }
    fun loadPlayer(playerId: String) = viewModelScope.launch {
        update { it.copy(exploreBusy = true, exploreError = null) }
        attempt { repository.player(playerId) }.onSuccess { data -> update { it.copy(playerDetail = data, exploreBusy = false) } }.onFailure { error -> update { it.copy(exploreBusy = false, exploreError = error.message ?: "Player could not load") } }
    }
    fun loadSquad(team: String) = viewModelScope.launch {
        if (team.isBlank()) { update { it.copy(squadDepth = null, exploreError = null) }; return@launch }
        update { it.copy(exploreBusy = true, exploreError = null) }
        attempt { repository.squad(team) }.onSuccess { data -> update { it.copy(squadDepth = data, exploreBusy = false) } }.onFailure { error -> update { it.copy(exploreBusy = false, exploreError = error.message ?: "Squad could not load") } }
    }
    fun clearPaymentMessage() = update { it.copy(paymentMessage = null) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = PrediqViewModel(PrediqRepository(context.applicationContext)) as T
        }
    }
}
