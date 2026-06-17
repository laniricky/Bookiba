package co.booknook.core.domain.repository

import co.booknook.core.domain.model.Review
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getReviews(bookId: String): Flow<List<Review>>
    suspend fun submitReview(bookId: String, rating: Int, comment: String?): Result<Unit>
}
