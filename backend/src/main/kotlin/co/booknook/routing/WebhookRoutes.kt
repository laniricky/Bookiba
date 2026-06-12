package co.booknook.routing

import co.booknook.database.models.Orders
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Route.webhookRoutes() {
    route("/webhooks") {
        post("/paystack") {
            val secretKey = System.getenv("PAYSTACK_SECRET_KEY") ?: "sk_test_placeholder"
            val signature = call.request.header("x-paystack-signature")
            val body = call.receiveText()

            // Verify Paystack Signature
            val mac = Mac.getInstance("HmacSHA512")
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA512")
            mac.init(secretKeySpec)
            val hash = mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }

            if (hash != signature) {
                return@post call.respond(HttpStatusCode.Unauthorized, "Invalid signature")
            }

            try {
                val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
                val event = json["event"]?.jsonPrimitive?.content
                val data = json["data"]?.jsonObject
                val reference = data?.get("reference")?.jsonPrimitive?.content

                if (event == "charge.success" && reference != null) {
                    transaction {
                        Orders.update({ Orders.id eq reference }) {
                            it[status] = "PAID"
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.OK)
                }
            } catch (e: Exception) {
                println("Webhook processing error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Error processing webhook")
            }
        }
    }
}
