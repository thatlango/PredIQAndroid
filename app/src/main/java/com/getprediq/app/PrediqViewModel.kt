package com.getprediq.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getprediq.app.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


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
    val matchIntelligence: MatchIntelligenceResponse? = null,
    val leagueForecasts: List<LeagueForecast> = emptyList(),
    val selectedSport: String = "",
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

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun bootstrap() = viewModelScope.launch {
        val session = attempt { repository.hasSession() }.getOrDefault(false)
        val picksJob = async { attempt { repository.picks() }.getOrNull() }
        val dashboardJob = async { attempt { repository.resultsDashboard() }.getOrNull() }
        val plansJob = async { attempt { repository.plans() }.getOrNull() }
        val capsJob = async { attempt { repository.paymentCapabilities() }.getOrNull() }
        val filtersJob = async { attempt { repository.filters() }.getOrNull() }
        val account = if (session) attempt { repository.me() }.getOrNull() else null
        val picks = picksJob.await()?.picks.orEmpty()
        val dashboard = dashboardJob.await() ?: ResultsDashboard()
        val plans = plansJob.await()?.plans.orEmpty()
        val capabilities = capsJob.await() ?: PaymentCapabilities()
        val filters = filtersJob.await() ?: FilterOptions()
        update {
            it.copy(
                account = account,
                picks = picks,
                resultsDashboard = dashboard,
                plans = plans,
                paymentCapabilities = capabilities,
                filterOptions = filters,
                loadingAccount = false,
            )
        }
        loadToday(); loadLive(); loadResults()
        if (account != null) loadNotifications()
    }

    val fullAccess: Boolean get() = state.value.account?.access?.fullSelections == true

    fun loadToday() = viewModelScope.launch {
        update { it.copy(loadingToday = it.assessments.isEmpty(), todayError = null) }
        if (!fullAccess) { update { it.copy(loadingToday = false) }; return@launch }
        val current = state.value
        val status = if (current.todayMode == "upcoming") "upcoming" else null
        attempt {
            repository.assessments(
                status = status,
                sport = current.selectedSport.takeIf { it.isNotBlank() },
                competition = current.selectedCompetition.takeIf { it.isNotBlank() },
                confidence = current.selectedConfidence.takeIf { it.isNotBlank() },
                market = current.selectedMarket.takeIf { it.isNotBlank() },
            )
        }.onSuccess { response ->
            update { it.copy(assessments = response.assessments, loadingToday = false, todayError = null) }
        }.onFailure { error ->
            update { it.copy(loadingToday = false, todayError = error.message ?: "Could not refresh today’s analysis") }
        }
    }

    fun loadLive() = viewModelScope.launch {
        val hadData = state.value.live != null
        update { it.copy(loadingLive = !hadData, liveError = null) }
        attempt { repository.live(fullAccess) }
            .onSuccess { response -> update { it.copy(live = response, loadingLive = false, liveError = null) } }
            .onFailure { error -> update { it.copy(loadingLive = false, liveError = error.message ?: "Live could not refresh") } }
    }

    fun loadResults() = viewModelScope.launch {
        update { it.copy(loadingResults = it.results.isEmpty(), resultError = null) }
        val current = state.value
        val dashboardJob = async { attempt { repository.resultsDashboard() }.getOrNull() }
        val result = attempt {
            repository.results(
                days = current.resultDays,
                outcome = current.resultOutcome.takeIf { it.isNotBlank() },
                sport = current.selectedSport.takeIf { it.isNotBlank() },
                competition = current.selectedCompetition.takeIf { it.isNotBlank() },
                market = current.selectedMarket.takeIf { it.isNotBlank() },
                confidence = current.selectedConfidence.takeIf { it.isNotBlank() },
            )
        }
        val dashboard = dashboardJob.await()
        result.onSuccess { response ->
            update { it.copy(results = response.results, resultsDashboard = dashboard ?: it.resultsDashboard, loadingResults = false, resultError = null) }
        }.onFailure { error ->
            update { it.copy(loadingResults = false, resultError = error.message ?: "Results could not refresh") }
        }
    }

    fun selectSport(sport: String) { update { it.copy(selectedSport = sport) }; loadToday(); loadResults() }
    fun applyFilters(sport: String, competition: String, confidence: String, market: String) {
        update { it.copy(selectedSport = sport, selectedCompetition = competition, selectedConfidence = confidence, selectedMarket = market) }
        loadToday(); loadResults()
    }
    fun clearAdvancedFilters() { update { it.copy(selectedSport = "", selectedCompetition = "", selectedConfidence = "", selectedMarket = "") }; loadToday(); loadResults() }
    fun setTodayMode(mode: String) { update { it.copy(todayMode = mode) }; loadToday() }
    fun setResultOutcome(outcome: String) { update { it.copy(resultOutcome = outcome) }; loadResults() }
    fun setResultDays(days: Int) { update { it.copy(resultDays = days) }; loadResults() }

    fun login(email: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        attempt { repository.login(email, password) }
            .onSuccess { account ->
                update { it.copy(account = account, authBusy = false, authError = null) }
                loadToday(); loadLive(); loadNotifications(); onDone()
            }
            .onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Sign in failed") } }
    }

    fun register(name: String, email: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        attempt { repository.register(name, email, password) }
            .onSuccess { account ->
                update { it.copy(account = account, authBusy = false, authError = null) }
                loadToday(); loadLive(); loadNotifications(); onDone()
            }
            .onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Account creation failed") } }
    }

    fun logout() = viewModelScope.launch {
        attempt { repository.logout() }
        update { it.copy(account = null, assessments = emptyList(), notifications = null, matchIntelligence = null, leagueForecasts = emptyList()) }
        loadLive(); loadToday()
    }

    fun refreshAccount() = viewModelScope.launch {
        if (!attempt { repository.hasSession() }.getOrDefault(false)) return@launch
        attempt { repository.me() }.onSuccess { account -> update { it.copy(account = account, loadingAccount = false) } }
    }

    fun checkout(plan: String, phone: String) = viewModelScope.launch {
        update { it.copy(paymentBusy = true, paymentMessage = null) }
        attempt { repository.checkout(plan, phone) }
            .onSuccess { response -> update { it.copy(paymentBusy = false, paymentMessage = response.message) }; refreshAccount() }
            .onFailure { error -> update { it.copy(paymentBusy = false, paymentMessage = error.message ?: "Payment request failed") } }
    }

    fun loadNotifications() = viewModelScope.launch {
        if (state.value.account == null) return@launch
        attempt { repository.notificationSettings() }.onSuccess { settings -> update { it.copy(notifications = settings) } }
    }

    fun saveNotifications(settings: NotificationSettings) = viewModelScope.launch {
        attempt { repository.updateNotificationSettings(settings) }.onSuccess { saved -> update { it.copy(notifications = saved) } }
    }

    fun loadMatch(eventId: String) = viewModelScope.launch {
        update { it.copy(matchIntelligence = null) }
        attempt { repository.matchIntelligence(eventId) }.onSuccess { data -> update { it.copy(matchIntelligence = data) } }
    }

    fun loadLeagueForecasts() = viewModelScope.launch {
        attempt { repository.leagueForecasts() }.onSuccess { data -> update { it.copy(leagueForecasts = data.leagues) } }
    }

    fun clearPaymentMessage() = update { it.copy(paymentMessage = null) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PrediqViewModel(PrediqRepository(context.applicationContext)) as T
        }
    }
}
