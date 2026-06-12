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
    val items: List<NetworkOrderItemRequest>,
    val shippingAddress: String,
    val paymentMethod: String,
    val phoneNumber: String? = null
)

@Serializable
data class NetworkOrderItemRequest(
    val bookId: String,
    val quantity: Int
)

@Serializable
data class NetworkCheckoutResponse(
    val orderId: String? = null,
    val total: Long? = null,
    val status: String? = null,
    val authorizationUrl: String? = null
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

