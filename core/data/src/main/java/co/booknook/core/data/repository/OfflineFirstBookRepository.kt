package co.booknook.core.data.repository

import co.booknook.core.data.model.toDomain
import co.booknook.core.data.model.toEntity
import co.booknook.core.database.dao.BookDao
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.network.api.BookibaApi
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OfflineFirstBookRepository @Inject constructor(
    private val bookibaApi: BookibaApi,
    private val bookDao: BookDao
) : BookRepository {

    override suspend fun getFeaturedBooks(): List<Book> {
        return try {
            val networkBooks = bookibaApi.getFeaturedBooks()
            val entities = networkBooks.map { it.toEntity() }
            bookDao.insertBooks(entities)
            
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            // Fallback to offline data
            bookDao.getAllBooks().first().map { it.toDomain() }
        }
    }

    override suspend fun getBookById(id: String): Book? {
        return try {
            // Try fetching from network to get the freshest data
            val networkBook = bookibaApi.getBookDetails(id)
            bookDao.insertBook(networkBook.toEntity())
            networkBook.toDomain()
        } catch (e: Exception) {
            // Fallback to database
            bookDao.getBookById(id).first()?.toDomain()
        }
    }

    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            val networkBooks = bookibaApi.getBooks(query = query)
            bookDao.insertBooks(networkBooks.map { it.toEntity() })
            networkBooks.map { it.toDomain() }
        } catch (e: Exception) {
            // Fallback to local filtering
            bookDao.getAllBooks().first()
                .map { it.toDomain() }
                .filter { it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true) }
        }
    }
}
