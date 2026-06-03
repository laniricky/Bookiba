package co.booknook.core.data.repository

import co.booknook.core.data.model.toDomain
import co.booknook.core.data.model.toEntity
import co.booknook.core.database.dao.BookDao
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.network.api.BookibaApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstBookRepository @Inject constructor(
    private val bookibaApi: BookibaApi,
    private val bookDao: BookDao
) : BookRepository {

    override fun getFeaturedBooks(): Flow<List<Book>> = flow {
        try {
            val response = bookibaApi.getHome()
            if (response.ok) {
                emit(response.data?.featured?.map { it.toDomain() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getNewArrivals(): Flow<List<Book>> = flow {
        try {
            val response = bookibaApi.getHome()
            if (response.ok) {
                emit(response.data?.newArrivals?.map { it.toDomain() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getStaffPicks(): Flow<List<Book>> = flow {
        try {
            val response = bookibaApi.getHome()
            val staffPick = response.data?.staffPick
            if (response.ok && staffPick != null) {
                emit(listOf(staffPick.toDomain()))
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getBooksByGenre(genre: String): Flow<List<Book>> = flow {
        try {
            val response = bookibaApi.getBooks(category = genre)
            if (response.ok) {
                emit(response.data.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getBookById(id: String): Flow<Book?> = flow {
        try {
            val response = bookibaApi.getBookDetails(id)
            if (response.ok) {
                emit(response.data?.toDomain())
            } else {
                // Fallback to local DB
                bookDao.getBookById(id).collect { emit(it?.toDomain()) }
            }
        } catch (e: Exception) {
            bookDao.getBookById(id).collect { emit(it?.toDomain()) }
        }
    }

    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            val response = bookibaApi.getBooks(query = query)
            if (response.ok) response.data.map { it.toDomain() } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun checkout(items: List<Pair<String, Int>>, totalAmount: Double): Boolean {
        return try {
            val cartItems = items.map { (id, quantity) ->
                co.booknook.core.network.model.NetworkCartItem(bookId = id, quantity = quantity, price = totalAmount / items.size) 
                // Note: accurate price requires price per item, simplified for now
            }
            val request = co.booknook.core.network.model.NetworkCheckoutRequest(items = cartItems)
            val response = bookibaApi.checkout(request)
            response.ok
        } catch (e: Exception) {
            false
        }
    }
}
