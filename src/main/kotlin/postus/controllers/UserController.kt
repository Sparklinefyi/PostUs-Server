package postus.controllers


import com.auth0.jwt.interfaces.Claim
import org.mindrot.jbcrypt.BCrypt
import postus.models.auth.RegistrationRequest
import postus.models.auth.User
import postus.models.auth.UserInfo
import postus.repositories.UserRepository
import postus.utils.JwtHandler
import java.time.LocalDateTime.now


class UserController(
    private val userRepository: UserRepository,
) {
    fun registerUser(request: RegistrationRequest) {
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

    suspend fun updateUser(userId: Int, description: String?, currentPassword: String?, newPassword: String?) {
        val user = userRepository.findById(userId) ?: return
        val encryptedPassword = if (BCrypt.checkpw(currentPassword, user.passwordHash)) {
            BCrypt.hashpw(newPassword, BCrypt.gensalt())
        } else
            null

        userRepository.updateUser(userId, description!!, encryptedPassword!!)
    }

    fun fetchUserDataByToken(token: String): UserInfo? {
        val userId = JwtHandler().validateTokenAndGetUserId(token) ?: return null
        return userRepository.findById(userId.toInt())
            ?.let { userRepository.toUserInfo(it) }
    }

    fun fetchUserDataByTokenWithPlatform(token: String): Triple<String, UserInfo, String?> {
        try {
            val verifier = JwtHandler().makeJwtVerifier(System.getProperty("JWT_ISSUER")!!)
            val decodedJWT = verifier.verify(token)

            val userClaim: Claim = decodedJWT.getClaim("user")
            val user = if (!userClaim.isNull) {
                userRepository.toUserInfo(userRepository.findById(userClaim.asInt()))
            } else {
                return Triple("", UserInfo(0, "", "", "", "", ""), "")
            }

            // Safely access "platform" claim
            val platformClaim: Claim = decodedJWT.getClaim("platform")
            val platform = if (!platformClaim.isNull) {
                platformClaim.asString().removeSurrounding("\"")
            } else {
                return Triple("", user, "")
            }

            // Access other claims as needed
            val codeVerifierClaim: Claim = decodedJWT.getClaim("code_verifier")
            val codeVerifier = if (!codeVerifierClaim.isNull) {
               codeVerifierClaim.asString().removeSurrounding("\"")
            } else {
                return Triple(platform, user, "")
            }

            return Triple(platform, user, codeVerifier)
        } catch (e: Exception) {
            println("Error verifying token: ${e.message}")
            return Triple("", UserInfo(0, "", "", "", "", ""), "")
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
