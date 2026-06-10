package co.booknook.core.domain.model

data class Order(
    val id: String,
    val dateMs: Long,
    val totalAmount: Long,
    val status: OrderStatus,
    val items: List<OrderItem>
)
