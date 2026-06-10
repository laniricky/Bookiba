package co.booknook.core.data.repository

import co.booknook.core.database.dao.OrderDao
import co.booknook.core.database.model.OrderEntity
import co.booknook.core.database.model.OrderItemEntity
import co.booknook.core.domain.model.CartItem
import co.booknook.core.domain.model.Order
import co.booknook.core.domain.model.OrderItem
import co.booknook.core.domain.model.OrderStatus
import co.booknook.core.domain.repository.OrderRepository
import co.booknook.core.network.api.BookibaApi
import co.booknook.core.network.model.NetworkCartItem
import co.booknook.core.network.model.NetworkCheckoutRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class LocalOrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val bookibaApi: BookibaApi
) : OrderRepository {

    override fun getOrders(): Flow<List<Order>> {
        return orderDao.getOrders().map { orderList ->
            orderList.map { orderWithItems ->
                Order(
                    id = orderWithItems.order.id,
                    dateMs = orderWithItems.order.dateMs,
                    totalAmount = orderWithItems.order.totalAmount,
                    status = OrderStatus.valueOf(orderWithItems.order.status),
                    items = orderWithItems.items.map { item ->
                        OrderItem(
                            bookId = item.bookId,
                            title = item.title,
                            author = item.author,
                            coverUrl = item.coverUrl,
                            priceKsh = item.priceKsh,
                            quantity = item.quantity
                        )
                    }
                )
            }
        }
    }

    override suspend fun createOrder(totalAmount: Long, items: List<CartItem>) {
        // 1. Sync with backend API
        val networkItems = items.map {
            NetworkCartItem(
                bookId = it.bookId,
                quantity = it.quantity,
                price = it.priceKsh.toDouble()
            )
        }
        
        val response = bookibaApi.checkout(NetworkCheckoutRequest(items = networkItems))
        
        if (!response.ok) {
            throw Exception(response.error ?: "Failed to create order on server")
        }

        // 2. Save locally with the server-generated order ID
        val orderId = response.orderId ?: UUID.randomUUID().toString().take(8).uppercase()
        val orderEntity = OrderEntity(
            id = orderId,
            dateMs = System.currentTimeMillis(),
            totalAmount = totalAmount,
            status = OrderStatus.PROCESSING.name
        )
        val orderItemEntities = items.map { item ->
            OrderItemEntity(
                orderId = orderId,
                bookId = item.bookId,
                title = item.title,
                author = item.author,
                coverUrl = item.coverUrl,
                priceKsh = item.priceKsh,
                quantity = item.quantity
            )
        }
        
        orderDao.insertOrder(orderEntity)
        orderDao.insertOrderItems(orderItemEntities)
    }
}
