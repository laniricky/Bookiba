package co.booknook.routing

import co.booknook.database.models.Books
import co.booknook.database.models.Reels
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ReelDto(
    val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val bookId: String?,
    val bookTitle: String?,
    val isActive: Boolean,
    val createdAt: String
)

fun Route.reelRoutes() {
    route("/api/reels") {
        get {
            val reels = transaction {
                Reels.join(Books, JoinType.LEFT, onColumn = Reels.bookId, otherColumn = Books.id)
                    .select { Reels.isActive eq true }
                    .orderBy(Reels.createdAt to SortOrder.DESC)
                    .map { row ->
                        ReelDto(
                            id = row[Reels.id],
                            title = row[Reels.title],
                            videoUrl = row[Reels.videoUrl],
                            thumbnailUrl = row[Reels.thumbnailUrl],
                            bookId = row[Reels.bookId],
                            bookTitle = row.getOrNull(Books.title),
                            isActive = row[Reels.isActive],
                            createdAt = row[Reels.createdAt].toString()
                        )
                    }
            }
            call.respond(reels)
        }
    }
}
