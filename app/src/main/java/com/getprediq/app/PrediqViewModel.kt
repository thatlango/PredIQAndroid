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

    private fun bootstrap() = viewModelScope.launch {
        val session = runCatching { repository.hasSession() }.getOrDefault(false)
        val publicJobs = listOf(
            async { runCatching { repository.picks() }.getOrNull() },
            async { runCatching { repository.resultsDashboard() }.getOrNull() },
            async { runCatching { repository.plans() }.getOrNull() },
            async { runCatching { repository.paymentCapabilities() }.getOrNull() },
            async { runCatching { repository.filters() }.getOrNull() },
        )
        val account = if (session) runCatching { repository.me() }.getOrNull() else null
        val picks = publicJobs[0].await() as? PicksResponse
        val dashboard = publicJobs[1].await() as? ResultsDashboard
        val plans = publicJobs[2].await() as? PlansResponse
        val caps = publicJobs[3].await() as? PaymentCapabilities
        val filters = publicJobs[4].await() as? FilterOptions
        update { it.copy(account = account, picks = picks?.picks.orEmpty(), resultsDashboard = dashboard ?: ResultsDashboard(), plans = plans?.plans.orEmpty(), paymentCapabilities = caps ?: PaymentCapabilities(), filterOptions = filters ?: FilterOptions(), loadingAccount = false) }
        loadToday()
        loadLive()
        loadResults()
        if (account != null) loadNotifications()
    }

    val fullAccess: Boolean get() = state.value.account?.access?.fullSelections == true

    fun loadToday() = viewModelScope.launch {
        update { it.copy(loadingToday = it.assessments.isEmpty(), todayError = null) }
        if (!fullAccess) {
            update { it.copy(loadingToday = false) }
            return@launch
        }
        val current = state.value
        val status = if (current.todayMode == "upcoming") "upcoming" else null
        runCatching { repository.assessments(status = status, sport = current.selectedSport.takeIf { it.isNotBlank() }) }
            .onSuccess { response -> update { it.copy(assessments = response.assessments, loadingToday = false, todayError = null) } }
            .onFailure { error -> update { it.copy(loadingToday = false, todayError = error.message ?: "Could not refresh today’s analysis") } }
    }

    fun loadLive() = viewModelScope.launch {
        val hadData = state.value.live != null
        update { it.copy(loadingLive = !hadData, liveError = null) }
        runCatching { repository.live(fullAccess) }
            .onSuccess { response -> update { it.copy(live = response, loadingLive = false, liveError = null) } }
            .onFailure { error -> update { it.copy(loadingLive = false, liveError = error.message ?: "Live could not refresh") } }
    }

    fun loadResults() = viewModelScope.launch {
        update { it.copy(loadingResults = it.results.isEmpty(), resultError = null) }
        val current = state.value
        val dash = async { runCatching { repository.resultsDashboard() }.getOrNull() }
        runCatching { repository.results(days = current.resultDays, outcome = current.resultOutcome.takeIf { it.isNotBlank() }, sport = current.selectedSport.takeIf { it.isNotBlank() }) }
            .onSuccess { response -> update { it.copy(results = response.results, resultsDashboard = dash.await() ?: it.resultsDashboard, loadingResults = false) } }
            .onFailure { error -> update { it.copy(loadingResults = false, resultError = error.message ?: "Results could not refresh") } }
    }

    fun selectSport(sport: String) { update { it.copy(selectedSport = sport) }; loadToday(); loadResults() }
    fun setTodayMode(mode: String) { update { it.copy(todayMode = mode) }; loadToday() }
    fun setResultOutcome(outcome: String) { update { it.copy(resultOutcome = outcome) }; loadResults() }
    fun setResultDays(days: Int) { update { it.copy(resultDays = days) }; loadResults() }

    fun login(email: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        runCatching { repository.login(email, password) }
            .onSuccess { account -> update { it.copy(account = account, authBusy = false, authError = null) }; loadToday(); loadLive(); loadNotifications(); onDone() }
            .onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Sign in failed") } }
    }

    fun register(name: String, email: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        update { it.copy(authBusy = true, authError = null) }
        runCatching { repository.register(name, email, password) }
            .onSuccess { account -> update { it.copy(account = account, authBusy = false, authError = null) }; loadToday(); loadLive(); loadNotifications(); onDone() }
            .onFailure { error -> update { it.copy(authBusy = false, authError = error.message ?: "Account creation failed") } }
    }

    fun logout() = viewModelScope.launch {
        repository.logout()
        update { it.copy(account = null, assessments = emptyList(), notifications = null, matchIntelligence = null, leagueForecasts = emptyList()) }
        loadLive(); loadToday()
    }

    fun refreshAccount() = viewModelScope.launch {
        if (!repository.hasSession()) return@launch
        runCatching { repository.me() }.onSuccess { account -> update { it.copy(account = account, loadingAccount = false) } }
    }

    fun checkout(plan: String, phone: String) = viewModelScope.launch {
        update { it.copy(paymentBusy = true, paymentMessage = null) }
        runCatching { repository.checkout(plan, phone) }
            .onSuccess { response -> update { it.copy(paymentBusy = false, paymentMessage = response.message) }; refreshAccount() }
            .onFailure { error -> update { it.copy(paymentBusy = false, paymentMessage = error.message ?: "Payment request failed") } }
    }

    fun loadNotifications() = viewModelScope.launch {
        if (!fullAccess && state.value.account == null) return@launch
        runCatching { repository.notificationSettings() }.onSuccess { settings -> update { it.copy(notifications = settings) } }
    }

    fun saveNotifications(settings: NotificationSettings) = viewModelScope.launch {
        runCatching { repository.updateNotificationSettings(settings) }.onSuccess { saved -> update { it.copy(notifications = saved) } }
    }

    fun loadMatch(eventId: String) = viewModelScope.launch {
        update { it.copy(matchIntelligence = null) }
        runCatching { repository.matchIntelligence(eventId) }.onSuccess { data -> update { it.copy(matchIntelligence = data) } }
    }

    fun loadLeagueForecasts() = viewModelScope.launch {
        runCatching { repository.leagueForecasts() }.onSuccess { data -> update { it.copy(leagueForecasts = data.leagues) } }
    }

    fun clearPaymentMessage() = update { it.copy(paymentMessage = null) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PrediqViewModel(PrediqRepository(context.applicationContext)) as T
            }
        }
    }
}
