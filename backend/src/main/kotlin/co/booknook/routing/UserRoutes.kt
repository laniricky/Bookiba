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

                call.respond(mapOf(
                    "name" to user[Users.name],
                    "email" to user[Users.email],
                    "ordersCount" to stats.first,
                    "wishlistCount" to stats.second,
                    "reviewsCount" to stats.third
                ))
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
                    // Order items are linked to orders, skip for now (orders kept for records)
                    Users.deleteWhere { Users.id eq userId }
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Account deleted successfully"))
            }
        }
    }
}

