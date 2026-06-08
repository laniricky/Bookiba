package co.booknook.core.network.model

import kotlinx.serialization.Serializable

// ── Books ────────────────────────────────────────────────────────────────────

@Serializable
data class NetworkBooksResponse(
    val books: List<NetworkBook> = emptyList(),
    val page: Int? = null,
    val pageSize: Int? = null,
    val error: String? = null
)

// ── Checkout / Orders ────────────────────────────────────────────────────────

@Serializable
data class NetworkCheckoutRequest(
    val items: List<NetworkCartItem>
)

@Serializable
data class NetworkCartItem(
    val bookId: String,
    val quantity: Int,
    val price: Double
)

@Serializable
data class NetworkCheckoutResponse(
    val ok: Boolean = false,
    val orderId: String? = null,
    val message: String? = null,
    val error: String? = null
)

// ── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class NetworkLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class NetworkRegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class NetworkAuthResponse(
    val token: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val error: String? = null
)

// ── Profile ──────────────────────────────────────────────────────────────────

@Serializable
data class NetworkUserProfileResponse(
    val name: String? = null,
    val email: String? = null,
    val ordersCount: Int? = null,
    val wishlistCount: Int? = null,
    val reviewsCount: Int? = null,
    val error: String? = null
)

