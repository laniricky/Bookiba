package co.booknook.routing

import co.booknook.database.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Route.userRoutes() {
    route("/user") {
        authenticate("auth-jwt") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))

                val user = transaction {
                    Users.select { Users.id eq userId }.firstOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@get
                }

                val stats = transaction {
                    val orders = Orders.select { Orders.userId eq userId }.count()
                    val wishlist = Wishlists.select { Wishlists.userId eq userId }.count()
                    val reviews = Reviews.select { Reviews.userId eq userId }.count()
                    Triple(orders, wishlist, reviews)
                }

                call.respond(UserProfileResponse(
                    name = user[Users.name],
                    email = user[Users.email],
                    bio = user[Users.bio],
                    avatarUrl = user[Users.avatarUrl],
                    ordersCount = stats.first,
                    wishlistCount = stats.second,
                    reviewsCount = stats.third
                ))
            }

            put("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val request = call.receive<UpdateProfileRequest>()
                transaction {
                    Users.update({ Users.id eq userId }) {
                        it[name] = request.name
                        it[bio] = request.bio
                        it[avatarUrl] = request.avatarUrl
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Profile updated successfully"))
            }

            put("/email") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val request = call.receive<UpdateEmailRequest>()
                
                val user = transaction { Users.select { Users.id eq userId }.firstOrNull() }
                if (user == null || !co.booknook.security.PasswordHash.checkPassword(request.currentPassword, user[Users.passwordHash])) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Incorrect current password"))
                    return@put
                }
                
                // Check if new email is already taken
                val existingEmail = transaction { Users.select { Users.email eq request.newEmail }.firstOrNull() }
                if (existingEmail != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already in use"))
                    return@put
                }
                
                transaction {
                    Users.update({ Users.id eq userId }) { it[email] = request.newEmail }
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Email updated successfully"))
            }

            put("/password") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val request = call.receive<UpdatePasswordRequest>()
                
                val user = transaction { Users.select { Users.id eq userId }.firstOrNull() }
                if (user == null || !co.booknook.security.PasswordHash.checkPassword(request.currentPassword, user[Users.passwordHash])) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Incorrect current password"))
                    return@put
                }
                
                val newHash = co.booknook.security.PasswordHash.hashPassword(request.newPassword)
                transaction {
                    Users.update({ Users.id eq userId }) { it[passwordHash] = newHash }
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Password updated successfully"))
            }

            // ── Account Deletion ──────────────────────────────────────────────
            delete("/account") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))

                transaction {
                    // Cascade deletes in dependency order
                    Wishlists.deleteWhere { Wishlists.userId eq userId }
                    Reviews.deleteWhere { Reviews.userId eq userId }
                    Addresses.deleteWhere { Addresses.userId eq userId }
                    co.booknook.database.models.UserTokens.deleteWhere { co.booknook.database.models.UserTokens.userId eq userId }
                    // Order items are linked to orders, skip for now (orders kept for records)
                    Users.deleteWhere { Users.id eq userId }
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Account deleted successfully"))
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class UserProfileResponse(
    val name: String,
    val email: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val ordersCount: Long,
    val wishlistCount: Long,
    val reviewsCount: Long
)

@kotlinx.serialization.Serializable
data class UpdateProfileRequest(
    val name: String,
    val bio: String? = null,
    val avatarUrl: String? = null
)

@kotlinx.serialization.Serializable
data class UpdateEmailRequest(
    val currentPassword: String,
    val newEmail: String
)

@kotlinx.serialization.Serializable
data class UpdatePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
