package com.getprediq.app.data

import androidx.compose.runtime.mutableStateMapOf

object MediaCatalogStore {
    private val entities = mutableStateMapOf<String, MediaEntity>()
    private fun key(type: String, sport: String, name: String) = "${type.lowercase()}|${sport.lowercase()}|${name.trim().lowercase()}"
    fun replace(response: MediaCatalogResponse) {
        entities.clear()
        response.entities.forEach { entities[key(it.entityType, it.sportCode, it.canonicalName)] = it }
    }
    fun team(name: String, sport: String = "football") = entities[key("team", sport, name)]
    fun competition(name: String, sport: String = "football") = entities[key("competition", sport, name)]
    fun player(name: String, sport: String = "football") = entities[key("player", sport, name)]
}
