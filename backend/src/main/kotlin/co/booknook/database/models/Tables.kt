package co.booknook.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : Table("users") {
    val id = varchar("id", 36)
    val name = varchar("name", 100)
    val email = varchar("email", 150).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object Books : Table("books") {
    val id = varchar("id", 36)
    val title = varchar("title", 200)
    val author = varchar("author", 150)
    val description = text("description").nullable()
    val priceKsh = long("price_ksh")
    val condition = varchar("condition", 50).nullable()
    val coverUrl = varchar("cover_url", 500)
    val imageUrls = text("image_urls").nullable() // Comma separated for simplicity
    val category = varchar("category", 100)
    val edition = varchar("edition", 100).nullable()
    val publisher = varchar("publisher", 150).nullable()
    val genre = varchar("genre", 100).nullable()
    val sellerId = varchar("seller_id", 36) // Optional relation
    val isRare = bool("is_rare").default(false)
    val isFeatured = bool("is_featured").default(false)
    val isStaffPick = bool("is_staff_pick").default(false)
    val tags = text("tags").nullable() // Comma separated
    val inventoryCount = integer("inventory_count").default(0)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object Orders : Table("orders") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(Users.id)
    val totalAmount = long("total_amount")
    val status = varchar("status", 50) // PROCESSING, SHIPPED, DELIVERED
    val paymentMethod = varchar("payment_method", 50) // MPESA, CARD
    val shippingAddress = text("shipping_address")
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val orderId = varchar("order_id", 36).references(Orders.id)
    val bookId = varchar("book_id", 36).references(Books.id)
    val quantity = integer("quantity")
    val priceKsh = long("price_ksh")
}

object Wishlists : Table("wishlists") {
    val userId = varchar("user_id", 36).references(Users.id)
    val bookId = varchar("book_id", 36).references(Books.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(userId, bookId)
}
