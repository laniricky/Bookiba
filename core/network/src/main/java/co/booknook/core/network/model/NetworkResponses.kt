package co.booknook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkHomeResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("data") val data: NetworkHomeData? = null
)

@Serializable
data class NetworkHomeData(
    @SerialName("featured") val featured: List<NetworkBook> = emptyList(),
    @SerialName("staffPick") val staffPick: NetworkBook? = null,
    @SerialName("newArrivals") val newArrivals: List<NetworkBook> = emptyList(),
    @SerialName("stories") val stories: List<NetworkStory> = emptyList()
)

@Serializable
data class NetworkStory(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String
)

@Serializable
data class NetworkBooksResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("data") val data: List<NetworkBook> = emptyList()
)

@Serializable
data class NetworkSingleBookResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("data") val data: NetworkBook? = null
)

@Serializable
data class NetworkCheckoutRequest(
    @SerialName("items") val items: List<NetworkCartItem>
)

@Serializable
data class NetworkCartItem(
    @SerialName("book_id") val bookId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("price") val price: Double
)

@Serializable
data class NetworkCheckoutResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null
)
