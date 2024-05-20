package postus.endpoints

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAuthRouting() {
    routing {
        route("auth"){
            get("/") {
                call.respondText("Hello World!")
            }
        }
    }
}
