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
import java.util.*
import java.sql.Date
import postus.utils.EncryptionUtil

fun Application.configureAuthRouting() {
    routing {
        route("auth") {
            get("/") {
                call.respondText("Hello World!")
            }

            get("/google/callback") {
                val code = call.request.queryParameters["code"]
                if (code == null) {
                    call.respondText("Authorization code is missing", status = HttpStatusCode.BadRequest)
                } else {
                    val tokens = exchangeCodeForToken(code)
                    if (tokens == null) {
                        call.respondText("Failed to exchange code for tokens", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val accessToken = tokens["access_token"] ?: return@get
                    val refreshToken = tokens["refresh_token"] ?: return@get

                    val userInfo = fetchGoogleUser(accessToken)
                    if (userInfo == null) {
                        call.respondText("Failed to fetch user information", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val id = userInfo["sub"].asString
                    val email = userInfo["email"].asString
                    val first = userInfo["given_name"].asString
                    val last = userInfo["family_name"].asString
                    val picture = userInfo["picture"].asString
                    val subExpire = Date(Date().time - 1 * 24 * 60 * 60 * 1000) // expired yesterday

                    // Encrypt refresh token before setting it in cookie
                    val encryptedRefreshToken = EncryptionUtil.encrypt(refreshToken)

                    val userController = UserController()
                    userController.addUser(id, email, first, last, picture, encryptedRefreshToken, subExpire)

                    // Set secure cookie with encrypted refresh token
                    call.response.cookies.append(
                        Cookie(
                            name = "refresh_token",
                            value = encryptedRefreshToken,
                            httpOnly = true,
                            secure = true,
                            path = "/",
                            maxAge = 60 * 60 * 24 * 30 // 30 days
                        )
                    )

                    val state = call.request.queryParameters["state"]
                    val frontendOrigin = call.request.queryParameters["frontend_origin"] ?: "http://localhost:3000"
                    call.respondRedirect("$frontendOrigin$state")
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
