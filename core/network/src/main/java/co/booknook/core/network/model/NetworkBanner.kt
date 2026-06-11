package co.booknook.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkBanner(
    val id: String,
    val imageUrl: String,
    val title: String? = null,
    val subtitle: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class NetworkBannersResponse(
    val banners: List<NetworkBanner>
)
