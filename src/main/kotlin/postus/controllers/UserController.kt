package postus.controllers


import com.auth0.jwt.interfaces.Claim
import org.mindrot.jbcrypt.BCrypt
import postus.models.auth.AccountInfoModel
import postus.models.auth.RegistrationRequest
import postus.models.auth.UserModel
import postus.models.auth.UserInfo
import postus.repositories.UserRepository
import postus.repositories.UserRole
import postus.utils.JwtHandler
import java.time.LocalDateTime.now


class UserController(
    private val userRepository: UserRepository,
) {

    fun fetchUserDataByToken(token: String): UserModel? {
        val userId = JwtHandler().validateTokenAndGetUserId(token) ?: return null
        return userRepository.findById(userId.toInt())
    }

    fun fetchUserDataByTokenWithPlatform(token: String): Triple<String, UserInfo, String?> {
        try {
            val verifier = JwtHandler().makeJwtVerifier(System.getProperty("JWT_ISSUER")!!)
            val decodedJWT = verifier.verify(token)

            val userClaim: Claim = decodedJWT.getClaim("userId")
            val user = if (!userClaim.isNull) {
                userRepository.findById(userClaim.asInt())?.toUserInfo() ?: UserInfo(0, "", "", UserRole.USER, now().toString(), "", "")
            } else {
                UserInfo(userClaim.asInt(), "", "", UserRole.USER, now().toString(), "", "")
            }

            // Safely access "platform" claim
            val platformClaim: Claim = decodedJWT.getClaim("platform")
            val platform = if (!platformClaim.isNull) {
                platformClaim.asString().removeSurrounding("\"")
            } else {
                ""
            }

            // Access other claims as needed
            val codeVerifierClaim: Claim = decodedJWT.getClaim("code_verifier")
            val codeVerifier = if (!codeVerifierClaim.isNull) {
               codeVerifierClaim.asString().removeSurrounding("\"")
            } else {
                ""
            }

            return Triple(platform, user, codeVerifier)
        } catch (e: Exception) {
            println("Error verifying token: ${e.message}")
            return Triple("", UserInfo(0, "", "", UserRole.USER, now().toString(), "", ""), "")
        }
    }

    fun linkAccount(userId: Int, provider: String, accountId: String?, accessToken: String?, refreshToken: String?) {
        val user = userRepository.findById(userId)
        val updatedUser = user!!.copy(
            accounts = user.accounts + AccountInfoModel(
                userId = userId,
                type = "oauth",
                provider = provider,
                accountId = accountId ?: "",
                refreshToken = refreshToken,
                accessToken = accessToken,
                expiresAt = null,
                tokenType = null,
                scope = null,
                idToken = null,
                sessionState = null
            )
        )

        if (updatedUser == user) {
            throw Exception("must update some user information")
        }

        userRepository.update(updatedUser)
    }
}
