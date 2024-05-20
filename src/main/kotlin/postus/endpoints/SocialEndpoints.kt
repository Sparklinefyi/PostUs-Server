package postus.endpoints

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSocialRouting() {
    routing {
        route("/socials") {
            get("/") {
                call.respondText("Socials")
            }
        }
    }
}