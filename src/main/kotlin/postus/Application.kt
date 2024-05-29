package postus
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import postus.endpoints.*
import postus.repositories.*
import postus.controllers.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

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
