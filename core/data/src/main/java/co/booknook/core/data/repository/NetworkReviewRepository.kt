package co.booknook.core.data.repository

import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.domain.model.Review
import co.booknook.core.domain.repository.ReviewRepository
import co.booknook.core.network.api.BookibaApi
import co.booknook.core.network.model.NetworkSubmitReviewRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkReviewRepository @Inject constructor(
    private val api: BookibaApi,
    private val preferencesDataSource: BookibaPreferencesDataSource
) : ReviewRepository {

    override fun getReviews(bookId: String): Flow<List<Review>> = flow {
        try {
            val response = api.getReviews(bookId)
            emit(response.reviews.map { n ->
                Review(
                    id = n.id,
                    userId = n.userId,
                    bookId = n.bookId,
                    rating = n.rating,
                    comment = n.comment,
                    createdAt = n.createdAt
                )
            })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun submitReview(bookId: String, rating: Int, comment: String?): Result<Unit> {
        return try {
            val token = preferencesDataSource.authToken.first()
                ?: return Result.failure(IllegalStateException("Not logged in"))
            api.submitReview(
                bookId = bookId,
                token = "Bearer $token",
                request = NetworkSubmitReviewRequest(rating = rating, comment = comment)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
