package co.booknook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkBook(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("author") val author: String,
    @SerialName("description") val description: String? = null,
    @SerialName("priceKsh") val priceKsh: Long,
    @SerialName("condition") val condition: String? = null,
    @SerialName("coverUrl") val coverUrl: String,
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerialName("category") val category: String,
    @SerialName("genre") val genre: String? = null,
    @SerialName("edition") val edition: String? = null,
    @SerialName("publisher") val publisher: String? = null,
    @SerialName("isRare") val isRare: Boolean = false,
    @SerialName("isFeatured") val isFeatured: Boolean = false,
    @SerialName("isStaffPick") val isStaffPick: Boolean = false,
    @SerialName("tags") val tags: List<String> = emptyList()
)
