package postus.controllers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.request.headers
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import postus.dto.*
import postus.repositories.*
import org.mindrot.jbcrypt.BCrypt
import java.lang.IllegalArgumentException

class UserController(
    private val userRepository: UserRepository,
) {

    fun registerUser(request: RegistrationRequest): User {
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val user = User(
            id = 0,
            email = request.email,
            name = request.name,
            passwordHash = hashedPassword,
            googleRefresh = "",
            facebookRefresh = "",
            twitterRefresh = "",
            instagramRefresh = ""
        )
        return userRepository.save(user)
    }

    fun linkAccount(userId: Int, provider: String, refreshToken: String) {
        val user = userRepository.findById(userId) ?: throw IllegalArgumentException("User not found")

        val updatedUser = when (provider) {
            "google" -> user.copy(googleRefresh = refreshToken)
            "facebook" -> user.copy(facebookRefresh = refreshToken)
            "twitter" -> user.copy(twitterRefresh = refreshToken)
            "instagram" -> user.copy(instagramRefresh = refreshToken)
            else -> throw IllegalArgumentException("Unsupported provider")
        }

        userRepository.update(updatedUser)
    }

    fun authenticateWithEmailPassword(email: String, password: String, call: ApplicationCall): User? {
        val user = userRepository.findByEmail(email)
        println(user)
        println("Password: $password")
        println("Stored Hash: ${user!!.passwordHash}")
        val passwordMatches = BCrypt.checkpw(password, user.passwordHash)
        println("Password Matches: $passwordMatches")

        if (passwordMatches) {
            println("User: $user")
            return user
        }
        return null
    }

    fun authenticateWithOAuth(request: SignInRequest): User? {
    val newUser = User(
                id = -20,
                email = "",
                name = "",
                passwordHash = "",
                googleRefresh = "",
                facebookRefresh = "",
                twitterRefresh = "",
                instagramRefresh = "",
            )
            return newUser
    }

    fun verifyOAuthToken(code: String?, provider: String): UserInfo {
        val tokenMap = runBlocking {
            exchangeCodeForToken(code?: "", provider)
        } ?: throw IllegalArgumentException("Failed to exchange code for token")

        val accessToken = tokenMap["access_token"]
            ?: throw IllegalArgumentException("Access token not found in token response")

        val idToken = tokenMap["id_token"]
            ?: throw IllegalArgumentException("ID token not found in token response")

        val refreshToken = tokenMap["refresh_token"]

        val userInfoJson = runBlocking {
            fetchOAuthUser(accessToken, provider)
        }
            ?: throw IllegalArgumentException("Failed to fetch user info")

        return UserInfo(
            id = -10,
            provider = provider,
            providerUserId = idToken,
            email = userInfoJson["email"].asString,
            name = userInfoJson["name"].asString,
            accessToken = accessToken,
            refreshToken = refreshToken ?: ""
        )
    }

    private suspend fun fetchOAuthUser(accessToken: String, provider: String): JsonObject? {
        val config = ConfigFactory.load().getConfig(provider)
        val userInfoUrl = config.getString("userInfoUrl")

        val client = HttpClient()
        val userInfoResponse = client.get(Url(userInfoUrl)) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        val userInfoResponseJson = userInfoResponse.readBytes().decodeToString()
        return JsonParser.parseString(userInfoResponseJson).asJsonObject
    }

    private suspend fun exchangeCodeForToken(code: String, provider: String): Map<String, String>? {
        val config = ConfigFactory.load().getConfig(provider)
        val clientID = config.getString("clientID")
        val clientSecret = config.getString("clientSecret")
        val redirectUri = config.getString("redirectUri")

        val params = listOf(
            "code" to code,
            "client_id" to clientID,
            "client_secret" to clientSecret,
            "redirect_uri" to redirectUri,
            "grant_type" to "authorization_code"
        )

        val client = HttpClient()
        val tokenResponse = client.post(Url(getTokenUrl(provider))) {
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

    private fun getTokenUrl(provider: String): String {
        val config = ConfigFactory.load().getConfig(provider)
        return config.getString("tokenUrl")
    }
}

class RegistrationException(message: String) : RuntimeException(message)
