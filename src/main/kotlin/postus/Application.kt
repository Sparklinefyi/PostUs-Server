package postus
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import postus.endpoints.*
import postus.controllers.UserController

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    val userController = UserController()
    userController.getAllUsers()

    configureAuthRouting()
    configureMediaRouting()
    configureSocialsRouting()
}
