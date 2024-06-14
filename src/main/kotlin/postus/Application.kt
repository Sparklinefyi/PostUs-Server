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
import okhttp3.OkHttpClient

fun main() {
    // Initialize the database
    Database

    val client = OkHttpClient()
    val userRepository = UserRepository()
    val mediaController = MediaController()
    val userService = UserController(userRepository)
    val socialController = SocialsController(client, userRepository, userService, mediaController)

    startScheduledPostsChecker(socialController)


    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port, host = if (port == 8080) "localhost" else "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val isProduction = System.getenv("ENVIRONMENT") == "prod"
    val dotenv = if (!isProduction) Dotenv.load() else null

    fun setEnvVariable(key: String, value: String?) {
        if (value != null) {
            System.setProperty(key, value)
        }
    }

    // Set environment variables
    val keys = listOf(
        "DB_URL", "DB_USER", "DB_PASSWORD", "DB_DRIVER", "DB_MAX_POOL_SIZE",
        "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET", "GOOGLE_API_KEY", "GOOGLE_REDIRECT_URI",
        "GOOGLE_TOKEN_URL", "GOOGLE_USER_INFO_URL", "FACEBOOK_CLIENT_ID", "FACEBOOK_CLIENT_SECRET",
        "FACEBOOK_REDIRECT_URI", "FACEBOOK_TOKEN_URL", "FACEBOOK_USER_INFO_URL", "INSTAGRAM_CLIENT_ID",
        "INSTAGRAM_CLIENT_SECRET", "INSTAGRAM_REDIRECT_URI", "INSTAGRAM_TOKEN_URL", "INSTAGRAM_USER_INFO_URL",
        "JWT_SECRET", "JWT_EXPIRATION", "JWT_ISSUER"
    )

    keys.forEach { key ->
        setEnvVariable(key, dotenv?.get(key) ?: System.getenv(key))
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
            cookie.path = "/"
            cookie.maxAgeInSeconds = 1 * 24 * 60 * 60
            cookie.httpOnly = true
            cookie.secure = isProduction
            cookie.extensions["SameSite"] = "None"
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
            verifier(JwtHandler().makeJwtVerifier(System.getProperty("JWT_ISSUER")))
            validate { credential ->
                if (credential.payload.audience.contains(System.getProperty("JWT_ISSUER"))) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Initialize the database
    Database

    val client = OkHttpClient()
    val userRepository = UserRepository()
    val mediaController = MediaController()
    val userService = UserController(userRepository)
    val socialController = SocialsController(client, userRepository, userService, mediaController)

    // Pass userService to configureAuthRouting
    configureAuthRouting(userService)
    configureMediaRouting(userService, mediaController)
    configureSocialsRouting(userService, socialController)
}

data class MySession(val token: String) : Principal
