package co.booknook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkBook(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("author")
    val author: String,
    @SerialName("price")
    val price: Double,
    @SerialName("condition")
    val condition: String, // e.g., "Good", "Like New", "Vintage"
    @SerialName("cover_url")
    val coverUrl: String,
    @SerialName("description")
    val description: String,
    @SerialName("seller_id")
    val sellerId: String,
    @SerialName("is_featured")
    val isFeatured: Boolean = false
)
