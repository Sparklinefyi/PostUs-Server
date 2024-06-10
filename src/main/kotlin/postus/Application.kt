package postus
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
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.sessions.*
import postus.utils.JwtHandler
import postus.workers.startScheduledPostsChecker

import io.github.cdimascio.dotenv.Dotenv
import java.io.File
import java.util.*

fun main() {
    Database
    startScheduledPostsChecker()
    val port = System.getenv("PORT")?.toInt() ?: 8080
    if (port != 8080)
        embeddedServer(Netty, port, module = Application::module)
            .start(wait = true)
    else
        embeddedServer(Netty, port, host = "localhost", module = Application::module)
            .start()
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
            cookie.path = "/" // Applies to the entire application
            cookie.maxAgeInSeconds = 1 * 24 * 60 * 60 // One week
            cookie.httpOnly = true // Mitigates XSS attacks
            cookie.secure = false // Use secure cookies in production
            cookie.extensions["SameSite"] = "None" // Helps mitigate CSRF attacks
        }
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(Authentication) {
        jwt("jwt") {
            val issuer = "your_issuer_here"
            verifier(JwtHandler().makeJwtVerifier(issuer))
            validate { credential ->
                if (credential.payload.audience.contains(issuer)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }


    // Initialize the database
    Database

    // Create instances of repositories
    val userRepository = UserRepository()

    // Create an instance of UserService
    val userService = UserController(userRepository)

    // Pass userService to configureAuthRouting
    configureAuthRouting(userService)
    configureMediaRouting()
    configureSocialsRouting(userService)
}

data class MySession(val token: String) : Principal

