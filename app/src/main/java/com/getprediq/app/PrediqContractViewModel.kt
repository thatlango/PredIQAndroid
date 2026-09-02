package com.getprediq.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getprediq.app.data.SessionStore
import com.getprediq.app.data.v2.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


data class PrediqContractState(
    val ready: Boolean = false,
    val busy: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: V2TodayResponse? = null,
    val live: V2LiveResponse? = null,
    val resultsSummary: V2ResultsSummary? = null,
    val results: V2ResultsFeed? = null,
    val research: V2ResearchResponse? = null,
    val account: V2AccountResponse? = null,
    val follows: V2FollowsResponse? = null,
    val notifications: V2NotificationSettings? = null,
    val prediction: V2PredictionDetail? = null,
    val resultReview: V2ResultReview? = null,
    val team: V2TeamDetail? = null,
    val player: V2PlayerDetail? = null,
    val competition: V2CompetitionDetail? = null,
    val search: V2SearchResponse? = null,
    val searchQuery: String = "",
    val selectedSport: String = "",
    val selectedCompetition: String = "",
    val followingOnly: Boolean = false,
    val resultPeriodDays: Int = 30,
    val resultOutcome: String = "",
    val resultMarket: String = "",
)

class PrediqContractViewModel(context: Context) : ViewModel() {
    private val session = SessionStore(context.applicationContext)
    private val api = V2Api(session)

    var state by mutableStateOf(PrediqContractState())
        private set

    private var loadedForSession = false

    init {
        viewModelScope.launch {
            if (!session.accessToken.first().isNullOrBlank()) bootstrap()
            else state = state.copy(ready = true)
        }
    }

    private fun mutate(block: (PrediqContractState) -> PrediqContractState) { state = block(state) }
    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try { Result.success(block()) } catch (t: Throwable) { Result.failure(t) }

    fun bootstrap(force: Boolean = false) {
        if (loadedForSession && !force) return
        loadedForSession = true
        viewModelScope.launch {
            mutate { it.copy(busy = true, error = null) }
            val today = async { safe { api.today() }.getOrNull() }
            val live = async { safe { api.live() }.getOrNull() }
            val summary = async { safe { api.resultsSummary(state.resultPeriodDays) }.getOrNull() }
            val results = async { safe { api.results(state.resultPeriodDays) }.getOrNull() }
            val research = async { safe { api.research() }.getOrNull() }
            val account = async { safe { api.me() }.getOrNull() }
            val follows = async { safe { api.follows() }.getOrNull() }
            val notifications = async { safe { api.notifications() }.getOrNull() }
            mutate {
                it.copy(
                    ready = true,
                    busy = false,
                    today = today.await(),
                    live = live.await(),
                    resultsSummary = summary.await(),
                    results = results.await(),
                    research = research.await(),
                    account = account.await(),
                    follows = follows.await(),
                    notifications = notifications.await(),
                )
            }
        }
    }

    fun clearForLogout() {
        loadedForSession = false
        state = PrediqContractState(ready = true)
    }

    fun refreshCurrent() = viewModelScope.launch {
        mutate { it.copy(refreshing = true, error = null) }
        val s = state
        val today = async { safe { api.today(s.selectedSport.takeIf(String::isNotBlank), s.selectedCompetition.takeIf(String::isNotBlank), s.followingOnly, s.today?.changesCursor) }.getOrNull() }
        val live = async { safe { api.live(s.selectedSport.takeIf(String::isNotBlank), s.selectedCompetition.takeIf(String::isNotBlank), s.followingOnly) }.getOrNull() }
        val account = async { safe { api.me() }.getOrNull() }
        mutate { it.copy(refreshing = false, today = today.await() ?: it.today, live = live.await() ?: it.live, account = account.await() ?: it.account) }
    }

    fun loadToday() = viewModelScope.launch {
        val s = state
        mutate { it.copy(refreshing = true, error = null) }
        safe { api.today(s.selectedSport.takeIf(String::isNotBlank), s.selectedCompetition.takeIf(String::isNotBlank), s.followingOnly, s.today?.changesCursor) }
            .onSuccess { data -> mutate { it.copy(today = data, refreshing = false) } }
            .onFailure { error -> mutate { it.copy(refreshing = false, error = error.message) } }
    }

    fun loadLive() = viewModelScope.launch {
        val s = state
        mutate { it.copy(refreshing = true, error = null) }
        safe { api.live(s.selectedSport.takeIf(String::isNotBlank), s.selectedCompetition.takeIf(String::isNotBlank), s.followingOnly) }
            .onSuccess { data -> mutate { it.copy(live = data, refreshing = false) } }
            .onFailure { error -> mutate { it.copy(refreshing = false, error = error.message) } }
    }

    fun loadResults() = viewModelScope.launch {
        val s = state
        mutate { it.copy(refreshing = true, error = null) }
        val summary = async { safe { api.resultsSummary(s.resultPeriodDays) }.getOrNull() }
        val feed = safe {
            api.results(
                periodDays = s.resultPeriodDays,
                sport = s.selectedSport.takeIf(String::isNotBlank),
                competition = s.selectedCompetition.takeIf(String::isNotBlank),
                market = s.resultMarket.takeIf(String::isNotBlank),
                outcome = s.resultOutcome.takeIf(String::isNotBlank),
            )
        }
        feed.onSuccess { data -> mutate { it.copy(results = data, resultsSummary = summary.await() ?: it.resultsSummary, refreshing = false) } }
            .onFailure { error -> mutate { it.copy(resultsSummary = summary.await() ?: it.resultsSummary, refreshing = false, error = error.message) } }
    }

    fun loadResearch() = viewModelScope.launch {
        mutate { it.copy(refreshing = true, error = null) }
        safe { api.research() }
            .onSuccess { data -> mutate { it.copy(research = data, refreshing = false) } }
            .onFailure { error -> mutate { it.copy(refreshing = false, error = error.message) } }
    }

    fun loadAccount() = viewModelScope.launch {
        val account = async { safe { api.me() }.getOrNull() }
        val follows = async { safe { api.follows() }.getOrNull() }
        val notifications = async { safe { api.notifications() }.getOrNull() }
        mutate { it.copy(account = account.await() ?: it.account, follows = follows.await() ?: it.follows, notifications = notifications.await() ?: it.notifications) }
    }

    fun setSport(value: String) {
        mutate { it.copy(selectedSport = value, selectedCompetition = "") }
        loadToday(); loadLive(); loadResults()
    }

    fun setCompetition(value: String) {
        mutate { it.copy(selectedCompetition = value) }
        loadToday(); loadLive(); loadResults()
    }

    fun setFollowingOnly(value: Boolean) {
        mutate { it.copy(followingOnly = value) }
        loadToday(); loadLive()
    }

    fun setResultPeriod(days: Int) { mutate { it.copy(resultPeriodDays = days) }; loadResults() }
    fun setResultOutcome(outcome: String) { mutate { it.copy(resultOutcome = outcome) }; loadResults() }
    fun setResultMarket(market: String) { mutate { it.copy(resultMarket = market) }; loadResults() }

    fun search(query: String) {
        mutate { it.copy(searchQuery = query) }
        if (query.trim().length < 2) { mutate { it.copy(search = V2SearchResponse()) }; return }
        viewModelScope.launch {
            safe { api.search(query.trim()) }
                .onSuccess { data -> if (state.searchQuery == query) mutate { it.copy(search = data) } }
                .onFailure { error -> mutate { it.copy(error = error.message) } }
        }
    }

    fun loadPrediction(ref: String) = viewModelScope.launch {
        mutate { it.copy(prediction = null, busy = true, error = null) }
        safe { api.prediction(ref) }
            .onSuccess { data -> mutate { it.copy(prediction = data, busy = false) } }
            .onFailure { error -> mutate { it.copy(busy = false, error = error.message) } }
    }

    fun loadResultReview(id: String) = viewModelScope.launch {
        mutate { it.copy(resultReview = null, busy = true, error = null) }
        safe { api.resultReview(id) }
            .onSuccess { data -> mutate { it.copy(resultReview = data, busy = false) } }
            .onFailure { error -> mutate { it.copy(busy = false, error = error.message) } }
    }

    fun loadTeam(id: String) = viewModelScope.launch {
        mutate { it.copy(team = null, busy = true, error = null) }
        safe { api.team(id) }.onSuccess { data -> mutate { it.copy(team = data, busy = false) } }.onFailure { e -> mutate { it.copy(busy = false, error = e.message) } }
    }

    fun loadPlayer(id: String) = viewModelScope.launch {
        mutate { it.copy(player = null, busy = true, error = null) }
        safe { api.player(id) }.onSuccess { data -> mutate { it.copy(player = data, busy = false) } }.onFailure { e -> mutate { it.copy(busy = false, error = e.message) } }
    }

    fun loadCompetition(id: String) = viewModelScope.launch {
        mutate { it.copy(competition = null, busy = true, error = null) }
        safe { api.competition(id) }.onSuccess { data -> mutate { it.copy(competition = data, busy = false) } }.onFailure { e -> mutate { it.copy(busy = false, error = e.message) } }
    }

    fun follow(entityType: String, entityKey: String, label: String?, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        safe { api.follow(entityType, entityKey, label) }
            .onSuccess { loadAccount(); loadToday(); onDone?.invoke() }
            .onFailure { error -> mutate { it.copy(error = error.message) } }
    }

    fun updateFollow(id: String, alerts: V2FollowAlerts) = viewModelScope.launch {
        safe { api.updateFollow(id, alerts) }.onSuccess { loadAccount() }.onFailure { error -> mutate { it.copy(error = error.message) } }
    }

    fun unfollow(id: String) = viewModelScope.launch {
        safe { api.unfollow(id) }.onSuccess { loadAccount(); loadToday(); loadLive() }.onFailure { error -> mutate { it.copy(error = error.message) } }
    }

    fun saveNotifications(settings: V2NotificationSettings) = viewModelScope.launch {
        safe { api.saveNotifications(settings) }
            .onSuccess { saved -> mutate { it.copy(notifications = saved) } }
            .onFailure { error -> mutate { it.copy(error = error.message) } }
    }

    fun clearError() { mutate { it.copy(error = null) } }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PrediqContractViewModel(context.applicationContext) as T
        }
    }
}
