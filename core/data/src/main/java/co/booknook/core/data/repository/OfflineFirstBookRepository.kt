package co.booknook.core.data.repository

import co.booknook.core.data.model.toDomain
import co.booknook.core.data.model.toEntity
import co.booknook.core.database.dao.BookDao
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.network.api.BookibaApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstBookRepository @Inject constructor(
    private val bookibaApi: BookibaApi,
    private val bookDao: BookDao
) : BookRepository {

    override fun getFeaturedBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getNewArrivals(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getStaffPicks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getBooksByGenre(genre: String): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getBookById(id: String): Flow<Book?> {
        return bookDao.getBookById(id).map { it?.toDomain() }
    }

    override suspend fun searchBooks(query: String): List<Book> {
        return emptyList()
    }
}
