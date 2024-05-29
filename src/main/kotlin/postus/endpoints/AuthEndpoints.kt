package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.UserController
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.request.forms.FormDataContent
import io.ktor.util.*
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json
import java.util.*
import java.sql.Date
import postus.utils.EncryptionUtil


import postus.dto.*

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

                val request = call.receive<String>()
                val matchResult = Regex("""\"code\":\"(.*?)\",\"provider\":\"(.*?)\"""").find(request)
                val code = matchResult?.groupValues?.get(1) ?: throw IllegalArgumentException("Invalid request format")
                val provider = matchResult?.groupValues?.get(2) ?: throw IllegalArgumentException("Invalid request format")
                val userInfo = userService.verifyOAuthToken(code, provider)

                //val session = call.sessions.get<postus.MySession>()
                //val userId = session?.userId ?: throw IllegalArgumentException("User not authenticated")
                userService.linkAccount(13, userInfo.provider, userInfo.refreshToken!!)
                call.respond(HttpStatusCode.OK, "Account linked")
            }

            post("/signin") {
                // parse request for json data

                val request = call.receive<SignInRequest>()
                val user = if (1 < 0) {
                    userService.authenticateWithOAuth(request)
                } else {
                    userService.authenticateWithEmailPassword(request.email, request.password, call)
                }
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            }
        }
    }
}

suspend fun fetchGoogleUser(accessToken: String): JsonObject? {
    val userInfoUrl = "https://openidconnect.googleapis.com/v1/userinfo"
    val client = HttpClient()
    val userInfoResponse = client.get(Url(userInfoUrl)) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }

    val userInfoResponseJson = userInfoResponse.readBytes().decodeToString()
    return JsonParser.parseString(userInfoResponseJson).asJsonObject
}

@OptIn(InternalAPI::class)
suspend fun exchangeCodeForToken(code: String): Map<String, String>? {
    val config = ConfigFactory.load().getConfig("google")
    val clientID = config.getString("clientID")
    val clientSecret = config.getString("clientSecret")
    val redirectUri = config.getString("redirectUri")

    val tokenUrl = "https://oauth2.googleapis.com/token"
    val params = listOf(
        "code" to code,
        "client_id" to clientID,
        "client_secret" to clientSecret,
        "redirect_uri" to redirectUri,
        "grant_type" to "authorization_code"
    )

    val client = HttpClient()

    val tokenResponse = client.post(Url(tokenUrl)) {
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        }
        setBody(FormDataContent(Parameters.build {
            params.forEach { (key, value) ->
                append(key, value)
            }
        }))
    }

    val tokenResponseJson = tokenResponse.readBytes().decodeToString()
    val tokenJsonObject = JsonParser.parseString(tokenResponseJson).asJsonObject

    if (!tokenJsonObject.has("access_token") || !tokenJsonObject.has("id_token")) {
        println("Token response did not contain access_token or id_token: $tokenResponseJson")
        return null
    }

    return tokenJsonObject.entrySet().associate { it.key to it.value.asString }
}
