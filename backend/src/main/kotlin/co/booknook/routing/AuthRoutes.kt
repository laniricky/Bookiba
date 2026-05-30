package co.booknook.routing

import co.booknook.database.models.Users
import co.booknook.security.JwtConfig
import co.booknook.security.PasswordHash
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String, val name: String)

fun Route.authRoutes() {
    route("/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()

            val existingUser = transaction {
                Users.select { Users.email eq request.email }.firstOrNull()
            }

            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                return@post
            }

            val userId = UUID.randomUUID().toString()
            val hashedPassword = PasswordHash.hashPassword(request.password)

            transaction {
                Users.insert {
                    it[id] = userId
                    it[name] = request.name
                    it[email] = request.email
                    it[passwordHash] = hashedPassword
                }
            }

            val token = JwtConfig.generateToken(userId)
            call.respond(HttpStatusCode.Created, AuthResponse(token, userId, request.name))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = transaction {
                Users.select { Users.email eq request.email }.firstOrNull()
            }

            if (user == null || !PasswordHash.checkPassword(request.password, user[Users.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
                return@post
            }

            val token = JwtConfig.generateToken(user[Users.id])
            call.respond(AuthResponse(token, user[Users.id], user[Users.name]))
        }
    }
}
