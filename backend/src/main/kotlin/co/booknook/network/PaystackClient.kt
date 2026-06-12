package co.booknook.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PaystackInitializeRequest(
    val email: String,
    val amount: Long, // in minor currency (e.g. kobo or cents). For Ksh, multiply by 100.
    val reference: String
)

@Serializable
data class PaystackInitializeResponse(
    val status: Boolean,
    val message: String,
    val data: PaystackInitializeData? = null
)

@Serializable
data class PaystackInitializeData(
    @SerialName("authorization_url") val authorizationUrl: String,
    @SerialName("access_code") val accessCode: String,
    val reference: String
)

object PaystackClient {
    private val secretKey = System.getenv("PAYSTACK_SECRET_KEY") ?: "sk_test_placeholder"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun initializeTransaction(email: String, amountKsh: Long, reference: String): String? {
        return try {
            val amountInCents = amountKsh * 100
            val response: PaystackInitializeResponse = client.post("https://api.paystack.co/transaction/initialize") {
                header(HttpHeaders.Authorization, "Bearer $secretKey")
                contentType(ContentType.Application.Json)
                setBody(PaystackInitializeRequest(email, amountInCents, reference))
            }.body()

            if (response.status) {
                response.data?.authorizationUrl
            } else {
                println("Paystack init failed: ${response.message}")
                null
            }
        } catch (e: Exception) {
            println("Paystack client error: ${e.message}")
            null
        }
    }
}
