package postus.controllers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.cdimascio.dotenv.Dotenv
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
import postus.repositories.UserInfo
import postus.utils.JwtHandler
import java.lang.IllegalArgumentException

import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

class UserController(
    private val userRepository: UserRepository,
) {
    fun registerUser(request: Registration) {
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val user = User(
            id = 0,
            email = request.email,
            name = request.name,
            role = "inactive",
            description = "",
            createdAt = now().toString(),
            updatedAt = now().toString(),
            passwordHash = hashedPassword,
            googleAccountId = null,
            googleAccessToken = null,
            googleRefresh = null,
            facebookAccountId = null,
            facebookAccessToken = null,
            facebookRefresh = null,
            twitterAccountId = null,
            twitterAccessToken = null,
            twitterRefresh = null,
            instagramAccountId = null,
            instagramAccessToken = null,
            instagramRefresh = null
        )
        userRepository.save(user)
    }

    suspend fun updateUser(userId: Int, description: String) {
        // Update the user information in the database
        userRepository.updateUserDescription(userId, description)
    }
    fun fetchUserDataByToken(token: String): UserInfo? {
        val userId = JwtHandler().validateTokenAndGetUserId(token) ?: return null
        return userRepository.findById(userId.toInt())
            ?.let { UserInfo(it.id, it.email, it.name, it.role, it.description, it.createdAt) }
    }

    fun fetchUserDataByTokenWithProvider(token: String): Pair<String, UserInfo?>? {
        val verifier = JwtHandler().makeJwtVerifier("your_issuer_here")
        val decodedJWT = verifier.verify(token)

        val user = decodedJWT.getClaim("user").asString()?.removeSurrounding("\"") ?: return null
        val userInfo = fetchUserDataByToken(user) ?: return null
        val platform = decodedJWT.getClaim("platform").asString()?.removeSurrounding("\"") ?: return null


        return Pair(platform, userRepository.findById(userInfo.id)
            ?.let { UserInfo(it.id, it.email, it.name, it.role, it.description, it.createdAt) })
    }

    fun authenticateWithEmailPassword(email: String, password: String): UserInfo? {
        val user = userRepository.findByEmail(email) ?: return null
        if (!BCrypt.checkpw(password, user.passwordHash))
            return null

        return UserInfo(user.id, user.email, user.name, user.role, user.description, user.createdAt, user.updatedAt)
    }

    fun linkAccount(userId: Int, provider: String, accountId: String?, accessToken: String?, refreshToken: String?) {
        val user = userRepository.findById(userId) ?: throw IllegalArgumentException("User not found")
        val updatedUser = user.copy(
            googleAccountId = if (provider == "GOOGLE" && accountId != null) accountId else user.googleAccountId,
            googleAccessToken = if (provider == "GOOGLE" && accessToken != null) accessToken else user.googleAccessToken,
            googleRefresh = if (provider == "GOOGLE" && refreshToken != null) refreshToken else user.googleRefresh,
            facebookAccountId = if (provider == "FACEBOOK" && accountId != null) accountId else user.facebookAccountId,
            facebookAccessToken = if (provider == "FACEBOOK" && accessToken != null) accessToken else user.facebookAccessToken,
            facebookRefresh = if (provider == "FACEBOOK" && refreshToken != null) refreshToken else user.facebookRefresh,
            twitterAccountId = if (provider == "TWITTER" && accountId != null) accountId else user.twitterAccountId,
            twitterAccessToken = if (provider == "TWITTER" && accessToken != null) accessToken else user.twitterAccessToken,
            twitterRefresh = if (provider == "TWITTER" && refreshToken != null) refreshToken else user.twitterRefresh,
            instagramAccountId = if (provider == "INSTAGRAM" && accountId != null) accountId else user.instagramAccountId,
            instagramAccessToken = if (provider == "INSTAGRAM" && accessToken != null) accessToken else user.instagramAccessToken,
            instagramRefresh = if (provider == "INSTAGRAM" && refreshToken != null) refreshToken else user.instagramRefresh
        )

        if (updatedUser == user) {
            throw Exception("must update some user information")
        }

        userRepository.update(updatedUser)
    }

    fun verifyOAuthToken(code: String?, provider: String): TokenResponse {
        val tokenMap = runBlocking {
            exchangeCodeForToken(code?: "", provider)
        } ?: throw IllegalArgumentException("Failed to exchange code for token")

        val idToken = tokenMap["id_token"]
        val accessToken = tokenMap["access_token"]
        val refreshToken = tokenMap["refresh_token"]
        val userInfoJson = runBlocking {fetchOAuthUser(accessToken!!, provider) }

        return TokenResponse(
            accessToken = accessToken!!,
            refreshToken = refreshToken!!
        )
    }
    private suspend fun exchangeCodeForToken(code: String, provider: String): Map<String, String>? {
        val params = listOf(
            "code" to code,
            "client_id" to System.getProperty("GOOGLE_CLIENT_ID"),
            "client_secret" to System.getProperty("GOOGLE_CLIENT_SECRET"),
            "redirect_uri" to System.getProperty("GOOGLE_REDIRECT_URI"),
            "grant_type" to "authorization_code"
        )

        val client = HttpClient()
        val tokenResponse = client.post(Url(System.getProperty("GOOGLE_TOKEN_URL")!!)) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            }
            setBody(FormDataContent(Parameters.build {
                params.forEach { (key, value) ->
                    append(key, value!!)
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

    private suspend fun fetchOAuthUser(accessToken: String, provider: String): JsonObject? {
        val dotenv = Dotenv.configure().ignoreIfMissing().load()
        val userInfoUrl = dotenv["GOOGLE_USER_INFO_URL"]

        val client = HttpClient()
        val userInfoResponse = client.get(Url(userInfoUrl!!)) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        val userInfoResponseJson = userInfoResponse.readBytes().decodeToString()
        return JsonParser.parseString(userInfoResponseJson).asJsonObject
    }

    fun userInfo(user: User): UserInfo {
        return UserInfo(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            createdAt = user.createdAt,
            description = user.description,
        )
    }
}
