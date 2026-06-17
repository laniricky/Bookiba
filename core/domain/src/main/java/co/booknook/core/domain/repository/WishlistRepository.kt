package co.booknook.core.domain.repository

import co.booknook.core.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun getWishlist(): Flow<List<Book>>
    suspend fun addToWishlist(bookId: String): Result<Unit>
    suspend fun removeFromWishlist(bookId: String): Result<Unit>
    suspend fun checkWishlistStatus(bookId: String): Boolean
}
