package com.getprediq.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaEntity(
    val id: Long,
    @SerialName("entity_type") val entityType: String,
    @SerialName("sport_code") val sportCode: String,
    @SerialName("canonical_name") val canonicalName: String,
    val provider: String,
    @SerialName("provider_id") val providerId: String? = null,
    @SerialName("short_code") val shortCode: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("optimized_image_url") val optimizedImageUrl: String? = null,
    @SerialName("image_kind") val imageKind: String = "logo",
    @SerialName("is_canonical_code") val isCanonicalCode: Boolean = false,
)

@Serializable
data class MediaProviderStatus(
    val sportmonks: Boolean = false,
    @SerialName("api_sports") val apiSports: Boolean = false,
    val sportradar: Boolean = false,
)

@Serializable
data class MediaCatalogResponse(
    @SerialName("provider_status") val providerStatus: MediaProviderStatus = MediaProviderStatus(),
    val entities: List<MediaEntity> = emptyList(),
)
