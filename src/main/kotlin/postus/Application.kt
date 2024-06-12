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

fun main() {
    Database // Ensure the database is initialized
    startScheduledPostsChecker()
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

    // Setting environment variables
    setEnvVariable("DB_URL", dotenv?.get("DB_URL") ?: System.getenv("DB_URL"))
    setEnvVariable("DB_USER", dotenv?.get("DB_USER") ?: System.getenv("DB_USER"))
    setEnvVariable("DB_PASSWORD", dotenv?.get("DB_PASSWORD") ?: System.getenv("DB_PASSWORD"))
    setEnvVariable("DB_DRIVER", dotenv?.get("DB_DRIVER") ?: System.getenv("DB_DRIVER"))
    setEnvVariable("DB_MAX_POOL_SIZE", dotenv?.get("DB_MAX_POOL_SIZE") ?: System.getenv("DB_MAX_POOL_SIZE"))

    setEnvVariable("GOOGLE_CLIENT_ID", dotenv?.get("GOOGLE_CLIENT_ID") ?: System.getenv("GOOGLE_CLIENT_ID"))
    setEnvVariable("GOOGLE_CLIENT_SECRET", dotenv?.get("GOOGLE_CLIENT_SECRET") ?: System.getenv("GOOGLE_CLIENT_SECRET"))
    setEnvVariable("GOOGLE_API_KEY", dotenv?.get("GOOGLE_API_KEY") ?: System.getenv("GOOGLE_API_KEY"))
    setEnvVariable("GOOGLE_REDIRECT_URI", dotenv?.get("GOOGLE_REDIRECT_URI") ?: System.getenv("GOOGLE_REDIRECT_URI"))
    setEnvVariable("GOOGLE_TOKEN_URL", dotenv?.get("GOOGLE_TOKEN_URL") ?: System.getenv("GOOGLE_TOKEN_URL"))
    setEnvVariable("GOOGLE_USER_INFO_URL", dotenv?.get("GOOGLE_USER_INFO_URL") ?: System.getenv("GOOGLE_USER_INFO_URL"))

    setEnvVariable("FACEBOOK_CLIENT_ID", dotenv?.get("FACEBOOK_CLIENT_ID") ?: System.getenv("FACEBOOK_CLIENT_ID"))
    setEnvVariable("FACEBOOK_CLIENT_SECRET", dotenv?.get("FACEBOOK_CLIENT_SECRET") ?: System.getenv("FACEBOOK_CLIENT_SECRET"))
    setEnvVariable("FACEBOOK_REDIRECT_URI", dotenv?.get("FACEBOOK_REDIRECT_URI") ?: System.getenv("FACEBOOK_REDIRECT_URI"))
    setEnvVariable("FACEBOOK_TOKEN_URL", dotenv?.get("FACEBOOK_TOKEN_URL") ?: System.getenv("FACEBOOK_TOKEN_URL"))
    setEnvVariable("FACEBOOK_USER_INFO_URL", dotenv?.get("FACEBOOK_USER_INFO_URL") ?: System.getenv("FACEBOOK_USER_INFO_URL"))

    setEnvVariable("INSTAGRAM_CLIENT_ID", dotenv?.get("INSTAGRAM_CLIENT_ID") ?: System.getenv("INSTAGRAM_CLIENT_ID"))
    setEnvVariable("INSTAGRAM_CLIENT_SECRET", dotenv?.get("INSTAGRAM_CLIENT_SECRET") ?: System.getenv("INSTAGRAM_CLIENT_SECRET"))
    setEnvVariable("INSTAGRAM_REDIRECT_URI", dotenv?.get("INSTAGRAM_REDIRECT_URI") ?: System.getenv("INSTAGRAM_REDIRECT_URI"))
    setEnvVariable("INSTAGRAM_TOKEN_URL", dotenv?.get("INSTAGRAM_TOKEN_URL") ?: System.getenv("INSTAGRAM_TOKEN_URL"))
    setEnvVariable("INSTAGRAM_USER_INFO_URL", dotenv?.get("INSTAGRAM_USER_INFO_URL") ?: System.getenv("INSTAGRAM_USER_INFO_URL"))

    setEnvVariable("JWT_SECRET", dotenv?.get("JWT_SECRET") ?: System.getenv("JWT_SECRET"))
    setEnvVariable("JWT_EXPIRATION", dotenv?.get("JWT_EXPIRATION") ?: System.getenv("JWT_EXPIRATION"))
    setEnvVariable("JWT_ISSUER", dotenv?.get("JWT_ISSUER") ?: System.getenv("JWT_ISSUER"))

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

    // Create an instance of UserService
    val userService = UserController(UserRepository())

    // Pass userService to configureAuthRouting
    configureAuthRouting(userService)
    configureMediaRouting(userService)
    configureSocialsRouting(userService)
}

data class MySession(val token: String) : Principal
