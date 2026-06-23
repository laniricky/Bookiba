package co.booknook.core.domain.repository

import co.booknook.core.domain.model.CartItem
import co.booknook.core.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun syncOrders()
    suspend fun createOrder(totalAmount: Long, items: List<CartItem>, paymentMethod: String = "MPESA", phoneNumber: String = "", shippingAddress: String = ""): String?
}
