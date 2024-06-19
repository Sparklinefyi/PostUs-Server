package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.UserController
import io.ktor.server.request.*
import postus.models.auth.*
import postus.utils.JwtHandler

fun Application.configureAuthRouting(userService: UserController) {
    routing {
        route("/auth") {
            post("/register") {
                val request = call.receive<RegistrationRequest>()
                val user = userService.registerUser(request)

                call.respond(HttpStatusCode.Created, user)
            }

            post("/signin") {
                // parse request for json data
                val request = call.receive<LoginRequest>()
                val user = userService.authenticateWithEmailPassword(request.email, request.password)
                if (user != null) {
                    val token = JwtHandler().makeToken(user.id.toInt())
                    val response = UserInfo(
                        user.id, user.email, user.name, user.role, user.createdAt, user.description, token,
                        user.googleAccountId, user.facebookAccountId, user.twitterAccountId, user.instagramAccountId, user.linkedInAccountId
                    );
                    call.respond(HttpStatusCode.OK, response)
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
                userService.updateUser(userInfo.id, request.description)
                call.respond(HttpStatusCode.OK, userInfo)
            }
        }

    }
}
