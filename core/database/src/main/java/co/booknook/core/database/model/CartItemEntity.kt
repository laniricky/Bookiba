package co.booknook.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val priceKsh: Long,
    val quantity: Int,
    val addedAt: Long = System.currentTimeMillis()
)
