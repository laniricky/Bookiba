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
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.auth.oauth2.GoogleCredentials

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

    val envPath = System.getenv("FIREBASE_CREDENTIALS_PATH")
    val possiblePaths = listOfNotNull(
        envPath,
        "/etc/secrets/firebase-service-account.json",
        "firebase-service-account.json"
    )

    var serviceAccount: java.io.InputStream? = null
    for (path in possiblePaths) {
        val file = java.io.File(path)
        if (file.exists()) {
            serviceAccount = file.inputStream()
            println("Loading firebase credentials from $path")
            break
        }
    }

    if (serviceAccount == null) {
        serviceAccount = this::class.java.classLoader.getResourceAsStream("firebase-service-account.json")
        if (serviceAccount != null) {
            println("Loading firebase credentials from classpath")
        }
    }

    if (serviceAccount != null) {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    } else {
        println("WARNING: firebase-service-account.json not found in secrets or resources!")
    }

    configureSecurity()
    configureRouting()
}
