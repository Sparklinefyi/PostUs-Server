package postus
import io.ktor.network.sockets.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import postus.endpoints.*
import postus.repositories.*
import postus.controllers.*
import postus.utils.Database
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import kotlinx.serialization.json.*
import java.io.*
import kotlinx.serialization.*

fun main() {
    embeddedServer(Netty, port = 8080, host="localhost", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost()
    }

    install(Sessions) {
        cookie<MySession>("SESSION") {
            // Configure the session cookie settings
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Initialize the database
    Database

    // Create instances of repositories
    val userRepository = UserRepository()
    val linkedAccountRepository = LinkedAccountRepository()

    // Create an instance of UserService
    val userService = UserController(userRepository, linkedAccountRepository)

    // Pass userService to configureAuthRouting
    configureAuthRouting(userService)
    configureMediaRouting()
    configureSocialsRouting()
}

data class MySession(val userId: Int)

