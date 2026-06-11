package co.booknook.routing

import co.booknook.database.models.Orders
import co.booknook.database.models.OrderItems
import co.booknook.database.models.Books
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val shippingAddress: String,
    val paymentMethod: String // MPESA or CARD
)

@Serializable
data class CreateOrderResponse(
    val orderId: String,
    val total: Long,
    val status: String
)

@Serializable
data class OrderItemRequest(val bookId: String, val quantity: Int)

@Serializable
data class OrderDto(
    val id: String,
    val totalAmount: Long,
    val status: String,
    val paymentMethod: String,
    val shippingAddress: String,
    val createdAt: String,
    val items: List<OrderItemDto>
)

@Serializable
data class OrderItemDto(val bookId: String, val title: String, val quantity: Int, val priceKsh: Long)

fun Route.orderRoutes() {
    authenticate("auth-jwt") {
        route("/orders") {

            // POST /api/v1/orders — place an order
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateOrderRequest>()

                // Calculate total from DB prices (never trust client-side prices)
                val bookPrices = transaction {
                    Books.select { Books.id inList request.items.map { it.bookId } }
                        .associate { it[Books.id] to it[Books.priceKsh] }
                }

                val total = request.items.sumOf { item ->
                    (bookPrices[item.bookId] ?: 0L) * item.quantity
                }

                val orderId = UUID.randomUUID().toString()

                transaction {
                    Orders.insert {
                        it[id] = orderId
                        it[Orders.userId] = userId
                        it[totalAmount] = total
                        it[status] = "PROCESSING"
                        it[paymentMethod] = request.paymentMethod
                        it[shippingAddress] = request.shippingAddress
                    }

                    request.items.forEach { item ->
                        OrderItems.insert {
                            it[OrderItems.orderId] = orderId
                            it[OrderItems.bookId] = item.bookId
                            it[OrderItems.quantity] = item.quantity
                            it[OrderItems.priceKsh] = bookPrices[item.bookId] ?: 0L
                        }
                        
                        val currentStock = Books.select { Books.id eq item.bookId }
                            .firstOrNull()?.get(Books.inventoryCount) ?: 0
                        val newStock = maxOf(0, currentStock - item.quantity)
                        
                        Books.update({ Books.id eq item.bookId }) {
                            it[inventoryCount] = newStock
                        }
                    }
                }

                val responseDto = CreateOrderResponse(
                    orderId = orderId,
                    total = total,
                    status = "PROCESSING"
                )
                call.respond(HttpStatusCode.Created, responseDto)
            }

            // GET /api/v1/orders — list user's orders
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val orders = transaction {
                    Orders.select { Orders.userId eq userId }
                        .orderBy(Orders.createdAt, SortOrder.DESC)
                        .map { orderRow ->
                            val items = (OrderItems innerJoin Books)
                                .select { OrderItems.orderId eq orderRow[Orders.id] }
                                .map { itemRow ->
                                    OrderItemDto(
                                        bookId = itemRow[Books.id],
                                        title = itemRow[Books.title],
                                        quantity = itemRow[OrderItems.quantity],
                                        priceKsh = itemRow[OrderItems.priceKsh]
                                    )
                                }
                            OrderDto(
                                id = orderRow[Orders.id],
                                totalAmount = orderRow[Orders.totalAmount],
                                status = orderRow[Orders.status],
                                paymentMethod = orderRow[Orders.paymentMethod],
                                shippingAddress = orderRow[Orders.shippingAddress],
                                createdAt = orderRow[Orders.createdAt].toString(),
                                items = items
                            )
                        }
                }

                call.respond(mapOf("orders" to orders))
            }

            // GET /api/v1/orders/{id}
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val orderId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val order = transaction {
                    val orderRow = Orders.select {
                        (Orders.id eq orderId) and (Orders.userId eq userId)
                    }.firstOrNull() ?: return@transaction null

                    val items = (OrderItems innerJoin Books)
                        .select { OrderItems.orderId eq orderId }
                        .map { itemRow ->
                            OrderItemDto(
                                bookId = itemRow[Books.id],
                                title = itemRow[Books.title],
                                quantity = itemRow[OrderItems.quantity],
                                priceKsh = itemRow[OrderItems.priceKsh]
                            )
                        }

                    OrderDto(
                        id = orderRow[Orders.id],
                        totalAmount = orderRow[Orders.totalAmount],
                        status = orderRow[Orders.status],
                        paymentMethod = orderRow[Orders.paymentMethod],
                        shippingAddress = orderRow[Orders.shippingAddress],
                        createdAt = orderRow[Orders.createdAt].toString(),
                        items = items
                    )
                }

                if (order == null) call.respond(HttpStatusCode.NotFound)
                else call.respond(order)
            }
        }
    }
}
