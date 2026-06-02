package co.booknook.routing

import co.booknook.database.models.Wishlists
import co.booknook.database.models.Books
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

@Serializable
data class WishlistRequest(val bookId: String)

fun Route.wishlistRoutes() {
    authenticate("auth-jwt") {
        route("/wishlist") {

            // GET /api/v1/wishlist
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val books = transaction {
                    (Wishlists innerJoin Books)
                        .select { Wishlists.userId eq userId }
                        .orderBy(Wishlists.createdAt, SortOrder.DESC)
                        .map { it.toBookDto() }
                }

                call.respond(mapOf("books" to books))
            }

            // POST /api/v1/wishlist
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<WishlistRequest>()

                val alreadyExists = transaction {
                    Wishlists.select {
                        (Wishlists.userId eq userId) and (Wishlists.bookId eq request.bookId)
                    }.count() > 0
                }

                if (alreadyExists) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Already in wishlist"))
                    return@post
                }

                transaction {
                    Wishlists.insert {
                        it[Wishlists.userId] = userId
                        it[bookId] = request.bookId
                    }
                }

                call.respond(HttpStatusCode.Created, mapOf("message" to "Added to wishlist"))
            }

            // DELETE /api/v1/wishlist/{bookId}
            delete("/{bookId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val bookId = call.parameters["bookId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                transaction {
                    Wishlists.deleteWhere {
                        org.jetbrains.exposed.sql.SqlExpressionBuilder.run {
                            (Wishlists.userId eq userId) and (Wishlists.bookId eq bookId)
                        }
                    }
                }

                call.respond(mapOf("message" to "Removed from wishlist"))
            }
        }
    }
}
