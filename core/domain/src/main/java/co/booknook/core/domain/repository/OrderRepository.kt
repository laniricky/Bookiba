package co.booknook.core.domain.repository

import co.booknook.core.domain.model.CartItem
import co.booknook.core.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun createOrder(totalAmount: Long, items: List<CartItem>)
}
