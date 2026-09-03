package com.getprediq.app.data.v2

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class V2FilterContractTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    @Test
    fun structuredFilterOptionsDecodeFromTodayContract() {
        val payload = """{"filter_options":{"sports":[{"code":"tennis","label":"Tennis","events":12}],"competitions":[{"name":"ATP US Open","sport":"tennis","country":"United States","events":8}]}}"""
        val decoded = json.decodeFromString<FilterEnvelope>(payload)
        assertEquals("tennis", decoded.filterOptions.sports.single().code)
        assertEquals("ATP US Open", decoded.filterOptions.competitions.single().name)
    }

    @kotlinx.serialization.Serializable
    private data class FilterEnvelope(@kotlinx.serialization.SerialName("filter_options") val filterOptions: V2FilterOptions)
}
