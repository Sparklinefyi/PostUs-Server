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

    val dotenv = Dotenv.load()

    environment.config.config("database").apply {
        val dbUrl = dotenv["DB_URL"]
        val dbUser = dotenv["DB_USER"]
        val dbPassword = dotenv["DB_PASSWORD"]
        val dbDriver = dotenv["DB_DRIVER"]
        val dbMaxPoolSize = dotenv["DB_MAX_POOL_SIZE"]?.toInt()
    }

    environment.config.config("google").apply {
        val googleClientId = dotenv["GOOGLE_CLIENT_ID"]
        val googleClientSecret = dotenv["GOOGLE_CLIENT_SECRET"]
        val googleApiKey = dotenv["GOOGLE_API_KEY"]
        val googleRedirectUri = dotenv["GOOGLE_REDIRECT_URI"]
        val googleTokenUrl = dotenv["GOOGLE_TOKEN_URL"]
        val googleUserInfoUrl = dotenv["GOOGLE_USER_INFO_URL"]
    }

    environment.config.config("facebook").apply {
        val facebookClientId = dotenv["FACEBOOK_CLIENT_ID"]
        val facebookClientSecret = dotenv["FACEBOOK_CLIENT_SECRET"]
        val facebookRedirectUri = dotenv["FACEBOOK_REDIRECT_URI"]
        val facebookTokenUrl = dotenv["FACEBOOK_TOKEN_URL"]
        val facebookUserInfoUrl = dotenv["FACEBOOK_USER_INFO_URL"]
    }

    environment.config.config("instagram").apply {
        val instagramClientId = dotenv["INSTAGRAM_CLIENT_ID"]
        val instagramClientSecret = dotenv["INSTAGRAM_CLIENT_SECRET"]
        val instagramRedirectUri = dotenv["INSTAGRAM_REDIRECT_URI"]
        val instagramTokenUrl = dotenv["INSTAGRAM_TOKEN_URL"]
        val instagramUserInfoUrl = dotenv["INSTAGRAM_USER_INFO_URL"]
    }

    environment.config.config("jwt").apply {
        val jwtSecret = dotenv["JWT_SECRET"]
        val jwtExpiration = dotenv["JWT_EXPIRATION"]?.toInt()
        val jwtIssuer = dotenv["JWT_ISSUER"]
    }

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
    configureSocialsRouting(userService, dotenv)
}

data class MySession(val token: String) : Principal

