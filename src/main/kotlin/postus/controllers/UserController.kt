package postus.controllers


import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import postus.repositories.*
import org.mindrot.jbcrypt.BCrypt
import postus.models.auth.Registration
import postus.models.auth.TokenResponse
import postus.models.auth.UserInfo
import postus.models.auth.User
import postus.utils.JwtHandler
import java.lang.IllegalArgumentException
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
            instagramRefresh = null,
            linkedinAccountId = null,
            linkedinAccessToken = null,
            linkedinRefresh = null,
            tiktokAccountId = null,
            tiktokAccessToken = null,
            tiktokRefresh = null
        )
        userRepository.save(user)
    }

    suspend fun updateUser(userId: Int, description: String) {
        userRepository.updateUserDescription(userId, description)
    }

    fun fetchUserDataByToken(token: String): UserInfo? {
        val userId = JwtHandler().validateTokenAndGetUserId(token) ?: return null
        return userRepository.findById(userId.toInt())
            ?.let { userRepository.toUserInfo(it) }
    }

    fun fetchUserDataByTokenWithPlatform(token: String): Pair<String, UserInfo?>? {
        try {
            val verifier = JwtHandler().makeJwtVerifier(System.getProperty("JWT_ISSUER")!!)
            val decodedJWT = verifier.verify(token)

            // Strip surrounding quotes
            var user = decodedJWT.getClaim("user").asString()!!.removeSurrounding("\"")
            var platform = decodedJWT.getClaim("platform").asString()!!.removeSurrounding("\"")

            val userInfo = fetchUserDataByToken(user) ?: return null

            return if (userRepository.findById(userInfo.id) != null) {
                Pair(platform, userInfo)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error verifying token: ${e.message}")
            return null
        }
    }

    fun authenticateWithEmailPassword(email: String, password: String): UserInfo? {
        val user = userRepository.findByEmail(email) ?: return null
        if (!BCrypt.checkpw(password, user.passwordHash)) return null

        return userRepository.toUserInfo(user)
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
            instagramRefresh = if (provider == "INSTAGRAM" && refreshToken != null) refreshToken else user.instagramRefresh,
            linkedinAccountId = if (provider == "LINKEDIN" && accountId != null) accountId else user.linkedinAccountId,
            linkedinAccessToken = if (provider == "LINKEDIN" && accessToken != null) accessToken else user.linkedinAccessToken,
            linkedinRefresh = if (provider == "LINKEDIN" && refreshToken != null) refreshToken else user.linkedinRefresh,
            tiktokAccountId = if (provider == "TIKTOK" && accountId != null) accountId else user.tiktokAccountId,
            tiktokAccessToken = if (provider == "TIKTOK" && accessToken != null) accessToken else user.tiktokAccessToken,
            tiktokRefresh = if (provider == "TIKTOK" && refreshToken != null) refreshToken else user.tiktokRefresh
        )

        if (updatedUser == user) {
            throw Exception("must update some user information")
        }

        userRepository.update(updatedUser)
    }

}
