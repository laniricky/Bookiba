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
                    status = OrderStatus.fromBackendString(orderWithItems.order.status),
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

    override suspend fun syncOrders() {
        try {
            val response = bookibaApi.getOrders()
            val orderEntities = mutableListOf<OrderEntity>()
            val itemEntities = mutableListOf<OrderItemEntity>()

            response.orders.forEach { networkOrder ->
                val dateMs = try {
                    java.time.LocalDateTime.parse(networkOrder.createdAt).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                orderEntities.add(
                    OrderEntity(
                        id = networkOrder.id,
                        dateMs = dateMs,
                        totalAmount = networkOrder.totalAmount,
                        status = networkOrder.status
                    )
                )

                networkOrder.items.forEach { item ->
                    itemEntities.add(
                        OrderItemEntity(
                            orderId = networkOrder.id,
                            bookId = item.bookId,
                            title = item.title,
                            author = "", // backend might not send author, fallback empty
                            coverUrl = "", // fallback empty
                            priceKsh = item.priceKsh,
                            quantity = item.quantity
                        )
                    )
                }
            }
            if (orderEntities.isNotEmpty()) {
                orderDao.insertOrders(orderEntities)
                orderDao.insertOrderItems(itemEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun createOrder(totalAmount: Long, items: List<CartItem>, paymentMethod: String, phoneNumber: String, shippingAddress: String): String? {
        // 1. Sync with backend API
        val networkItems = items.map {
            co.booknook.core.network.model.NetworkOrderItemRequest(
                bookId = it.bookId,
                quantity = it.quantity
            )
        }
        
        val response = bookibaApi.createOrder(
            NetworkCheckoutRequest(
                items = networkItems,
                shippingAddress = shippingAddress,
                paymentMethod = paymentMethod,
                phoneNumber = phoneNumber
            )
        )

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

        return response.authorizationUrl
    }
}
