package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.UserController
import io.ktor.server.request.*
import postus.models.HashedPasswordRequest
import postus.models.auth.*
import postus.utils.JwtHandler

fun Application.configureAuthRouting(userService: UserController) {
    routing {
        route("/auth") {
            post("/signin") {
                val request = call.receive<LoginRequest>()
                val user = userService.authenticateWithEmailPassword(request.email, request.password)
                if (user != null) {
                    if (user.emailVerified != null) {
                        val response = user.toUserInfo()
                        call.respond(HttpStatusCode.OK, response)
                    }
                    else {
                        call.respond(HttpStatusCode.Accepted, "Verify Email")
                    }
                } else
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }

            post("/signout") {
                call.respond(HttpStatusCode.OK, "Signed out")
            }
        }
        route("/user") {

            post("/info") {
                val request = call.receive<TokenRequest>()
                val userInfo = userService.fetchUserDataByToken(request.token)
                    ?: throw IllegalArgumentException("Invalid token")

                call.respond(HttpStatusCode.OK, userInfo)
            }

            post("/update") {
                val request = call.receive<UpdateUserRequest>()
                val userInfo = userService.fetchUserDataByToken(request.token)
                    ?: throw IllegalArgumentException("Invalid token")

                // Update the user information
                userService.updateUser(userInfo.id, request.description, request.currentPassword, request.newPassword)
                call.respond(HttpStatusCode.OK, userInfo)
            }
        }

    }
}
