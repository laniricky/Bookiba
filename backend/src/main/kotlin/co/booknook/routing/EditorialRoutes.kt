package co.booknook.routing

import co.booknook.database.models.Editorials
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class EditorialDto(
    val id: String,
    val label: String,
    val imageUrl: String?,
    val queryTag: String,
    val sortOrder: Int
)

@Serializable
data class CreateEditorialRequest(
    val label: String,
    val imageUrl: String? = null,
    val queryTag: String,
    val sortOrder: Int = 0
)

fun Route.editorialRoutes() {
    route("/editorials") {
        // Public: list all active editorials for the Story Tray
        get {
            val editorials = transaction {
                Editorials.select { Editorials.isActive eq true }
                    .orderBy(Editorials.sortOrder, SortOrder.ASC)
                    .map {
                        EditorialDto(
                            id = it[Editorials.id],
                            label = it[Editorials.label],
                            imageUrl = it[Editorials.imageUrl],
                            queryTag = it[Editorials.queryTag],
                            sortOrder = it[Editorials.sortOrder]
                        )
                    }
            }

            // Seed defaults if the table is empty so the tray always shows something
            if (editorials.isEmpty()) {
                val defaults = listOf(
                    Triple("New", null, "new"),
                    Triple("Staff Picks", null, "staff picks"),
                    Triple("Fiction", null, "fiction"),
                    Triple("Philosophy", null, "philosophy"),
                    Triple("Vintage", null, "vintage")
                )
                transaction {
                    defaults.forEachIndexed { idx, (label, img, tag) ->
                        Editorials.insert {
                            it[id] = UUID.randomUUID().toString()
                            it[Editorials.label] = label
                            it[imageUrl] = img
                            it[queryTag] = tag
                            it[sortOrder] = idx
                            it[isActive] = true
                        }
                    }
                }
                call.respond(mapOf("editorials" to defaults.mapIndexed { idx, (label, img, tag) ->
                    EditorialDto(UUID.randomUUID().toString(), label, img, tag, idx)
                }))
                return@get
            }

            call.respond(mapOf("editorials" to editorials))
        }

        // Admin: create a new editorial pick
        post {
            val body = call.receive<CreateEditorialRequest>()
            val newId = UUID.randomUUID().toString()
            transaction {
                Editorials.insert {
                    it[id] = newId
                    it[label] = body.label
                    it[imageUrl] = body.imageUrl
                    it[queryTag] = body.queryTag
                    it[sortOrder] = body.sortOrder
                    it[isActive] = true
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to newId))
        }

        // Admin: delete an editorial pick
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            transaction { Editorials.deleteWhere { Editorials.id eq id } }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Deleted"))
        }
    }
}
