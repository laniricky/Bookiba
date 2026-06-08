package co.booknook

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.http.HttpStatusCode
import co.booknook.security.configureSecurity
import co.booknook.routing.configureRouting

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to cause.localizedMessage))
        }
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // For dev only
    }

    co.booknook.database.DatabaseFactory.init()
    configureSecurity()
    configureRouting()
}
