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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class ThemeDto(
    val id: Int,
    val name: String,
    val tags: List<String> = emptyList(),
    val sortOrder: Int
)

@Serializable
data class ThemesResponse(
    val themes: List<ThemeDto>
)

@Serializable
data class CreateThemeRequest(
    val name: String,
    val tags: List<String> = emptyList(),
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
                            tags = it[Themes.tags]?.split(",")?.map { t -> t.trim() }?.filter { t -> t.isNotEmpty() } ?: emptyList(),
                            sortOrder = it[Themes.sortOrder]
                        )
                    }
            }

            // Seed defaults when the table is empty so Explore always shows something
            if (themes.isEmpty()) {
                val defaults = listOf(
                    Triple("Keep me up all night", listOf("thriller", "mystery", "suspense"), 0),
                    Triple("Make me 1% better", listOf("business", "self-help"), 1),
                    Triple("Escape reality", listOf("fantasy", "sci-fi"), 2),
                    Triple("Cry your eyes out", listOf("romance", "drama"), 3),
                    Triple("Vintage aesthetic", listOf("rare", "classic"), 4),
                    Triple("Deep thoughts", listOf("philosophy"), 5)
                )
                transaction {
                    defaults.forEach { (name, tagsList, order) ->
                        Themes.insert {
                            it[Themes.name] = name
                            it[Themes.tags] = tagsList.joinToString(",")
                            it[sortOrder] = order
                            it[isActive] = true
                        }
                    }
                }
                call.respond(ThemesResponse(
                    themes = defaults.mapIndexed { idx, (name, tagsList, order) ->
                        ThemeDto(id = idx + 1, name = name, tags = tagsList, sortOrder = order)
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
                val stmt = Themes.insert {
                    it[name] = body.name
                    it[tags] = body.tags.joinToString(",").takeIf { s -> s.isNotEmpty() }
                    it[sortOrder] = body.sortOrder
                    it[isActive] = true
                }
                stmt[Themes.id]
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to newId))
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
