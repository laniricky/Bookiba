package co.booknook.routing

import co.booknook.database.models.Orders
import co.booknook.database.models.UserTokens
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

@Serializable
data class FcmTokenRequest(val fcmToken: String)

@Serializable
data class NotifyOrderRequest(val orderId: String, val status: String)

fun Route.fcmRoutes() {
    authenticate("auth-jwt") {
        post("/fcm-token") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            
            val request = call.receive<FcmTokenRequest>()
            
            transaction {
                val existing = UserTokens.select { 
                    (UserTokens.userId eq userId) and (UserTokens.fcmToken eq request.fcmToken) 
                }.singleOrNull()
                
                if (existing == null) {
                    UserTokens.insert {
                        it[UserTokens.userId] = userId
                        it[UserTokens.fcmToken] = request.fcmToken
                        it[updatedAt] = LocalDateTime.now()
                    }
                } else {
                    UserTokens.update({ (UserTokens.userId eq userId) and (UserTokens.fcmToken eq request.fcmToken) }) {
                        it[updatedAt] = LocalDateTime.now()
                    }
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
        }
    }

    // Internal webhook endpoint for PHP dashboard
    post("/internal/notify-order") {
        val expectedSecret = System.getenv("INTERNAL_WEBHOOK_SECRET") ?: "my-internal-secret"
        val authHeader = call.request.header("Authorization")
        if (authHeader != "Bearer $expectedSecret") {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            return@post
        }

        val request = call.receive<NotifyOrderRequest>()
        
        // 1. Get the user associated with this order
        val userIdToNotify = transaction {
            Orders.select { Orders.id eq request.orderId }
                .map { it[Orders.userId] }
                .singleOrNull()
        }

        if (userIdToNotify == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            return@post
        }

        // 2. Get the latest FCM token for this user
        val tokens = transaction {
            UserTokens.select { UserTokens.userId eq userIdToNotify }
                .orderBy(UserTokens.updatedAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .map { it[UserTokens.fcmToken] }
        }

        if (tokens.isEmpty()) {
            call.respond(HttpStatusCode.OK, mapOf("status" to "skipped", "reason" to "No FCM token for user"))
            return@post
        }

        try {
            val notificationTitle = "Order Update"
            val notificationBody = when (request.status) {
                "Processing" -> "Your order #${request.orderId.take(8)} is now being processed."
                "Shipped"    -> "Your order #${request.orderId.take(8)} has shipped! 🚚"
                "Delivered"  -> "Your order #${request.orderId.take(8)} has been delivered. 📦"
                "Cancelled"  -> "Your order #${request.orderId.take(8)} was cancelled."
                else         -> "Order #${request.orderId.take(8)} status changed to ${request.status}"
            }

            var sentCount = 0
            for (token in tokens) {
                val message = Message.builder()
                    // Notification payload → Android shows the notification in ALL app states
                    .setNotification(
                        Notification.builder()
                            .setTitle(notificationTitle)
                            .setBody(notificationBody)
                            .build()
                    )
                    // Data payload → available in onMessageReceived for deep-linking
                    .putData("orderId", request.orderId)
                    .putData("status", request.status)
                    .putData("target_route", "orders")
                    .setToken(token)
                    .build()

                FirebaseMessaging.getInstance().send(message)
                sentCount++
            }
            call.respond(HttpStatusCode.OK, mapOf("status" to "success", "sent" to sentCount))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.localizedMessage))
        }
    }

    // Debug endpoint — returns Firebase init status and token count, no side effects
    get("/internal/debug-fcm") {
        val expectedSecret = System.getenv("INTERNAL_WEBHOOK_SECRET") ?: "my-internal-secret"
        val authHeader = call.request.header("Authorization")
        if (authHeader != "Bearer $expectedSecret") {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            return@get
        }

        val firebaseInitialized = try { FirebaseApp.getApps().isNotEmpty() } catch (e: Exception) { false }
        val totalTokenCount = try { transaction { UserTokens.selectAll().count() } } catch (e: Exception) { -1L }
        val recentTokens = try {
            transaction {
                UserTokens.selectAll()
                    .orderBy(UserTokens.updatedAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(5)
                    .map { mapOf("userId" to it[UserTokens.userId], "updatedAt" to it[UserTokens.updatedAt].toString()) }
            }
        } catch (e: Exception) { listOf(mapOf("error" to e.localizedMessage)) }

        call.respond(HttpStatusCode.OK, mapOf(
            "firebaseInitialized" to firebaseInitialized,
            "totalFcmTokens" to totalTokenCount,
            "recentTokens" to recentTokens
        ))
    }
}
