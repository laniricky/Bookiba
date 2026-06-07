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
    @SerialName("data") val data: List<NetworkBook> = emptyList(),
    @SerialName("page") val page: Int = 1,
    @SerialName("limit") val limit: Int = 20,
    @SerialName("total") val total: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false
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

// ── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class NetworkAuthRequest(
    @SerialName("action") val action: String,        // "login" or "register"
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("name") val name: String? = null     // register only
)

@Serializable
data class NetworkUserInfo(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String
)

@Serializable
data class NetworkAuthResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("token") val token: String? = null,
    @SerialName("user") val user: NetworkUserInfo? = null,
    @SerialName("error") val error: String? = null
)

// ── Profile ──────────────────────────────────────────────────────────────────

@Serializable
data class NetworkUserProfileResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("data") val data: NetworkUserProfileData? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class NetworkUserProfileData(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("ordersCount") val ordersCount: Int,
    @SerialName("wishlistCount") val wishlistCount: Int,
    @SerialName("reviewsCount") val reviewsCount: Int
)

