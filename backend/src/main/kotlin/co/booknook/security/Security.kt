package co.booknook.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*

object JwtConfig {
    private const val secret = "bookiba-super-secret-key-for-dev"
    private const val issuer = "bookiba.co"
    private const val validityInMs = 36_000_00 * 24 * 7 // 7 days
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    fun generateToken(userId: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("id", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}

object PasswordHash {
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun checkPassword(password: String, hashed: String): Boolean {
        return BCrypt.checkpw(password, hashed)
    }
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Bookiba API"
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("id").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
