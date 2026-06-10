package co.booknook.core.domain.repository

import co.booknook.core.domain.model.Book
import co.booknook.core.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItem>>
    fun isInCart(bookId: String): Flow<Boolean>
    suspend fun addToCart(book: Book)
    suspend fun updateQuantity(bookId: String, quantity: Int)
    suspend fun removeFromCart(bookId: String)
    suspend fun clearCart()
    fun getCartCount(): Flow<Int>
}
