package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.UserController
import io.ktor.server.request.*
import postus.dto.*
import postus.repositories.UserInfo
import postus.utils.JwtHandler

fun Application.configureAuthRouting(userService: UserController) {
    routing {
        route("/auth") {
            post("/register") {
                // Convert the request payload to RegistrationRequest
                val request = call.receive<Registration>()
                val user = userService.registerUser(request)

                call.respond(HttpStatusCode.Created, user)
            }

            post("/signin") {
                // parse request for json data
                val request = call.receive<Login>()
                val user = userService.authenticateWithEmailPassword(request.email, request.password)
                if (user != null) {
                    val token = JwtHandler().makeToken(user.id.toString())
                    val userInfo = UserInfo(user.id, user.email, user.name, user.role, user.description);
                    call.respond(HttpStatusCode.OK, LoginResponse(userInfo.id, userInfo.email, userInfo.name, userInfo.role, userInfo.description, token))
                }
                else
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }

            post("/signout") {
                call.respond(HttpStatusCode.OK, "Signed out")
            }

            post("/userinfo") {
                val request = call.receive<UserInfoRequest>()
                val userInfo = userService.fetchUserDataByToken(request.token)
                    ?: throw IllegalArgumentException("Invalid token")

                call.respond(HttpStatusCode.OK, LoginResponse(userInfo.id, userInfo.email, userInfo.name, userInfo.role, userInfo.description, request.token))
            }

            post("/update-user") {
                val request = call.receive<UpdateUserRequest>()
                val userInfo = userService.fetchUserDataByToken(request.token)
                    ?: throw IllegalArgumentException("Invalid token")

                // Update the user information
                userService.updateUser(userInfo.id, request.description)
                call.respond(HttpStatusCode.OK, LoginResponse(userInfo.id, userInfo.email, userInfo.name, userInfo.role, userInfo.description, request.token))
            }

            post("/link-account") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                    ?: throw IllegalArgumentException("Missing or invalid Authorization header")

                // Validate the token and retrieve the user ID
                //val userId = JwtHandler().validateTokenAndGetUserId(token)
                //    ?: throw IllegalArgumentException("Invalid token")

                // Extract code and provider from the request body
                val request = call.receive<GoogleResponse>()

                val tokenInfo = userService.verifyOAuthToken(request.code, request.provider)

                // Link the account
                userService.linkAccount(16, request.provider, null, null, tokenInfo.refreshToken)
                call.respond(HttpStatusCode.OK, "Account linked")
            }

            post("/signin") {
                // parse request for json data
                val request = call.receive<Login>()
                val user = userService.authenticateWithEmailPassword(request.email, request.password)
                if (user != null) {
                    val token = JwtHandler().makeToken(user.id.toString())
                    val userInfo = UserInfo(user.id, user.email, user.name, user.role, user.description);
                    call.respond(HttpStatusCode.OK, LoginResponse(userInfo.id, userInfo.email, userInfo.name, userInfo.role, userInfo.description, token))
                }
                 else
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}
