package co.booknook.core.domain.usecase

import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeaturedBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    operator fun invoke(): Flow<List<Book>> = repository.getFeaturedBooks()
}
