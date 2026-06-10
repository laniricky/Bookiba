package co.booknook.core.data.repository

import co.booknook.core.data.model.toDomain
import co.booknook.core.database.dao.CartDao
import co.booknook.core.database.model.CartItemEntity
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.model.CartItem
import co.booknook.core.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalCartRepository @Inject constructor(
    private val cartDao: CartDao
) : CartRepository {

    override fun getCartItems(): Flow<List<CartItem>> {
        return cartDao.getCartItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun isInCart(bookId: String): Flow<Boolean> {
        return cartDao.getCartItem(bookId).map { it != null }
    }

    override suspend fun addToCart(book: Book) {
        val existingItem = cartDao.getCartItem(book.id).firstOrNull()
        if (existingItem != null) {
            cartDao.updateQuantity(book.id, existingItem.quantity + 1)
        } else {
            val entity = CartItemEntity(
                bookId = book.id,
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                priceKsh = book.priceKsh,
                quantity = 1
            )
            cartDao.upsertItem(entity)
        }
    }

    override suspend fun updateQuantity(bookId: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteItem(bookId)
        } else {
            cartDao.updateQuantity(bookId, quantity)
        }
    }

    override suspend fun removeFromCart(bookId: String) {
        cartDao.deleteItem(bookId)
    }

    override suspend fun clearCart() {
        cartDao.clearCart()
    }

    override fun getCartCount(): Flow<Int> {
        return cartDao.getCartCount()
    }
}
