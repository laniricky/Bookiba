package co.booknook.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val price: Double,
    val condition: String,
    val coverImageUrl: String,
    val isAvailable: Boolean,
    val sellerId: String,
    val createdAt: Long
)
