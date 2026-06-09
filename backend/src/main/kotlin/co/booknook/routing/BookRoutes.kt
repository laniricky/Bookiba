package co.booknook.routing

import co.booknook.database.models.Books
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class BookDto(
    val id: String,
    val title: String,
    val author: String,
    val description: String?,
    val priceKsh: Long,
    val condition: String?,
    val coverUrl: String,
    val imageUrls: List<String>,
    val category: String,
    val genre: String?,
    val edition: String?,
    val publisher: String?,
    val isRare: Boolean,
    val isFeatured: Boolean,
    val isStaffPick: Boolean,
    val tags: List<String>,
    val inventoryCount: Int
)

fun ResultRow.toBookDto() = BookDto(
    id = this[Books.id],
    title = this[Books.title],
    author = this[Books.author],
    description = this[Books.description],
    priceKsh = this[Books.priceKsh],
    condition = this[Books.condition],
    coverUrl = this[Books.coverUrl],
    imageUrls = this[Books.imageUrls]?.split(",")?.map { it.trim() } ?: emptyList(),
    category = this[Books.category],
    genre = this[Books.genre],
    edition = this[Books.edition],
    publisher = this[Books.publisher],
    isRare = this[Books.isRare],
    isFeatured = this[Books.isFeatured],
    isStaffPick = this[Books.isStaffPick],
    tags = this[Books.tags]?.split(",")?.map { it.trim() } ?: emptyList(),
    inventoryCount = this[Books.inventoryCount]
)

@Serializable
data class BooksResponse(
    val books: List<BookDto>,
    val page: Int,
    val pageSize: Int
)

fun Route.bookRoutes() {
    route("/books") {

        // GET /api/v1/books?search=&genre=&page=1&pageSize=20
        get {
            val search = call.request.queryParameters["search"]
            val genre = call.request.queryParameters["genre"]
            val featured = call.request.queryParameters["featured"]?.toBooleanStrictOrNull()
            val staffPick = call.request.queryParameters["staffPick"]?.toBooleanStrictOrNull()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val books = transaction {
                var query = Books.selectAll()
                if (!search.isNullOrBlank()) {
                    query = query.andWhere {
                        (Books.title.lowerCase() like "%${search.lowercase()}%") or
                        (Books.author.lowerCase() like "%${search.lowercase()}%")
                    }
                }
                if (!genre.isNullOrBlank()) query = query.andWhere { Books.genre eq genre }
                if (featured == true) query = query.andWhere { Books.isFeatured eq true }
                if (staffPick == true) query = query.andWhere { Books.isStaffPick eq true }

                query
                    .orderBy(Books.createdAt, SortOrder.DESC)
                    .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                    .map { it.toBookDto() }
            }

            call.respond(BooksResponse(books, page, pageSize))
        }

        // GET /api/v1/books/featured
        get("/featured") {
            val books = transaction {
                Books.select { Books.isFeatured eq true }
                    .orderBy(Books.createdAt, SortOrder.DESC)
                    .limit(10)
                    .map { it.toBookDto() }
            }
            call.respond(mapOf("books" to books))
        }

        // GET /api/v1/books/staff-pick
        get("/staff-pick") {
            val books = transaction {
                Books.select { Books.isStaffPick eq true }
                    .limit(5)
                    .map { it.toBookDto() }
            }
            call.respond(mapOf("books" to books))
        }

        // GET /api/v1/books/{id}
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing book ID"))
            val book = transaction {
                Books.select { Books.id eq id }.firstOrNull()?.toBookDto()
            }
            if (book == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Book not found"))
            } else {
                call.respond(book)
            }
        }

        // GET /api/v1/books/genres
        get("/genres") {
            val genres = transaction {
                Books.slice(Books.genre).selectAll()
                    .mapNotNull { it[Books.genre] }
                    .distinct()
                    .sorted()
            }
            call.respond(mapOf("genres" to genres))
        }
    }
}
