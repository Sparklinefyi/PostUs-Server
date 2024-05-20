package postus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import postus.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRouting()
}
