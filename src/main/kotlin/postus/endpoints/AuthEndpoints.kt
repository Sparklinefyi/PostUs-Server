package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.UserController
import io.ktor.server.request.*
import postus.dto.*
import postus.utils.JwtHandler

fun Application.configureAuthRouting(userService: UserController) {
    routing {
        route("/auth") {
            post("/register") {
                // Convert the request payload to RegistrationRequest
                val request = call.receive<RegistrationRequest>()

                val user = userService.registerUser(request)

                call.respond(HttpStatusCode.Created, user)
            }

            post("/link-account") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                    ?: throw IllegalArgumentException("Missing or invalid Authorization header")

                // Validate the token and retrieve the user ID
                val userId = JwtHandler().validateTokenAndGetUserId(token)
                    ?: throw IllegalArgumentException("Invalid token")

                // Extract code and provider from the request body
                val request = call.receive<String>()
                val matchResult = Regex("""\"code\":\"(.*?)\",\"provider\":\"(.*?)\"""").find(request)
                val code = matchResult?.groupValues?.get(1) ?: throw IllegalArgumentException("Invalid request format")
                val provider = matchResult?.groupValues?.get(2) ?: throw IllegalArgumentException("Invalid request format")
                val userInfo = userService.verifyOAuthToken(code, provider)

                // Link the account
                userService.linkAccount(userId.toInt(), userInfo.provider, userInfo.refreshToken!!)
                call.respond(HttpStatusCode.OK, "Account linked")
            }

            post("/signin") {
                // parse request for json data
                val request = call.receive<SignInRequest>()
                val user = userService.authenticateWithEmailPassword(request.email, request.password, call)

                println(user)
                if (user != null) {

                    val token = JwtHandler().makeToken(user.id.toString())
                    call.respond(HttpStatusCode.OK, mapOf("token" to token))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            }
        }
    }
}
