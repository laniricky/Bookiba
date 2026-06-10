package co.booknook.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val dateMs: Long,
    val totalAmount: Long,
    val status: String
)
