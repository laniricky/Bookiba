package co.booknook.core.domain.model

data class CartItem(
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val priceKsh: Long,
    val quantity: Int
)
