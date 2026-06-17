package co.booknook.routing

import co.booknook.database.models.Themes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ThemeDto(
    val id: Int,
    val name: String,
    val tag: String?,
    val sortOrder: Int
)

@Serializable
data class ThemesResponse(
    val themes: List<ThemeDto>
)

@Serializable
data class CreateThemeRequest(
    val name: String,
    val tag: String? = null,
    val sortOrder: Int = 0
)

fun Route.themeRoutes() {
    route("/themes") {

        // GET /api/v1/themes — public, returns active themes ordered by sort_order
        get {
            val themes = transaction {
                Themes.select { Themes.isActive eq true }
                    .orderBy(Themes.sortOrder, SortOrder.ASC)
                    .map {
                        ThemeDto(
                            id = it[Themes.id],
                            name = it[Themes.name],
                            tag = it[Themes.tag],
                            sortOrder = it[Themes.sortOrder]
                        )
                    }
            }

            // Seed defaults when the table is empty so Explore always shows something
            if (themes.isEmpty()) {
                val defaults = listOf(
                    Triple("Keep me up all night", "thriller", 0),
                    Triple("Make me 1% better", "business", 1),
                    Triple("Escape reality", "fantasy", 2),
                    Triple("Cry your eyes out", "romance", 3),
                    Triple("Vintage aesthetic", "rare", 4),
                    Triple("Deep thoughts", "philosophy", 5)
                )
                transaction {
                    defaults.forEach { (name, tag, order) ->
                        Themes.insert {
                            it[Themes.name] = name
                            it[Themes.tag] = tag
                            it[sortOrder] = order
                            it[isActive] = true
                        }
                    }
                }
                call.respond(ThemesResponse(
                    themes = defaults.mapIndexed { idx, (name, tag, order) ->
                        ThemeDto(id = idx + 1, name = name, tag = tag, sortOrder = order)
                    }
                ))
                return@get
            }

            call.respond(ThemesResponse(themes = themes))
        }

        // POST /api/v1/themes — admin: create a theme
        post {
            val body = call.receive<CreateThemeRequest>()
            val newId = transaction {
                Themes.insertAndGetId {
                    it[name] = body.name
                    it[tag] = body.tag
                    it[sortOrder] = body.sortOrder
                    it[isActive] = true
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to newId.value))
        }

        // DELETE /api/v1/themes/{id} — admin: remove a theme
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            transaction { Themes.deleteWhere { Themes.id eq id } }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Deleted"))
        }
    }
}
