package co.booknook.routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("📚 Bookiba API v1 is running!")
        }

        get("/health") {
            call.respondText("OK")
        }

        route("/api/v1") {
            authRoutes()
            bookRoutes()
            orderRoutes()
            wishlistRoutes()
            userRoutes()
            reelRoutes()
            bannerRoutes()
        }
    }
}
