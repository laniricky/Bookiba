package co.booknook.core.database.model

import androidx.room.Entity

@Entity(tableName = "order_items", primaryKeys = ["orderId", "bookId"])
data class OrderItemEntity(
    val orderId: String,
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val priceKsh: Long,
    val quantity: Int
)
