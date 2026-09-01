package com.getprediq.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TeamIntelligenceSummary(
    @SerialName("sport_code") val sportCode: String = "football",
    @SerialName("team_name") val teamName: String = "",
    val profile: JsonObject = JsonObject(emptyMap()),
    @SerialName("matches_count") val matchesCount: Int = 0,
    @SerialName("as_of") val asOf: String? = null,
)

@Serializable
data class TeamsResponse(
    val teams: List<TeamIntelligenceSummary> = emptyList(),
)

@Serializable
data class LeagueIntelligenceSummary(
    @SerialName("sport_code") val sportCode: String = "football",
    val competition: String = "",
    val season: String? = null,
    val profile: JsonObject = JsonObject(emptyMap()),
    @SerialName("matches_count") val matchesCount: Int = 0,
    @SerialName("as_of") val asOf: String? = null,
)

@Serializable
data class LeagueIntelligenceResponse(
    val leagues: List<LeagueIntelligenceSummary> = emptyList(),
)
