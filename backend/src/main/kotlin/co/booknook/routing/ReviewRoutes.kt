package co.booknook.routing

import co.booknook.database.models.Books
import co.booknook.database.models.Reviews
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class ReviewDto(
    val id: String,
    val userId: String,
    val bookId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String
)

@Serializable
data class SubmitReviewRequest(
    val rating: Int,
    val comment: String? = null
)

fun Route.reviewRoutes() {
    route("/books/{bookId}/reviews") {
        
        // GET /api/v1/books/{bookId}/reviews
        get {
            val bookId = call.parameters["bookId"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing book ID"))
            
            val reviews = transaction {
                Reviews.select { Reviews.bookId eq bookId }
                    .orderBy(Reviews.createdAt, SortOrder.DESC)
                    .map {
                        ReviewDto(
                            id = it[Reviews.id],
                            userId = it[Reviews.userId],
                            bookId = it[Reviews.bookId],
                            rating = it[Reviews.rating],
                            comment = it[Reviews.comment],
                            createdAt = it[Reviews.createdAt].toString()
                        )
                    }
            }
            call.respond(mapOf("reviews" to reviews))
        }

        // POST /api/v1/books/{bookId}/reviews (Requires Auth)
        authenticate("auth-jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }
                
                val bookId = call.parameters["bookId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing book ID"))
                val request = call.receiveNullable<SubmitReviewRequest>() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                
                if (request.rating < 1 || request.rating > 5) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Rating must be between 1 and 5"))
                    return@post
                }
                
                // Ensure book exists
                val bookExists = transaction {
                    Books.select { Books.id eq bookId }.count() > 0
                }
                if (!bookExists) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Book not found"))
                    return@post
                }

                // Check if user already reviewed
                val alreadyReviewed = transaction {
                    Reviews.select { (Reviews.bookId eq bookId) and (Reviews.userId eq userId) }.count() > 0
                }
                if (alreadyReviewed) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "You have already reviewed this book"))
                    return@post
                }

                val reviewId = UUID.randomUUID().toString()

                transaction {
                    // Insert the review
                    Reviews.insert {
                        it[id] = reviewId
                        it[Reviews.userId] = userId
                        it[Reviews.bookId] = bookId
                        it[rating] = request.rating
                        it[comment] = request.comment
                    }

                    // Recalculate average rating for the book
                    val allRatings = Reviews.slice(Reviews.rating)
                        .select { Reviews.bookId eq bookId }
                        .map { it[Reviews.rating] }
                    
                    val newCount = allRatings.size
                    val newAverage = if (newCount > 0) allRatings.average() else 0.0

                    // Update book stats
                    Books.update({ Books.id eq bookId }) {
                        it[averageRating] = newAverage
                        it[reviewCount] = newCount
                    }
                }
                
                call.respond(HttpStatusCode.Created, mapOf("message" to "Review submitted successfully", "reviewId" to reviewId))
            }
        }
    }
}
