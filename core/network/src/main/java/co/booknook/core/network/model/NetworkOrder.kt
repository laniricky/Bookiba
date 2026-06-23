package co.booknook.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkOrderDto(
    val id: String,
    val totalAmount: Long,
    val status: String,
    val paymentMethod: String,
    val phoneNumber: String? = null,
    val shippingAddress: String,
    val createdAt: String,
    val items: List<NetworkOrderItemDto>
)

@Serializable
data class NetworkOrderItemDto(
    val bookId: String,
    val title: String,
    val quantity: Int,
    val priceKsh: Long
)

@Serializable
data class NetworkOrdersResponse(
    val orders: List<NetworkOrderDto> = emptyList()
)
