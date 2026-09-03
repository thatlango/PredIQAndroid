package com.getprediq.app.data.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable data class V2Chance(
    val available: Boolean = false,
    val probability: Double? = null,
    val percent: Int? = null,
    val label: String = "Chance unavailable",
    val simple: String? = null,
)

@Serializable data class V2Evidence(
    val level: String = "limited",
    val label: String = "Limited evidence",
    @SerialName("signals_count") val signalsCount: Int = 0,
    @SerialName("sources_count") val sourcesCount: Int? = null,
    @SerialName("lineup_state") val lineupState: String = "unknown",
    @SerialName("latest_evidence_at") val latestEvidenceAt: String? = null,
    val signals: List<JsonObject> = emptyList(),
)

@Serializable data class V2Risk(val level: String = "medium", val label: String = "Medium risk")

@Serializable data class V2Value(
    val available: Boolean = false,
    val status: String = "unpriced",
    val label: String = "Price not available",
    @SerialName("current_odds") val currentOdds: Double? = null,
    @SerialName("market_probability") val marketProbability: Double? = null,
    @SerialName("market_percent") val marketPercent: Int? = null,
    @SerialName("edge_points") val edgePoints: Double? = null,
    @SerialName("expected_value") val expectedValue: Double? = null,
    @SerialName("price_updated_at") val priceUpdatedAt: String? = null,
    @SerialName("market_signal") val marketSignal: JsonObject? = null,
)

@Serializable data class V2Freshness(
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("source_updated_at") val sourceUpdatedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val state: String = "fresh",
)

@Serializable data class V2DecisionState(
    val code: String = "watching",
    val label: String = "Watching",
    val reason: String? = null,
)

@Serializable data class V2Reason(val label: String = "", val direction: String? = null)
@Serializable data class V2WatchOut(val label: String = "")
@Serializable data class V2FollowState(val following: Boolean = false)
@Serializable data class V2EntityRef(val id: String? = null, val name: String = "")
@Serializable data class V2CompetitionRef(val id: String? = null, val name: String? = null, val country: String? = null)
@Serializable data class V2Score(val home: JsonElement? = null, val away: JsonElement? = null, @SerialName("status_text") val statusText: String? = null)
@Serializable data class V2Participants(val home: V2EntityRef = V2EntityRef(), val away: V2EntityRef = V2EntityRef())

@Serializable data class V2Event(
    val id: String = "",
    val sport: String? = null,
    val competition: V2CompetitionRef = V2CompetitionRef(),
    val participants: V2Participants = V2Participants(),
    @SerialName("starts_at") val startsAt: String? = null,
    val status: String? = null,
    val score: V2Score? = null,
)

@Serializable data class V2Pick(
    val market: String? = null,
    val selection: String? = null,
    val label: String? = null,
    val line: Double? = null,
)

@Serializable data class V2LatestChange(val summary: String? = null, val at: String? = null)
@Serializable data class V2ResultState(val outcome: String = "pending", val actual: JsonObject? = null, @SerialName("settled_at") val settledAt: String? = null)
@Serializable data class V2ClosingMarket(
    @SerialName("closing_odds") val closingOdds: Double? = null,
    @SerialName("closing_probability") val closingProbability: Double? = null,
    @SerialName("clv_probability") val clvProbability: Double? = null,
    @SerialName("clv_price") val clvPrice: Double? = null,
)

@Serializable data class V2DecisionCard(
    val id: String = "",
    @SerialName("prediction_id") val predictionId: String? = null,
    @SerialName("published_forecast_id") val publishedForecastId: String? = null,
    val event: V2Event = V2Event(),
    val pick: V2Pick = V2Pick(),
    val chance: V2Chance = V2Chance(),
    val strength: String? = null,
    val evidence: V2Evidence = V2Evidence(),
    val risk: V2Risk = V2Risk(),
    val value: V2Value = V2Value(),
    val reasons: List<V2Reason> = emptyList(),
    @SerialName("latest_change") val latestChange: V2LatestChange? = null,
    val freshness: V2Freshness = V2Freshness(),
    @SerialName("follow_state") val followState: V2FollowState = V2FollowState(),
    val decision: V2DecisionState = V2DecisionState(),
    val actions: List<String> = emptyList(),
    val result: V2ResultState? = null,
    @SerialName("closing_market") val closingMarket: V2ClosingMarket? = null,
)

@Serializable data class V2Viewer(@SerialName("display_name") val displayName: String? = null, val country: String? = null, val currency: String? = null)
@Serializable data class V2Briefing(
    val headline: String = "Today's PredIQ",
    @SerialName("top_picks") val topPicks: Int = 0,
    val picks: Int = 0,
    @SerialName("games_checked") val gamesChecked: Int = 0,
    @SerialName("changed_since") val changedSince: Int = 0,
    val following: Int = 0,
)

@Serializable data class V2Change(
    @SerialName("event_id") val eventId: String = "",
    val type: String = "updated",
    val title: String = "",
    val summary: String? = null,
    @SerialName("old_chance") val oldChance: V2Chance? = null,
    @SerialName("new_chance") val newChance: V2Chance? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
)

@Serializable data class V2Upcoming(val event: V2Event = V2Event(), val decision: V2DecisionState = V2DecisionState(), @SerialName("follow_state") val followState: V2FollowState = V2FollowState())
@Serializable data class V2SportFilter(val code: String = "", val label: String = "", val events: Int = 0)
@Serializable data class V2CompetitionFilter(val name: String = "", val sport: String? = null, val country: String? = null, val events: Int = 0)
@Serializable data class V2FilterOptions(
    val sports: List<V2SportFilter> = emptyList(),
    val competitions: List<V2CompetitionFilter> = emptyList(),
    val markets: List<String> = emptyList(),
    val outcomes: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
)

@Serializable data class V2TodayResponse(
    @SerialName("contract_version") val contractVersion: String = "2.0",
    val status: String = "ok",
    val message: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    val viewer: V2Viewer = V2Viewer(),
    val briefing: V2Briefing = V2Briefing(),
    val changes: List<V2Change> = emptyList(),
    @SerialName("top_picks") val topPicks: List<V2DecisionCard> = emptyList(),
    val waiting: List<V2DecisionCard> = emptyList(),
    val upcoming: List<V2Upcoming> = emptyList(),
    @SerialName("filter_options") val filterOptions: V2FilterOptions = V2FilterOptions(),
    @SerialName("changes_cursor") val changesCursor: String? = null,
)

@Serializable data class V2LiveSummary(@SerialName("live_games") val liveGames: Int = 0, val opportunities: Int = 0, val following: Int = 0)
@Serializable data class V2LiveChange(val direction: String = "stable", val points: Double? = null, val label: String = "Stable")
@Serializable data class V2LiveCard(
    val id: String = "",
    @SerialName("prediction_id") val predictionId: String? = null,
    @SerialName("published_forecast_id") val publishedForecastId: String? = null,
    val event: V2Event = V2Event(),
    val pick: V2Pick = V2Pick(),
    val chance: V2Chance = V2Chance(),
    val evidence: V2Evidence = V2Evidence(),
    val risk: V2Risk = V2Risk(),
    val value: V2Value = V2Value(),
    val reasons: List<V2Reason> = emptyList(),
    val freshness: V2Freshness = V2Freshness(),
    @SerialName("follow_state") val followState: V2FollowState = V2FollowState(),
    val decision: V2DecisionState = V2DecisionState(),
    @SerialName("original_chance") val originalChance: V2Chance? = null,
    @SerialName("current_chance") val currentChance: V2Chance = V2Chance(),
    val change: V2LiveChange = V2LiveChange(),
    @SerialName("analysis_quality") val analysisQuality: String = "none",
)

@Serializable data class V2LiveResponse(
    @SerialName("contract_version") val contractVersion: String = "2.0",
    val status: String = "ok",
    val message: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("live_state") val liveState: String = "empty",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("next_refresh_seconds") val nextRefreshSeconds: Int = 300,
    val summary: V2LiveSummary = V2LiveSummary(),
    val following: List<V2LiveCard> = emptyList(),
    val opportunities: List<V2LiveCard> = emptyList(),
    val changes: List<V2Change> = emptyList(),
    val games: List<V2LiveCard> = emptyList(),
    @SerialName("filter_options") val filterOptions: V2FilterOptions = V2FilterOptions(),
)

@Serializable data class V2ExpectedGoals(val home: Double? = null, val away: Double? = null)
@Serializable data class V2Outlook(
    @SerialName("home_win") val homeWin: Double? = null,
    val draw: Double? = null,
    @SerialName("away_win") val awayWin: Double? = null,
    @SerialName("expected_goals") val expectedGoals: V2ExpectedGoals = V2ExpectedGoals(),
)
@Serializable data class V2Alternative(val rank: Int = 0, val pick: V2Pick = V2Pick(), val chance: V2Chance = V2Chance(), val value: V2Value = V2Value())
@Serializable data class V2TimelineItem(
    val at: String? = null,
    val type: String = "analysis",
    val title: String = "Update",
    val detail: String? = null,
    @SerialName("chance_before") val chanceBefore: V2Chance? = null,
    @SerialName("chance_after") val chanceAfter: V2Chance? = null,
    @SerialName("odds_before") val oddsBefore: Double? = null,
    @SerialName("odds_after") val oddsAfter: Double? = null,
)
@Serializable data class V2SimilarCalls(val range: String? = null, val settled: Int = 0, val wins: Int = 0, @SerialName("observed_rate") val observedRate: Double? = null, val label: String = "Not enough history")
@Serializable data class V2Record(val settled: Int = 0, val wins: Int = 0, @SerialName("hit_rate") val hitRate: Double? = null)
@Serializable data class V2Availability(val pricing: String = "unavailable", val lineups: String = "pending", @SerialName("expected_goals") val expectedGoals: String = "unavailable", @SerialName("player_data") val playerData: String = "partial", val news: String = "partial")
@Serializable data class V2Share(val text: String? = null, val url: String? = null)

@Serializable data class V2PredictionDetail(
    @SerialName("contract_version") val contractVersion: String = "2.0",
    val status: String = "ok",
    val message: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    val decision: V2DecisionCard = V2DecisionCard(),
    val reasons: List<V2Reason> = emptyList(),
    @SerialName("watch_outs") val watchOuts: List<V2WatchOut> = emptyList(),
    val value: V2Value = V2Value(),
    val outlook: V2Outlook = V2Outlook(),
    val alternatives: List<V2Alternative> = emptyList(),
    val timeline: List<V2TimelineItem> = emptyList(),
    val teams: JsonObject = JsonObject(emptyMap()),
    val lineup: JsonObject = JsonObject(emptyMap()),
    @SerialName("similar_calls") val similarCalls: V2SimilarCalls = V2SimilarCalls(),
    @SerialName("prediq_record") val prediqRecord: V2Record = V2Record(),
    val evidence: V2Evidence = V2Evidence(),
    val availability: V2Availability = V2Availability(),
    val share: V2Share = V2Share(),
)

@Serializable data class V2RecordTotals(val settled: Int = 0, val won: Int = 0, val lost: Int = 0, val void: Int = 0, @SerialName("hit_rate") val hitRate: Double? = null)
@Serializable data class V2Calibration(val range: String = "", @SerialName("predicted_midpoint") val predictedMidpoint: Double? = null, @SerialName("observed_rate") val observedRate: Double? = null, val settled: Int = 0, val status: String = "insufficient")
@Serializable data class V2PerformanceSlice(val sport: String? = null, val market: String? = null, val competition: String? = null, val settled: Int = 0, val wins: Int = 0, @SerialName("hit_rate") val hitRate: Double? = null)
@Serializable data class V2PricePerformance(val tracked: Int = 0, @SerialName("beat_closing_price") val beatClosingPrice: Int = 0, @SerialName("beat_closing_rate") val beatClosingRate: Double? = null, @SerialName("mean_clv") val meanClv: Double? = null)
@Serializable data class V2ResultsSummary(
    @SerialName("contract_version") val contractVersion: String = "2.0",
    val status: String = "ok",
    @SerialName("period_days") val periodDays: Int = 30,
    val record: V2RecordTotals = V2RecordTotals(),
    val calibration: List<V2Calibration> = emptyList(),
    @SerialName("by_market") val byMarket: List<V2PerformanceSlice> = emptyList(),
    @SerialName("by_sport") val bySport: List<V2PerformanceSlice> = emptyList(),
    @SerialName("by_competition") val byCompetition: List<V2PerformanceSlice> = emptyList(),
    @SerialName("price_performance") val pricePerformance: V2PricePerformance = V2PricePerformance(),
)
@Serializable data class V2ResultsFeed(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val results: List<V2DecisionCard> = emptyList(), @SerialName("next_cursor") val nextCursor: String? = null)
@Serializable data class V2ResultReview(
    @SerialName("contract_version") val contractVersion: String = "2.0",
    val status: String = "ok",
    @SerialName("original_prediction") val originalPrediction: V2DecisionCard = V2DecisionCard(),
    @SerialName("actual_result") val actualResult: V2ResultState = V2ResultState(),
    @SerialName("publication_context") val publicationContext: JsonObject = JsonObject(emptyMap()),
    @SerialName("closing_market") val closingMarket: V2ClosingMarket = V2ClosingMarket(),
    val integrity: JsonObject = JsonObject(emptyMap()),
)

@Serializable data class V2ResearchTeam(val id: String = "", val sport: String = "", val name: String = "", val country: String? = null, val profile: JsonObject = JsonObject(emptyMap()), @SerialName("matches_count") val matchesCount: Int = 0, @SerialName("updated_at") val updatedAt: String? = null)
@Serializable data class V2ResearchLeague(val id: String = "", val sport: String = "", val name: String = "", val country: String? = null, val season: String? = null, val profile: JsonObject = JsonObject(emptyMap()), @SerialName("matches_count") val matchesCount: Int = 0, @SerialName("updated_at") val updatedAt: String? = null)
@Serializable data class V2ResearchPlayer(val id: String = "", @SerialName("sport_code") val sportCode: String = "football", val name: String = "", val nationality: String? = null, val age: Int? = null, val position: String? = null, @SerialName("position_group") val positionGroup: String = "Unknown", val headline: String? = null)
@Serializable data class V2ResearchResponse(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", @SerialName("teams_in_today_picks") val teamsInTodayPicks: List<V2ResearchTeam> = emptyList(), val leagues: List<V2ResearchLeague> = emptyList(), @SerialName("players_to_watch") val playersToWatch: List<V2ResearchPlayer> = emptyList())
@Serializable data class V2SearchResult(val type: String = "team", val id: String = "", val name: String = "", @SerialName("sport_code") val sportCode: String? = null, val sport: String? = null, val subtitle: String? = null)
@Serializable data class V2SearchResponse(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val results: List<V2SearchResult> = emptyList())

@Serializable data class V2TeamDetail(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val team: JsonObject = JsonObject(emptyMap()), val profile: JsonObject = JsonObject(emptyMap()), @SerialName("matches_count") val matchesCount: Int = 0, val upcoming: List<V2Upcoming> = emptyList(), @SerialName("squad_summary") val squadSummary: JsonObject = JsonObject(emptyMap()), @SerialName("prediq_record") val prediqRecord: V2Record = V2Record(), @SerialName("updated_at") val updatedAt: String? = null)
@Serializable data class V2PlayerDetail(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val player: JsonObject = JsonObject(emptyMap()), @SerialName("current_signal") val currentSignal: JsonObject = JsonObject(emptyMap()), @SerialName("headline_stats") val headlineStats: JsonObject = JsonObject(emptyMap()), @SerialName("recent_activity") val recentActivity: List<JsonObject> = emptyList(), val signals: List<String> = emptyList(), @SerialName("data_quality") val dataQuality: JsonObject = JsonObject(emptyMap()), @SerialName("updated_at") val updatedAt: String? = null)
@Serializable data class V2CompetitionDetail(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val competition: JsonObject = JsonObject(emptyMap()), val profile: JsonObject = JsonObject(emptyMap()), val outlook: JsonObject = JsonObject(emptyMap()), val trends: JsonObject = JsonObject(emptyMap()), @SerialName("prediq_strengths") val prediqStrengths: List<JsonObject> = emptyList(), @SerialName("today_opportunities") val todayOpportunities: List<V2DecisionCard> = emptyList(), @SerialName("track_record") val trackRecord: List<JsonObject> = emptyList(), val teams: List<JsonObject> = emptyList(), @SerialName("updated_at") val updatedAt: String? = null)

@Serializable data class V2Profile(val id: String = "", val name: String? = null, val email: String = "", val country: String? = null, val currency: String? = null)
@Serializable data class V2Membership(@SerialName("plan_name") val planName: String? = null, val state: String = "free", @SerialName("days_remaining") val daysRemaining: Int? = null, @SerialName("ends_at") val endsAt: String? = null, @SerialName("full_access") val fullAccess: Boolean = false)
@Serializable data class V2FollowingSummary(val total: Int = 0, val teams: Int = 0, @SerialName("events") val matches: Int = 0, @SerialName("competitions") val leagues: Int = 0, val players: Int = 0)
@Serializable data class V2AccountResponse(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val profile: V2Profile = V2Profile(), val membership: V2Membership = V2Membership(), @SerialName("following_summary") val followingSummary: V2FollowingSummary = V2FollowingSummary(), val notifications: JsonObject = JsonObject(emptyMap()), @SerialName("payments_summary") val paymentsSummary: JsonObject = JsonObject(emptyMap()), @SerialName("affiliate_summary") val affiliateSummary: JsonObject = JsonObject(emptyMap()))

@Serializable data class V2FollowAlerts(@SerialName("prediction_changes") val predictionChanges: Boolean = true, val lineup: Boolean = true, val live: Boolean = true, val result: Boolean = true, @SerialName("team_news") val teamNews: Boolean = true)
@Serializable data class V2Follow(val id: String = "", @SerialName("entity_type") val entityType: String = "event", @SerialName("entity_key") val entityKey: String = "", @SerialName("entity_label") val entityLabel: String? = null, @SerialName("alert_preferences") val alerts: V2FollowAlerts = V2FollowAlerts(), val active: Boolean = true, @SerialName("created_at") val createdAt: String? = null)
@Serializable data class V2FollowsResponse(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val follows: List<V2Follow> = emptyList())
@Serializable data class V2FollowMutationResponse(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", val follow: V2Follow = V2Follow())

@Serializable data class V2NotificationAlerts(@SerialName("daily_picks") val dailyPicks: Boolean = true, @SerialName("live_changes") val liveChanges: Boolean = true, @SerialName("lineup_changes") val lineupChanges: Boolean = true, val results: Boolean = true, val subscription: Boolean = true)
@Serializable data class V2NotificationSettings(@SerialName("contract_version") val contractVersion: String = "2.0", val status: String = "ok", @SerialName("push_enabled") val pushEnabled: Boolean = true, @SerialName("email_enabled") val emailEnabled: Boolean = true, @SerialName("sms_enabled") val smsEnabled: Boolean = false, @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean = true, val timezone: String = "Africa/Kampala", val alerts: V2NotificationAlerts = V2NotificationAlerts())
