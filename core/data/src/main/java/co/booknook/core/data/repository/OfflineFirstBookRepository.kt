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
        val response = bookibaApi.getFeaturedBooks()
        emit(response.books.map { it.toDomain() })
    }

    override fun getNewArrivals(): Flow<List<Book>> = flow {
        val response = bookibaApi.getBooks(page = 1, pageSize = 10)
        emit(response.books.map { it.toDomain() })
    }

    override fun getStaffPicks(): Flow<List<Book>> = flow {
        val response = bookibaApi.getStaffPickBooks()
        emit(response.books.map { it.toDomain() })
    }

    override fun getBooksByGenre(genre: String): Flow<List<Book>> = flow {
        try {
            val response = bookibaApi.getBooks(genre = genre)
            emit(response.books.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getBookById(id: String): Flow<Book?> = flow {
        try {
            val response = bookibaApi.getBookDetails(id)
            emit(response.toDomain())
        } catch (e: Exception) {
            bookDao.getBookById(id).collect { emit(it?.toDomain()) }
        }
    }

    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            val response = bookibaApi.getBooks(search = query)
            response.books.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun checkout(items: List<Triple<String, Int, Double>>): Boolean {
        return try {
            val cartItems = items.map { (id, quantity, _) ->
                co.booknook.core.network.model.NetworkOrderItemRequest(bookId = id, quantity = quantity) 
            }
            val request = co.booknook.core.network.model.NetworkCheckoutRequest(
                items = cartItems,
                shippingAddress = "Nairobi, Kenya",
                paymentMethod = "CARD"
            )
            val response = bookibaApi.createOrder(request)
            response.orderId != null
        } catch (e: Exception) {
            false
        }
    }
}
