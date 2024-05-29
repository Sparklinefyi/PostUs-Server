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
import kotlinx.coroutines.runBlocking
import postus.dto.*
import postus.repositories.*
import org.mindrot.jbcrypt.BCrypt
import java.lang.IllegalArgumentException

class UserController(
    private val userRepository: UserRepository,
    private val linkedAccountRepository: LinkedAccountRepository
) {

    fun registerUser(request: RegistrationRequest): User {
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val user = User(0, email = request.email, name = request.name, passwordHash = hashedPassword)
        return userRepository.save(user)
    }

    fun linkAccount(userId: Int, userInfo: UserInfo) {
        if (linkedAccountRepository.existsByProviderAndProviderUserId(userInfo.provider, userInfo.providerUserId)) {
            throw IllegalArgumentException("Account already linked")
        }

        val linkedAccount = LinkedAccount(
            id = 0,
            userId = userId,
            provider = userInfo.provider,
            providerUserId = userInfo.providerUserId,
            accessToken = userInfo.accessToken,
            refreshToken = userInfo.refreshToken
        )

        linkedAccountRepository.save(linkedAccount)
    }

    fun authenticateWithEmailPassword(email: String, password: String): User? {
        val user = userRepository.findByEmail(email)
        return if (user != null && BCrypt.checkpw(password, user.passwordHash)) {
            user
        } else {
            null
        }
    }

    fun authenticateWithOAuth(request: SignInRequest): User? {
        val userInfo = verifyOAuthToken(request.oauthToken!!, request.provider!!)
        val linkedAccount = linkedAccountRepository.findByProviderAndProviderUserId(userInfo.provider, userInfo.providerUserId)
        return linkedAccount?.let { userRepository.findById(it.userId) }
    }

    fun verifyOAuthToken(oauthToken: OAuthTokenRequest, provider: String): UserInfo {
        val tokenMap = runBlocking {
            // switch provider
            if (provider == "google") exchangeCodeForToken(oauthToken.accessToken, provider)
            else if (provider == "facebook") exchangeCodeForToken(oauthToken.accessToken, provider)
            else if (provider == "instagram") exchangeCodeForToken(oauthToken.accessToken, provider)
            else if (provider == "twitter") exchangeCodeForToken(oauthToken.accessToken, provider)
            else if (provider == "linkedin") exchangeCodeForToken(oauthToken.accessToken, provider)
            else throw IllegalArgumentException("Invalid provider")
        }
            ?: throw IllegalArgumentException("Failed to exchange code for token")

        val accessToken = tokenMap["access_token"]
            ?: throw IllegalArgumentException("Access token not found in token response")

        val idToken = tokenMap["id_token"]
            ?: throw IllegalArgumentException("ID token not found in token response")

        val userInfoJson = runBlocking { fetchOAuthUser(accessToken, provider) }
            ?: throw IllegalArgumentException("Failed to fetch user info")

        return UserInfo(
            provider = provider,
            providerUserId = idToken, // Assuming the ID token is used as the providerUserId
            email = userInfoJson["email"].asString,
            name = userInfoJson["name"].asString,
            accessToken = accessToken,
            refreshToken = oauthToken.refreshToken
        )
    }

    suspend fun fetchOAuthUser(accessToken: String, provider: String): JsonObject? {

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

    suspend fun exchangeCodeForToken(code: String, provider: String): Map<String, String>? {
        val config = ConfigFactory.load().getConfig(provider)
        val clientID = config.getString("clientID")
        val clientSecret = config.getString("clientSecret")
        val redirectUri = config.getString("redirectUri")
        val tokenUrl = config.getString("tokenUrl")
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

    fun getTokenUrl(provider: String, code: String): String {
        val config = ConfigFactory.load().getConfig(provider)
        val clientID = config.getString("clientID")
        val redirectUri = config.getString("redirectUri")
        val tokenUrl = config.getString("tokenUrl")

        return "$tokenUrl?client_id=$clientID&redirect_uri=$redirectUri&code=$code"
    }
}