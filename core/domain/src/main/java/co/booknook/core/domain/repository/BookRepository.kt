package co.booknook.core.domain.repository

import co.booknook.core.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getFeaturedBooks(): Flow<List<Book>>
    fun getNewArrivals(): Flow<List<Book>>
    fun getStaffPicks(): Flow<List<Book>>
    fun getBooksByGenre(genre: String): Flow<List<Book>>
    fun getBookById(id: String): Flow<Book?>
    suspend fun searchBooks(query: String): List<Book>
    suspend fun checkout(items: List<Pair<String, Int>>, totalAmount: Double): Boolean
}
