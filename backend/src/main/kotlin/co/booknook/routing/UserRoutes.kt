package co.booknook.routing

import co.booknook.database.models.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.userRoutes() {
    route("/user") {
        authenticate {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))

                val user = transaction {
                    Users.select { Users.id eq userId }.firstOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@get
                }

                // In a real app we'd query Orders and Wishlists counts.
                // For now, return 0 for counts.
                call.respond(mapOf(
                    "name" to user[Users.name],
                    "email" to user[Users.email],
                    "ordersCount" to 0,
                    "wishlistCount" to 0,
                    "reviewsCount" to 0
                ))
            }
        }
    }
}
