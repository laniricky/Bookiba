package co.booknook.core.data.repository

import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.WishlistRepository
import co.booknook.core.network.api.BookibaApi
import co.booknook.core.network.model.NetworkWishlistRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkWishlistRepository @Inject constructor(
    private val api: BookibaApi
) : WishlistRepository {

    override fun getWishlist(): Flow<List<Book>> = flow {
        try {
            val response = api.getWishlist()
            val books = response.books.map { networkBook ->
                Book(
                    id = networkBook.id,
                    title = networkBook.title,
                    author = networkBook.author,
                    priceKsh = networkBook.priceKsh,
                    coverUrl = networkBook.coverUrl ?: "",
                    category = networkBook.category ?: "",
                    description = networkBook.description,
                    condition = networkBook.condition,
                    imageUrls = networkBook.imageUrls ?: emptyList(),
                    edition = networkBook.edition,
                    publisher = networkBook.publisher,
                    genre = networkBook.genre,
                    sellerId = "",
                    isRare = networkBook.isRare ?: false,
                    isFeatured = networkBook.isFeatured ?: false,
                    isStaffPick = networkBook.isStaffPick ?: false,
                    tags = networkBook.tags ?: emptyList(),
                    inventoryCount = networkBook.inventoryCount ?: 0,
                    averageRating = networkBook.averageRating ?: 0.0,
                    reviewCount = networkBook.reviewCount ?: 0
                )
            }
            emit(books)
        } catch (e: Exception) {
            emit(emptyList()) // Handle better in a real app
        }
    }

    override suspend fun addToWishlist(bookId: String): Result<Unit> {
        return try {
            api.addToWishlist(NetworkWishlistRequest(bookId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromWishlist(bookId: String): Result<Unit> {
        return try {
            api.removeFromWishlist(bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkWishlistStatus(bookId: String): Boolean {
        return try {
            val response = api.getWishlist()
            response.books.any { it.id == bookId }
        } catch (e: Exception) {
            false
        }
    }
}
