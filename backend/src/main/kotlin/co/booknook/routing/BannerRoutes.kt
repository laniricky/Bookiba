package co.booknook.routing

import co.booknook.database.models.Banners
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class BannerDto(
    val id: String,
    val imageUrl: String,
    val title: String?,
    val subtitle: String?,
    val sortOrder: Int
)

fun Route.bannerRoutes() {
    route("/banners") {
        get {
            val banners = transaction {
                Banners.select { Banners.isActive eq 1 }
                    .orderBy(Banners.sortOrder, SortOrder.ASC)
                    .map {
                        BannerDto(
                            id = it[Banners.id],
                            imageUrl = it[Banners.imageUrl],
                            title = it[Banners.title],
                            subtitle = it[Banners.subtitle],
                            sortOrder = it[Banners.sortOrder]
                        )
                    }
            }
            call.respond(mapOf("banners" to banners))
        }
    }
}
