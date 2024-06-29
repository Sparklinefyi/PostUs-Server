package postus.endpoints

import TokenService.generateVerificationToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.UserController
import io.ktor.server.request.*
import postus.controllers.EmailController
import postus.models.auth.*

fun Application.configureAuthRouting(userService: UserController) {
    routing {
        route("/auth") {
            post("/register") {
                val request = call.receive<RegistrationRequest>()
                val user = userService.registerUser(request)
                val verificationToken = generateVerificationToken(user.email!!)
                EmailController().sendVerificationEmail("re_9wxtBML6_7JmF2EHdaZYn82B9RntH7Yb8", user.email, verificationToken.token)
                call.respond(HttpStatusCode.OK, "User registered successfully, verification email sent)")
            }

            post("/signin") {
                val request = call.receive<LoginRequest>()
                val user = userService.authenticateWithEmailPassword(request.email, request.password)
                if (user != null) {
                    if (user.emailVerified != null) {
                        val response = user.toUserInfo()
                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        val verificationToken = generateVerificationToken(user.email!!)
                        EmailController().sendVerificationEmail("re_9wxtBML6_7JmF2EHdaZYn82B9RntH7Yb8", user.email, verificationToken.token)
                        call.respond(HttpStatusCode.OK, "Email not verified, resending confirmation email")
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            }
            post("/test-email") {

                val request = call.receive<LoginRequest>()
                val verificationToken = generateVerificationToken("iblooze2@gmail.com")
                EmailController().sendVerificationEmail("re_9wxtBML6_7JmF2EHdaZYn82B9RntH7Yb8", "iblooze2@gmail.com", verificationToken.token)
                call.respond(HttpStatusCode.OK, "Email not verified, resending confirmation email")
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
