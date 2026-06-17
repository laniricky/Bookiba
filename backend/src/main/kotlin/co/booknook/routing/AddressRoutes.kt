package co.booknook.routing

import co.booknook.database.models.Addresses
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
data class AddressDto(
    val id: String,
    val label: String,
    val fullAddress: String,
    val isDefault: Boolean
)

@Serializable
data class CreateAddressRequest(
    val label: String,
    val fullAddress: String,
    val isDefault: Boolean = false
)

@Serializable
data class UpdateAddressRequest(
    val label: String? = null,
    val fullAddress: String? = null,
    val isDefault: Boolean? = null
)

fun Route.addressRoutes() {
    authenticate("auth-jwt") {
        route("/addresses") {

            // GET /api/v1/addresses — list all addresses for user
            get {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val addresses = transaction {
                    Addresses.select { Addresses.userId eq userId }
                        .orderBy(Addresses.isDefault, SortOrder.DESC)
                        .orderBy(Addresses.createdAt, SortOrder.ASC)
                        .map {
                            AddressDto(
                                id = it[Addresses.id],
                                label = it[Addresses.label],
                                fullAddress = it[Addresses.fullAddress],
                                isDefault = it[Addresses.isDefault]
                            )
                        }
                }
                call.respond(mapOf("addresses" to addresses))
            }

            // POST /api/v1/addresses — create a new address
            post {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateAddressRequest>()
                val addressId = UUID.randomUUID().toString()

                transaction {
                    // If new address is default, clear existing default
                    if (request.isDefault) {
                        Addresses.update({ Addresses.userId eq userId }) {
                            it[isDefault] = false
                        }
                    }
                    Addresses.insert {
                        it[id] = addressId
                        it[Addresses.userId] = userId
                        it[label] = request.label
                        it[fullAddress] = request.fullAddress
                        it[isDefault] = request.isDefault
                    }
                }

                call.respond(HttpStatusCode.Created, mapOf("addressId" to addressId))
            }

            // PUT /api/v1/addresses/{id} — update an address
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val addressId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateAddressRequest>()

                val updated = transaction {
                    // If setting as default, clear others first
                    if (request.isDefault == true) {
                        Addresses.update({ Addresses.userId eq userId }) {
                            it[isDefault] = false
                        }
                    }
                    Addresses.update({ (Addresses.id eq addressId) and (Addresses.userId eq userId) }) {
                        request.label?.let { l -> it[label] = l }
                        request.fullAddress?.let { a -> it[fullAddress] = a }
                        request.isDefault?.let { d -> it[isDefault] = d }
                    }
                }

                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Address not found"))
                else call.respond(mapOf("message" to "Address updated"))
            }

            // DELETE /api/v1/addresses/{id}
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val addressId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val deleted = transaction {
                    Addresses.deleteWhere {
                        (Addresses.id eq addressId) and (Addresses.userId eq userId)
                    }
                }

                if (deleted == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Address not found"))
                else call.respond(mapOf("message" to "Address deleted"))
            }
        }
    }
}
