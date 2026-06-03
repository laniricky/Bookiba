package co.booknook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkBook(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("author") val author: String,
    @SerialName("price_ksh") val priceKsh: Double,
    @SerialName("condition") val condition: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("inventory_count") val inventoryCount: Int = 0
)
